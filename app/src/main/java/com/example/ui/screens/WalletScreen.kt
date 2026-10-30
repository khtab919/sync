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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.BrandRoyalPlum
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold
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
    transactions: List<WalletTransaction>,
    onTopUpConfirm: (amountUsd: Int, bonusUsd: Int, methodTitle: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val packages = remember {
        listOf(
            TopUpPackageOption(
                id = "pkg_50",
                amountUsd = 50,
                bonusUsd = 5,
                titleAr = "باقة $50",
                subtitleAr = "(احصل على $5 مجاناً)"
            ),
            TopUpPackageOption(
                id = "pkg_100",
                amountUsd = 100,
                bonusUsd = 15,
                titleAr = "باقة $100",
                subtitleAr = "(احصل على $15 مجاناً)"
            ),
            TopUpPackageOption(
                id = "pkg_200",
                amountUsd = 200,
                bonusUsd = 35,
                titleAr = "باقة $200",
                subtitleAr = "(احصل على $35 مجاناً)"
            )
        )
    }

    var selectedPackageIndex by remember { mutableIntStateOf(0) }
    var customAmountInput by remember { mutableStateOf("") }
    var selectedTopUpMethod by remember { mutableStateOf("بطاقة ائتمانية (Visa / Mastercard)") }

    val goldCardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF6DF9E),
            Color(0xFFD9B15E),
            Color(0xFFB88736)
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 500f)
    )

    val goldButtonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFE7C77C),
            Color(0xFFC49746)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF230D30),
                        Color(0xFF170821),
                        Color(0xFF12061A)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Bar: "شراء رصيد - المحفظة"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(44.dp))
                    Text(
                        text = "شراء رصيد - المحفظة",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("wallet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = BrandChampagneGold
                        )
                    }
                }
            }

            // Luxury Metallic Gold Balance Card (Screen 4 of Case Study)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(goldCardGradient)
                        .border(1.5.dp, Color(0xFFFFF1C5).copy(alpha = 0.7f), RoundedCornerShape(24.dp))
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
                            Text(
                                text = "رصيدك رصيد VIP",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF21102B),
                                fontWeight = FontWeight.Bold
                            )

                            // Dual overlapping metallic circles emblem
                            Canvas(modifier = Modifier.size(width = 52.dp, height = 32.dp)) {
                                drawCircle(
                                    color = Color(0xFF7B5624).copy(alpha = 0.75f),
                                    radius = size.height * 0.46f,
                                    center = Offset(size.width * 0.34f, size.height * 0.5f)
                                )
                                drawCircle(
                                    color = Color(0xFF9E7334).copy(alpha = 0.75f),
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
                            Text(
                                text = "رصيدك الحالي:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2A1437),
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "$${walletBalanceUsd}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A0A24),
                                modifier = Modifier.testTag("wallet_current_balance_text")
                            )
                        }
                    }
                }
            }

            // Quick Top-up Packages Header
            item {
                Text(
                    text = "شحن رصيد سريع",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Package Option Cards
            items(packages.size) { index ->
                val pkg = packages[index]
                val isSelected = customAmountInput.isBlank() && selectedPackageIndex == index

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(goldButtonGradient)
                            } else {
                                Modifier
                                    .background(Color(0xFF331842).copy(alpha = 0.88f))
                                    .border(
                                        1.dp,
                                        BrandChampagneGold.copy(alpha = 0.32f),
                                        RoundedCornerShape(20.dp)
                                    )
                            }
                        )
                        .clickable {
                            customAmountInput = ""
                            selectedPackageIndex = index
                        }
                        .padding(vertical = 18.dp, horizontal = 20.dp)
                        .testTag("wallet_package_${pkg.amountUsd}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pkg.titleAr,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isSelected) Color(0xFF1E0B28) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pkg.subtitleAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color(0xFF2D143B) else BrandChampagneGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Custom Amount Input Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "كتابة مبلغ مخصص",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { input ->
                            customAmountInput = input.filter { it.isDigit() }
                        },
                        placeholder = {
                            Text(
                                text = "أدخل مبلغاً مخصصاً لشحن الرصيد ($)",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = BrandChampagneGold
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2E153D),
                            unfocusedContainerColor = Color(0xFF2A1238),
                            focusedBorderColor = BrandChampagneGold,
                            unfocusedBorderColor = BrandChampagneGold.copy(alpha = 0.35f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_custom_amount_input")
                    )
                }
            }

            // Payment Method Chips for Top-up
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "وسيلة الشحن المفضلة:",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandChampagneGold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val methods = listOf(
                            "بطاقة ائتمانية (Visa / Mastercard)",
                            "Apple Pay / Google Pay"
                        )
                        methods.forEach { method ->
                            val selected = selectedTopUpMethod == method
                            FilterChip(
                                selected = selected,
                                onClick = { selectedTopUpMethod = method },
                                label = { Text(method, maxLines = 1) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandChampagneGold,
                                    selectedLabelColor = ObsidianNight,
                                    selectedLeadingIconColor = ObsidianNight
                                )
                            )
                        }
                    }
                }
            }

            // Primary Interactive CTA Button: "تأكيد شحن الرصيد"
            item {
                val customParsed = customAmountInput.toIntOrNull()
                val activeAmount = if (customParsed != null && customParsed > 0) {
                    customParsed
                } else {
                    packages[selectedPackageIndex].amountUsd
                }
                val activeBonus = if (customParsed != null && customParsed > 0) {
                    if (customParsed >= 100) 15 else if (customParsed >= 50) 5 else 0
                } else {
                    packages[selectedPackageIndex].bonusUsd
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(goldButtonGradient)
                            .clickable {
                                onTopUpConfirm(activeAmount, activeBonus, selectedTopUpMethod)
                                customAmountInput = ""
                            }
                            .testTag("confirm_wallet_topup_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF1E0B28),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (activeBonus > 0) {
                                    "تأكيد شحن الرصيد ($$activeAmount + $$activeBonus مجاناً)"
                                } else {
                                    "تأكيد شحن الرصيد ($$activeAmount)"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF1E0B28),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldVerified,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "عملية شحن مشفرة وآمنة 100% • يضاف الرصيد فوراً",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftGold
                        )
                    }
                }
            }

            // Recent Wallet Transactions History
            if (transactions.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = BrandChampagneGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سجل حركات المحفظة الأخيرة",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(transactions, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2B1339)
                        ),
                        border = BorderStroke(
                            0.8.dp,
                            if (tx.isCredit) EmeraldVerified.copy(alpha = 0.45f) else BrandDustyRose.copy(alpha = 0.45f)
                        )
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
                                    tint = if (tx.isCredit) EmeraldVerified else BrandDustyRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = tx.timestampLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SoftGold
                                    )
                                }
                            }
                            Text(
                                text = if (tx.isCredit) "+$${tx.amountUsd}" else "-$${tx.amountUsd}",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (tx.isCredit) EmeraldVerified else BrandDustyRose,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
