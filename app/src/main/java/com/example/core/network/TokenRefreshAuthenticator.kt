package com.example.core.network

import com.example.core.common.AppResult
import com.example.core.security.SessionManager
import com.example.domain.model.UserSession
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

interface SessionTokenRefresher {
    suspend fun refreshSession(): AppResult<UserSession>
}

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenRefresher: dagger.Lazy<SessionTokenRefresher>
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_RETRIES) {
            return null
        }

        val failedRequestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        return runBlocking {
            mutex.withLock {
                val currentSession = sessionManager.getActiveSession() ?: return@withLock null

                // Another concurrent request already refreshed the token
                if (failedRequestToken != null &&
                    currentSession.accessToken != failedRequestToken &&
                    !currentSession.isExpired()
                ) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer ${currentSession.accessToken}")
                        .build()
                }

                when (val refreshed = tokenRefresher.get().refreshSession()) {
                    is AppResult.Success -> {
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${refreshed.data.accessToken}")
                            .build()
                    }
                    is AppResult.Error -> {
                        sessionManager.clearSession()
                        null
                    }
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_AUTH_RETRIES = 2
    }
}
