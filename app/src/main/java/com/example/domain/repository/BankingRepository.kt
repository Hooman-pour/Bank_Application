package com.example.domain.repository

import com.example.core.common.AppResult
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest

interface BankingRepository {
    suspend fun getAccounts(): AppResult<List<BankAccount>>
    suspend fun getAccountBalance(depositNumber: String): AppResult<BankAccount>
    suspend fun getAccountTransactions(depositNumber: String): AppResult<List<BankTransaction>>
    suspend fun inquireIban(iban: String): AppResult<IbanInquiryResult>
}

interface TransferRepository {
    suspend fun executeTransfer(request: TransferRequest): AppResult<TransferReceipt>
}
