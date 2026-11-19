package com.example.data.remote.source

import com.example.core.network.ProviderCredentialsProvider
import com.example.data.remote.faraboom.FaraboomAchTransferRequestDto
import com.example.data.remote.faraboom.FaraboomApiService
import com.example.data.remote.faraboom.FaraboomCardTransferRequestDto
import com.example.data.remote.mapper.OpenBankingMappers.toDomain
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransferRail
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.model.UserSession
import java.util.UUID
import javax.inject.Inject

class FaraboomRemoteDataSource @Inject constructor(
    private val apiService: FaraboomApiService,
    private val credentialsProvider: ProviderCredentialsProvider
) : OpenBankingRemoteDataSource {

    override val provider: OpenBankingProvider = OpenBankingProvider.FARABOOM

    override suspend fun exchangeAuthorizationCode(
        authorizationCode: String,
        redirectUri: String
    ): UserSession {
        val token = apiService.requestToken(
            grantType = "authorization_code",
            code = authorizationCode,
            redirectUri = redirectUri
        )
        return UserSession(
            provider = OpenBankingProvider.FARABOOM,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + token.expiresInSeconds * 1000L,
            clientId = credentialsProvider.faraboomAppKey(),
            scopes = token.scope?.split(" ").orEmpty()
        )
    }

    override suspend fun requestClientCredentialsToken(): UserSession {
        val token = apiService.requestToken(grantType = "client_credentials")
        return UserSession(
            provider = OpenBankingProvider.FARABOOM,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + token.expiresInSeconds * 1000L,
            clientId = credentialsProvider.faraboomAppKey(),
            scopes = token.scope?.split(" ").orEmpty()
        )
    }

    override suspend fun refreshToken(refreshToken: String): UserSession {
        val token = apiService.requestToken(
            grantType = "refresh_token",
            refreshToken = refreshToken
        )
        return UserSession(
            provider = OpenBankingProvider.FARABOOM,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken ?: refreshToken,
            expiresAtEpochMs = System.currentTimeMillis() + token.expiresInSeconds * 1000L,
            clientId = credentialsProvider.faraboomAppKey(),
            scopes = token.scope?.split(" ").orEmpty()
        )
    }

    override suspend fun fetchAccounts(clientId: String): List<BankAccount> {
        val response = apiService.getDeposits(trackId = UUID.randomUUID().toString())
        return response.deposits.map { it.toDomain() }
    }

    override suspend fun fetchAccountBalance(
        clientId: String,
        depositNumber: String
    ): BankAccount {
        val accounts = fetchAccounts(clientId)
        return accounts.firstOrNull { it.depositNumber == depositNumber }
            ?: throw IllegalStateException("Deposit $depositNumber not found on Faraboom")
    }

    override suspend fun fetchTransactions(
        clientId: String,
        depositNumber: String
    ): List<BankTransaction> {
        val response = apiService.getStatements(
            depositNumber = depositNumber,
            trackId = UUID.randomUUID().toString()
        )
        return response.statements.map { it.toDomain(depositNumber) }
    }

    override suspend fun inquireIban(clientId: String, iban: String): IbanInquiryResult {
        return apiService.inquireIban(
            iban = iban,
            trackId = UUID.randomUUID().toString()
        ).toDomain()
    }

    override suspend fun executeTransfer(
        clientId: String,
        request: TransferRequest
    ): TransferReceipt {
        val response = when (request.rail) {
            TransferRail.CARD_TO_CARD -> apiService.executeCardTransfer(
                trackId = request.idempotencyTrackId,
                body = FaraboomCardTransferRequestDto(
                    sourceCard = request.sourceDepositOrCard,
                    destinationCard = request.destinationIbanOrCard,
                    amount = request.amountRial,
                    description = request.description
                )
            )
            TransferRail.SATNA_RTGS -> apiService.executeRtgsTransfer(
                trackId = request.idempotencyTrackId,
                body = FaraboomAchTransferRequestDto(
                    sourceDepositNumber = request.sourceDepositOrCard,
                    destinationIban = request.destinationIbanOrCard,
                    ownerName = request.recipientName.orEmpty().ifBlank { "Account Holder" },
                    amount = request.amountRial,
                    description = request.description
                )
            )
            TransferRail.PAYA_ACH, TransferRail.POL_INSTANT -> apiService.executeAchTransfer(
                trackId = request.idempotencyTrackId,
                body = FaraboomAchTransferRequestDto(
                    sourceDepositNumber = request.sourceDepositOrCard,
                    destinationIban = request.destinationIbanOrCard,
                    ownerName = request.recipientName.orEmpty().ifBlank { "Account Holder" },
                    amount = request.amountRial,
                    description = request.description
                )
            )
        }
        return response.toDomain(request)
    }
}
