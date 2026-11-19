package com.example.data.remote.source

import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.model.UserSession

interface OpenBankingRemoteDataSource {
    val provider: OpenBankingProvider

    suspend fun exchangeAuthorizationCode(
        authorizationCode: String,
        redirectUri: String
    ): UserSession

    suspend fun requestClientCredentialsToken(): UserSession

    suspend fun refreshToken(refreshToken: String): UserSession

    suspend fun fetchAccounts(clientId: String): List<BankAccount>

    suspend fun fetchAccountBalance(clientId: String, depositNumber: String): BankAccount

    suspend fun fetchTransactions(clientId: String, depositNumber: String): List<BankTransaction>

    suspend fun inquireIban(clientId: String, iban: String): IbanInquiryResult

    suspend fun executeTransfer(clientId: String, request: TransferRequest): TransferReceipt
}
