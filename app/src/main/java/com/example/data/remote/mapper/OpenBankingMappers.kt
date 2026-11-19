package com.example.data.remote.mapper

import com.example.core.common.IranianBankRegistry
import com.example.data.remote.faraboom.FaraboomDepositDto
import com.example.data.remote.faraboom.FaraboomIbanInquiryResponseDto
import com.example.data.remote.faraboom.FaraboomStatementDto
import com.example.data.remote.faraboom.FaraboomTransferResponseDto
import com.example.data.remote.finotech.FinotechDepositBalanceResultDto
import com.example.data.remote.finotech.FinotechDepositDto
import com.example.data.remote.finotech.FinotechIbanInquiryResultDto
import com.example.data.remote.finotech.FinotechStatementItemDto
import com.example.data.remote.finotech.FinotechTransferResultDto
import com.example.domain.model.AccountStatus
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransactionDirection
import com.example.domain.model.TransferRail
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest

object OpenBankingMappers {

    fun FinotechDepositDto.toDomain(): BankAccount {
        val resolvedIban = iban.orEmpty()
        val bank = IranianBankRegistry.findBySheba(resolvedIban)
        return BankAccount(
            depositNumber = depositNumber,
            iban = resolvedIban,
            cardNumberMasked = cardNumber?.let(::maskCard),
            bankName = bank?.englishName ?: "Iranian Deposit Account",
            bankCode = bank?.code ?: "000",
            accountTitle = depositTitle ?: "Current Account",
            balanceRial = balance ?: 0L,
            withdrawableBalanceRial = availableBalance ?: balance ?: 0L,
            status = mapAccountStatus(depositStatus),
            provider = OpenBankingProvider.FINOTECH
        )
    }

    fun FinotechDepositBalanceResultDto.toDomain(fallbackDepositNumber: String): BankAccount {
        val resolvedDeposit = deposit ?: fallbackDepositNumber
        val resolvedIban = iban.orEmpty()
        val bank = IranianBankRegistry.findBySheba(resolvedIban)
        val total = balance ?: 0L
        val withdrawable = availableBalance ?: (total - (blockedBalance ?: 0L)).coerceAtLeast(0L)
        return BankAccount(
            depositNumber = resolvedDeposit,
            iban = resolvedIban,
            cardNumberMasked = null,
            bankName = bank?.englishName ?: "Iranian Deposit Account",
            bankCode = bank?.code ?: "000",
            accountTitle = depositTitle ?: "Deposit $resolvedDeposit",
            balanceRial = total,
            withdrawableBalanceRial = withdrawable,
            status = mapAccountStatus(depositStatus),
            provider = OpenBankingProvider.FINOTECH
        )
    }

    fun FaraboomDepositDto.toDomain(): BankAccount {
        val resolvedIban = iban.orEmpty()
        val bank = IranianBankRegistry.findBySheba(resolvedIban)
        return BankAccount(
            depositNumber = depositNumber,
            iban = resolvedIban,
            cardNumberMasked = cardNumber?.let(::maskCard),
            bankName = bank?.englishName ?: "Iranian Deposit Account",
            bankCode = bank?.code ?: "000",
            accountTitle = depositTitle ?: "Current Account",
            balanceRial = balance ?: 0L,
            withdrawableBalanceRial = availableBalance ?: balance ?: 0L,
            status = mapAccountStatus(status),
            provider = OpenBankingProvider.FARABOOM
        )
    }

    fun FinotechStatementItemDto.toDomain(depositNumber: String): BankTransaction {
        val creditRial = parsePaddedRial(transactionAmountCredit)
        val debitRial = parsePaddedRial(transactionAmountDebit)
        val isCredit = when {
            creditRial > 0L -> true
            debitRial > 0L -> false
            else -> balanceSign.equals("C", ignoreCase = true)
        }
        val txAmount = if (isCredit) creditRial else debitRial
        val parsedBalance = balance?.let(::parsePaddedRial)

        val datePart = transactionDate ?: effectiveDate.orEmpty()
        val formattedDate = if (transactionTime.isNullOrBlank()) datePart else "$datePart • $transactionTime"

        return BankTransaction(
            id = "${rowNumber.orEmpty()}_${serialNumber.orEmpty()}_${documentNumber.orEmpty()}_${datePart}_${txAmount}",
            depositNumber = depositNumber,
            amountRial = txAmount,
            balanceAfterRial = parsedBalance,
            direction = if (isCredit) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
            description = description?.takeIf { it.isNotBlank() } ?: branchName ?: "Banking transaction",
            dateLabel = formattedDate,
            referenceNumber = documentNumber ?: serialNumber ?: paymentId,
            rail = null
        )
    }

