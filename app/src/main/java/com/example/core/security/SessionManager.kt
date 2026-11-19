package com.example.core.security

import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.UserSession
import com.example.domain.repository.SessionState
import kotlinx.coroutines.flow.StateFlow

data class VaultDiagnostics(
    val prefsFileName: String,
    val keyEncryptionScheme: String,
    val valueEncryptionScheme: String,
    val masterKeyScheme: String,
    val backupExcluded: Boolean,
    val hasActiveToken: Boolean,
    val hasRefreshToken: Boolean,
    val activeProvider: OpenBankingProvider,
    val expiresAtEpochMs: Long?
)

interface SessionManager {
    val sessionState: StateFlow<SessionState>
    fun getActiveSession(): UserSession?
    fun getSelectedProvider(): OpenBankingProvider
    fun setSelectedProvider(provider: OpenBankingProvider)
    fun saveSession(session: UserSession)
    fun clearSession()
    fun savePendingOAuthState(state: String, provider: OpenBankingProvider)
    fun consumePendingOAuthState(): Pair<String, OpenBankingProvider>?
    fun getVaultDiagnostics(): VaultDiagnostics
}
