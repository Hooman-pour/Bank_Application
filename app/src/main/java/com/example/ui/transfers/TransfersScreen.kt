package com.example.ui.transfers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.CurrencyDisplayUnit
import com.example.core.common.CurrencyFormatter
import com.example.core.common.IranianBankRegistry
import com.example.domain.model.TransferRail
import com.example.ui.components.BankStatusBanner
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessGreen

@Composable
fun TransfersScreen(
    onNavigateToSecurity: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransfersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshAccountsAndProvider()
    }

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
                    text = "انتقال وجه",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "ارسال وجه از طریق کارت به کارت شتاب، پایا، ساتنا یا پل با ${state.activeProvider.persianName}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransferRail.entries.forEach { rail ->
                    FilterChip(
                        selected = state.selectedRail == rail,
                        onClick = { viewModel.selectRail(rail) },
                        label = { Text(rail.persianTitle) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rail_chip_${rail.name}")
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سامانه ${state.selectedRail.persianTitle}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = state.selectedRail.persianSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.availableAccounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "انتخاب سریع حساب مبدا",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.availableAccounts.forEach { account ->
                            val bankName = IranianBankRegistry.findBySheba(account.iban)?.persianName
                                ?: account.bankName
                            FilterChip(
                                selected = state.sourceInput == account.depositNumber,
                                onClick = { viewModel.selectSourceAccount(account) },
                                label = {
                                    Text("$bankName (${CurrencyFormatter.formatAmountPersian(account.balanceRial, state.currencyUnit)})")
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.sourceInput,
                onValueChange = viewModel::updateSource,
                label = {
                    Text(
                        if (state.selectedRail == TransferRail.CARD_TO_CARD) {
                            "شماره کارت یا سپرده مبدا"
                        } else {
                            "شماره سپرده مبدا"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (state.selectedRail == TransferRail.CARD_TO_CARD) {
                            "62198610••••"
                        } else {
                            "مثال: 100-800-452190-1"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transfer_source_input")
            )

            OutlinedTextField(
                value = state.destinationInput,
                onValueChange = viewModel::updateDestination,
                label = {
                    Text(
                        if (state.selectedRail.requiresSheba) {
                            "شماره شبا مقصد (IR + ۲۴ رقم)"
                        } else {
                            "شماره کارت شتاب مقصد (۱۶ رقم)"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (state.selectedRail.requiresSheba) {
                            "IR820540102680020817909002"
                        } else {
                            "6219861000000009"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (state.selectedRail.requiresSheba) KeyboardType.Text else KeyboardType.Number
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transfer_destination_input")
            )

            if (state.detectedBank != null || state.isDestinationChecksumValid) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.isDestinationChecksumValid) {
                        SuccessGreen.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (state.isDestinationChecksumValid) SuccessGreen.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (state.isDestinationChecksumValid) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.AccountBalance
                                },
                                contentDescription = null,
                                tint = if (state.isDestinationChecksumValid) SuccessGreen else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = state.detectedBank?.let { "${it.persianName} (کد ${CurrencyFormatter.toPersianDigits(it.code)})" }
                                        ?: "کد کنترلی معتبر است",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = if (state.isDestinationChecksumValid) {
                                        "اعتبارسنجی الگوریتمی با موفقیت انجام شد"
                                    } else {
                                        "بانک شناسایی شد • در انتظار تکمیل شماره"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (state.selectedRail.requiresSheba && state.isDestinationChecksumValid) {
                            OutlinedButton(
                                onClick = { viewModel.inquireIbanOwner() },
                                enabled = !state.isInquiringIban,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("inquire_iban_button")
                            ) {
                                if (state.isInquiringIban) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonSearch,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("استعلام نام")
                                }
                            }
                        }
                    }
                }
            }

            state.inquiryResult?.let { inquiry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("صاحب حساب تاییدشده", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                        Text(inquiry.ownerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${inquiry.bankName} • وضعیت حساب: ${inquiry.depositStatus}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("مبلغ (${state.currencyUnit.persianLabel})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transfer_amount_input")
                )
                OutlinedButton(
                    onClick = { viewModel.toggleCurrencyUnit() },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("transfer_currency_toggle")
                ) {
                    Text(text = state.currencyUnit.persianSuffix)
                }
            }

            val parsedRial = CurrencyFormatter.parseRialInput(state.amountInput, state.currencyUnit)
            if (parsedRial != null) {
                val otherUnit = if (state.currencyUnit == CurrencyDisplayUnit.TOMAN) {
                    CurrencyDisplayUnit.RIAL
                } else {
                    CurrencyDisplayUnit.TOMAN
                }
                Text(
                    text = "معادل: ${CurrencyFormatter.formatAmountPersian(parsedRial, otherUnit)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = state.descriptionInput,
                onValueChange = viewModel::updateDescription,
                label = { Text("شرح انتقال (اختیاری)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transfer_description_input")
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = "Track-Id: ${state.trackId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.submitTransfer() },
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_transfer_button")
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت انتقال (${state.activeProvider.persianName})")
                }
            }

            state.error?.let { error ->
                BankStatusBanner(
                    error = error,
                    provider = state.activeProvider,
                    onActionClick = onNavigateToSecurity,
                    actionLabel = "ورود به گاوصندوق امنیتی"
                )
            }

            state.receipt?.let { receipt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_receipt_card"),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "انتقال وجه با موفقیت ثبت شد (${receipt.status})",
                            style = MaterialTheme.typography.titleMedium,
                            color = SuccessGreen
                        )
                        Text("مبلغ: ${CurrencyFormatter.formatAmountPersian(receipt.amountRial, state.currencyUnit)}")
                        Text("مقصد: ${receipt.destinationMasked}")
                        Text("شماره پیگیری: ${CurrencyFormatter.toPersianDigits(receipt.referenceNumber)}")
                        Text("درگاه: ${receipt.provider.persianName} • ${receipt.rail.persianTitle}")
                    }
                }
            }
        }
    }
}
