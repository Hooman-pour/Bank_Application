package com.example.domain.usecase

import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.core.common.IranianValidators
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransferRail
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.model.UserSession
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BankingRepository
import com.example.domain.repository.SessionState
import com.example.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<SessionState> = authRepository.observeSessionState()
}

class SwitchProviderUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(provider: OpenBankingProvider) =
        authRepository.switchProvider(provider)
}

class ExchangeOAuthCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        provider: OpenBankingProvider,
        authorizationCode: String,
        redirectUri: String
    ): AppResult<UserSession> {
        if (authorizationCode.isBlank()) {
            return AppResult.Error(BankError.Validation("کد مجوز OAuth نمی‌تواند خالی باشد."))
        }
        return authRepository.exchangeAuthorizationCode(provider, authorizationCode.trim(), redirectUri)
    }
}

class AuthenticateClientUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(provider: OpenBankingProvider): AppResult<UserSession> =
        authRepository.requestClientCredentialsSession(provider)
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.logout()
}

class GetBankAccountsUseCase @Inject constructor(
    private val bankingRepository: BankingRepository
) {
    suspend operator fun invoke(): AppResult<List<BankAccount>> = bankingRepository.getAccounts()
}

class GetAccountBalanceUseCase @Inject constructor(
    private val bankingRepository: BankingRepository
) {
    suspend operator fun invoke(depositNumber: String): AppResult<BankAccount> {
        if (depositNumber.isBlank()) {
            return AppResult.Error(BankError.Validation("شماره سپرده الزامی است."))
        }
        return bankingRepository.getAccountBalance(depositNumber.trim())
    }
}

class GetAccountTransactionsUseCase @Inject constructor(
    private val bankingRepository: BankingRepository
) {
    suspend operator fun invoke(depositNumber: String): AppResult<List<BankTransaction>> {
        if (depositNumber.isBlank()) {
            return AppResult.Error(BankError.Validation("شماره سپرده الزامی است."))
        }
        return bankingRepository.getAccountTransactions(depositNumber)
    }
}

class InquireIbanUseCase @Inject constructor(
    private val bankingRepository: BankingRepository
) {
    suspend operator fun invoke(rawIban: String): AppResult<IbanInquiryResult> {
        val normalized = IranianValidators.normalizeSheba(rawIban)
        if (!IranianValidators.isValidSheba(normalized)) {
            return AppResult.Error(
                BankError.Validation("شماره شبا نامعتبر است. باید با IR شروع شود و ۲۴ رقم با کد کنترلی معتبر Mod-97 داشته باشد.")
            )
        }
        return bankingRepository.inquireIban(normalized)
    }
}

class ExecuteTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository
) {
    suspend operator fun invoke(request: TransferRequest): AppResult<TransferReceipt> {
        if (request.amountRial <= 0L) {
            return AppResult.Error(BankError.Validation("مبلغ انتقال باید بیشتر از صفر باشد."))
        }
        if (request.rail == TransferRail.CARD_TO_CARD) {
            if (!IranianValidators.isValidShetabCard(request.destinationIbanOrCard)) {
                return AppResult.Error(BankError.Validation("شماره کارت ۱۶ رقمی مقصد در بررسی الگوریتم Luhn معتبر نیست."))
            }
        } else {
            if (!IranianValidators.isValidSheba(request.destinationIbanOrCard)) {
                return AppResult.Error(BankError.Validation("شماره شبا مقصد (IR + ۲۴ رقم) در بررسی Mod-97 معتبر نیست."))
            }
        }
        return transferRepository.executeTransfer(request)
    }
}
