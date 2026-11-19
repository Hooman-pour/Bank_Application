package com.example.core.common

import java.text.NumberFormat
import java.util.Locale

enum class CurrencyDisplayUnit(
    val suffix: String,
    val label: String,
    val persianSuffix: String,
    val persianLabel: String
) {
    TOMAN("IRT", "Toman", "تومان", "تومان"),
    RIAL("IRR", "Rial", "ریال", "ریال")
}

object CurrencyFormatter {

    private val numberFormat: NumberFormat
        get() = NumberFormat.getNumberInstance(Locale.US)

    fun formatAmount(amountRial: Long, unit: CurrencyDisplayUnit): String {
        val displayValue = when (unit) {
            CurrencyDisplayUnit.RIAL -> amountRial
            CurrencyDisplayUnit.TOMAN -> amountRial / 10L
        }
        return "${numberFormat.format(displayValue)} ${unit.suffix}"
    }

    fun formatAmountPersian(amountRial: Long, unit: CurrencyDisplayUnit): String {
        val displayValue = when (unit) {
            CurrencyDisplayUnit.RIAL -> amountRial
            CurrencyDisplayUnit.TOMAN -> amountRial / 10L
        }
        return "${toPersianDigits(numberFormat.format(displayValue))} ${unit.persianSuffix}"
    }

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return buildString(input.length) {
            input.forEach { ch ->
                if (ch in '0'..'9') {
                    append(persianDigits[ch - '0'])
                } else if (ch == ',') {
                    append('،')
                } else {
                    append(ch)
                }
            }
        }
    }

    fun parseRialInput(raw: String, inputUnit: CurrencyDisplayUnit): Long? {
        val digits = IranianValidators.normalizeDigits(raw).filter(Char::isDigit)
        val parsed = digits.toLongOrNull() ?: return null
        if (parsed <= 0L) return null
        return when (inputUnit) {
            CurrencyDisplayUnit.RIAL -> parsed
            CurrencyDisplayUnit.TOMAN -> parsed * 10L
        }
    }
}
