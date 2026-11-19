package com.example.core.network

import com.example.BuildConfig
import com.example.domain.model.OpenBankingProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderCredentialsProvider @Inject constructor() {

    fun finotechClientId(): String = BuildConfig.FINOTECH_CLIENT_ID
    fun finotechClientSecret(): String = BuildConfig.FINOTECH_CLIENT_SECRET
    fun faraboomAppKey(): String = BuildConfig.FARABOOM_APP_KEY
    fun faraboomTokenId(): String = BuildConfig.FARABOOM_TOKEN_ID

    fun isConfigured(provider: OpenBankingProvider): Boolean = when (provider) {
        OpenBankingProvider.FREE_SANDBOX -> true
        OpenBankingProvider.FINOTECH ->
            finotechClientId().isRealSecret() && finotechClientSecret().isRealSecret()
        OpenBankingProvider.FARABOOM ->
            faraboomAppKey().isRealSecret() && faraboomTokenId().isRealSecret()
    }

    private fun String.isRealSecret(): Boolean =
        isNotBlank() && !endsWith("_PLACEHOLDER")
}
