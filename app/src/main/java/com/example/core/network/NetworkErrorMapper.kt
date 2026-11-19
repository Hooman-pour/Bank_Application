package com.example.core.network

import com.example.core.common.BankError
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorMapper {

    fun map(throwable: Throwable): BankError = when (throwable) {
        is HttpException -> mapHttpException(throwable)
        is UnknownHostException -> BankError.Network("دسترسی به سرور بانکداری باز برقرار نشد. اتصال اینترنت یا آدرس سرویس را بررسی کنید.")
        is SocketTimeoutException -> BankError.Network("زمان پاسخ‌گویی درگاه بانکداری باز به پایان رسید. لطفاً دوباره تلاش کنید.")
        is IOException -> BankError.Network(throwable.message ?: "خطای شبکه هنگام ارتباط با درگاه بانکی.")
        else -> BankError.Unknown(throwable.message ?: "خطای غیرمنتظره در عملیات بانکی رخ داد.")
    }

    private fun mapHttpException(ex: HttpException): BankError {
        val code = ex.code()
        if (code == 401 || code == 403) {
            return BankError.Unauthorized("درگاه بانکی اعتبار توکن یا کلید دسترسی را نپذیرفت (کد $code).")
        }

        val rawBody = try {
            ex.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        var parsedMessage: String? = null
        var providerCode: String? = null
        var trackId: String? = null

        if (!rawBody.isNullOrBlank()) {
            try {
                val json = JSONObject(rawBody)
                trackId = json.optString("trackId").takeIf { it.isNotBlank() }
                val errorObj = json.optJSONObject("error")
                if (errorObj != null) {
                    parsedMessage = errorObj.optString("message").takeIf { it.isNotBlank() }
                    providerCode = errorObj.optString("code").takeIf { it.isNotBlank() }
                }
                if (parsedMessage == null) {
                    parsedMessage = json.optString("message").takeIf { it.isNotBlank() }
                        ?: json.optString("error_description").takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) {
                // Non-JSON error body
            }
        }

        return BankError.ProviderApi(
            httpCode = code,
            providerCode = providerCode,
            trackId = trackId,
            message = parsedMessage ?: "خطای سرویس‌دهنده بانکی (کد HTTP $code)"
        )
    }
}
