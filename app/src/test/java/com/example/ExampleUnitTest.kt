package com.example

import com.example.core.common.CurrencyDisplayUnit
import com.example.core.common.CurrencyFormatter
import com.example.core.common.IranianBankRegistry
import com.example.core.common.IranianValidators
import com.example.data.remote.finotech.FinotechDepositBalanceResultDto
import com.example.data.remote.finotech.FinotechEnvelope
import com.example.data.remote.finotech.FinotechStatementsResultDto
import com.example.data.remote.mapper.OpenBankingMappers.toDomain
import com.example.domain.model.TransactionDirection
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `persian digits normalize to latin digits`() {
        assertEquals("IR820540102680020817909002", IranianValidators.normalizeSheba("ir۸۲۰۵۴۰۱۰۲۶۸۰۰۲۰۸۱۷۹۰۹۰۰۲"))
    }

    @Test
    fun `validates authentic Iranian Sheba Mod-97 and detects bank`() {
        val sampleValidSheba = "IR820540102680020817909002"
        assertTrue(IranianValidators.isValidSheba(sampleValidSheba))
        val bank = IranianBankRegistry.findBySheba(sampleValidSheba)
        assertEquals("Parsian Bank", bank?.englishName)
        assertFalse(IranianValidators.isValidSheba("IR000540102680020817909002"))
    }

    @Test
    fun `validates Shetab Luhn card checksum and BIN lookup`() {
        val card = "6219861000000009"
        assertTrue(IranianValidators.isValidShetabCard(card))
        assertEquals("Saman Bank", IranianBankRegistry.findByCardNumber(card)?.englishName)
        assertFalse(IranianValidators.isValidShetabCard("6219861000000008"))
    }

    @Test
    fun `formats and parses Rial and Toman accurately`() {
        assertEquals("150,000 IRT", CurrencyFormatter.formatAmount(1_500_000L, CurrencyDisplayUnit.TOMAN))
        assertEquals("1,500,000 IRR", CurrencyFormatter.formatAmount(1_500_000L, CurrencyDisplayUnit.RIAL))
        assertEquals(2_500_000L, CurrencyFormatter.parseRialInput("۲۵۰,۰۰۰", CurrencyDisplayUnit.TOMAN))
    }

    @Test
    fun `deserializes Finotech Oak statement JSON and maps zero-padded amounts`() {
        val json = """
            {
              "status": "DONE",
              "trackId": "billingInquiry876543",
              "result": {
                "deposit": "10023456789",
                "statements": [
                  {
                    "rowNumber": "1",
                    "transactionDate": "000510",
                    "effectiveDate": "000510",
                    "branchCode": "1234",
                    "branchName": "Tehran Central",
                    "serialNumber": "000123",
                    "description": "Paya settlement",
                    "balance": "0000000000166895",
                    "balanceSign": "C",
                    "transactionAmountCredit": "0000000000000000",
                    "transactionAmountDebit": "0000000000012000",
                    "documentNumber": "567891"
                  }
                ],
                "transactionChain": "N",
                "lastRecordNumber": ""
              },
              "error": null
            }
        """.trimIndent()

        val type = Types.newParameterizedType(
            FinotechEnvelope::class.java,
            FinotechStatementsResultDto::class.java
        )
        val adapter = moshi.adapter<FinotechEnvelope<FinotechStatementsResultDto>>(type)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals("DONE", parsed?.status)
        assertEquals("billingInquiry876543", parsed?.trackId)

        val tx = parsed?.result?.statements?.first()?.toDomain("10023456789")
        assertNotNull(tx)
        assertEquals(12_000L, tx?.amountRial)
        assertEquals(166_895L, tx?.balanceAfterRial)
        assertEquals(TransactionDirection.DEBIT, tx?.direction)
        assertEquals("567891", tx?.referenceNumber)
    }

    @Test
    fun `deserializes Finotech Oak deposit balance JSON and maps to BankAccount`() {
        val json = """
            {
              "status": "DONE",
              "trackId": "bal-track-001",
              "result": {
                "deposit": "10023456789",
                "iban": "IR820540102680020817909002",
                "balance": 54000000,
                "availableBalance": 52000000,
                "blockedBalance": 2000000,
                "currency": "IRR",
                "depositStatus": "ACTIVE"
              },
              "error": null
            }
        """.trimIndent()

        val type = Types.newParameterizedType(
            FinotechEnvelope::class.java,
            FinotechDepositBalanceResultDto::class.java
        )
        val adapter = moshi.adapter<FinotechEnvelope<FinotechDepositBalanceResultDto>>(type)
        val parsed = adapter.fromJson(json)
        val account = parsed?.result?.toDomain("10023456789")
        assertNotNull(account)
        assertEquals("Parsian Bank", account?.bankName)
        assertEquals(54_000_000L, account?.balanceRial)
        assertEquals(52_000_000L, account?.withdrawableBalanceRial)
    }
}
