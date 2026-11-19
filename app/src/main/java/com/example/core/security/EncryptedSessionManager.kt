@file:Suppress("DEPRECATION")

package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.UserSession
import com.example.domain.repository.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.GeneralSecurityException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionManager {

    private val lock = Any()
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private val _sessionState: MutableStateFlow<SessionState> by lazy {
        MutableStateFlow(readInitialState())
    }
    override val sessionState: StateFlow<SessionState>
        get() = _sessionState.asStateFlow()

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            buildEncryptedPrefs()
        } catch (_: GeneralSecurityException) {
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            buildEncryptedPrefs()
        }
    }

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun readInitialState(): SessionState {
        val existing = readSessionInternal()
        if (existing != null) {
            return SessionState.Authenticated(existing)
        }
        val selected = getSelectedProvider()
        val loggedOutManually = prefs.getBoolean(KEY_USER_LOGGED_OUT, false)
        return if (selected == OpenBankingProvider.FREE_SANDBOX && !loggedOutManually) {
            val freeSession = createDefaultFreeSession()
            persistSessionInternal(freeSession)
            SessionState.Authenticated(freeSession)
        } else {
            SessionState.Unauthenticated(selected)
        }
    }

    override fun getActiveSession(): UserSession? = synchronized(lock) {
        readSessionInternal()
    }

    override fun getSelectedProvider(): OpenBankingProvider = synchronized(lock) {
        val raw = prefs.getString(KEY_PROVIDER, null)
        OpenBankingProvider.entries.firstOrNull { it.name == raw } ?: OpenBankingProvider.FREE_SANDBOX
    }

    override fun setSelectedProvider(provider: OpenBankingProvider) {
        synchronized(lock) {
            val currentSession = readSessionInternal()
            prefs.edit().putString(KEY_PROVIDER, provider.name).apply()
            if (currentSession == null || currentSession.provider != provider) {
                if (provider == OpenBankingProvider.FREE_SANDBOX) {
                    val freeSession = createDefaultFreeSession()
                    persistSessionInternal(freeSession)
                    _sessionState.value = SessionState.Authenticated(freeSession)
                } else {
                    clearTokenKeysOnly()
                    _sessionState.value = SessionState.Unauthenticated(provider)
                }
            }
        }
    }

    override fun saveSession(session: UserSession) {
        synchronized(lock) {
            persistSessionInternal(session)
            _sessionState.value = SessionState.Authenticated(session)
        }
    }

    override fun clearSession() {
        synchronized(lock) {
            val provider = getSelectedProvider()
            prefs.edit().putBoolean(KEY_USER_LOGGED_OUT, true).apply()
            clearTokenKeysOnly()
            _sessionState.value = SessionState.Unauthenticated(provider)
        }
    }

    override fun savePendingOAuthState(state: String, provider: OpenBankingProvider) {
        synchronized(lock) {
            prefs.edit()
                .putString(KEY_PENDING_OAUTH_STATE, state)
                .putString(KEY_PENDING_OAUTH_PROVIDER, provider.name)
                .apply()
        }
    }

    override fun consumePendingOAuthState(): Pair<String, OpenBankingProvider>? = synchronized(lock) {
        val state = prefs.getString(KEY_PENDING_OAUTH_STATE, null)
        val providerName = prefs.getString(KEY_PENDING_OAUTH_PROVIDER, null)
        prefs.edit()
            .remove(KEY_PENDING_OAUTH_STATE)
            .remove(KEY_PENDING_OAUTH_PROVIDER)
            .apply()
        if (state.isNullOrBlank()) return@synchronized null
        val provider = OpenBankingProvider.entries.firstOrNull { it.name == providerName }
            ?: getSelectedProvider()
        state to provider
    }

    override fun getVaultDiagnostics(): VaultDiagnostics = synchronized(lock) {
        val session = readSessionInternal()
        VaultDiagnostics(
            prefsFileName = "$PREFS_FILE_NAME.xml",
            keyEncryptionScheme = "AES256_SIV (Deterministic Key Encryption)",
            valueEncryptionScheme = "AES256_GCM (Authenticated Value Encryption)",
            masterKeyScheme = "AndroidKeyStore AES256_GCM MasterKey",
            backupExcluded = true,
            hasActiveToken = session != null && !session.isExpired(),
            hasRefreshToken = !session?.refreshToken.isNullOrBlank(),
            activeProvider = session?.provider ?: getSelectedProvider(),
            expiresAtEpochMs = session?.expiresAtEpochMs
        )
    }

    private fun createDefaultFreeSession(): UserSession = UserSession(
        provider = OpenBankingProvider.FREE_SANDBOX,
        accessToken = "free_zarbank_${UUID.randomUUID().toString().take(12)}",
        refreshToken = "free_refresh_${UUID.randomUUID().toString().take(8)}",
        expiresAtEpochMs = System.currentTimeMillis() + FREE_SESSION_LIFETIME_MS,
        clientId = "zarbank-free-local",
        scopes = OAuthConsentCoordinator.DEFAULT_BANKING_SCOPES
    )

    private fun persistSessionInternal(session: UserSession) {
        prefs.edit()
            .putBoolean(KEY_USER_LOGGED_OUT, false)
            .putString(KEY_PROVIDER, session.provider.name)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochMs)
            .putString(KEY_CLIENT_ID, session.clientId)
            .putString(KEY_NATIONAL_ID, session.nationalId)
            .putString(KEY_SCOPES, session.scopes.joinToString(","))
            .putLong(KEY_CREATED_AT, session.createdAtEpochMs)
            .apply()
    }

    private fun clearTokenKeysOnly() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_CLIENT_ID)
            .remove(KEY_NATIONAL_ID)
            .remove(KEY_SCOPES)
            .remove(KEY_CREATED_AT)
            .apply()
    }

    private fun readSessionInternal(): UserSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val providerName = prefs.getString(KEY_PROVIDER, null)
        val provider = OpenBankingProvider.entries.firstOrNull { it.name == providerName }
            ?: OpenBankingProvider.FREE_SANDBOX
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val clientId = prefs.getString(KEY_CLIENT_ID, null).orEmpty()
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val nationalId = prefs.getString(KEY_NATIONAL_ID, null)
        val scopesRaw = prefs.getString(KEY_SCOPES, "").orEmpty()
        val scopes = if (scopesRaw.isBlank()) emptyList() else scopesRaw.split(",")
        val createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())

        return UserSession(
            provider = provider,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochMs = expiresAt,
            clientId = clientId,
            nationalId = nationalId,
            scopes = scopes,
            createdAtEpochMs = createdAt
        )
    }

    companion object {
        const val PREFS_FILE_NAME = "zarbank_secure_session"
        private const val FREE_SESSION_LIFETIME_MS = 30L * 24L * 60L * 60L * 1000L
        private const val KEY_PROVIDER = "ob_provider"
        private const val KEY_USER_LOGGED_OUT = "ob_user_logged_out"
        private const val KEY_ACCESS_TOKEN = "ob_access_token"
        private const val KEY_REFRESH_TOKEN = "ob_refresh_token"
        private const val KEY_EXPIRES_AT = "ob_expires_at"
        private const val KEY_CLIENT_ID = "ob_client_id"
        private const val KEY_NATIONAL_ID = "ob_national_id"
        private const val KEY_SCOPES = "ob_scopes"
        private const val KEY_CREATED_AT = "ob_created_at"
        private const val KEY_PENDING_OAUTH_STATE = "ob_pending_oauth_state"
        private const val KEY_PENDING_OAUTH_PROVIDER = "ob_pending_oauth_provider"
    }
}
