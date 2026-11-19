package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.common.BankError
import com.example.core.common.CurrencyDisplayUnit
import com.example.core.common.CurrencyFormatter
import com.example.core.common.IranianBankRegistry
import com.example.domain.model.BankAccount
import com.example.domain.model.BankTransaction
import com.example.domain.model.TransactionDirection
import com.example.ui.components.BankCardHero
import com.example.ui.components.BankStatusBanner
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTransfers: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val provider = state.activeProvider

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (state.contentState is DashboardContentState.Success) {
            viewModel.refreshAccounts(isPullRefresh = true)
        }
    }

    val totalRial = (state.contentState as? DashboardContentState.Success)
        ?.accounts
        ?.sumOf { it.balanceRial }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refreshAccounts(isPullRefresh = true) },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_icon_zarbank_1795121828580),
                                contentDescription = stringResource(R.string.cd_brand_emblem),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "زربانک",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "درگاه بانکداری باز ایران",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.refreshAccounts() },
                            modifier = Modifier.testTag("dashboard_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh)
                            )
                        }
                    }
                }

                item {
                    BankCardHero(
                        sessionState = state.sessionState,
                        totalBalanceRial = totalRial,
                        currencyUnit = state.currencyUnit,
                        hideBalance = state.hideBalances,
                        isProviderConfigured = state.isProviderConfigured,
                        onToggleCurrency = { viewModel.toggleCurrencyUnit() },
                        onToggleVisibility = { viewModel.toggleBalanceVisibility() },
                        onOpenVault = onNavigateToSecurity
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.AutoMirrored.Rounded.Send,
                            title = "انتقال وجه",
                            subtitle = "پایا / ساتنا / کارت",
                            onClick = onNavigateToTransfers,
                            testTag = "quick_action_transfer",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.Calculate,
                            title = "شبا و کارت",
                            subtitle = "اعتبارسنجی آنلاین/آفلاین",
                            onClick = onNavigateToTools,
                            testTag = "quick_action_tools",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Rounded.Key,
                            title = "گاوصندوق",
                            subtitle = provider.persianName,
                            onClick = onNavigateToSecurity,
                            testTag = "quick_action_vault",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                when (val content = state.contentState) {
                    is DashboardContentState.Unauthenticated -> {
                        item {
                            val bannerError = if (!content.isProviderConfigured) {
                                BankError.MissingCredentials(
                                    providerName = provider.persianName,
                                    message = "کلیدهای دسترسی ${provider.persianName} در پیکربندی برنامه یافت نشد. پس از افزودن آن‌ها در بخش Secrets، نشست OAuth2 را در گاوصندوق امنیتی فعال کنید."
                                )
                            } else {
                                BankError.Unauthorized(
                                    message = "پیکربندی درگاه ${provider.persianName} آماده است. برای دریافت مجوز بانکی یا توکن کلاینت وارد گاوصندوق امنیتی شوید."
                                )
                            }
                            BankStatusBanner(
                                error = bannerError,
                                provider = provider,
                                onActionClick = onNavigateToSecurity,
                                actionLabel = "ورود به گاوصندوق امنیتی"
                            )
                        }
                        item {
                            ArchitectureSummaryCard(
                                providerName = provider.persianName,
                                onOpenTools = onNavigateToTools
                            )
                        }
                    }

                    is DashboardContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp)
                                    .testTag("dashboard_loading_indicator"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "در حال دریافت سپرده‌ها از ${provider.persianName}...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is DashboardContentState.Error -> {
                        item {
                            BankStatusBanner(
                                error = content.error,
                                provider = provider,
                                onActionClick = { viewModel.refreshAccounts() },
                                actionLabel = "تلاش مجدد",
                                modifier = Modifier.testTag("dashboard_error_banner")
                            )
                        }
                    }

                    is DashboardContentState.Success -> {
                        item {
                            Text(
                                text = "سپرده‌های متصل (${CurrencyFormatter.toPersianDigits(content.accounts.size.toString())})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (content.accounts.isEmpty()) {
                            item {
                                EmptyGatewayState(
                                    title = "سپرده‌ای یافت نشد",
                                    message = "توکن ${provider.persianName} تایید شد اما حساب سپرده‌ای با این محدوده دسترسی بازگردانده نشد."
                                )
                            }
                        } else {
                            items(content.accounts, key = { it.depositNumber }) { account ->
                                DepositAccountCard(
                                    account = account,
                                    isSelected = account.depositNumber == content.selectedDepositNumber,
                                    currencyUnit = state.currencyUnit,
                                    hideBalance = state.hideBalances,
                                    onClick = { viewModel.selectAccount(account.depositNumber) }
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "تراکنش‌های اخیر",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        when (val txState = content.transactionsState) {
                            is TransactionsUiState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                            .testTag("transactions_loading_indicator"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            is TransactionsUiState.Error -> {
                                item {
                                    BankStatusBanner(
                                        error = txState.error,
                                        provider = provider,
                                        onActionClick = {
                                            content.selectedDepositNumber?.let(viewModel::selectAccount)
                                        },
                                        actionLabel = "دریافت مجدد صورت‌حساب",
                                        modifier = Modifier.testTag("transactions_error_banner")
                                    )
                                }
                            }

                            is TransactionsUiState.Success -> {
                                if (txState.transactions.isEmpty()) {
                                    item {
                                        EmptyGatewayState(
                                            title = "تراکنش اخیری ثبت نشده است",
                                            message = "گردش حساب دریافتی از درگاه ${provider.persianName} برای سپرده انتخاب‌شده در اینجا نمایش داده می‌شود."
                                        )
                                    }
                                } else {
                                    items(txState.transactions, key = { it.id }) { tx ->
                                        TransactionRowCard(
                                            tx = tx,
                                            currencyUnit = state.currencyUnit,
                                            hideBalance = state.hideBalances
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DepositAccountCard(
    account: BankAccount,
    isSelected: Boolean,
    currencyUnit: CurrencyDisplayUnit,
    hideBalance: Boolean,
    onClick: () -> Unit
) {
    val bankPersianName = IranianBankRegistry.findBySheba(account.iban)?.persianName ?: account.bankName
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deposit_card_${account.depositNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bankPersianName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (account.accountTitle == "Current Account") "سپرده جاری" else account.accountTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (hideBalance) "••••••" else CurrencyFormatter.formatAmountPersian(account.balanceRial, currencyUnit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (account.iban.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = account.iban,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    tx: BankTransaction,
    currencyUnit: CurrencyDisplayUnit,
    hideBalance: Boolean
) {
    val isCredit = tx.direction == TransactionDirection.CREDIT
    val color = if (isCredit) SuccessGreen else ErrorCrimson
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CurrencyFormatter.toPersianDigits(
                            tx.dateLabel.ifBlank { tx.referenceNumber ?: "ثبت صورت‌حساب" }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            val sign = if (isCredit) "واریز" else "برداشت"
            Text(
                text = if (hideBalance) "••••" else "$sign ${CurrencyFormatter.formatAmountPersian(tx.amountRial, currencyUnit)}",
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}

@Composable
private fun ArchitectureSummaryCard(
    providerName: String,
    onOpenTools: () -> Unit
) {
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
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "زیرساخت امن بانکداری باز",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "درگاه فعال: $providerName با مسیریابی ماژولار Hilt، تمدید خودکار توکن OkHttp و ذخیره‌سازی رمزنگاری‌شده AES256-GCM.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                onClick = onOpenTools,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("open_offline_validators_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آزمایش ابزارهای شماره شبا (Mod-97) و کارت شتاب",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGatewayState(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
