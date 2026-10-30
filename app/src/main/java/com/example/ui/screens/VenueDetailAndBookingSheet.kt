package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerReview
import com.example.data.model.ServicePackage
import com.example.data.model.Venue
import com.example.ui.components.SmartServiceMediaView
import com.example.ui.theme.CharcoalButton
import com.example.ui.theme.CoralPinkPrimary
import com.example.ui.theme.LightBorderGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SnowGraySurface
import com.example.ui.theme.SubtitleGrayText
import com.example.ui.theme.TitleDarkText
import com.example.ui.theme.VioletBlueVip
import com.example.util.LayaliNotificationHelper
import java.util.Locale

data class VipPlanOption(
    val id: String,
    val tierTitle: String,
    val discountBadge: String,
    val priceUsd: Int,
    val durationLabel: String,
    val benefits: List<String>,
    val sourcePackage: ServicePackage
)

fun buildVipPlanOptions(venue: Venue): List<VipPlanOption> {
    val basePkg = venue.packages.firstOrNull() ?: ServicePackage(
        id = "pkg_default_${venue.id}",
        title = venue.name,
        durationLabel = "ليلة كاملة",
        priceUsd = venue.startingPriceUsd,
        description = venue.shortDescription,
        isPopular = true
    )
    val basePrice = venue.startingPriceUsd.coerceAtLeast(15)
    val secondPkg = venue.packages.getOrNull(1)
    val thirdPkg = venue.packages.getOrNull(2)

    return listOf(
        VipPlanOption(
            id = basePkg.id,
            tierTitle = "الباقة الفضية • ${basePkg.title}",
            discountBadge = "توفير 10%",
            priceUsd = basePrice,
            durationLabel = basePkg.durationLabel,
            benefits = listOf(
                "حجز فوري مؤكد في ${venue.name} (${venue.cityName})",
                basePkg.description.ifBlank { "استقبال خاص وضيافة ترحيبية فاخرة" },
                "إمكانية تعديل الموعد مجاناً قبل السهرة"
            ),
            sourcePackage = basePkg.copy(priceUsd = basePrice)
        ),
        VipPlanOption(
            id = secondPkg?.id ?: "${basePkg.id}_gold",
            tierTitle = secondPkg?.let { "الباقة الذهبية • ${it.title}" } ?: "الباقة الذهبية • جناح خاص",
            discountBadge = "الأكثر طلباً • 20%",
            priceUsd = secondPkg?.priceUsd ?: (basePrice + 30),
            durationLabel = secondPkg?.durationLabel ?: "سهرة كاملة VIP",
            benefits = listOf(
                "طاولة أو جناح مميز مع أولوية الدخول بدون انتظار",
                secondPkg?.description ?: "ضيافة متكاملة وقائمة عشاء أو جلسة عناية ملكية",
                "مضيف شخصي وخدمة كونسيرج مخصصة طوال السهرة",
                "هدية ترحيبية مجانية عند الوصول"
            ),
            sourcePackage = secondPkg ?: basePkg.copy(
                id = "${basePkg.id}_gold",
                title = "الباقة الذهبية • ${venue.name}",
                priceUsd = basePrice + 30
            )
        ),
        VipPlanOption(
            id = thirdPkg?.id ?: "${basePkg.id}_royal_vip",
            tierTitle = thirdPkg?.let { "الباقة الملكية VIP • ${it.title}" } ?: "الباقة الملكية VIP الشاملة",
            discountBadge = "VIP حصري • 30%",
            priceUsd = thirdPkg?.priceUsd ?: (basePrice + 65),
            durationLabel = thirdPkg?.durationLabel ?: "ليلة ملكية شاملة",
            benefits = listOf(
                "الجناح الملكي الحصري بأعلى درجات الخصوصية والرفاهية",
                thirdPkg?.description ?: "برنامج ضيافة ملكي مفتوح مع استقبال كبار الشخصيات",
                "خدمة طلبات خاصة وتنسيق كامل للمناسبة",
                "دعم فوري مباشر عبر الكونسيرج على مدار الساعة"
            ),
            sourcePackage = thirdPkg ?: basePkg.copy(
                id = "${basePkg.id}_royal_vip",
                title = "الباقة الملكية VIP • ${venue.name}",
                priceUsd = basePrice + 65
            )
        )
    )
}

