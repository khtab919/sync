package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServicePackage
import com.example.data.model.Venue
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.LuxuryGradientButton
import com.example.ui.components.MandatoryReceiptAttachmentCard
import com.example.ui.components.ShamCashOfficialCard
import com.example.ui.components.UsdtBep20OfficialCard
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ShamCashTeal
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SubtitleGrayText
import com.example.ui.theme.UsdtTetherGreen
import com.example.ui.theme.VioletBlueVip
import java.util.Locale

enum class PaymentMethodType(val titleAr: String, val subtitleAr: String, val methodKey: String) {
    USDT_BEP20(
        "USDT (BEP-20) Tether",
        "إيداع فوري عبر باركود + رمز QR مع إرفاق الإيصال",
        "USDT_BEP20"
    ),
    SHAM_CASH(
        "شام كاش السوري (Sham Cash)",
        "تحويل مباشر عبر رمز QR شام كاش مع إرفاق الإيصال",
        "SHAM_CASH"
    ),
    WALLET(
        "الدفع من رصيد المحفظة الموثق",
        "خصم فوري من رصيد محفظتك المشحون مسبقاً",
        "WALLET"
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(
    venue: Venue,
    selectedPackage: ServicePackage,
    walletBalanceUsd: Int,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onOpenWalletTopUp: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    onConfirmCheckout: (
        bookingDate: String,
        timeSlot: String,
        guestCount: Int,
        notes: String,
        paymentMethod: PaymentMethodType,
        receiptImageUri: String,
        transactionRef: String
    ) -> Unit
) {
    BackHandler { onBack() }

    // Never offer synthetic Silver/Gold tiers with prices absent from the backend.
    val planOptions = remember(venue) {
        buildVipPlanOptions(venue).take(1).map { plan ->
            plan.copy(
                tierTitle = venue.name,
                discountBadge = "السعر المعلن",
                priceUsd = venue.startingPriceUsd,
                benefits = listOf(venue.shortDescription)
            )
        }
    }
    var selectedPlanIndex by remember(selectedPackage.id) {
        val idx = planOptions.indexOfFirst { it.id == selectedPackage.id || it.priceUsd == selectedPackage.priceUsd }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    val activePlan = planOptions.getOrElse(selectedPlanIndex) { planOptions.first() }

    // Canonical yyyy-MM-dd dates match server-side availability keys.
    val availableDates = remember {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        (0..13).map { offset ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, offset)
            formatter.format(calendar.time)
        }
    }
    var selectedDate by remember(venue.id) { mutableStateOf(availableDates.first()) }
    var selectedTimeSlot by remember(venue.id) { mutableStateOf("") }
    var guestCount by remember { mutableIntStateOf(1) }
    var availableSlots by remember(venue.id) { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    LaunchedEffect(venue.id, selectedDate, guestCount) {
        availableSlots = emptyList()
        selectedTimeSlot = ""
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
            .collection("service_availability").document(venue.id).collection("slots")
            .whereEqualTo("status", "open").snapshots()
            .catch { availableSlots = emptyList() }
            .collect { snapshot ->
                val slots = snapshot.documents.filter { doc ->
                    doc.getString("bookingDate") == selectedDate &&
                        (doc.getLong("remainingGuests") ?: 0L) >= guestCount.toLong()
                }.mapNotNull { it.getString("timeSlot") }.distinct().sorted()
                availableSlots = slots
                if (selectedTimeSlot !in slots) selectedTimeSlot = slots.firstOrNull().orEmpty()
            }
    }
    var notes by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethodType.USDT_BEP20) }
    var attachedReceiptUri by remember { mutableStateOf("") }
    var transactionRef by remember { mutableStateOf("") }

    val totalPriceUsd = (activePlan.priceUsd * guestCount).coerceAtLeast(5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("checkout_screen_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 175.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header: Framed Provider Avatar + Name + @username + Close Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "موثق",
                                    tint = VioletBlueVip,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = venue.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${venue.providerUsername} • ${venue.cityName} • ${venue.category}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SovereignGold
                            )
                        }

                        FramedUserAvatar(
                            avatarUri = venue.visualPresetKey,
                            fallbackInitial = venue.name.take(1),
                            frameId = venue.providerFrameId,
                            level = venue.providerLevel,
                            avatarSize = 48.dp,
                            isOnline = venue.isOnlineNow
                        )
                    }
                }
            }

            item {
                Text(
                    text = "تأكيد الحجز الملكي والباقة",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Tier Selection Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    planOptions.forEachIndexed { index, plan ->
                        val isSelected = selectedPlanIndex == index
                        val borderColor = if (isSelected) {
                            if (index == 2) SovereignGold else VioletBlueVip
                        } else {
                            MaterialTheme.colorScheme.outline
                        }

                        Surface(
                            onClick = { selectedPlanIndex = index },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) {
                                borderColor.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = borderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_plan_tier_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPlanIndex = index },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = borderColor,
                                        unselectedColor = SubtitleGrayText
                                    )
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = if (isSelected) borderColor else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = plan.discountBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) CharcoalNight else SubtitleGrayText,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = plan.tierTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "/ ${plan.durationLabel}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SubtitleGrayText
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%d.00", plan.priceUsd),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SovereignGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Benefits List
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_order_summary_card")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "مميزات الباقة المختارة",
                            style = MaterialTheme.typography.labelLarge,
                            color = SovereignGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        activePlan.benefits.forEachIndexed { idx, benefit ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (idx % 2 == 0) SovereignGold else VioletBlueVip)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Date, Time & Guest Count Selector
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "تحديد موعد الحجز والعدد",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableDates.forEach { d ->
                                FilterChip(
                                    selected = selectedDate == d,
                                    onClick = { selectedDate = d },
                                    label = { Text(d, maxLines = 1) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SovereignGold,
                                        selectedLabelColor = CharcoalNight
                                    )
                                )
                            }
                        }

                        if (availableSlots.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableSlots.forEach { slot ->
                                    FilterChip(
                                        selected = selectedTimeSlot == slot,
                                        onClick = { selectedTimeSlot = slot },
                                        label = { Text(slot, maxLines = 1) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VioletBlueVip,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "لا توجد مواعيد متاحة لهذا التاريخ حالياً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (guestCount > 1) guestCount-- }) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = SovereignGold)
                                }
                                Text(
                                    text = "$guestCount",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { if (guestCount < 15) guestCount++ }) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = SovereignGold)
                                }
                            }
                            Text(
                                text = "عدد الأشخاص / الجلسات",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SubtitleGrayText
                            )
                        }
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("ملاحظات خاصة للحجز (اختياري)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 5. Real Payment Gateway Selection (USDT BEP-20, Sham Cash, or Verified Wallet)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "وسيلة الدفع الحقيقي وتوثيق الإيصال:",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    PaymentMethodType.entries.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        val methodIcon = when (method) {
                            PaymentMethodType.USDT_BEP20 -> Icons.Default.QrCode2
                            PaymentMethodType.SHAM_CASH -> Icons.Default.ReceiptLong
                            PaymentMethodType.WALLET -> Icons.Default.AccountBalanceWallet
                        }

                        Surface(
                            onClick = { selectedPaymentMethod = method },
                            color = if (isSelected) SovereignGold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SovereignGold else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_method_${method.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPaymentMethod = method },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = SovereignGold,
                                        unselectedColor = SubtitleGrayText
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
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (method == PaymentMethodType.WALLET) {
                                                String.format(Locale.US, "الرصيد المتوفر: $%d.00", walletBalanceUsd)
                                            } else {
                                                method.subtitleAr
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (method == PaymentMethodType.WALLET && walletBalanceUsd >= totalPriceUsd) {
                                                EmeraldVerified
                                            } else {
                                                SubtitleGrayText
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = methodIcon,
                                        contentDescription = method.titleAr,
                                        tint = SovereignGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Live Payment Gateway Card + Mandatory Receipt Attachment
            if (selectedPaymentMethod == PaymentMethodType.USDT_BEP20) {
                item {
                    UsdtBep20OfficialCard(
                        amountUsd = totalPriceUsd,
                        onCopyMessage = onShowMessage
                    )
                }
                item {
                    MandatoryReceiptAttachmentCard(
                        receiptImageUri = attachedReceiptUri,
                        transactionRef = transactionRef,
                        onReceiptSelected = { uri ->
                            attachedReceiptUri = uri
                            onShowMessage("تم إرفاق صورة إيصال USDT بنجاح ✓")
                        },
                        onTransactionRefChange = { transactionRef = it }
                    )
                }
            } else if (selectedPaymentMethod == PaymentMethodType.SHAM_CASH) {
                item {
                    ShamCashOfficialCard(
                        amountUsd = totalPriceUsd,
                        onCopyMessage = onShowMessage
                    )
                }
                item {
                    MandatoryReceiptAttachmentCard(
                        receiptImageUri = attachedReceiptUri,
                        transactionRef = transactionRef,
                        onReceiptSelected = { uri ->
                            attachedReceiptUri = uri
                            onShowMessage("تم إرفاق صورة إيصال شام كاش بنجاح ✓")
                        },
                        onTransactionRefChange = { transactionRef = it }
                    )
                }
            }
        }

        // 7. Sticky Bottom Confirmation Bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شحن المحفظة عبر شام كاش / USDT +",
                        style = MaterialTheme.typography.labelLarge,
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenWalletTopUp() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("checkout_topup_wallet_link")
                    )

                    Text(
                        text = String.format(Locale.US, "رصيدك: $%d.00", walletBalanceUsd),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                LuxuryGradientButton(
                    text = if (isSubmitting) {
                        "جارٍ توثيق الحجز والإيصال..."
                    } else if (selectedPaymentMethod != PaymentMethodType.WALLET && attachedReceiptUri.isBlank()) {
                        "أرفق إيصال الدفع لتأكيد الحجز ($$totalPriceUsd)"
                    } else {
                        "تأكيد الحجز وإرسال الإيصال • $$totalPriceUsd"
                    },
                    icon = Icons.Default.CheckCircle,
                    enabled = !isSubmitting,
                    isEmeraldStyle = attachedReceiptUri.isNotBlank() ||
                        (selectedPaymentMethod == PaymentMethodType.WALLET && walletBalanceUsd >= totalPriceUsd),
                    onClick = {
                        if (selectedTimeSlot.isBlank()) {
                            onShowMessage("هذه الخدمة ليس لديها موعد متاح موثق من الخادم حالياً.")
                        } else if (selectedPaymentMethod == PaymentMethodType.WALLET) {
                            if (walletBalanceUsd < totalPriceUsd) {
                                onShowMessage("رصيد المحفظة ($$walletBalanceUsd) غير كافٍ لطلب ($$totalPriceUsd). اختر شام كاش أو USDT وأرفق الإيصال.")
                            } else {
                                onConfirmCheckout(
                                    selectedDate,
                                    selectedTimeSlot,
                                    guestCount,
                                    notes,
                                    selectedPaymentMethod,
                                    "",
                                    "WALLET_DEDUCT"
                                )
                            }
                        } else {
                            if (attachedReceiptUri.isBlank()) {
                                onShowMessage("يرجى إرفاق صورة إيصال التحويل (${selectedPaymentMethod.titleAr}) أولاً لإتمام الحجز!")
                            } else {
                                onConfirmCheckout(
                                    selectedDate,
                                    selectedTimeSlot,
                                    guestCount,
                                    notes,
                                    selectedPaymentMethod,
                                    attachedReceiptUri,
                                    transactionRef
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_secure_checkout_button"
                )

                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        text = "إلغاء والعودة",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