    fun FaraboomStatementDto.toDomain(depositNumber: String): BankTransaction = BankTransaction(
        id = transactionId ?: referenceNumber ?: "${transactionDate}_${amount}",
        depositNumber = depositNumber,
        amountRial = kotlin.math.abs(amount),
        balanceAfterRial = balance,
        direction = if (side.equals("CREDIT", ignoreCase = true) || side.equals("DEPOSIT", ignoreCase = true)) {
            TransactionDirection.CREDIT
        } else {
            TransactionDirection.DEBIT
        },
        description = description ?: "Banking transaction",
        dateLabel = transactionDate.orEmpty(),
        referenceNumber = referenceNumber,
        rail = null
    )

    fun FinotechIbanInquiryResultDto.toDomain(): IbanInquiryResult {
        val bank = IranianBankRegistry.findBySheba(iban)
        val ownersJoined = depositOwners
            ?.joinToString(", ") { listOfNotNull(it.firstName, it.lastName).joinToString(" ").trim() }
            ?.takeIf { it.isNotBlank() }
            ?: "Verified Account Holder"
        return IbanInquiryResult(
            iban = iban,
            bankName = bankName ?: bank?.englishName ?: "Unknown Iranian Bank",
            bankCode = bank?.code ?: iban.drop(4).take(3),
            ownerName = ownersJoined,
            depositNumber = deposit,
            depositStatus = depositStatus ?: "ACTIVE",
            provider = OpenBankingProvider.FINOTECH
        )
    }

    fun FaraboomIbanInquiryResponseDto.toDomain(): IbanInquiryResult {
        val bank = IranianBankRegistry.findBySheba(iban)
        val ownersJoined = owners
            ?.joinToString(", ") { listOfNotNull(it.firstName, it.lastName).joinToString(" ").trim() }
            ?.takeIf { it.isNotBlank() }
            ?: "Verified Account Holder"
        return IbanInquiryResult(
            iban = iban,
            bankName = bankName ?: bank?.englishName ?: "Unknown Iranian Bank",
            bankCode = bank?.code ?: iban.drop(4).take(3),
            ownerName = ownersJoined,
            depositNumber = depositNumber,
            depositStatus = depositStatus ?: "ACTIVE",
            provider = OpenBankingProvider.FARABOOM
        )
    }

    fun FinotechTransferResultDto.toDomain(request: TransferRequest): TransferReceipt = TransferReceipt(
        trackingId = traceNumber ?: request.idempotencyTrackId,
        referenceNumber = refCode ?: traceNumber ?: request.idempotencyTrackId,
        rail = request.rail,
        amountRial = request.amountRial,
        destinationMasked = maskDestination(request.destinationIbanOrCard, request.rail),
        status = message ?: "Submitted",
        timestampLabel = transactionDate ?: "Just now",
        provider = OpenBankingProvider.FINOTECH
    )

    fun FaraboomTransferResponseDto.toDomain(request: TransferRequest): TransferReceipt = TransferReceipt(
        trackingId = trackingCode ?: request.idempotencyTrackId,
        referenceNumber = referenceId ?: trackingCode ?: request.idempotencyTrackId,
        rail = request.rail,
        amountRial = request.amountRial,
        destinationMasked = maskDestination(request.destinationIbanOrCard, request.rail),
        status = status ?: "Submitted",
        timestampLabel = registerDate ?: "Just now",
        provider = OpenBankingProvider.FARABOOM
    )

    private fun parsePaddedRial(raw: String?): Long =
        raw?.trim()?.toLongOrNull() ?: 0L

    private fun mapAccountStatus(raw: String?): AccountStatus = when (raw?.uppercase()) {
        "DORMANT", "INACTIVE" -> AccountStatus.DORMANT
        "FROZEN", "BLOCKED", "LOCKED" -> AccountStatus.FROZEN
        else -> AccountStatus.ACTIVE
    }

    private fun maskCard(card: String): String {
        val digits = card.filter(Char::isDigit)
        return if (digits.length >= 10) "${digits.take(6)}••••${digits.takeLast(4)}" else card
    }

    private fun maskDestination(dest: String, rail: TransferRail): String {
        return if (rail == TransferRail.CARD_TO_CARD) {
            maskCard(dest)
        } else {
            if (dest.length > 10) "${dest.take(6)}••••${dest.takeLast(4)}" else dest
        }
    }
}