/**
 * Modern "Choose a Plan" ModalBottomSheet for Package Selection & Instant Wallet Payment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoosePlanBookingBottomSheet(
    venue: Venue,
    walletBalanceUsd: Int,
    onDismiss: () -> Unit,
    onConfirmPlanBooking: (ServicePackage, Int) -> Unit,
    onOpenWalletTopUp: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val planOptions = remember(venue) { buildVipPlanOptions(venue) }
    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    val selectedPlan = planOptions.getOrElse(selectedPlanIndex) { planOptions.first() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LightBorderGray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sheet Header: Circular venue avatar + venue name + close X button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("close_choose_plan_sheet")
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
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${venue.cityName} • ${venue.category}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SubtitleGrayText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(VioletBlueVip.copy(alpha = 0.12f))
                            .border(1.dp, VioletBlueVip.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = venue.name.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = VioletBlueVip,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = "اختر الباقة المناسبة",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Tier Selection Cards (Choose a Plan style)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                planOptions.forEachIndexed { index, plan ->
                    val isSelected = selectedPlanIndex == index
                    val borderColor = if (isSelected) {
                        if (index == 2) CoralPinkPrimary else VioletBlueVip
                    } else {
                        MaterialTheme.colorScheme.outline
                    }

                    Surface(
                        onClick = { selectedPlanIndex = index },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            borderColor.copy(alpha = 0.06f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_tier_card_$index")
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
                                            color = if (isSelected) PureWhite else SubtitleGrayText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = plan.tierTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
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
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Benefits List with small colored dots & thin HorizontalDividers
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "مميزات الباقة المختارة",
                        style = MaterialTheme.typography.labelLarge,
                        color = SubtitleGrayText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    selectedPlan.benefits.forEachIndexed { idx, benefit ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 6.dp)
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
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (idx % 2 == 0) VioletBlueVip else CoralPinkPrimary)
                            )
                        }
                    }
                }
            }

            // 4. Quick Payment Footer: Current balance + top-up link + 50.dp capsule CTA + Cancel capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شحن الرصيد +",
                    style = MaterialTheme.typography.titleMedium,
                    color = CoralPinkPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onOpenWalletTopUp() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("sheet_topup_wallet_link")
                )

                Text(
                    text = String.format(Locale.US, "رصيدك الحالي: $%d.00", walletBalanceUsd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = {
                    onConfirmPlanBooking(selectedPlan.sourcePackage, selectedPlan.priceUsd)
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralPinkPrimary,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sheet_confirm_booking_button")
            ) {
                Text(
                    text = "تأكيد الحجز والدفع • $${selectedPlan.priceUsd}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("sheet_cancel_booking_button")
            ) {
                Text(
                    text = "إلغاء",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VenueDetailScreen(
    venue: Venue,
    liveReviews: List<CustomerReview>,
    isFavorite: Boolean,
    isSubmitting: Boolean,
    isAdminUnlocked: Boolean = false,
    walletBalanceUsd: Int = 0,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onProceedToCheckout: (ServicePackage) -> Unit,
    onSubmitReview: (authorName: String, serviceType: String, rating: Int, comment: String) -> Unit,
    onDeleteReview: (String) -> Unit,
    onUpdateStartingPrice: (Int) -> Unit = {},
    onUpdatePhoto: (String) -> Unit = {},
    onOpenWalletTopUp: () -> Unit = {}
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var showChoosePlanSheet by remember { mutableStateOf(false) }
    var showReviewSheet by remember { mutableStateOf(false) }
    var showAdminEditSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("venue_detail_list"),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header & Admin-only Media Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Navigation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAdminUnlocked) {
                                IconButton(
                                    onClick = { showAdminEditSheet = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(VioletBlueVip.copy(alpha = 0.12f))
                                        .border(1.dp, VioletBlueVip, CircleShape)
                                        .testTag("detail_admin_edit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل السعر والصورة",
                                        tint = VioletBlueVip
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .testTag("detail_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "حفظ",
                                    tint = if (isFavorite) CoralPinkPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Creator/Venue Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = VioletBlueVip.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${venue.cityName} • ${venue.category}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VioletBlueVip,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = venue.providerDisplayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SubtitleGrayText
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(VioletBlueVip.copy(alpha = 0.12f))
                                            .border(1.dp, VioletBlueVip.copy(alpha = 0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = venue.name.take(1),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = VioletBlueVip,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Admin-assigned Media View (or clean placeholder)
                            SmartServiceMediaView(
                                imageUrl = venue.visualPresetKey,
                                accentHex = venue.accentHex,
                                secondaryHex = venue.secondaryHex,
                                category = venue.category,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )

                            // Price & Rating summary row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CoralPinkPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${venue.ratingAverage} (${venue.reviewCount} تقييم)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SubtitleGrayText
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "يبدأ من $%d.00", venue.startingPriceUsd),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = venue.fullDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SubtitleGrayText
                            )

                            // Secondary 2-button capsule row ("مراسلة", "إكرامية / طلب خاص")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { LayaliNotificationHelper.openTelegramSupport(context) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = VioletBlueVip
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مراسلة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { showChoosePlanSheet = true },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = CoralPinkPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "إكرامية / باقات",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reviews Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showReviewSheet = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_review_dialog_button")
                            ) {
                                Text(
                                    text = "+ أضف تقييمك",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = "آراء الضيوف الموثقة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        venue.curatedReviews.forEach { review ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${review.rating}.0 ★",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = CoralPinkPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${review.authorName} • ${review.cityBadge}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = review.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SubtitleGrayText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sticky Capsule CTA Bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showChoosePlanSheet = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPinkPrimary,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_booking_sheet_button")
                ) {
                    Text(
                        text = "احجز الآن • باقات تبدأ من $${venue.startingPriceUsd}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (showChoosePlanSheet) {
        ChoosePlanBookingBottomSheet(
            venue = venue,
            walletBalanceUsd = walletBalanceUsd,
            onDismiss = { showChoosePlanSheet = false },
            onConfirmPlanBooking = { selectedPkg, _ ->
                showChoosePlanSheet = false
                onProceedToCheckout(selectedPkg)
            },
            onOpenWalletTopUp = {
                showChoosePlanSheet = false
                onOpenWalletTopUp()
            }
        )
    }

    if (showReviewSheet) {
        AddReviewDialog(
            venue = venue,
            isSubmitting = isSubmitting,
            onDismiss = { showReviewSheet = false },
            onConfirmReview = { author, service, rating, comment ->
                onSubmitReview(author, service, rating, comment)
                showReviewSheet = false
            }
        )
    }

    if (showAdminEditSheet && isAdminUnlocked) {
        AdminEditVenueSheet(
            venue = venue,
            onDismiss = { showAdminEditSheet = false },
            onSavePrice = { newPrice ->
                onUpdateStartingPrice(newPrice)
                showAdminEditSheet = false
            },
            onSelectPhoto = { photoKey ->
                onUpdatePhoto(photoKey)
                showAdminEditSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminEditVenueSheet(
    venue: Venue,
    onDismiss: () -> Unit,
    onSavePrice: (Int) -> Unit,
    onSelectPhoto: (String) -> Unit
) {
    var priceInput by remember(venue.startingPriceUsd) {
        mutableStateOf(venue.startingPriceUsd.toString())
    }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectPhoto(uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "تعديل السعر وصورة الخدمة (الإدارة)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = priceInput,
                onValueChange = { input -> priceInput = input.filter { it.isDigit() } },
                label = { Text("السعر المعتمد بالدولار ($)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_venue_price_input")
            )

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VioletBlueVip,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_pick_gallery_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تعيين / تغيير الصورة من المعرض",
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Normal, maxLines = 1)
                }

                Button(
                    onClick = {
                        val parsed = priceInput.toIntOrNull() ?: venue.startingPriceUsd
                        onSavePrice(parsed)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPinkPrimary,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_save_venue_price_button")
                ) {
                    Text("حفظ السعر", fontWeight = FontWeight.Normal, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewDialog(
    venue: Venue,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmReview: (authorName: String, serviceType: String, rating: Int, comment: String) -> Unit
) {
    var authorName by remember { mutableStateOf("") }
    var selectedService by remember {
        mutableStateOf(venue.packages.firstOrNull()?.title ?: venue.category)
    }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "إضافة تقييم موثق • ${venue.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = authorName,
                onValueChange = { authorName = it },
                label = { Text("الاسم الكريم") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_author_input")
            )

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("اكتب تجربتك") },
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_comment_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Normal, maxLines = 1)
                }

                Button(
                    onClick = {
                        onConfirmReview(
                            authorName.ifBlank { "ضيف VIP" },
                            selectedService,
                            rating,
                            comment.ifBlank { "خدمة ممتازة وراقية جداً." }
                        )
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPinkPrimary,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_review_button")
                ) {
                    Text("نشر التقييم", fontWeight = FontWeight.Normal, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
