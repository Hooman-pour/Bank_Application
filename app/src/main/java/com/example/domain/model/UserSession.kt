package com.example.domain.model

data class UserSession(
    val provider: OpenBankingProvider,
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochMs: Long,
    val clientId: String,
    val nationalId: String? = null,
    val scopes: List<String> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs >= expiresAtEpochMs - EXPIRY_BUFFER_MS

    fun maskedAccessToken(): String {
        if (accessToken.length <= 12) return "••••••••"
        return "${accessToken.take(6)}••••••${accessToken.takeLast(4)}"
    }

    private companion object {
        const val EXPIRY_BUFFER_MS = 30_000L
    }
}
