package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.core.network.NetworkErrorMapper
import com.example.core.network.ProviderCredentialsProvider
import com.example.core.security.SessionManager
import com.example.data.remote.source.ProviderGatewayRouter
import com.example.di.IoDispatcher
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.model.UserSession
import com.example.domain.repository.BankingRepository
import com.example.domain.repository.TransferRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankingRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val gatewayRouter: ProviderGatewayRouter,
    private val credentialsProvider: ProviderCredentialsProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BankingRepository {

    override suspend fun getAccounts(): AppResult<List<BankAccount>> = withAuthenticatedSession { session ->
        gatewayRouter.resolve(session.provider).fetchAccounts(session.clientId)
    }

    override suspend fun getAccountBalance(depositNumber: String): AppResult<BankAccount> =
        withAuthenticatedSession { session ->
            gatewayRouter.resolve(session.provider).fetchAccountBalance(session.clientId, depositNumber)
        }

    override suspend fun getAccountTransactions(depositNumber: String): AppResult<List<BankTransaction>> =
        withAuthenticatedSession { session ->
            gatewayRouter.resolve(session.provider).fetchTransactions(session.clientId, depositNumber)
        }

    override suspend fun inquireIban(iban: String): AppResult<IbanInquiryResult> =
        withAuthenticatedSession { session ->
            gatewayRouter.resolve(session.provider).inquireIban(session.clientId, iban)
        }

    private suspend fun <T> withAuthenticatedSession(
        block: suspend (UserSession) -> T
    ): AppResult<T> = withContext(ioDispatcher) {
        val selectedProvider = sessionManager.getSelectedProvider()
        if (!credentialsProvider.isConfigured(selectedProvider)) {
            return@withContext AppResult.Error(
                BankError.MissingCredentials(
                    providerName = selectedProvider.persianName,
                    message = "کلیدهای دسترسی ${selectedProvider.persianName} در بخش Secrets تنظیم نشده‌اند."
                )
            )
        }
        val session = sessionManager.getActiveSession()
            ?: return@withContext AppResult.Error(BankError.Unauthorized())
        try {
            AppResult.Success(block(session))
        } catch (t: Throwable) {
            AppResult.Error(NetworkErrorMapper.map(t))
        }
    }
}

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val gatewayRouter: ProviderGatewayRouter,
    private val credentialsProvider: ProviderCredentialsProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransferRepository {

    override suspend fun executeTransfer(request: TransferRequest): AppResult<TransferReceipt> =
        withContext(ioDispatcher) {
            val selectedProvider = sessionManager.getSelectedProvider()
            if (!credentialsProvider.isConfigured(selectedProvider)) {
                return@withContext AppResult.Error(
                    BankError.MissingCredentials(
                        providerName = selectedProvider.persianName,
                        message = "کلیدهای دسترسی ${selectedProvider.persianName} در بخش Secrets تنظیم نشده‌اند."
                    )
                )
            }
            val session = sessionManager.getActiveSession()
                ?: return@withContext AppResult.Error(BankError.Unauthorized())
            try {
                val receipt = gatewayRouter.resolve(session.provider)
                    .executeTransfer(session.clientId, request)
                AppResult.Success(receipt)
            } catch (t: Throwable) {
                AppResult.Error(NetworkErrorMapper.map(t))
            }
        }
}
