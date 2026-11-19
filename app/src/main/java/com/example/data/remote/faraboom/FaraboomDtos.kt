package com.example.data.remote.faraboom

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FaraboomTokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresInSeconds: Long = 3600L,
    @Json(name = "scope") val scope: String? = null
)

@JsonClass(generateAdapter = true)
data class FaraboomDepositsResponseDto(
    @Json(name = "deposits") val deposits: List<FaraboomDepositDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FaraboomDepositDto(
    @Json(name = "deposit_number") val depositNumber: String,
    @Json(name = "iban") val iban: String? = null,
    @Json(name = "deposit_title") val depositTitle: String? = null,
    @Json(name = "balance") val balance: Long? = null,
    @Json(name = "available_balance") val availableBalance: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "card_number") val cardNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class FaraboomStatementsResponseDto(
    @Json(name = "statements") val statements: List<FaraboomStatementDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FaraboomStatementDto(
    @Json(name = "transaction_id") val transactionId: String? = null,
    @Json(name = "amount") val amount: Long,
    @Json(name = "balance") val balance: Long? = null,
    @Json(name = "side") val side: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "transaction_date") val transactionDate: String? = null,
    @Json(name = "reference_number") val referenceNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class FaraboomIbanInquiryResponseDto(
    @Json(name = "iban") val iban: String,
    @Json(name = "bank_name") val bankName: String? = null,
    @Json(name = "deposit_number") val depositNumber: String? = null,
    @Json(name = "deposit_status") val depositStatus: String? = null,
    @Json(name = "owners") val owners: List<FaraboomOwnerDto>? = null
)

@JsonClass(generateAdapter = true)
data class FaraboomOwnerDto(
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null
)

@JsonClass(generateAdapter = true)
data class FaraboomAchTransferRequestDto(
    @Json(name = "source_deposit_number") val sourceDepositNumber: String,
    @Json(name = "destination_iban") val destinationIban: String,
    @Json(name = "owner_name") val ownerName: String,
    @Json(name = "amount") val amount: Long,
    @Json(name = "description") val description: String,
    @Json(name = "reason_code") val reasonCode: String = "GPPC"
)

@JsonClass(generateAdapter = true)
data class FaraboomCardTransferRequestDto(
    @Json(name = "source_card") val sourceCard: String,
    @Json(name = "destination_card") val destinationCard: String,
    @Json(name = "amount") val amount: Long,
    @Json(name = "description") val description: String
)

@JsonClass(generateAdapter = true)
data class FaraboomTransferResponseDto(
    @Json(name = "tracking_code") val trackingCode: String? = null,
    @Json(name = "reference_id") val referenceId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "register_date") val registerDate: String? = null
)
