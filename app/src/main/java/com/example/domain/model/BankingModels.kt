package com.example.domain.model

data class BankAccount(
    val depositNumber: String,
    val iban: String,
    val cardNumberMasked: String?,
    val bankName: String,
    val bankCode: String,
    val accountTitle: String,
    val balanceRial: Long,
    val withdrawableBalanceRial: Long,
    val status: AccountStatus,
    val provider: OpenBankingProvider
)

enum class AccountStatus {
    ACTIVE,
    DORMANT,
    FROZEN
}

enum class TransactionDirection {
    CREDIT,
    DEBIT
}

data class BankTransaction(
    val id: String,
    val depositNumber: String,
    val amountRial: Long,
    val balanceAfterRial: Long?,
    val direction: TransactionDirection,
    val description: String,
    val dateLabel: String,
    val referenceNumber: String?,
    val rail: TransferRail?
)

data class IbanInquiryResult(
    val iban: String,
    val bankName: String,
    val bankCode: String,
    val ownerName: String,
    val depositNumber: String?,
    val depositStatus: String,
    val provider: OpenBankingProvider
)

enum class TransferRail(
    val title: String,
    val persianTitle: String,
    val subtitle: String,
    val persianSubtitle: String,
    val requiresSheba: Boolean
) {
    CARD_TO_CARD(
        title = "Card to Card",
        persianTitle = "کارت به کارت",
        subtitle = "Instant Shetab rail up to 100M IRR",
        persianSubtitle = "انتقال آنی شتاب تا سقف ۱۰ میلیون تومان",
        requiresSheba = false
    ),
    PAYA_ACH(
        title = "Paya (ACH)",
        persianTitle = "پایا",
        subtitle = "Batch Sheba clearing cycle",
        persianSubtitle = "انتقال بین‌بانکی پایا در چرخه‌های تسویه",
        requiresSheba = true
    ),
    SATNA_RTGS(
        title = "Satna (RTGS)",
        persianTitle = "ساتنا",
        subtitle = "Real-time gross settlement (> 500M IRR)",
        persianSubtitle = "تسویه آنی مبالغ کلان (بالای ۵۰ میلیون تومان)",
        requiresSheba = true
    ),
    POL_INSTANT(
        title = "Pol (Instant)",
        persianTitle = "پل",
        subtitle = "24/7 instant Sheba transfer",
        persianSubtitle = "انتقال لحظه‌ای شبا در تمام ساعات شبانه‌روز",
        requiresSheba = true
    )
}

data class TransferRequest(
    val sourceDepositOrCard: String,
    val destinationIbanOrCard: String,
    val recipientName: String?,
    val amountRial: Long,
    val rail: TransferRail,
    val description: String,
    val idempotencyTrackId: String
)

data class TransferReceipt(
    val trackingId: String,
    val referenceNumber: String,
    val rail: TransferRail,
    val amountRial: Long,
    val destinationMasked: String,
    val status: String,
    val timestampLabel: String,
    val provider: OpenBankingProvider
)
