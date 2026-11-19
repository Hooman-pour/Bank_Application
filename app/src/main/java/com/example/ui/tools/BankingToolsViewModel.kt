package com.example.ui.tools

import androidx.lifecycle.ViewModel
import com.example.core.common.CurrencyFormatter
import com.example.core.common.IranianBankInfo
import com.example.core.common.IranianBankRegistry
import com.example.core.common.IranianValidators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class ValidationInspectorState(
    val input: String = "",
    val detectedMode: String = "شماره کارت ۱۶ رقمی شتاب یا شماره شبا ۲۶ کاراکتری (IR...) را وارد کنید",
    val isValid: Boolean = false,
    val formattedValue: String = "",
    val bankInfo: IranianBankInfo? = null,
    val bankDirectory: List<IranianBankInfo> = IranianBankRegistry.allSupportedBanks(),
    val rialInput: String = "",
    val tomanEquivalent: String = "۰"
)

@HiltViewModel
class BankingToolsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ValidationInspectorState())
    val uiState: StateFlow<ValidationInspectorState> = _uiState.asStateFlow()

    fun onIdentifierChanged(raw: String) {
        val normalized = IranianValidators.normalizeDigits(raw)
        val trimmed = normalized.trim().uppercase()

        if (trimmed.startsWith("IR") || trimmed.any { it.isLetter() }) {
            val shebaClean = IranianValidators.normalizeSheba(trimmed)
            val valid = IranianValidators.isValidSheba(shebaClean)
            val bank = IranianBankRegistry.findBySheba(shebaClean)
            _uiState.update {
                it.copy(
                    input = raw,
                    detectedMode = "شماره شبا (ISO 7064 Mod-97-10) • ${CurrencyFormatter.toPersianDigits(shebaClean.length.toString())} از ۲۶ کاراکتر",
                    isValid = valid,
                    formattedValue = IranianValidators.formatSheba(shebaClean),
                    bankInfo = bank
                )
            }
        } else {
            val digits = normalized.filter(Char::isDigit)
            val valid = IranianValidators.isValidShetabCard(digits)
            val bank = IranianBankRegistry.findByCardNumber(digits)
            _uiState.update {
                it.copy(
                    input = raw,
                    detectedMode = "کارت بانکی شتاب (الگوریتم Luhn) • ${CurrencyFormatter.toPersianDigits(digits.length.toString())} از ۱۶ رقم",
                    isValid = valid,
                    formattedValue = IranianValidators.formatCardNumber(digits),
                    bankInfo = bank
                )
            }
        }
    }

    fun onRialInputChanged(raw: String) {
        val digits = IranianValidators.normalizeDigits(raw).filter(Char::isDigit)
        val rial = digits.toLongOrNull() ?: 0L
        val toman = rial / 10L
        val formattedToman = NumberFormat.getNumberInstance(Locale.US).format(toman)
        _uiState.update {
            it.copy(
                rialInput = digits,
                tomanEquivalent = CurrencyFormatter.toPersianDigits(formattedToman)
            )
        }
    }
}
