package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentReceiptRecord
import com.example.ui.components.LuxuryGradientButton
import com.example.ui.components.MandatoryReceiptAttachmentCard
import com.example.ui.components.ShamCashEmblemIcon
import com.example.ui.components.ShamCashOfficialCard
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.components.UsdtBep20OfficialCard
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ShamCashTeal
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdtTetherGreen
import com.example.ui.theme.VioletBlueVip
import com.example.ui.viewmodel.WalletTransaction

data class TopUpPackageOption(
    val id: String,
    val amountUsd: Int,
    val bonusUsd: Int,
    val titleAr: String,
    val subtitleAr: String
)

@Composable
fun WalletScreen(
    walletBalanceUsd: Int,
    creatorAvailableUsd: Int = 0,
    creatorReservedUsd: Int = 0,
    onRequestCreatorWithdrawal: (Int, String) -> Unit = { _, _ -> },
    transactions: List<WalletTransaction>,
    paymentReceipts: List<PaymentReceiptRecord> = emptyList(),
    onSubmitRealDepositReceipt: (
        amountUsd: Int,
        bonusUsd: Int,
        methodKey: String,
        receiptImageUri: String,
        transactionRef: String
    ) -> Unit = { _, _, _, _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onTopUpConfirm: (amountUsd: Int, bonusUsd: Int, methodTitle: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val packages = remember {
        listOf(
            TopUpPackageOption(
                id = "pkg_10",
                amountUsd = 10,
                bonusUsd = 0,
                titleAr = "باقة $10 (USDT / شام كاش)",
                subtitleAr = "(حد الإيداع الأساسي)"
            ),
            TopUpPackageOption(
                id = "pkg_50",
                amountUsd = 50,
                bonusUsd = 0,
                titleAr = "باقة $50 الملكية",
                subtitleAr = "(القيمة بدون إضافات غير موثقة)"
            ),
            TopUpPackageOption(
                id = "pkg_100",
                amountUsd = 100,
                bonusUsd = 0,
                titleAr = "باقة $100 الماسية",
                subtitleAr = "(القيمة بدون إضافات غير موثقة)"
            ),
            TopUpPackageOption(
                id = "pkg_200",
                amountUsd = 200,
                bonusUsd = 0,
                titleAr = "باقة $200 الإمبراطورية",
                subtitleAr = "(القيمة بدون إضافات غير موثقة)"
            )
        )
    }

    var selectedPackageIndex by remember { mutableIntStateOf(0) }
    var customAmountInput by remember { mutableStateOf("") }
    // "USDT_BEP20" or "SHAM_CASH"
    var selectedGatewayKey by remember { mutableStateOf("USDT_BEP20") }
    var attachedReceiptUri by remember { mutableStateOf("") }
    var transactionReferenceInput by remember { mutableStateOf("") }
    var withdrawalAmountText by remember { mutableStateOf("") }
    var withdrawalDestination by remember { mutableStateOf("") }

    val customParsed = customAmountInput.toIntOrNull()
    val activeAmount = if (customParsed != null && customParsed > 0) {
        customParsed
    } else {
        packages[selectedPackageIndex].amountUsd
    }
    val activeBonus = if (customParsed != null && customParsed > 0) {
        0 // Only a server-managed promotion may grant a deposit bonus.
    } else {
        packages[selectedPackageIndex].bonusUsd
    }

    val vipNightCardGradient = Brush.linearGradient(
        colors = listOf(
            CharcoalNight,
            DarkCardSurface,
            Color(0xFF0F293A)
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 500f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Bar: "الإيداع الحقيقي - شام كاش & USDT"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(42.dp))
                    Text(
                        text = "الإيداع الحقيقي • شام كاش & USDT",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .testTag("wallet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Server-authoritative creator funds; no local editing of balances.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("creator_withdrawal_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "أرباح المذيعة • سحب موثق",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المتاح للسحب: ${creatorAvailableUsd} • قيد المراجعة: ${creatorReservedUsd}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تُحسب نسبة المذيعة 80% بعد إتمام الخدمة من الخادم. طلب السحب ليس إشعاراً بتنفيذ التحويل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = withdrawalAmountText,
                            onValueChange = { withdrawalAmountText = it.filter(Char::isDigit).take(6) },
                            label = { Text("مبلغ السحب بالدولار") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("creator_withdraw_amount")
                        )
                        OutlinedTextField(
                            value = withdrawalDestination,
                            onValueChange = { withdrawalDestination = it.take(160) },
                            label = { Text("عنوان أو وجهة استلام السحب") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("creator_withdraw_destination")
                        )
                        LuxuryGradientButton(
                            text = "إرسال طلب سحب للمراجعة",
                            icon = Icons.Default.AccountBalanceWallet,
                            onClick = {
                                val amount = withdrawalAmountText.toIntOrNull()
                                if (amount == null || amount <= 0 || amount > creatorAvailableUsd || withdrawalDestination.trim().length < 8) {
                                    onShowMessage("تحقق من مبلغ الأرباح المتاحة ووجهة الاستلام قبل تقديم الطلب.")
                                } else {
                                    onRequestCreatorWithdrawal(amount, withdrawalDestination.trim())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "request_creator_withdrawal_button"
                        )
                    }
                }
            }

            // 2. Luxury Sapphire & Sovereign Gold Balance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(vipNightCardGradient)
                        .border(1.5.dp, SovereignGold.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                        .padding(22.dp)
                        .testTag("wallet_balance_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SovereignGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = "محفظة VIP الموثقة بالإيصالات",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Canvas(modifier = Modifier.size(width = 52.dp, height = 32.dp)) {
                                drawCircle(
                                    color = SovereignGold.copy(alpha = 0.88f),
                                    radius = size.height * 0.46f,
                                    center = Offset(size.width * 0.34f, size.height * 0.5f)
                                )
                                drawCircle(
                                    color = VioletBlueVip.copy(alpha = 0.88f),
                                    radius = size.height * 0.46f,
                                    center = Offset(size.width * 0.66f, size.height * 0.5f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "بوابات الدفع المعتمدة:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "شام كاش السوري • USDT BEP-20",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldVerified,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "رصيدك الحالي:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = "$$walletBalanceUsd",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SovereignGold,
                                    modifier = Modifier.testTag("wallet_current_balance_text")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Payment Gateway Selector Tabs: USDT (BEP-20) vs شام كاش السوري
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. اختر بوابة الدفع الرسمية:",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option 1: USDT (BEP-20) Tether
                        val isUsdt = selectedGatewayKey == "USDT_BEP20"
                        Surface(
                            onClick = { selectedGatewayKey = "USDT_BEP20" },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isUsdt) UsdtTetherGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isUsdt) 2.dp else 1.dp,
                                color = if (isUsdt) UsdtTetherGreen else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_gateway_usdt_bep20")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = UsdtTetherGreen,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "₮",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "USDT (BEP-20)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "باركود + QR فوري",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UsdtTetherGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Option 2: Syrian Sham Cash (شام كاش السوري)
                        val isSham = selectedGatewayKey == "SHAM_CASH"
                        Surface(
                            onClick = { selectedGatewayKey = "SHAM_CASH" },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSham) ShamCashTeal.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSham) 2.dp else 1.dp,
                                color = if (isSham) ShamCashTeal else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_gateway_sham_cash")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, ShamCashTeal),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        ShamCashEmblemIcon(modifier = Modifier.size(26.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "شام كاش السوري",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Sham Cash QR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShamCashTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Amount Selection Packages
            item {
                Text(
                    text = "2. حدد مبلغ الإيداع المطلوب ($):",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(packages.size) { index ->
                val pkg = packages[index]
                val isSelected = customAmountInput.isBlank() && selectedPackageIndex == index

                Surface(
                    onClick = {
                        customAmountInput = ""
                        selectedPackageIndex = index
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) {
                        SovereignGold.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) SovereignGold else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_package_${pkg.amountUsd}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isSelected) SovereignGold else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = pkg.subtitleAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) CharcoalNight else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = pkg.titleAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Custom Amount Input
            item {
                OutlinedTextField(
                    value = customAmountInput,
                    onValueChange = { input ->
                        customAmountInput = input.filter { it.isDigit() }
                    },
                    placeholder = {
                        Text(
                            text = "أو اكتب مبلغاً مخصصاً بالدولار ($)...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SovereignGold
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_custom_amount_input")
                )
            }

            // 5. Active Payment Gateway Card (USDT BEP-20 with Barcode + QR OR Sham Cash with QR)
            item {
                if (selectedGatewayKey == "SHAM_CASH") {
                    ShamCashOfficialCard(
                        amountUsd = activeAmount,
                        onCopyMessage = onShowMessage
                    )
                } else {
                    UsdtBep20OfficialCard(
                        amountUsd = activeAmount,
                        onCopyMessage = onShowMessage
                    )
                }
            }

            // 6. Mandatory Payment Receipt Attachment Section ("اي شخص يقوم بالدفع يرفق ايصال الدفع")
            item {
                MandatoryReceiptAttachmentCard(
                    receiptImageUri = attachedReceiptUri,
                    transactionRef = transactionReferenceInput,
                    onReceiptSelected = { uriStr ->
                        attachedReceiptUri = uriStr
                        onShowMessage("تم إرفاق صورة إيصال الدفع بنجاح ✓")
                    },
                    onTransactionRefChange = { transactionReferenceInput = it }
                )
            }

            // 7. Submit Real Deposit Button (Requires Receipt)
            item {
                val gatewayLabel = if (selectedGatewayKey == "SHAM_CASH") {
                    "شام كاش السوري"
                } else {
                    "USDT (BEP-20)"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LuxuryGradientButton(
                        text = if (attachedReceiptUri.isBlank()) {
                            "أرفق إيصال الدفع أولاً لتأكيد إيداع $$activeAmount"
                        } else if (activeBonus > 0) {
                            "تأكيد الإيداع وإرسال الإيصال ($$activeAmount + $$activeBonus)"
                        } else {
                            "تأكيد الإيداع وإرسال الإيصال ($$activeAmount)"
                        },
                        icon = Icons.Default.ReceiptLong,
                        isEmeraldStyle = attachedReceiptUri.isNotBlank(),
                        onClick = {
                            if (attachedReceiptUri.isBlank()) {
                                onShowMessage("لا يوجد دفع وهمي! يرجى إرفاق صورة إيصال التحويل ($gatewayLabel) أولاً.")
                            } else {
                                onSubmitRealDepositReceipt(
                                    activeAmount,
                                    activeBonus,
                                    selectedGatewayKey,
                                    attachedReceiptUri,
                                    transactionReferenceInput
                                )
                                attachedReceiptUri = ""
                                transactionReferenceInput = ""
                                customAmountInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "confirm_wallet_topup_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldVerified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "جميع عمليات الدفع موثقة بإيصالات حقيقية عبر شام كاش و USDT BEP-20",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 8. Attached Payment Receipts History
            if (paymentReceipts.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = SovereignGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سجل إيصالات الدفع المرفقة (${paymentReceipts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(paymentReceipts, key = { it.id }) { receipt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, EmeraldVerified, RoundedCornerShape(12.dp))
                            ) {
                                SmartServiceMediaView(
                                    imageUrl = receipt.receiptImageUri,
                                    accentHex = 0xFF10B981,
                                    secondaryHex = 0xFF09111E,
                                    category = "إيصال",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "${receipt.purposeAr} • ${receipt.methodTitleAr}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                if (receipt.transactionReference.isNotBlank()) {
                                    Text(
                                        text = "مرجع العملية: ${receipt.transactionReference}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SovereignGold
                                    )
                                }
                                Text(
                                    text = "${receipt.timestampLabel} • إيصال مرفق وموثق ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldVerified
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "+$${receipt.amountUsd + receipt.bonusUsd}",
                                style = MaterialTheme.typography.titleLarge,
                                color = EmeraldVerified,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // 9. Wallet Ledger Transactions History
            if (transactions.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = VioletBlueVip,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سجل حركات المحفظة الأخيرة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                items(transactions, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (tx.isCredit) EmeraldVerified else CoralPinkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = tx.timestampLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (tx.isCredit) "+$${tx.amountUsd}" else "-$${tx.amountUsd}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (tx.isCredit) EmeraldVerified else CoralPinkPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
