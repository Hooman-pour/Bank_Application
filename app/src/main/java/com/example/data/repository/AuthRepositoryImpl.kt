package com.example.data.repository

import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.core.network.NetworkErrorMapper
import com.example.core.network.ProviderCredentialsProvider
import com.example.core.network.SessionTokenRefresher
import com.example.core.security.SessionManager
import com.example.data.remote.source.ProviderGatewayRouter
import com.example.di.IoDispatcher
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.UserSession
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SessionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val gatewayRouter: ProviderGatewayRouter,
    private val credentialsProvider: ProviderCredentialsProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository, SessionTokenRefresher {

    override fun observeSessionState(): Flow<SessionState> = sessionManager.sessionState

    override fun getSelectedProvider(): OpenBankingProvider = sessionManager.getSelectedProvider()

    override suspend fun switchProvider(provider: OpenBankingProvider) {
        withContext(ioDispatcher) {
            sessionManager.setSelectedProvider(provider)
        }
    }

    override suspend fun exchangeAuthorizationCode(
        provider: OpenBankingProvider,
        authorizationCode: String,
        redirectUri: String
    ): AppResult<UserSession> = withContext(ioDispatcher) {
        if (!credentialsProvider.isConfigured(provider)) {
            return@withContext AppResult.Error(
                BankError.MissingCredentials(
                    providerName = provider.persianName,
                    message = "کلیدهای دسترسی ${provider.persianName} پیکربندی نشده‌اند. آن‌ها را در بخش Secrets تنظیم کنید."
                )
            )
        }
        try {
            val session = gatewayRouter.resolve(provider)
                .exchangeAuthorizationCode(authorizationCode, redirectUri)
            sessionManager.saveSession(session)
            AppResult.Success(session)
        } catch (t: Throwable) {
            AppResult.Error(NetworkErrorMapper.map(t))
        }
    }

    override suspend fun requestClientCredentialsSession(
        provider: OpenBankingProvider
    ): AppResult<UserSession> = withContext(ioDispatcher) {
        if (!credentialsProvider.isConfigured(provider)) {
            return@withContext AppResult.Error(
                BankError.MissingCredentials(
                    providerName = provider.persianName,
                    message = "کلیدهای دسترسی ${provider.persianName} پیکربندی نشده‌اند. آن‌ها را در بخش Secrets تنظیم کنید."
                )
            )
        }
        try {
            val session = gatewayRouter.resolve(provider).requestClientCredentialsToken()
            sessionManager.saveSession(session)
            AppResult.Success(session)
        } catch (t: Throwable) {
            AppResult.Error(NetworkErrorMapper.map(t))
        }
    }

    override suspend fun refreshActiveSession(): AppResult<UserSession> = refreshSession()

    override suspend fun refreshSession(): AppResult<UserSession> = withContext(ioDispatcher) {
        val current = sessionManager.getActiveSession()
            ?: return@withContext AppResult.Error(BankError.Unauthorized())
        val refreshToken = current.refreshToken
            ?: return@withContext AppResult.Error(BankError.Unauthorized("توکن بازیابی (Refresh Token) در دسترس نیست. لطفاً دوباره احراز هویت کنید."))

        try {
            val refreshed = gatewayRouter.resolve(current.provider).refreshToken(refreshToken)
            sessionManager.saveSession(refreshed)
            AppResult.Success(refreshed)
        } catch (t: Throwable) {
            AppResult.Error(NetworkErrorMapper.map(t))
        }
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            sessionManager.clearSession()
        }
    }

    override fun isProviderConfigured(provider: OpenBankingProvider): Boolean =
        credentialsProvider.isConfigured(provider)
}
