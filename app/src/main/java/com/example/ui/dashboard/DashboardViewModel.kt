package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.core.common.CurrencyDisplayUnit
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.OpenBankingProvider
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SessionState
import com.example.domain.usecase.GetAccountBalanceUseCase
import com.example.domain.usecase.GetAccountTransactionsUseCase
import com.example.domain.usecase.GetBankAccountsUseCase
import com.example.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed UI state hierarchy for the Banking Dashboard.
 */
sealed class DashboardContentState {
    data class Unauthenticated(val isProviderConfigured: Boolean) : DashboardContentState()
    data object Loading : DashboardContentState()
    data class Success(
        val accounts: List<BankAccount>,
        val selectedDepositNumber: String?,
        val transactionsState: TransactionsUiState
    ) : DashboardContentState()
    data class Error(val error: BankError) : DashboardContentState()
}

sealed class TransactionsUiState {
    data object Loading : TransactionsUiState()
    data class Success(val transactions: List<BankTransaction>) : TransactionsUiState()
    data class Error(val error: BankError) : TransactionsUiState()
}

data class DashboardUiState(
    val sessionState: SessionState,
    val activeProvider: OpenBankingProvider,
    val isProviderConfigured: Boolean,
    val currencyUnit: CurrencyDisplayUnit = CurrencyDisplayUnit.TOMAN,
    val hideBalances: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentState: DashboardContentState = DashboardContentState.Unauthenticated(false)
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val authRepository: AuthRepository,
    private val getBankAccountsUseCase: GetBankAccountsUseCase,
    private val getAccountBalanceUseCase: GetAccountBalanceUseCase,
    private val getAccountTransactionsUseCase: GetAccountTransactionsUseCase
) : ViewModel() {

    private val initialProvider = authRepository.getSelectedProvider()
    private val initialConfigured = authRepository.isProviderConfigured(initialProvider)

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            sessionState = SessionState.Unauthenticated(initialProvider),
            activeProvider = initialProvider,
            isProviderConfigured = initialConfigured,
            contentState = DashboardContentState.Unauthenticated(initialConfigured)
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val provider = when (session) {
                    is SessionState.Authenticated -> session.session.provider
                    is SessionState.Unauthenticated -> session.selectedProvider
                }
                val configured = authRepository.isProviderConfigured(provider)
                _uiState.update {
                    it.copy(
                        sessionState = session,
                        activeProvider = provider,
                        isProviderConfigured = configured,
                        contentState = if (session is SessionState.Unauthenticated) {
                            DashboardContentState.Unauthenticated(configured)
                        } else {
                            it.contentState
                        }
                    )
                }
                if (session is SessionState.Authenticated) {
                    loadDashboardData(isPullRefresh = false)
                }
            }
        }
    }

    fun refreshAccounts(isPullRefresh: Boolean = false) {
        loadDashboardData(isPullRefresh = isPullRefresh)
    }

    private fun loadDashboardData(isPullRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isRefreshing = isPullRefresh,
                    contentState = if (isPullRefresh && current.contentState is DashboardContentState.Success) {
                        current.contentState
                    } else {
                        DashboardContentState.Loading
                    }
                )
            }

            when (val accountsResult = getBankAccountsUseCase()) {
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            contentState = DashboardContentState.Error(accountsResult.error)
                        )
                    }
                }
                is AppResult.Success -> {
                    val accounts = accountsResult.data
                    val previousSelected = (_uiState.value.contentState as? DashboardContentState.Success)
                        ?.selectedDepositNumber
                    val activeDeposit = accounts.firstOrNull { it.depositNumber == previousSelected }?.depositNumber
                        ?: accounts.firstOrNull()?.depositNumber

                    if (activeDeposit == null) {
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                contentState = DashboardContentState.Success(
                                    accounts = emptyList(),
                                    selectedDepositNumber = null,
                                    transactionsState = TransactionsUiState.Success(emptyList())
                                )
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            contentState = DashboardContentState.Success(
                                accounts = accounts,
                                selectedDepositNumber = activeDeposit,
                                transactionsState = TransactionsUiState.Loading
                            )
                        )
                    }

                    fetchBalanceAndTransactions(activeDeposit)
                }
            }
        }
    }

    fun selectAccount(depositNumber: String) {
        _uiState.update { state ->
            val current = state.contentState as? DashboardContentState.Success ?: return@update state
            state.copy(
                contentState = current.copy(
                    selectedDepositNumber = depositNumber,
                    transactionsState = TransactionsUiState.Loading
                )
            )
        }
        viewModelScope.launch {
            fetchBalanceAndTransactions(depositNumber)
        }
    }

    private suspend fun fetchBalanceAndTransactions(depositNumber: String) = coroutineScope {
        val balanceDeferred = async { getAccountBalanceUseCase(depositNumber) }
        val transactionsDeferred = async { getAccountTransactionsUseCase(depositNumber) }

        val balanceResult = balanceDeferred.await()
        val txResult = transactionsDeferred.await()

        _uiState.update { state ->
            val current = state.contentState as? DashboardContentState.Success ?: return@update state
            if (current.selectedDepositNumber != depositNumber) return@update state

            val updatedAccounts = if (balanceResult is AppResult.Success) {
                val fresh = balanceResult.data
                current.accounts.map { existing ->
                    if (existing.depositNumber == depositNumber) {
                        existing.copy(
                            balanceRial = fresh.balanceRial,
                            withdrawableBalanceRial = fresh.withdrawableBalanceRial,
                            status = fresh.status
                        )
                    } else {
                        existing
                    }
                }
            } else {
                current.accounts
            }

            val updatedTxState = when (txResult) {
                is AppResult.Success -> TransactionsUiState.Success(txResult.data)
                is AppResult.Error -> TransactionsUiState.Error(txResult.error)
            }

            state.copy(
                contentState = current.copy(
                    accounts = updatedAccounts,
                    transactionsState = updatedTxState
                )
            )
        }
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

    fun toggleBalanceVisibility() {
        _uiState.update { it.copy(hideBalances = !it.hideBalances) }
    }
}
