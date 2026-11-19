package com.example.data.remote.source

import android.content.Context
import com.example.core.common.IranianBankRegistry
import com.example.core.common.IranianValidators
import com.example.domain.model.AccountStatus
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.IbanInquiryResult
import com.example.domain.model.OpenBankingProvider
import com.example.domain.model.TransactionDirection
import com.example.domain.model.TransferRail
import com.example.domain.model.TransferReceipt
import com.example.domain.model.TransferRequest
import com.example.domain.model.UserSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreeSandboxRemoteDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) : OpenBankingRemoteDataSource {

    override val provider: OpenBankingProvider = OpenBankingProvider.FREE_SANDBOX

    private val lock = Any()
    private val prefs by lazy {
        context.getSharedPreferences(LEDGER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val accountListType = Types.newParameterizedType(List::class.java, BankAccount::class.java)
    private val txListType = Types.newParameterizedType(List::class.java, BankTransaction::class.java)

    override suspend fun exchangeAuthorizationCode(
        authorizationCode: String,
        redirectUri: String
    ): UserSession = createFreeSession()

    override suspend fun requestClientCredentialsToken(): UserSession = createFreeSession()

    override suspend fun refreshToken(refreshToken: String): UserSession = createFreeSession()

    override suspend fun fetchAccounts(clientId: String): List<BankAccount> = synchronized(lock) {
        ensureSeededLocked()
        loadAccountsLocked()
    }

    override suspend fun fetchAccountBalance(
        clientId: String,
        depositNumber: String
    ): BankAccount = synchronized(lock) {
        ensureSeededLocked()
        loadAccountsLocked().firstOrNull { it.depositNumber == depositNumber }
            ?: throw IllegalStateException("سپرده $depositNumber یافت نشد.")
    }

    override suspend fun fetchTransactions(
        clientId: String,
        depositNumber: String
    ): List<BankTransaction> = synchronized(lock) {
        ensureSeededLocked()
        loadTransactionsLocked().filter { it.depositNumber == depositNumber }
    }

    override suspend fun inquireIban(clientId: String, iban: String): IbanInquiryResult {
        val normalized = IranianValidators.normalizeSheba(iban)
        val bank = IranianBankRegistry.findBySheba(normalized)
        return IbanInquiryResult(
            iban = normalized,
            bankName = bank?.persianName ?: "بانک عضو شبکه شبا",
            bankCode = bank?.code ?: normalized.drop(4).take(3),
            ownerName = "حساب تاییدشده (حالت رایگان)",
            depositNumber = normalized.takeLast(10),
            depositStatus = "فعال",
            provider = OpenBankingProvider.FREE_SANDBOX
        )
    }

    override suspend fun executeTransfer(
        clientId: String,
        request: TransferRequest
    ): TransferReceipt = synchronized(lock) {
        ensureSeededLocked()
        val accounts = loadAccountsLocked().toMutableList()
        val txs = loadTransactionsLocked().toMutableList()

        val sourceIndex = accounts.indexOfFirst {
            it.depositNumber == request.sourceDepositOrCard ||
                it.cardNumberMasked?.replace("•", "")?.take(6) == request.sourceDepositOrCard.take(6)
        }.let { idx -> if (idx >= 0) idx else 0 }

        val source = accounts.getOrNull(sourceIndex)
            ?: throw IllegalStateException("حساب مبدا یافت نشد.")

        if (source.balanceRial < request.amountRial) {
            throw IllegalStateException("موجودی حساب مبدا برای انجام این انتقال کافی نیست.")
        }

        val updatedBalance = source.balanceRial - request.amountRial
        val updatedWithdrawable = (source.withdrawableBalanceRial - request.amountRial).coerceAtLeast(0L)
        accounts[sourceIndex] = source.copy(
            balanceRial = updatedBalance,
            withdrawableBalanceRial = updatedWithdrawable
        )

        val refNum = (100000..999999).random().toString()
        val newTx = BankTransaction(
            id = "free_tx_${UUID.randomUUID().toString().take(8)}",
            depositNumber = source.depositNumber,
            amountRial = request.amountRial,
            balanceAfterRial = updatedBalance,
            direction = TransactionDirection.DEBIT,
            description = request.description.ifBlank { "انتقال ${request.rail.persianTitle}" },
            dateLabel = "امروز • ثبت آنی",
            referenceNumber = refNum,
            rail = request.rail
        )
        txs.add(0, newTx)

        saveAccountsLocked(accounts)
        saveTransactionsLocked(txs)

        TransferReceipt(
            trackingId = request.idempotencyTrackId,
            referenceNumber = refNum,
            rail = request.rail,
            amountRial = request.amountRial,
            destinationMasked = maskDestination(request.destinationIbanOrCard, request.rail),
            status = "ثبت موفق (آفلاین/رایگان)",
            timestampLabel = "هم‌اکنون",
            provider = OpenBankingProvider.FREE_SANDBOX
        )
    }

    private fun createFreeSession(): UserSession = UserSession(
        provider = OpenBankingProvider.FREE_SANDBOX,
        accessToken = "free_zarbank_${UUID.randomUUID().toString().take(12)}",
        refreshToken = "free_refresh_${UUID.randomUUID().toString().take(8)}",
        expiresAtEpochMs = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
        clientId = "zarbank-free-local",
        scopes = listOf(
            "oak:deposits:get",
            "oak:balance:get",
            "oak:cc-statement:get",
            "facility:iban-inquiry:get"
        )
    )

    private fun ensureSeededLocked() {
        if (prefs.contains(KEY_ACCOUNTS) && prefs.contains(KEY_TRANSACTIONS)) return
        saveAccountsLocked(defaultSeedAccounts())
        saveTransactionsLocked(defaultSeedTransactions())
    }

    private fun loadAccountsLocked(): List<BankAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return defaultSeedAccounts()
        val adapter = moshi.adapter<List<BankAccount>>(accountListType)
        return adapter.fromJson(raw).orEmpty()
    }

    private fun saveAccountsLocked(accounts: List<BankAccount>) {
        val adapter = moshi.adapter<List<BankAccount>>(accountListType)
        prefs.edit().putString(KEY_ACCOUNTS, adapter.toJson(accounts)).apply()
    }

    private fun loadTransactionsLocked(): List<BankTransaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return defaultSeedTransactions()
        val adapter = moshi.adapter<List<BankTransaction>>(txListType)
        return adapter.fromJson(raw).orEmpty()
    }

    private fun saveTransactionsLocked(txs: List<BankTransaction>) {
        val adapter = moshi.adapter<List<BankTransaction>>(txListType)
        prefs.edit().putString(KEY_TRANSACTIONS, adapter.toJson(txs)).apply()
    }

    private fun maskDestination(dest: String, rail: TransferRail): String {
        val clean = dest.trim()
        return if (rail == TransferRail.CARD_TO_CARD) {
            val digits = clean.filter(Char::isDigit)
            if (digits.length >= 10) "${digits.take(6)}••••${digits.takeLast(4)}" else clean
        } else {
            if (clean.length > 10) "${clean.take(6)}••••${clean.takeLast(4)}" else clean
        }
    }

    private fun defaultSeedAccounts(): List<BankAccount> = listOf(
        BankAccount(
            depositNumber = "100-800-452190-1",
            iban = "IR640560611828005166088001",
            cardNumberMasked = "621986••••4102",
            bankName = "بانک سامان",
            bankCode = "056",
            accountTitle = "سپرده جاری دیجیتال",
            balanceRial = 485_000_000L,
            withdrawableBalanceRial = 480_000_000L,
            status = AccountStatus.ACTIVE,
            provider = OpenBankingProvider.FREE_SANDBOX
        ),
        BankAccount(
            depositNumber = "201-440-981230-4",
            iban = "IR820540102680020817909002",
            cardNumberMasked = "622106••••8831",
            bankName = "بانک پارسیان",
            bankCode = "054",
            accountTitle = "حساب پس‌انداز کوتاه‌مدت",
            balanceRial = 162_500_000L,
            withdrawableBalanceRial = 162_500_000L,
            status = AccountStatus.ACTIVE,
            provider = OpenBankingProvider.FREE_SANDBOX
        ),
        BankAccount(
            depositNumber = "031-900-774120-8",
            iban = "IR270170000000109248810003",
            cardNumberMasked = "603799••••1954",
            bankName = "بانک ملی ایران",
            bankCode = "017",
            accountTitle = "حساب قرض‌الحسنه",
            balanceRial = 74_200_000L,
            withdrawableBalanceRial = 74_200_000L,
            status = AccountStatus.ACTIVE,
            provider = OpenBankingProvider.FREE_SANDBOX
        )
    )

    private fun defaultSeedTransactions(): List<BankTransaction> = listOf(
        BankTransaction(
            id = "seed_tx_1",
            depositNumber = "100-800-452190-1",
            amountRial = 185_000_000L,
            balanceAfterRial = 485_000_000L,
            direction = TransactionDirection.CREDIT,
            description = "واریز حقوق و مزایا (پایا)",
            dateLabel = "1403/08/28 • 09:15",
            referenceNumber = "849201",
            rail = TransferRail.PAYA_ACH
        ),
        BankTransaction(
            id = "seed_tx_2",
            depositNumber = "100-800-452190-1",
            amountRial = 14_500_000L,
            balanceAfterRial = 300_000_000L,
            direction = TransactionDirection.DEBIT,
            description = "خرید اینترنتی شاپرک",
            dateLabel = "1403/08/27 • 18:42",
            referenceNumber = "731904",
            rail = TransferRail.CARD_TO_CARD
        ),
        BankTransaction(
            id = "seed_tx_3",
            depositNumber = "100-800-452190-1",
            amountRial = 35_000_000L,
            balanceAfterRial = 314_500_000L,
            direction = TransactionDirection.DEBIT,
            description = "انتقال وجه پایا به بانک پارسیان",
            dateLabel = "1403/08/25 • 11:05",
            referenceNumber = "612088",
            rail = TransferRail.PAYA_ACH
        ),
        BankTransaction(
            id = "seed_tx_4",
            depositNumber = "201-440-981230-4",
            amountRial = 35_000_000L,
            balanceAfterRial = 162_500_000L,
            direction = TransactionDirection.CREDIT,
            description = "دریافت حواله پایا",
            dateLabel = "1403/08/25 • 11:10",
            referenceNumber = "612089",
            rail = TransferRail.PAYA_ACH
        ),
        BankTransaction(
            id = "seed_tx_5",
            depositNumber = "201-440-981230-4",
            amountRial = 4_200_000L,
            balanceAfterRial = 127_500_000L,
            direction = TransactionDirection.DEBIT,
            description = "پرداخت قبض خدماتی",
            dateLabel = "1403/08/22 • 16:30",
            referenceNumber = "504412",
            rail = null
        ),
        BankTransaction(
            id = "seed_tx_6",
            depositNumber = "031-900-774120-8",
            amountRial = 12_000_000L,
            balanceAfterRial = 74_200_000L,
            direction = TransactionDirection.CREDIT,
            description = "کارت به کارت شتابی",
            dateLabel = "1403/08/20 • 13:18",
            referenceNumber = "448210",
            rail = TransferRail.CARD_TO_CARD
        )
    )

    private companion object {
        const val LEDGER_PREFS_NAME = "zarbank_free_ledger"
        const val KEY_ACCOUNTS = "accounts_json"
        const val KEY_TRANSACTIONS = "transactions_json"
    }
}
