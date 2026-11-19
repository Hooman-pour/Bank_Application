package com.example.data.remote.source

import com.example.domain.model.OpenBankingProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderGatewayRouter @Inject constructor(
    private val dataSources: Map<OpenBankingProvider, @JvmSuppressWildcards OpenBankingRemoteDataSource>
) {
    fun resolve(provider: OpenBankingProvider): OpenBankingRemoteDataSource =
        dataSources[provider]
            ?: error("No OpenBankingRemoteDataSource bound for provider: $provider")
}
