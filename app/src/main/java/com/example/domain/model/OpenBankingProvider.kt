package com.example.domain.model

enum class OpenBankingProvider(
    val displayName: String,
    val persianName: String,
    val baseUrl: String,
    val authEndpointHint: String,
    val apiVersion: String,
    val persianApiVersion: String
) {
    FREE_SANDBOX(
        displayName = "Free Mode",
        persianName = "حالت رایگان (بدون کلید)",
        baseUrl = "local://zarbank.free/",
        authEndpointHint = "local://free-session",
        apiVersion = "Offline Free Ledger",
        persianApiVersion = "سرویس داخلی رایگان • بدون نیاز به کلید API"
    ),
    FINOTECH(
        displayName = "Finotech",
        persianName = "فینوتک",
        baseUrl = "https://apibeta.finnotech.ir/",
        authEndpointHint = "/dev/v2/oauth2/authorize",
        apiVersion = "v2 (OAK / Facility)",
        persianApiVersion = "نسخه ۲ (نیازمند کلید فینوتک)"
    ),
    FARABOOM(
        displayName = "Faraboom",
        persianName = "فرابوم",
        baseUrl = "https://api.faraboom.co/",
        authEndpointHint = "/v1/oauth/authorize",
        apiVersion = "v1 (Open Banking)",
        persianApiVersion = "نسخه ۱ (نیازمند کلید فرابوم)"
    )
}
