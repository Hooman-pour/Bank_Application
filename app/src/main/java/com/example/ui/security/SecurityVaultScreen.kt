package com.example.ui.security

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SyncLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.security.OAuthConsentCoordinator
import com.example.domain.model.OpenBankingProvider
import com.example.domain.repository.SessionState
import com.example.ui.components.BankStatusBanner
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun SecurityVaultScreen(
    modifier: Modifier = Modifier,
    viewModel: SecurityVaultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "گاوصندوق امنیت و نشست",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "استفاده رایگان با موتور داخلی بدون نیاز به کلید API، به همراه ذخیره‌سازی رمزنگاری‌شده EncryptedSharedPreferences و AndroidKeyStore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SyncLock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "انتخاب حالت یا درگاه بانکی",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    ProviderChoiceCard(
                        provider = OpenBankingProvider.FREE_SANDBOX,
                        isSelected = state.selectedProvider == OpenBankingProvider.FREE_SANDBOX,
                        isConfigured = true,
                        onClick = { viewModel.selectProvider(OpenBankingProvider.FREE_SANDBOX) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProviderChoiceCard(
                            provider = OpenBankingProvider.FINOTECH,
                            isSelected = state.selectedProvider == OpenBankingProvider.FINOTECH,
                            isConfigured = state.isFinotechConfigured,
                            onClick = { viewModel.selectProvider(OpenBankingProvider.FINOTECH) },
                            modifier = Modifier.weight(1f)
                        )
                        ProviderChoiceCard(
                            provider = OpenBankingProvider.FARABOOM,
                            isSelected = state.selectedProvider == OpenBankingProvider.FARABOOM,
                            isConfigured = state.isFaraboomConfigured,
                            onClick = { viewModel.selectProvider(OpenBankingProvider.FARABOOM) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = state.selectedProvider.baseUrl,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EnhancedEncryption,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "وضعیت گاوصندوق رمزنگاری‌شده",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    DiagnosticRow("فایل ذخیره‌سازی", state.diagnostics.prefsFileName)
                    DiagnosticRow("کلید اصلی", state.diagnostics.masterKeyScheme)
                    DiagnosticRow("الگوریتم کلید", state.diagnostics.keyEncryptionScheme)
                    DiagnosticRow("الگوریتم مقدار", state.diagnostics.valueEncryptionScheme)
                    DiagnosticRow(
                        "استثنا از بکاپ",
                        if (state.diagnostics.backupExcluded) "بله (allowBackup=false)" else "خیر",
                        isLtrValue = false
                    )
                    DiagnosticRow(
                        "وضعیت نشست",
                        when (val s = state.sessionState) {
                            is SessionState.Authenticated -> "فعال (${s.session.maskedAccessToken()})"
                            is SessionState.Unauthenticated -> "بدون نشست فعال (${s.selectedProvider.persianName})"
                        },
                        isLtrValue = false
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.selectedProvider == OpenBankingProvider.FREE_SANDBOX) {
                                "مدیریت نشست رایگان (بدون کلید)"
                            } else {
                                "احراز هویت OAuth2 و مدیریت توکن"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (state.selectedProvider == OpenBankingProvider.FREE_SANDBOX) {
                        Text(
                            text = "در این حالت تمامی امکانات مشاهده سپرده‌ها، صورت‌حساب، استعلام شبا و انتقال وجه بدون نیاز به کلید دسترسی فینوتک یا فرابوم به صورت رایگان در حافظه رمزنگاری‌شده دستگاه اجرا می‌شوند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { viewModel.authenticateClientCredentials() },
                            enabled = !state.isAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("client_token_button")
                        ) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (state.sessionState is SessionState.Authenticated) {
                                    "تجدید نشست رایگان در گاوصندوق"
                                } else {
                                    "فعال‌سازی فوری نشست رایگان"
                                }
                            )
                        }
                    } else {
                        Text(
                            text = "ورود و تایید رمز یک‌بارمصرف منحصراً در صفحه امن مرورگر بانک (Custom Tab) انجام می‌شود. دسترسی‌های درخواستی: ${OAuthConsentCoordinator.DEFAULT_BANKING_SCOPES.joinToString("، ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val consentUri = viewModel.prepareConsentUri() ?: return@Button
                                val intent = Intent(Intent.ACTION_VIEW, consentUri).apply {
                                    putExtra("android.support.customtabs.extra.TITLE_VISIBILITY", 1)
                                }
                                context.startActivity(intent)
                            },
                            enabled = !state.isAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("launch_bank_consent_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("باز کردن صفحه رضایت بانک (${state.selectedProvider.persianName})")
                        }

                        OutlinedTextField(
                            value = state.authCodeInput,
                            onValueChange = viewModel::updateAuthCode,
                            label = { Text("کد مجوز OAuth2") },
                            placeholder = { Text("کد بازگشتی از ${state.selectedProvider.persianName} را وارد کنید") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("oauth_code_input")
                        )

                        OutlinedTextField(
                            value = state.redirectUriInput,
                            onValueChange = viewModel::updateRedirectUri,
                            label = { Text("آدرس بازگشت (Redirect URI)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("oauth_redirect_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.exchangeAuthorizationCode() },
                                enabled = !state.isAuthenticating,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("exchange_oauth_button")
                            ) {
                                if (state.isAuthenticating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("دریافت توکن با کد")
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.authenticateClientCredentials() },
                                enabled = !state.isAuthenticating,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("client_token_button")
                            ) {
                                Text("توکن کلاینت")
                            }
                        }
                    }

                    if (state.sessionState is SessionState.Authenticated) {
                        OutlinedButton(
                            onClick = { viewModel.wipeSession() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCrimson),
                            border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("wipe_session_button")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("پاکسازی نشست رمزنگاری‌شده")
                        }
                    }
                }
            }

            state.statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            state.error?.let { err ->
                BankStatusBanner(
                    error = err,
                    provider = state.selectedProvider,
                    onActionClick = { viewModel.wipeSession() },
                    actionLabel = "بازنشانی گاوصندوق"
                )
            }
        }
    }
}

@Composable
private fun ProviderChoiceCard(
    provider: OpenBankingProvider,
    isSelected: Boolean,
    isConfigured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("provider_card_${provider.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = provider.persianName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = provider.persianApiVersion,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (provider == OpenBankingProvider.FREE_SANDBOX) {
                    "فعال و رایگان"
                } else if (isConfigured) {
                    "کلیدها آماده است"
                } else {
                    "اختیاری (نیازمند کلید)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isConfigured) SuccessGreen else WarningAmber
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    isLtrValue: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Spacer(Modifier.width(8.dp))
        if (isLtrValue) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    modifier = Modifier.weight(0.62f)
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.62f)
            )
        }
    }
}
