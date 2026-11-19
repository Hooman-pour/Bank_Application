package com.example.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: BankError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

sealed interface BankError {
    val message: String

    data class MissingCredentials(
        val providerName: String,
        override val message: String
    ) : BankError

    data class Unauthorized(
        override val message: String = "نشست بانکداری باز منقضی شده یا هنوز احراز هویت نشده است."
    ) : BankError

    data class Network(
        override val message: String = "برقراری ارتباط با درگاه بانکداری باز ممکن نشد. اتصال اینترنت را بررسی کنید."
    ) : BankError

    data class ProviderApi(
        val httpCode: Int,
        val providerCode: String? = null,
        val trackId: String? = null,
        override val message: String
    ) : BankError

    data class Validation(
        override val message: String
    ) : BankError

    data class Unknown(
        override val message: String = "خطای غیرمنتظره‌ای رخ داد."
    ) : BankError
}
