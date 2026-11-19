package com.example.core.common

import java.math.BigInteger

object IranianValidators {

    /**
     * Converts Persian (۰-۹) and Arabic-Indic (٠-٩) digits to ASCII (0-9).
     */
    fun normalizeDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> builder.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> builder.append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun normalizeSheba(input: String): String =
        normalizeDigits(input)
            .uppercase()
            .filter { it.isLetterOrDigit() }

    /**
     * Validates Iranian IBAN (Sheba): "IR" + 24 digits using ISO 7064 Mod-97-10.
     */
    fun isValidSheba(input: String): Boolean {
        val normalized = normalizeSheba(input)
        if (normalized.length != 26) return false
        if (!normalized.startsWith("IR")) return false
        if (!normalized.drop(2).all(Char::isDigit)) return false

        val rearranged = normalized.drop(4) + normalized.take(4)
        val numericString = buildString(rearranged.length * 2) {
            for (ch in rearranged) {
                if (ch.isDigit()) {
                    append(ch)
                } else {
                    append(ch.code - 'A'.code + 10)
                }
            }
        }
        return BigInteger(numericString).mod(BigInteger.valueOf(97)) == BigInteger.ONE
    }

    /**
     * Validates a 16-digit Iranian Shetab bank card number using the Luhn algorithm.
     */
    fun isValidShetabCard(input: String): Boolean {
        val digits = normalizeDigits(input).filter(Char::isDigit)
        if (digits.length != 16) return false
        var sum = 0
        for (i in digits.indices) {
            var digit = digits[i] - '0'
            if (i % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    fun formatCardNumber(input: String): String {
        val digits = normalizeDigits(input).filter(Char::isDigit).take(16)
        return digits.chunked(4).joinToString(" ")
    }

    fun formatSheba(input: String): String {
        val normalized = normalizeSheba(input).take(26)
        return normalized.chunked(4).joinToString(" ")
    }
}
