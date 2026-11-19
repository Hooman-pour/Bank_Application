package com.example.domain.repository

import com.example.core.common.AppResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

sealed interface SessionState {
    data class Unauthenticated(val selectedProvider: OpenBankingProvider) : SessionState
    data class Authenticated(val session: UserSession) : SessionState
}

interface AuthRepository {
    fun observeSessionState(): Flow<SessionState>
    fun getSelectedProvider(): OpenBankingProvider
    suspend fun switchProvider(provider: OpenBankingProvider)
    suspend fun exchangeAuthorizationCode(
        provider: OpenBankingProvider,
        authorizationCode: String,
        redirectUri: String
    ): AppResult<UserSession>
    suspend fun requestClientCredentialsSession(provider: OpenBankingProvider): AppResult<UserSession>
    suspend fun refreshActiveSession(): AppResult<UserSession>
    suspend fun logout()
    fun isProviderConfigured(provider: OpenBankingProvider): Boolean
}
