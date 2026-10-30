package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServicePackage
import com.example.data.model.Venue
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.theme.BrandChampagneGold
import com.example.ui.theme.BrandDeepViolet
import com.example.ui.theme.BrandDustyRose
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.SoftGold

enum class PaymentMethodType(val titleAr: String, val subtitleAr: String) {
    WALLET("رصيد المحفظة", "الرصيد المشحون مسبقاً"),
    CREDIT_CARD("بطاقة ائتمانية", "Visa / Mastercard • دفع مشفر"),
    APPLE_GOOGLE_PAY("Apple Pay / Google Pay", "دفع سريع بلمسة واحدة")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(
    venue: Venue,
    selectedPackage: ServicePackage,
    walletBalanceUsd: Int,
    isSubmitting: Boolean,
    onOpenWalletTopUp: () -> Unit,
    onConfirmCheckout: (
        date: String,
        timeSlot: String,
        guestCount: Int,
        notes: String,
        paymentMethod: PaymentMethodType
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val dateOptions = remember {
        listOf(
            "اليوم - المساء",
            "غداً - المساء",
            "الخميس القادم",
            "الجمعة القادمة",
            "السبت القادم"
        )
    }

    var currentStep by remember { mutableIntStateOf(3) } // 1: التفاصيل, 2: الوقت, 3: الدفع
    var selectedDate by remember { mutableStateOf(dateOptions.first()) }
    var selectedTimeSlot by remember {
        mutableStateOf(venue.availableTimeSlots.firstOrNull() ?: "08:30 مساءً")
    }
    var guestCount by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }

    var selectedPaymentMethod by remember {
        mutableStateOf(PaymentMethodType.WALLET)
    }

    // Credit Card Inputs when CREDIT_CARD is selected
    var cardNumber by remember { mutableStateOf("4532 •••• •••• 8891") }
    var cardExpiry by remember { mutableStateOf("08/28") }
    var cardCvv by remember { mutableStateOf("942") }

    val unitPriceUsd = selectedPackage.priceUsd
    val serviceFeeUsd = 0
    val totalPriceUsd = unitPriceUsd * guestCount + serviceFeeUsd

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
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 18.dp,
                bottom = 115.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header: "خطوات الدفع"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(44.dp))
                    Text(
                        text = "خطوات الدفع",
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
                            .testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = BrandChampagneGold
                        )
                    }
                }
            }

            // Step Progress Indicator (التفاصيل > الوقت > الدفع)
            item {
                Surface(
                    color = Color(0xFF2E143D),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepIndicatorPill(
                            stepNumber = 1,
                            label = "التفاصيل",
                            isActive = currentStep == 1,
                            isCompleted = currentStep > 1,
                            onClick = { currentStep = 1 }
                        )
                        Text(">", color = BrandChampagneGold, fontWeight = FontWeight.Bold)
                        StepIndicatorPill(
                            stepNumber = 2,
                            label = "الوقت",
                            isActive = currentStep == 2,
                            isCompleted = currentStep > 2,
                            onClick = { currentStep = 2 }
                        )
                        Text(">", color = BrandChampagneGold, fontWeight = FontWeight.Bold)
                        StepIndicatorPill(
                            stepNumber = 3,
                            label = "الدفع",
                            isActive = currentStep == 3,
                            isCompleted = false,
                            onClick = { currentStep = 3 }
                        )
                    }
                }
            }

            // Expandable Date & Time Customization when Step 1 or Step 2 is tapped
            item {
                AnimatedVisibility(visible = currentStep == 1 || currentStep == 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B133A)),
                        border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "تحديد الموعد وعدد الأشخاص:",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandChampagneGold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                dateOptions.forEach { d ->
                                    FilterChip(
                                        selected = selectedDate == d,
                                        onClick = { selectedDate = d },
                                        label = { Text(d) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandChampagneGold,
                                            selectedLabelColor = ObsidianNight
                                        )
                                    )
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                venue.availableTimeSlots.forEach { slot ->
                                    FilterChip(
                                        selected = selectedTimeSlot == slot,
                                        onClick = { selectedTimeSlot = slot },
                                        label = { Text(slot) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandDustyRose,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("عدد الأشخاص / الليالي:", color = SoftGold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (guestCount > 1) guestCount-- }) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = BrandChampagneGold)
                                    }
                                    Text(
                                        text = "$guestCount",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(onClick = { if (guestCount < 15) guestCount++ }) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BrandChampagneGold)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات خاصة للطلب (اختياري)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Order Summary Card ("ملخص الطلب" - Screen 5 of Case Study)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_order_summary_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D143C)
                    ),
                    border = BorderStroke(1.2.dp, BrandChampagneGold.copy(alpha = 0.42f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail Image on the Left
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, BrandChampagneGold, RoundedCornerShape(14.dp))
                            ) {
                                SmartServiceMediaView(
                                    imageUrl = venue.visualPresetKey,
                                    accentHex = venue.accentHex,
                                    secondaryHex = venue.secondaryHex,
                                    category = venue.category,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "ملخص الطلب",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = venue.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandChampagneGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$selectedDate • $selectedTimeSlot",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftGold,
                                    modifier = Modifier.clickable { currentStep = 2 }
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        // Invoice Breakdown Rows
                        InvoiceRow(
                            label = "الباقة (${selectedPackage.durationLabel} × $guestCount)",
                            value = "$$unitPriceUsd"
                        )
                        InvoiceRow(
                            label = "تفاصيل السعر والرسوم",
                            value = "$$serviceFeeUsd مجاناً"
                        )

                        HorizontalDivider(color = BrandChampagneGold.copy(alpha = 0.25f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$$totalPriceUsd",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandChampagneGold
                            )
                            Text(
                                text = "السعر الإجمالي",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Payment Methods Header ("خيارات الدفع")
            item {
                Text(
                    text = "خيارات الدفع",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment Method Radio Option Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentMethodType.entries.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        val methodIcon = when (method) {
                            PaymentMethodType.WALLET -> Icons.Default.AccountBalanceWallet
                            PaymentMethodType.CREDIT_CARD -> Icons.Default.CreditCard
                            PaymentMethodType.APPLE_GOOGLE_PAY -> Icons.Default.PhoneIphone
                        }

                        Surface(
                            onClick = { selectedPaymentMethod = method },
                            color = if (isSelected) Color(0xFF391A4C) else Color(0xFF281136),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.6.dp else 1.dp,
                                color = if (isSelected) BrandChampagneGold else Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_method_${method.name.lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPaymentMethod = method },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = BrandChampagneGold,
                                            unselectedColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = method.titleAr,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (method == PaymentMethodType.WALLET) {
                                                    "الرصيد المتوفر حالياً: $$walletBalanceUsd"
                                                } else {
                                                    method.subtitleAr
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (method == PaymentMethodType.WALLET && walletBalanceUsd >= totalPriceUsd) {
                                                    EmeraldVerified
                                                } else {
                                                    SoftGold
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = methodIcon,
                                            contentDescription = method.titleAr,
                                            tint = BrandChampagneGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // If Wallet is selected and balance is insufficient, show a 1-tap Top-up shortcut
                                if (method == PaymentMethodType.WALLET && isSelected && walletBalanceUsd < totalPriceUsd) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = onOpenWalletTopUp) {
                                            Text(
                                                text = "+ شحن رصيد المحفظة الآن",
                                                color = BrandChampagneGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "الرصيد أقل من قيمة الطلب ($$totalPriceUsd)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrandDustyRose
                                        )
                                    }
                                }

                                // If Credit Card is selected, expand card input fields
                                AnimatedVisibility(visible = method == PaymentMethodType.CREDIT_CARD && isSelected) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = cardNumber,
                                            onValueChange = { cardNumber = it },
                                            label = { Text("رقم البطاقة (Visa / Mastercard)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandChampagneGold,
                                                unfocusedBorderColor = BrandChampagneGold.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = cardExpiry,
                                                onValueChange = { cardExpiry = it },
                                                label = { Text("تاريخ الانتهاء") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = cardCvv,
                                                onValueChange = { cardCvv = it },
                                                label = { Text("رمز CVV") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
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

        // Bottom Wide CTA Bar: "تأكيد وإتمام الدفع الآمن"
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = Color(0xFF1B0A26).copy(alpha = 0.96f),
            tonalElevation = 12.dp,
            border = BorderStroke(1.dp, BrandChampagneGold.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(goldButtonGradient)
                        .clickable(enabled = !isSubmitting) {
                            onConfirmCheckout(
                                selectedDate,
                                selectedTimeSlot,
                                guestCount,
                                notes,
                                selectedPaymentMethod
                            )
                        }
                        .testTag("confirm_secure_checkout_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF1E0B28),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSubmitting) {
                                "جارٍ تأكيد الحجز والدفع..."
                            } else {
                                "تأكيد وإتمام الدفع الآمن ($$totalPriceUsd)"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1E0B28),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicatorPill(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isActive -> BrandChampagneGold
                        isCompleted -> EmeraldVerified
                        else -> Color.White.copy(alpha = 0.15f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ObsidianNight,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) ObsidianNight else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) BrandChampagneGold else Color.White.copy(alpha = 0.8f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = SoftGold,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
