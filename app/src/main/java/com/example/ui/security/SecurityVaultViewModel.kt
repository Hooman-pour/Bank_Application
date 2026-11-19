package com.example.ui.security

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.AppResult
import com.example.core.common.BankError
import com.example.core.security.OAuthCallbackEvent
import com.example.core.security.OAuthConsentCoordinator
import com.example.core.security.SessionManager
import com.example.core.security.VaultDiagnostics
import com.example.domain.model.OpenBankingProvider
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SessionState
import com.example.domain.usecase.AuthenticateClientUseCase
import com.example.domain.usecase.ExchangeOAuthCodeUseCase
import com.example.domain.usecase.LogoutUseCase
import com.example.domain.usecase.ObserveSessionUseCase
import com.example.domain.usecase.SwitchProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityVaultUiState(
    val sessionState: SessionState,
    val selectedProvider: OpenBankingProvider,
    val isFinotechConfigured: Boolean,
    val isFaraboomConfigured: Boolean,
    val diagnostics: VaultDiagnostics,
    val authCodeInput: String = "",
    val redirectUriInput: String = "zarbank://oauth/callback",
    val isAuthenticating: Boolean = false,
    val statusMessage: String? = null,
    val error: BankError? = null
)

@HiltViewModel
class SecurityVaultViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val oAuthConsentCoordinator: OAuthConsentCoordinator,
    private val switchProviderUseCase: SwitchProviderUseCase,
    private val exchangeOAuthCodeUseCase: ExchangeOAuthCodeUseCase,
    private val authenticateClientUseCase: AuthenticateClientUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val initialProvider = authRepository.getSelectedProvider()
    private val _uiState = MutableStateFlow(
        SecurityVaultUiState(
            sessionState = SessionState.Unauthenticated(initialProvider),
            selectedProvider = initialProvider,
            isFinotechConfigured = authRepository.isProviderConfigured(OpenBankingProvider.FINOTECH),
            isFaraboomConfigured = authRepository.isProviderConfigured(OpenBankingProvider.FARABOOM),
            diagnostics = sessionManager.getVaultDiagnostics()
        )
    )
    val uiState: StateFlow<SecurityVaultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                val provider = when (session) {
                    is SessionState.Authenticated -> session.session.provider
                    is SessionState.Unauthenticated -> session.selectedProvider
                }
                _uiState.update {
                    it.copy(
                        sessionState = session,
                        selectedProvider = provider,
                        diagnostics = sessionManager.getVaultDiagnostics()
                    )
                }
            }
        }
        viewModelScope.launch {
            oAuthConsentCoordinator.callbacks.collect { event ->
                when (event) {
                    is OAuthCallbackEvent.Authorized -> {
                        _uiState.update {
                            it.copy(
                                selectedProvider = event.provider,
                                authCodeInput = event.code,
                                redirectUriInput = event.redirectUri
                            )
                        }
                        exchangeAuthorizationCode()
                    }
                    is OAuthCallbackEvent.Failed -> {
                        _uiState.update {
                            it.copy(
                                isAuthenticating = false,
                                error = BankError.Unauthorized(event.reason)
                            )
                        }
                    }
                }
            }
        }
    }

    fun prepareConsentUri(): Uri? {
        val provider = _uiState.value.selectedProvider
        if (!authRepository.isProviderConfigured(provider)) {
            _uiState.update {
                it.copy(
                    error = BankError.MissingCredentials(
                        providerName = provider.persianName,
                        message = "کلیدهای دسترسی ${provider.persianName} باید پیش از باز کردن صفحه رضایت بانک در بخش Secrets تنظیم شوند."
                    )
                )
            }
            return null
        }
        return oAuthConsentCoordinator.buildConsentUri(
            provider = provider,
            redirectUri = _uiState.value.redirectUriInput.ifBlank { OAuthConsentCoordinator.DEFAULT_REDIRECT_URI }
        )
    }

    fun selectProvider(provider: OpenBankingProvider) {
        viewModelScope.launch {
            switchProviderUseCase(provider)
            _uiState.update {
                it.copy(
                    selectedProvider = provider,
                    diagnostics = sessionManager.getVaultDiagnostics(),
                    error = null,
                    statusMessage = "درگاه بانکداری باز به ${provider.persianName} تغییر یافت."
                )
            }
        }
    }

    fun updateAuthCode(code: String) {
        _uiState.update { it.copy(authCodeInput = code, error = null, statusMessage = null) }
    }

    fun updateRedirectUri(uri: String) {
        _uiState.update { it.copy(redirectUriInput = uri, error = null) }
    }

    fun exchangeAuthorizationCode() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, error = null, statusMessage = null) }
            when (
                val result = exchangeOAuthCodeUseCase(
                    provider = current.selectedProvider,
                    authorizationCode = current.authCodeInput,
                    redirectUri = current.redirectUriInput
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            authCodeInput = "",
                            diagnostics = sessionManager.getVaultDiagnostics(),
                            statusMessage = "توکن OAuth2 رمزنگاری شد و در گاوصندوق AndroidKeyStore ذخیره گردید."
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isAuthenticating = false, error = result.error) }
                }
            }
        }
    }

    fun authenticateClientCredentials() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, error = null, statusMessage = null) }
            when (val result = authenticateClientUseCase(current.selectedProvider)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            diagnostics = sessionManager.getVaultDiagnostics(),
                            statusMessage = "توکن نشست کلاینت رمزنگاری و ذخیره شد."
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isAuthenticating = false, error = result.error) }
                }
            }
        }
    }

    fun wipeSession() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update {
                it.copy(
                    diagnostics = sessionManager.getVaultDiagnostics(),
                    error = null,
                    statusMessage = "تمام کلیدهای نشست در EncryptedSharedPreferences پاکسازی شدند."
                )
            }
        }
    }
}
