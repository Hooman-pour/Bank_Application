package com.example.core.security

import android.net.Uri
import android.util.Base64
import com.example.core.network.ProviderCredentialsProvider
import com.example.domain.model.OpenBankingProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

sealed interface OAuthCallbackEvent {
    data class Authorized(
        val provider: OpenBankingProvider,
        val code: String,
        val redirectUri: String
    ) : OAuthCallbackEvent

    data class Failed(val reason: String) : OAuthCallbackEvent
}

@Singleton
class OAuthConsentCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val credentialsProvider: ProviderCredentialsProvider
) {
    private val secureRandom = SecureRandom()
    private val _callbacks = MutableSharedFlow<OAuthCallbackEvent>(extraBufferCapacity = 1)
    val callbacks: SharedFlow<OAuthCallbackEvent> = _callbacks.asSharedFlow()

    fun buildConsentUri(
        provider: OpenBankingProvider,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        scopes: List<String> = DEFAULT_BANKING_SCOPES
    ): Uri {
        val state = generateCsrfState()
        sessionManager.savePendingOAuthState(state, provider)

        return when (provider) {
            OpenBankingProvider.FREE_SANDBOX -> {
                Uri.parse(redirectUri).buildUpon()
                    .appendQueryParameter("code", "free_local_auth_code")
                    .appendQueryParameter("state", state)
                    .build()
            }
            OpenBankingProvider.FINOTECH -> {
                Uri.parse("https://apibeta.finnotech.ir/dev/v2/oauth2/authorize").buildUpon()
                    .appendQueryParameter("client_id", credentialsProvider.finotechClientId())
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", redirectUri)
                    .appendQueryParameter("scope", scopes.joinToString(","))
                    .appendQueryParameter("state", state)
                    .build()
            }
            OpenBankingProvider.FARABOOM -> {
                Uri.parse("https://api.faraboom.co/v1/oauth/authorize").buildUpon()
                    .appendQueryParameter("client_id", credentialsProvider.faraboomAppKey())
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", redirectUri)
                    .appendQueryParameter("scope", scopes.joinToString(" "))
                    .appendQueryParameter("state", state)
                    .build()
            }
        }
    }

    fun handleRedirectUri(uri: Uri?) {
        if (uri == null) return
        if (uri.scheme != REDIRECT_SCHEME || uri.host != REDIRECT_HOST) return

        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            sessionManager.consumePendingOAuthState()
            val desc = uri.getQueryParameter("error_description") ?: error
            _callbacks.tryEmit(OAuthCallbackEvent.Failed("درخواست تایید بانکی رد شد: $desc"))
            return
        }

        val code = uri.getQueryParameter("code")
        val returnedState = uri.getQueryParameter("state")
        val pending = sessionManager.consumePendingOAuthState()

        if (code.isNullOrBlank()) {
            _callbacks.tryEmit(OAuthCallbackEvent.Failed("کد مجوز در آدرس بازگشت OAuth یافت نشد."))
            return
        }

        if (pending == null || returnedState != pending.first) {
            _callbacks.tryEmit(OAuthCallbackEvent.Failed("عدم تطابق شناسه امنیتی State در بازگشت OAuth (محافظت CSRF)."))
            return
        }

        _callbacks.tryEmit(
            OAuthCallbackEvent.Authorized(
                provider = pending.second,
                code = code,
                redirectUri = DEFAULT_REDIRECT_URI
            )
        )
    }

    private fun generateCsrfState(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        const val REDIRECT_SCHEME = "zarbank"
        const val REDIRECT_HOST = "oauth"
        const val DEFAULT_REDIRECT_URI = "zarbank://oauth/callback"

        val DEFAULT_BANKING_SCOPES = listOf(
            "oak:deposits:get",
            "oak:balance:get",
            "oak:cc-statement:get",
            "facility:iban-inquiry:get"
        )
    }
}
