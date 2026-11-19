package com.example.ui.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.core.common.CurrencyDisplayUnit
import com.example.core.common.CurrencyFormatter
import com.example.core.common.IranianBankInfo
import com.example.core.common.IranianBankRegistry
import com.example.core.common.IranianValidators
import com.example.domain.model.BankAccount
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransferRail
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.repository.AuthRepository
import com.example.domain.usecase.ExecuteTransferUseCase
import com.example.domain.usecase.GetBankAccountsUseCase
import com.example.domain.usecase.InquireIbanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransfersUiState(
    val selectedRail: TransferRail = TransferRail.PAYA_ACH,
    val availableAccounts: List<BankAccount> = emptyList(),
    val sourceInput: String = "",
    val destinationInput: String = "",
    val amountInput: String = "",
    val currencyUnit: CurrencyDisplayUnit = CurrencyDisplayUnit.TOMAN,
    val descriptionInput: String = "",
    val detectedBank: IranianBankInfo? = null,
    val isDestinationChecksumValid: Boolean = false,
    val isInquiringIban: Boolean = false,
    val inquiryResult: IbanInquiryResult? = null,
    val isSubmitting: Boolean = false,
    val receipt: TransferReceipt? = null,
    val error: BankError? = null,
    val activeProvider: OpenBankingProvider = OpenBankingProvider.FREE_SANDBOX,
    val trackId: String = UUID.randomUUID().toString()
)

@HiltViewModel
class TransfersViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getBankAccountsUseCase: GetBankAccountsUseCase,
    private val inquireIbanUseCase: InquireIbanUseCase,
    private val executeTransferUseCase: ExecuteTransferUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransfersUiState(activeProvider = authRepository.getSelectedProvider())
    )
    val uiState: StateFlow<TransfersUiState> = _uiState.asStateFlow()

    init {
        refreshAccountsAndProvider()
    }

    fun refreshAccountsAndProvider() {
        _uiState.update { it.copy(activeProvider = authRepository.getSelectedProvider()) }
        viewModelScope.launch {
            val result = getBankAccountsUseCase()
            if (result is AppResult.Success) {
                val accounts = result.data
                _uiState.update { current ->
                    val defaultSource = if (current.sourceInput.isBlank()) {
                        accounts.firstOrNull()?.depositNumber.orEmpty()
                    } else {
                        current.sourceInput
                    }
                    current.copy(
                        availableAccounts = accounts,
                        sourceInput = defaultSource
                    )
                }
            }
        }
    }

    fun selectSourceAccount(account: BankAccount) {
        _uiState.update { it.copy(sourceInput = account.depositNumber, error = null) }
    }

    fun selectRail(rail: TransferRail) {
        _uiState.update { state ->
            val updated = state.copy(selectedRail = rail, error = null, inquiryResult = null, receipt = null)
            recomputeDestinationMetadata(updated)
        }
    }

    fun updateSource(value: String) {
        _uiState.update { it.copy(sourceInput = IranianValidators.normalizeDigits(value), error = null) }
    }

    fun updateDestination(value: String) {
        _uiState.update { state ->
            val normalized = IranianValidators.normalizeDigits(value)
            val updated = state.copy(
                destinationInput = normalized,
                inquiryResult = null,
                error = null,
                receipt = null
            )
            recomputeDestinationMetadata(updated)
        }
    }

    fun updateAmount(value: String) {
        val digitsOnly = IranianValidators.normalizeDigits(value).filter(Char::isDigit)
        _uiState.update { it.copy(amountInput = digitsOnly, error = null) }
    }

    fun toggleCurrencyUnit() {
        _uiState.update {
            val next = if (it.currencyUnit == CurrencyDisplayUnit.TOMAN) {
                CurrencyDisplayUnit.RIAL
            } else {
                CurrencyDisplayUnit.TOMAN
            }
            it.copy(currencyUnit = next)
        }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(descriptionInput = value, error = null) }
    }

    fun inquireIbanOwner() {
        val dest = _uiState.value.destinationInput
        viewModelScope.launch {
            _uiState.update { it.copy(isInquiringIban = true, error = null, inquiryResult = null) }
            when (val res = inquireIbanUseCase(dest)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isInquiringIban = false, inquiryResult = res.data) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isInquiringIban = false, error = res.error) }
                }
            }
        }
    }

    fun submitTransfer() {
        val current = _uiState.value
        val amountRial = CurrencyFormatter.parseRialInput(current.amountInput, current.currencyUnit)
        if (amountRial == null) {
            _uiState.update { it.copy(error = BankError.Validation("مبلغ معتبری برای انتقال وارد کنید.")) }
            return
        }
        if (current.sourceInput.isBlank()) {
            _uiState.update { it.copy(error = BankError.Validation("شماره سپرده یا شماره کارت مبدا را وارد کنید.")) }
            return
        }

        val normalizedDestination = if (current.selectedRail == TransferRail.CARD_TO_CARD) {
            IranianValidators.normalizeDigits(current.destinationInput).filter(Char::isDigit)
        } else {
            IranianValidators.normalizeSheba(current.destinationInput)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, receipt = null) }
            val request = TransferRequest(
                sourceDepositOrCard = current.sourceInput.trim(),
                destinationIbanOrCard = normalizedDestination,
                recipientName = current.inquiryResult?.ownerName,
                amountRial = amountRial,
                rail = current.selectedRail,
                description = current.descriptionInput.ifBlank { "انتقال ${current.selectedRail.persianTitle} زربانک" },
                idempotencyTrackId = current.trackId
            )
            when (val result = executeTransferUseCase(request)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            receipt = result.data,
                            trackId = UUID.randomUUID().toString()
                        )
                    }
                    refreshAccountsAndProvider()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }

    private fun recomputeDestinationMetadata(state: TransfersUiState): TransfersUiState {
        val bank = if (state.selectedRail == TransferRail.CARD_TO_CARD) {
            IranianBankRegistry.findByCardNumber(state.destinationInput)
        } else {
            IranianBankRegistry.findBySheba(state.destinationInput)
        }
        val valid = if (state.selectedRail == TransferRail.CARD_TO_CARD) {
            IranianValidators.isValidShetabCard(state.destinationInput)
        } else {
            IranianValidators.isValidSheba(state.destinationInput)
        }
        return state.copy(detectedBank = bank, isDestinationChecksumValid = valid)
    }
}
