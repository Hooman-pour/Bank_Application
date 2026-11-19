package com.example.core.common

data class IranianBankInfo(
    val code: String,
    val englishName: String,
    val persianName: String,
    val swiftBic: String,
    val accentHex: Long
)

object IranianBankRegistry {

    private val banksByShebaCode: Map<String, IranianBankInfo> = mapOf(
        "012" to IranianBankInfo("012", "Bank Mellat", "بانک ملت", "BKMTIRTH", 0xFFD32F2F),
        "013" to IranianBankInfo("013", "Bank Refah", "بانک رفاه کارگران", "REFAIRTH", 0xFF1565C0),
        "014" to IranianBankInfo("014", "Bank Maskan", "بانک مسکن", "BKMNIRTH", 0xFFE65100),
        "015" to IranianBankInfo("015", "Bank Sepah", "بانک سپه", "SEPBIRTH", 0xFFC62828),
        "016" to IranianBankInfo("016", "Bank Keshavarzi", "بانک کشاورزی", "KESHIRTH", 0xFF2E7D32),
        "017" to IranianBankInfo("017", "Bank Melli Iran", "بانک ملی ایران", "MELIIRTH", 0xFFB8860B),
        "018" to IranianBankInfo("018", "Bank Tejarat", "بانک تجارت", "BTEJIRTH", 0xFF0277BD),
        "019" to IranianBankInfo("019", "Bank Saderat", "بانک صادرات ایران", "BSIRIRTH", 0xFF0D47A1),
        "021" to IranianBankInfo("021", "Post Bank", "پست بانک ایران", "PBIRIRTH", 0xFF00695C),
        "054" to IranianBankInfo("054", "Parsian Bank", "بانک پارسیان", "BKPAIRTH", 0xFF8E24AA),
        "055" to IranianBankInfo("055", "Eghtesad Novin", "بانک اقتصاد نوین", "BEGNIRTH", 0xFF4527A0),
        "056" to IranianBankInfo("056", "Saman Bank", "بانک سامان", "SABCIRTH", 0xFF00838F),
        "057" to IranianBankInfo("057", "Bank Pasargad", "بانک پاسارگاد", "BKBPIRTH", 0xFFEF6C00),
        "058" to IranianBankInfo("058", "Sarmayeh Bank", "بانک سرمایه", "SRMBIRTH", 0xFF37474F),
        "059" to IranianBankInfo("059", "Sina Bank", "بانک سینا", "SINAIRTH", 0xFF00695C),
        "060" to IranianBankInfo("060", "Mehr Iran Bank", "بانک قرض‌الحسنه مهر ایران", "MEHRIRTH", 0xFF1B5E20),
        "061" to IranianBankInfo("061", "Shahr Bank", "بانک شهر", "CIYBIRTH", 0xFFD84315),
        "062" to IranianBankInfo("062", "Ayandeh Bank", "بانک آینده", "AYBKIRTH", 0xFF6A1B9A),
        "070" to IranianBankInfo("070", "Resalat Bank", "بانک رسالت", "RESAIRTH", 0xFF0288D1),
        "078" to IranianBankInfo("078", "Middle East Bank", "بانک خاورمیانه", "KHMIIRTH", 0xFF283593)
    )

    private val cardBinToShebaCode: Map<String, String> = mapOf(
        "603799" to "017", // Melli
        "610433" to "012", // Mellat
        "991975" to "012", // Mellat
        "621986" to "056", // Saman & Blu
        "502229" to "057", // Pasargad
        "639347" to "057", // Pasargad
        "622106" to "054", // Parsian
        "639194" to "054", // Parsian
        "627353" to "018", // Tejarat
        "585983" to "018", // Tejarat
        "589210" to "015", // Sepah
        "603770" to "016", // Keshavarzi
        "639217" to "016", // Keshavarzi
        "589463" to "013", // Refah
        "504706" to "061", // Shahr
        "502806" to "061", // Shahr
        "636214" to "062", // Ayandeh
        "639346" to "059", // Sina
        "606373" to "060", // Mehr Iran
        "603769" to "019", // Saderat
        "627412" to "055", // Eghtesad Novin
        "639607" to "058", // Sarmayeh
        "628023" to "014", // Maskan
        "627760" to "021", // Post Bank
        "504172" to "070", // Resalat
        "585947" to "078"  // Middle East
    )

    fun findBySheba(sheba: String): IranianBankInfo? {
        val normalized = IranianValidators.normalizeSheba(sheba)
        if (normalized.length < 7) return null
        val bankCode = normalized.substring(4, 7)
        return banksByShebaCode[bankCode]
    }

    fun findByCardNumber(cardNumber: String): IranianBankInfo? {
        val digits = IranianValidators.normalizeDigits(cardNumber).filter(Char::isDigit)
        if (digits.length < 6) return null
        val bin = digits.substring(0, 6)
        val shebaCode = cardBinToShebaCode[bin] ?: return null
        return banksByShebaCode[shebaCode]
    }

    fun allSupportedBanks(): List<IranianBankInfo> = banksByShebaCode.values.sortedBy { it.englishName }
}
