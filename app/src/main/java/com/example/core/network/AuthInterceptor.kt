package com.example.core.network

import com.example.core.security.SessionManager
import com.example.domain.model.OpenBankingProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val credentialsProvider: ProviderCredentialsProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // OAuth token exchange endpoints use Basic/client credentials instead of Bearer token
        val isTokenEndpoint = path.contains("oauth2/token") || path.contains("oauth/token")

        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        val session = sessionManager.getActiveSession()
        val activeProvider = session?.provider ?: sessionManager.getSelectedProvider()

        if (!isTokenEndpoint && session != null && session.accessToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${session.accessToken}")
        }

        if (activeProvider == OpenBankingProvider.FARABOOM) {
            val appKey = credentialsProvider.faraboomAppKey()
            val tokenId = credentialsProvider.faraboomTokenId()
            if (appKey.isNotBlank() && !appKey.endsWith("_PLACEHOLDER")) {
                requestBuilder.header("App-Key", appKey)
            }
            if (tokenId.isNotBlank() && !tokenId.endsWith("_PLACEHOLDER")) {
                requestBuilder.header("Token-Id", tokenId)
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
