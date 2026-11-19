package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.ProviderCredentialsProvider
import com.example.core.security.OAuthCallbackEvent
import com.example.core.security.OAuthConsentCoordinator
import com.example.core.security.SessionManager
import com.example.core.security.VaultDiagnostics
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.UserSession
import com.example.domain.repository.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZarBank", appName)
    }

    @Test
    fun `oauth consent coordinator verifies matching CSRF state and rejects tampering`() = runBlocking {
        val fakeSessionManager = InMemorySessionManager()
        val coordinator = OAuthConsentCoordinator(
            sessionManager = fakeSessionManager,
            credentialsProvider = ProviderCredentialsProvider()
        )

        val consentUri = coordinator.buildConsentUri(OpenBankingProvider.FINOTECH)
        val generatedState = consentUri.getQueryParameter("state")!!
        assertEquals("code", consentUri.getQueryParameter("response_type"))
        assertEquals(OAuthConsentCoordinator.DEFAULT_REDIRECT_URI, consentUri.getQueryParameter("redirect_uri"))

        var emitted: OAuthCallbackEvent? = null
        val job = launch { emitted = coordinator.callbacks.first() }
        kotlinx.coroutines.yield()

        coordinator.handleRedirectUri(
            Uri.parse("zarbank://oauth/callback?code=auth_code_123&state=$generatedState")
        )
        job.join()
        assertTrue(emitted is OAuthCallbackEvent.Authorized)
        assertEquals("auth_code_123", (emitted as OAuthCallbackEvent.Authorized).code)

        // Tampered / mismatched CSRF state must fail
        coordinator.buildConsentUri(OpenBankingProvider.FINOTECH)
        var tamperedEvent: OAuthCallbackEvent? = null
        val job2 = launch { tamperedEvent = coordinator.callbacks.first() }
        kotlinx.coroutines.yield()
        coordinator.handleRedirectUri(
            Uri.parse("zarbank://oauth/callback?code=auth_code_123&state=wrong_state")
        )
        job2.join()
        assertTrue(tamperedEvent is OAuthCallbackEvent.Failed)
    }

    @Test
    fun `free sandbox data source works without finotech keys and persists transfer updates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val dataSource = com.example.data.remote.source.FreeSandboxRemoteDataSource(context, moshi)

        val accounts = dataSource.fetchAccounts("zarbank-free-local")
        assertTrue(accounts.isNotEmpty())
        val first = accounts.first()
        val initialBalance = first.balanceRial

        val receipt = dataSource.executeTransfer(
            clientId = "zarbank-free-local",
            request = com.example.domain.model.TransferRequest(
                sourceDepositOrCard = first.depositNumber,
                destinationIbanOrCard = "IR820540102680020817909002",
                recipientName = "گیرنده آزمایشی",
                amountRial = 10_000_000L,
                rail = com.example.domain.model.TransferRail.PAYA_ACH,
                description = "تست انتقال رایگان",
                idempotencyTrackId = "free-track-1"
            )
        )
        assertEquals(OpenBankingProvider.FREE_SANDBOX, receipt.provider)

        val updatedBalance = dataSource.fetchAccountBalance("zarbank-free-local", first.depositNumber)
        assertEquals(initialBalance - 10_000_000L, updatedBalance.balanceRial)

        val txs = dataSource.fetchTransactions("zarbank-free-local", first.depositNumber)
        assertEquals("تست انتقال رایگان", txs.first().description)
    }

    private class InMemorySessionManager : SessionManager {
        private var pending: Pair<String, OpenBankingProvider>? = null
        override val sessionState: StateFlow<SessionState> =
            MutableStateFlow(SessionState.Unauthenticated(OpenBankingProvider.FINOTECH))

        override fun getActiveSession(): UserSession? = null
        override fun getSelectedProvider(): OpenBankingProvider = OpenBankingProvider.FINOTECH
        override fun setSelectedProvider(provider: OpenBankingProvider) = Unit
        override fun saveSession(session: UserSession) = Unit
        override fun clearSession() = Unit
        override fun savePendingOAuthState(state: String, provider: OpenBankingProvider) {
            pending = state to provider
        }

        override fun consumePendingOAuthState(): Pair<String, OpenBankingProvider>? {
            val copy = pending
            pending = null
            return copy
        }

        override fun getVaultDiagnostics(): VaultDiagnostics = VaultDiagnostics(
            prefsFileName = "test.xml",
            keyEncryptionScheme = "AES256_SIV",
            valueEncryptionScheme = "AES256_GCM",
            masterKeyScheme = "AndroidKeyStore",
            backupExcluded = true,
            hasActiveToken = false,
            hasRefreshToken = false,
            activeProvider = OpenBankingProvider.FINOTECH,
            expiresAtEpochMs = null
        )
    }
}
