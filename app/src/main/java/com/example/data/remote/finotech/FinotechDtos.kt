package com.example.data.remote.finotech

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard Finotech Open Banking v2 response envelope.
 * See: https://docs.finnotech.ir/
 */
@JsonClass(generateAdapter = true)
data class FinotechEnvelope<T>(
    @Json(name = "trackId") val trackId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "result") val result: T? = null,
    @Json(name = "error") val error: FinotechErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class FinotechErrorDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class FinotechTokenRequestDto(
    @Json(name = "grant_type") val grantType: String,
    @Json(name = "code") val code: String? = null,
    @Json(name = "redirect_uri") val redirectUri: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "nid") val nationalId: String? = null,
    @Json(name = "scopes") val scopes: String? = null
)

@JsonClass(generateAdapter = true)
data class FinotechTokenResultDto(
    @Json(name = "value") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "lifeTime") val lifeTimeMs: Long? = null,
    @Json(name = "scopes") val scopes: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class FinotechDepositsResultDto(
    @Json(name = "deposits") val deposits: List<FinotechDepositDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FinotechDepositDto(
    @Json(name = "depositNumber") val depositNumber: String,
    @Json(name = "iban") val iban: String? = null,
    @Json(name = "depositTitle") val depositTitle: String? = null,
    @Json(name = "balance") val balance: Long? = null,
    @Json(name = "availableBalance") val availableBalance: Long? = null,
    @Json(name = "depositStatus") val depositStatus: String? = null,
    @Json(name = "cardNumber") val cardNumber: String? = null
)

/**
 * Result payload for Finotech Account Balance (`GET /oak/v2/clients/{clientId}/deposits/{deposit}/balance`).
 * Amounts are denominated in Iranian Rials (IRR).
 */
@JsonClass(generateAdapter = true)
data class FinotechDepositBalanceResultDto(
    @Json(name = "deposit") val deposit: String? = null,
    @Json(name = "iban") val iban: String? = null,
    @Json(name = "balance") val balance: Long? = null,
    @Json(name = "availableBalance") val availableBalance: Long? = null,
    @Json(name = "blockedBalance") val blockedBalance: Long? = null,
    @Json(name = "currency") val currency: String? = "IRR",
    @Json(name = "depositTitle") val depositTitle: String? = null,
    @Json(name = "depositStatus") val depositStatus: String? = null
)

/**
 * Result payload for Finotech Account Statement (`GET /oak/v2/clients/{clientId}/deposits/{deposit}/cc/statement`).
 * Matches Finotech's `statements` array and zero-padded Rial string fields.
 */
@JsonClass(generateAdapter = true)
data class FinotechStatementsResultDto(
    @Json(name = "deposit") val deposit: String? = null,
    @Json(name = "statements") val statements: List<FinotechStatementItemDto> = emptyList(),
    @Json(name = "transactionChain") val transactionChain: String? = null,
    @Json(name = "lastRecordNumber") val lastRecordNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class FinotechStatementItemDto(
    @Json(name = "rowNumber") val rowNumber: String? = null,
    @Json(name = "transactionDate") val transactionDate: String? = null,
    @Json(name = "effectiveDate") val effectiveDate: String? = null,
    @Json(name = "transactionTime") val transactionTime: String? = null,
    @Json(name = "branchCode") val branchCode: String? = null,
    @Json(name = "branchName") val branchName: String? = null,
    @Json(name = "serialNumber") val serialNumber: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "balance") val balance: String? = null,
    @Json(name = "balanceSign") val balanceSign: String? = null,
    @Json(name = "transactionAmountCredit") val transactionAmountCredit: String? = null,
    @Json(name = "transactionAmountDebit") val transactionAmountDebit: String? = null,
    @Json(name = "documentNumber") val documentNumber: String? = null,
    @Json(name = "paymentId") val paymentId: String? = null
)

@JsonClass(generateAdapter = true)
data class FinotechIbanInquiryResultDto(
    @Json(name = "IBAN") val iban: String,
    @Json(name = "bankName") val bankName: String? = null,
    @Json(name = "deposit") val deposit: String? = null,
    @Json(name = "depositStatus") val depositStatus: String? = null,
    @Json(name = "depositOwners") val depositOwners: List<FinotechOwnerDto>? = null
)

@JsonClass(generateAdapter = true)
data class FinotechOwnerDto(
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null
)

@JsonClass(generateAdapter = true)
data class FinotechTransferRequestDto(
    @Json(name = "amount") val amount: Long,
    @Json(name = "description") val description: String,
    @Json(name = "destinationFirstname") val destinationFirstName: String,
    @Json(name = "destinationLastname") val destinationLastName: String,
    @Json(name = "destinationNumber") val destinationNumber: String,
    @Json(name = "paymentNumber") val paymentNumber: String? = null,
    @Json(name = "sourceDeposit") val sourceDeposit: String,
    @Json(name = "reasonDescription") val reasonDescription: String = "02"
)

@JsonClass(generateAdapter = true)
data class FinotechTransferResultDto(
    @Json(name = "refCode") val refCode: String? = null,
    @Json(name = "traceNumber") val traceNumber: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "transactionDate") val transactionDate: String? = null
)
