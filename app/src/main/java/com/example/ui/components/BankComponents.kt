package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.BankError
import com.example.core.common.CurrencyDisplayUnit
import com.example.core.common.CurrencyFormatter
import com.example.domain.model.OpenBankingProvider
import com.example.domain.repository.SessionState
import com.example.ui.theme.ChampagneBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun BankCardHero(
    sessionState: SessionState,
    totalBalanceRial: Long?,
    currencyUnit: CurrencyDisplayUnit,
    hideBalance: Boolean,
    isProviderConfigured: Boolean,
    onToggleCurrency: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProvider = when (sessionState) {
        is SessionState.Authenticated -> sessionState.session.provider
        is SessionState.Unauthenticated -> sessionState.selectedProvider
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_card),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                EmeraldDeep.copy(alpha = 0.88f),
                                EmeraldDeep.copy(alpha = 0.62f),
                                Color(0xFF06201D).copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ChampagneBright.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = ChampagneBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "درگاه ${activeProvider.persianName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Text(
                                text = activeProvider.persianApiVersion,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Surface(
                        color = if (sessionState is SessionState.Authenticated) {
                            SuccessGreen.copy(alpha = 0.25f)
                        } else {
                            ChampagneBright.copy(alpha = 0.18f)
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (sessionState is SessionState.Authenticated) {
                                Color(0xFF6CE5AE).copy(alpha = 0.5f)
                            } else {
                                ChampagneBright.copy(alpha = 0.45f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (sessionState is SessionState.Authenticated) {
                                    Color(0xFF8FF5C5)
                                } else {
                                    ChampagneBright
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (sessionState is SessionState.Authenticated) {
                                    "نشست فعال"
                                } else if (isProviderConfigured) {
                                    "آماده اتصال"
                                } else {
                                    "نیازمند کلید"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "مجموع موجودی تجمیعی",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneBright.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val balanceText = when {
                            sessionState !is SessionState.Authenticated -> "اتصال به درگاه بانکی"
                            hideBalance -> "•••••• ${currencyUnit.persianSuffix}"
                            totalBalanceRial != null -> CurrencyFormatter.formatAmountPersian(totalBalanceRial, currencyUnit)
                            else -> "۰ ${currencyUnit.persianSuffix}"
                        }
                        Text(
                            text = balanceText,
                            style = if (sessionState is SessionState.Authenticated) {
                                MaterialTheme.typography.headlineMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = onToggleCurrency,
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_currency_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = null,
                                        tint = ChampagneBright,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = currencyUnit.persianSuffix,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            if (sessionState is SessionState.Authenticated) {
                                IconButton(
                                    onClick = onToggleVisibility,
                                    modifier = Modifier.testTag("toggle_balance_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (hideBalance) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = stringResource(R.string.cd_toggle_balance),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (sessionState) {
                            is SessionState.Authenticated -> "توکن فعال: ${sessionState.session.maskedAccessToken()}"
                            is SessionState.Unauthenticated -> "ذخیره‌سازی رمزنگاری‌شده AES256-GCM"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    OutlinedButton(
                        onClick = onOpenVault,
                        border = BorderStroke(1.dp, ChampagneBright.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneBright),
                        modifier = Modifier.testTag("hero_vault_button")
                    ) {
                        Text(
                            text = if (sessionState is SessionState.Authenticated) "نشست امن" else "اتصال",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankStatusBanner(
    error: BankError,
    provider: OpenBankingProvider,
    onActionClick: () -> Unit,
    actionLabel: String,
    modifier: Modifier = Modifier
) {
    val isConfig = error is BankError.MissingCredentials || error is BankError.Unauthorized
    val accent = if (isConfig) WarningAmber else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when (error) {
                        is BankError.MissingCredentials -> "نیاز به کلیدهای دسترسی ${provider.persianName}"
                        is BankError.Unauthorized -> "نیاز به احراز هویت OAuth2"
                        is BankError.Network -> "خطای ارتباط با درگاه بانکی"
                        is BankError.ProviderApi -> "خطای سرویس ${provider.persianName} (کد ${CurrencyFormatter.toPersianDigits(error.httpCode.toString())})"
                        is BankError.Validation -> "خطای اعتبارسنجی اطلاعات"
                        is BankError.Unknown -> "خطای درگاه بانکی"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("status_banner_action")
            ) {
                Text(actionLabel)
            }
        }
    }
}
