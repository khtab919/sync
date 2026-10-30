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
    val totalPriceUsd: Long = 50L,
    val notes: String = "",
    val status: String = "confirmed",
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

data class UserProfile(
    val displayName: String = "",
    val telegramHandle: String = "",
    val roleTitle: String = "",
    val bio: String = "",
    val cityName: String = "",
    val country: String = "",
    val avatarUri: String = "",
    val isAvailableNow: Boolean = true,
    val customPhotos: List<String> = emptyList()
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
