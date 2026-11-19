package com.example.data.remote.source

import android.util.Base64
import com.example.core.network.ProviderCredentialsProvider
import com.example.data.remote.finotech.FinotechApiService
import com.example.data.remote.finotech.FinotechTokenRequestDto
import com.example.data.remote.finotech.FinotechTransferRequestDto
import com.example.data.remote.mapper.OpenBankingMappers.toDomain
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.model.UserSession
import java.util.UUID
import javax.inject.Inject

class FinotechRemoteDataSource @Inject constructor(
    private val apiService: FinotechApiService,
    private val credentialsProvider: ProviderCredentialsProvider
) : OpenBankingRemoteDataSource {

    override val provider: OpenBankingProvider = OpenBankingProvider.FINOTECH

    override suspend fun exchangeAuthorizationCode(
        authorizationCode: String,
        redirectUri: String
    ): UserSession {
        val response = apiService.requestToken(
            basicAuth = buildBasicAuthHeader(),
            body = FinotechTokenRequestDto(
                grantType = "authorization_code",
                code = authorizationCode,
                redirectUri = redirectUri
            )
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Finotech token response missing result")

        return UserSession(
            provider = OpenBankingProvider.FINOTECH,
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + (result.lifeTimeMs ?: DEFAULT_LIFETIME_MS),
            clientId = credentialsProvider.finotechClientId(),
            scopes = result.scopes.orEmpty()
        )
    }

    override suspend fun requestClientCredentialsToken(): UserSession {
        val response = apiService.requestToken(
            basicAuth = buildBasicAuthHeader(),
            body = FinotechTokenRequestDto(
                grantType = "client_credentials",
                scopes = DEFAULT_SCOPES
            )
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Finotech client token missing result")

        return UserSession(
            provider = OpenBankingProvider.FINOTECH,
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + (result.lifeTimeMs ?: DEFAULT_LIFETIME_MS),
            clientId = credentialsProvider.finotechClientId(),
            scopes = result.scopes.orEmpty()
        )
    }

    override suspend fun refreshToken(refreshToken: String): UserSession {
        val response = apiService.requestToken(
            basicAuth = buildBasicAuthHeader(),
            body = FinotechTokenRequestDto(
                grantType = "refresh_token",
                refreshToken = refreshToken
            )
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Finotech refresh token missing result")

        return UserSession(
            provider = OpenBankingProvider.FINOTECH,
            accessToken = result.accessToken,
            refreshToken = result.refreshToken ?: refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + (result.lifeTimeMs ?: DEFAULT_LIFETIME_MS),
            clientId = credentialsProvider.finotechClientId(),
            scopes = result.scopes.orEmpty()
        )
    }

    override suspend fun fetchAccounts(clientId: String): List<BankAccount> {
        val response = apiService.getDeposits(
            clientId = clientId,
            trackId = UUID.randomUUID().toString()
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Empty deposits response from Finotech")
        return result.deposits.map { it.toDomain() }
    }

    override suspend fun fetchAccountBalance(
        clientId: String,
        depositNumber: String
    ): BankAccount {
        val response = apiService.getDepositBalance(
            clientId = clientId,
            depositNumber = depositNumber,
            trackId = UUID.randomUUID().toString()
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Empty balance response from Finotech")
        return result.toDomain(fallbackDepositNumber = depositNumber)
    }

    override suspend fun fetchTransactions(
        clientId: String,
        depositNumber: String
    ): List<BankTransaction> {
        val response = apiService.getDepositStatement(
            clientId = clientId,
            depositNumber = depositNumber,
            trackId = UUID.randomUUID().toString()
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Empty statement response from Finotech")
        return result.statements.map { it.toDomain(depositNumber) }
    }

    override suspend fun inquireIban(clientId: String, iban: String): IbanInquiryResult {
        val response = apiService.inquireIban(
            clientId = clientId,
            iban = iban,
            trackId = UUID.randomUUID().toString()
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Empty IBAN inquiry result")
        return result.toDomain()
    }

    override suspend fun executeTransfer(
        clientId: String,
        request: TransferRequest
    ): TransferReceipt {
        val nameParts = request.recipientName.orEmpty().trim().split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0).orEmpty().ifBlank { "Account" }
        val lastName = nameParts.getOrNull(1).orEmpty().ifBlank { "Holder" }

        val response = apiService.transferTo(
            clientId = clientId,
            trackId = request.idempotencyTrackId,
            body = FinotechTransferRequestDto(
                amount = request.amountRial,
                description = request.description,
                destinationFirstName = firstName,
                destinationLastName = lastName,
                destinationNumber = request.destinationIbanOrCard,
                sourceDeposit = request.sourceDepositOrCard
            )
        )
        val result = response.result
            ?: throw IllegalStateException(response.error?.message ?: "Empty Finotech transfer response")
        return result.toDomain(request)
    }

    private fun buildBasicAuthHeader(): String {
        val raw = "${credentialsProvider.finotechClientId()}:${credentialsProvider.finotechClientSecret()}"
        val encoded = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $encoded"
    }

    private companion object {
        const val DEFAULT_LIFETIME_MS = 3_600_000L
        const val DEFAULT_SCOPES = "oak:deposits:get,oak:statement:get,oak:transfer-to:execute,facility:iban-inquiry:get"
    }
}
