package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val venueId: String = "",
    val venueName: String = "",
    val cityName: String = "",
    val country: String = "",
    val serviceTitle: String = "",
    val category: String = "",
    val bookingDate: String = "",
    val timeSlot: String = "",
    val guestCount: Long = 1L,
    val totalPriceUsd: Long = 0L,
    val notes: String = "",
    val status: String = "pending",
    val paymentMethodTitle: String = "شام كاش / USDT",
    val receiptImageUri: String = "",
    val transactionReference: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomerReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val venueId: String = "",
    val venueName: String = "",
    val cityName: String = "",
    val serviceType: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FavoriteVenue(
    @DocumentId val id: String = "",
    val userId: String = "",
    val venueId: String = "",
    val venueName: String = "",
    val cityName: String = "",
    val category: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublishedService(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val title: String = "",
    val cityName: String = "",
    val country: String = "سوريا",
    val category: String = "سبا ومساج",
    val description: String = "",
    val priceUsd: Long = 50L,
    val imageUrl: String = "",
    val videoUrl: String = "",
    val mediaType: String = "both",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedIdentifier: String = "",
    val reason: String = "",
    val cityScope: String = "جميع المدن",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "@layali_vip",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val cityName: String = "دمشق",
    val country: String = "سوريا",
    val avatarUri: String = "",
    val selectedFrameId: String = "emerald_crown",
    val level: Long = 1L,
    val xpPoints: Long = 150L,
    val nextLevelXp: Long = 2500L,
    val walletBalanceUsd: Long = 0L,
    val chatPriceUsd: Long = 15L,
    val isPaidChatEnabled: Boolean = true,
    val passwordSaltHex: String = "",
    val passwordHashHex: String = "",
    val role: String = "customer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class UserProfile(
    val displayName: String = "",
    val username: String = "@layali_vip",
    val telegramHandle: String = "",
    val roleTitle: String = "",
    val bio: String = "",
    val cityName: String = "",
    val country: String = "",
    val avatarUri: String = "",
    val isAvailableNow: Boolean = true,
    val customPhotos: List<String> = emptyList(),
    val level: Int = 1,
    val xpPoints: Int = 150,
    val nextLevelXp: Int = 2500,
    val selectedFrameId: String = "emerald_crown",
    val chatPriceUsd: Int = 15,
    val isPaidChatEnabled: Boolean = true
)

data class AvatarFrameStyle(
    val id: String,
    val nameAr: String,
    val subtitleAr: String,
    val minLevel: Int,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val badgeLabel: String
)

object AvatarFrameCatalog {
    val frames: List<AvatarFrameStyle> = listOf(
        AvatarFrameStyle(
            id = "emerald_crown",
            nameAr = "الزمرد الكلاسيكي",
            subtitleAr = "إطار زمردي مرصع بالبلاتين والياقوت",
            minLevel = 1,
            primaryHex = 0xFF10B981L,
            secondaryHex = 0xFF0EA5E9L,
            accentHex = 0xFF6EE7B7L,
            badgeLabel = "ROYAL"
        ),
        AvatarFrameStyle(
            id = "amber_sovereign",
            nameAr = "بتلات الروز الملكي",
            subtitleAr = "هالة وردية إمبراطورية مزدوجة لكبار الشخصيات",
            minLevel = 5,
            primaryHex = 0xFFEC4899L,
            secondaryHex = 0xFFA855F7L,
            accentHex = 0xFFF9A8D4L,
            badgeLabel = "ROSE VIP"
        ),
        AvatarFrameStyle(
            id = "sapphire_neon",
            nameAr = "سداسي السافير",
            subtitleAr = "إطار كريستالي مضيء بتصميم هندسي فاخر",
            minLevel = 12,
            primaryHex = 0xFF0EA5E9L,
            secondaryHex = 0xFF6366F1L,
            accentHex = 0xFF7DD3FCL,
            badgeLabel = "SAPPHIRE"
        ),
        AvatarFrameStyle(
            id = "amethyst_vip",
            nameAr = "كوكبة الجمشت",
            subtitleAr = "إطار أرجواني ملكي مرصع بـ 12 ماسة",
            minLevel = 20,
            primaryHex = 0xFFA855F7L,
            secondaryHex = 0xFFEC4899L,
            accentHex = 0xFFD8B4FEL,
            badgeLabel = "IMPERIAL"
        ),
        AvatarFrameStyle(
            id = "crimson_flame",
            nameAr = "شعلة الروبي",
            subtitleAr = "إطار ياقوتي متوهج لنخبة الداعمين",
            minLevel = 35,
            primaryHex = 0xFFF43F5EL,
            secondaryHex = 0xFF9333EAL,
            accentHex = 0xFFFDA4AFL,
            badgeLabel = "LEGEND"
        ),
        AvatarFrameStyle(
            id = "obsidian_elite",
            nameAr = "أوبسيديان ألماسي",
            subtitleAr = "إطار ألماسي بلاتيني حصري للمستويات العليا",
            minLevel = 50,
            primaryHex = 0xFFE2E8F0L,
            secondaryHex = 0xFFA855F7L,
            accentHex = 0xFFFFFFFFL,
            badgeLabel = "DIAMOND"
        )
    )

    fun findById(frameId: String): AvatarFrameStyle {
        return frames.find { it.id == frameId } ?: frames.first()
    }
}

object OfficialPaymentCredentials {
    // From User Screenshot 1: USDT (BEP-20) Tether USD/USDT
    const val USDT_NETWORK = "(BEP-20) Tether USD/USDT"
    const val USDT_ACCOUNT_ID = "MT5Standard1250311802"
    const val USDT_DEPOSIT_ADDRESS = "0xc35797768180752b67743d791f79f812bb0972d1"

    // From User Screenshot 2: Syrian Sham Cash (شام كاش السوري)
    const val SHAM_CASH_TITLE = "شام كاش السوري (Sham Cash)"
    const val SHAM_CASH_ADDRESS = "42c9f2bd84250c5ab6c444616b8ef0c2"
}

data class PaymentReceiptRecord(
    val id: String,
    val userId: String,
    val userName: String,
    val usernameHandle: String,
    val methodKey: String,          // "SHAM_CASH" or "USDT_BEP20"
    val methodTitleAr: String,
    val amountUsd: Int,
    val bonusUsd: Int = 0,
    val receiptImageUri: String,
    val transactionReference: String,
    val purposeAr: String,          // "شحن محفظة", "حجز خدمة", "فتح دردشة مدفوعة"
    val status: String = "pending", // Never assume receipt is verified by default
    val timestampLabel: String = "الآن"
)

data class ChatMemberProfile(
    val id: String,
    val displayName: String,
    val username: String,
    val cityName: String,
    val country: String,
    val category: String,
    val bio: String,
    val level: Int,
    val frameId: String,
    val chatPriceUsd: Int,
    val isOnline: Boolean,
    val avatarPreset: String,
    val isUnlocked: Boolean = false
)

data class ChatMessageItem(
    val id: String,
    val memberId: String,
    val senderName: String,
    val senderUsername: String,
    val text: String,
    val timestampLabel: String,
    val isFromCurrentUser: Boolean,
    val paidBadgeUsd: Int = 0
)

data class CityInfo(
    val id: String,
    val nameAr: String,
    val country: String,
    val tagline: String,
    val activeVenuesCount: Int
)

data class ServicePackage(
    val id: String,
    val title: String,
    val durationLabel: String,
    val priceUsd: Int,
    val description: String,
    val isPopular: Boolean = false
)

data class CuratedReview(
    val id: String,
    val authorName: String,
    val cityBadge: String,
    val serviceTitle: String,
    val rating: Int,
    val comment: String,
    val dateLabel: String,
    val isVerifiedBooking: Boolean = true
)

data class Venue(
    val id: String,
    val name: String,
    val cityName: String,
    val country: String,
    val neighborhood: String,
    val category: String,
    val shortDescription: String,
    val fullDescription: String,
    val ratingAverage: Double,
    val reviewCount: Int,
    val startingPriceUsd: Int,
    val openingHours: String,
    val badges: List<String>,
    val accentHex: Long,
    val secondaryHex: Long,
    val packages: List<ServicePackage>,
    val curatedReviews: List<CuratedReview>,
    val availableTimeSlots: List<String>,
    val providerDisplayName: String = "جناح ملكي معتمد",
    val providerUsername: String = "@layali_royal",
    val providerLevel: Int = 18,
    val providerFrameId: String = "emerald_crown",
    val providerChatPriceUsd: Int = 20,
    val isOnlineNow: Boolean = true,
    val visualPresetKey: String = "brand_dark_showcase",
    val showcaseVideoTitle: String = "عرض فيديو توضيحي 4K للأجنحة الملكية والسهرات",
    val showcaseDuration: String = "01:45 HD",
    val galleryHighlights: List<String> = listOf(
        "الجناح الملكي الخاص والشموع",
        "جلسة المساج بالزيوت العطرية الدافئة",
        "صالة السهرات الفاخرة والموسيقى الحية",
        "ركن الضيافة الملكية والاستقبال VIP"
    )
)
