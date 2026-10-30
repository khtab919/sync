package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.LayaliCatalog
import com.example.data.model.BlockedUser
import com.example.data.model.Booking
import com.example.data.model.CityInfo
import com.example.data.model.CustomerReview
import com.example.data.model.FavoriteVenue
import com.example.data.model.PublishedService
import com.example.data.model.UserProfile
import com.example.data.model.Venue
import com.example.data.repository.LayaliRepository
import com.example.security.MiddlewareResult
import com.example.security.SuperAdminSecurityManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SortOption(val labelAr: String) {
    HIGHEST_RATED("الأعلى تقييماً"),
    MOST_REVIEWED("الأكثر تقييماً"),
    LOWEST_PRICE("الأقل سعراً")
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val amountUsd: Int,
    val bonusUsd: Int = 0,
    val isCredit: Boolean = true,
    val timestampLabel: String = "الآن"
)

class LayaliViewModel(
    private val repository: LayaliRepository,
    val currentUserId: String
) : ViewModel() {

    private val _selectedCountry = MutableStateFlow("الكل")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.HIGHEST_RATED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedVenue = MutableStateFlow<Venue?>(null)
    val selectedVenue: StateFlow<Venue?> = _selectedVenue.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isSoftRoseLightTheme = MutableStateFlow(false)
    val isSoftRoseLightTheme: StateFlow<Boolean> = _isSoftRoseLightTheme.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Wallet Balance & Transactions (starts at $0 as shown in Screen 4 of the UI/UX Case Study)
    private val _walletBalanceUsd = MutableStateFlow(0)
    val walletBalanceUsd: StateFlow<Int> = _walletBalanceUsd.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    // Admin custom price, photo, and deletion overrides for catalog venues
    private val _adminSessionToken = MutableStateFlow<String?>(null)
    val adminSessionToken: StateFlow<String?> = _adminSessionToken.asStateFlow()

    private val _customVenuePrices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val customVenuePrices: StateFlow<Map<String, Int>> = _customVenuePrices.asStateFlow()

    private val _customVenuePhotos = MutableStateFlow<Map<String, String>>(emptyMap())
    val customVenuePhotos: StateFlow<Map<String, String>> = _customVenuePhotos.asStateFlow()

    private val _deletedCatalogVenueIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedCatalogVenueIds: StateFlow<Set<String>> = _deletedCatalogVenueIds.asStateFlow()

    // Initial curated published showcase offers (matching the UI/UX Case Study & Brand Identity)
    private val _localPublishedServices = MutableStateFlow(
        listOf(
            PublishedService(
                id = "vip_showcase_mira",
                authorId = "admin",
                title = "قصر السهرات والقدود الحلبية • جناح VIP",
                cityName = "حلب",
                country = "سوريا",
                category = "سهرات فنية",
                description = "سهرة طرب وقدود حلبية أصيلة في القاعة الملكية المذهبة مع ضيافة فاخرة.",
                priceUsd = 50L,
                imageUrl = "palace_hall",
                videoUrl = "عرض فيديو 4K لقصر السهرات والقدود الحلبية",
                mediaType = "both",
                status = "active"
            ),
            PublishedService(
                id = "vip_showcase_lina",
                authorId = "admin",
                title = "قصر العزت الفام الحلبية • لاونج خاص",
                cityName = "حلب",
                country = "سوريا",
                category = "سهرات فنية",
                description = "جلسات لاونج مخملية وسهرة فنية خاصة بأجواء ملكية دافئة.",
                priceUsd = 65L,
                imageUrl = "card_lounge",
                videoUrl = "فيديو حي من قصر العزت الفام الحلبية",
                mediaType = "both",
                status = "active"
            ),
            PublishedService(
                id = "vip_showcase_nour",
                authorId = "admin",
                title = "سبا ومساج ملكي خاص • زيوت عطرية",
                cityName = "دمشق",
                country = "سوريا",
                category = "سبا ومساج",
                description = "جلسة مساج استرخائي بالزيوت العطرية الدافئة والأحجار البركانية.",
                priceUsd = 45L,
                imageUrl = "card_spa",
                videoUrl = "جولة فيديو داخل جناح المساج والسبا الملكي",
                mediaType = "both",
                status = "active"
            ),
            PublishedService(
                id = "vip_showcase_sara",
                authorId = "admin",
                title = "بطاقة فنية وزيارات خارجية VIP",
                cityName = "إسطنبول",
                country = "تركيا",
                category = "زيارات خارجية",
                description = "خدمة الزيارات الخارجية الملكية للفنادق والمنازل مع سرية وخصوصية تامة.",
                priceUsd = 75L,
                imageUrl = "card_vip_night",
                videoUrl = "عرض فيديو HD لخدمة الزيارات الخارجية والسهرة الخاصة",
                mediaType = "both",
                status = "active"
            )
        )
    )

    private val _localBookings = MutableStateFlow<List<Booking>>(emptyList())
    private val _localReviews = MutableStateFlow<List<CustomerReview>>(emptyList())
    private val _localFavorites = MutableStateFlow<List<FavoriteVenue>>(emptyList())
    private val _localBlockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())

    // Tracks seen service IDs so newly published services trigger Android notifications for all connected clients
    private val seenPublishedIds = mutableSetOf(
        "vip_showcase_mira",
        "vip_showcase_lina",
        "vip_showcase_nour",
        "vip_showcase_sara"
    )
    private var initialSnapshotLoaded = false

    private val _incomingBroadcastAlert = MutableStateFlow<PublishedService?>(null)
    val incomingBroadcastAlert: StateFlow<PublishedService?> = _incomingBroadcastAlert.asStateFlow()

    val bookingsState: StateFlow<UiState<List<Booking>>> = combine(
        repository.observeBookings(currentUserId).catch { emit(emptyList()) },
        _localBookings
    ) { remote, local ->
        val merged = (local + remote).distinctBy { it.id }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(emptyList())
    )

    val userReviewsState: StateFlow<UiState<List<CustomerReview>>> = combine(
        repository.observeUserReviews(currentUserId).catch { emit(emptyList()) },
        _localReviews
    ) { remote, local ->
        val merged = (local + remote).distinctBy { it.id }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(emptyList())
    )

    val favoritesState: StateFlow<UiState<List<FavoriteVenue>>> = combine(
        repository.observeFavorites(currentUserId).catch { emit(emptyList()) },
        _localFavorites
    ) { remote, local ->
        val merged = (local + remote).distinctBy { it.venueId }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(emptyList())
    )

    val publishedServicesState: StateFlow<UiState<List<PublishedService>>> = combine(
        repository.observePublishedServices()
            .onEach { list ->
                if (!initialSnapshotLoaded) {
                    seenPublishedIds.addAll(list.map { it.id })
                    initialSnapshotLoaded = true
                } else {
                    val newlyAdded = list.firstOrNull { it.id.isNotBlank() && !seenPublishedIds.contains(it.id) }
                    list.forEach { if (it.id.isNotBlank()) seenPublishedIds.add(it.id) }
                    if (newlyAdded != null) {
                        _incomingBroadcastAlert.value = newlyAdded
                    }
                }
            }
            .catch { emit(emptyList()) },
        _localPublishedServices
    ) { remote, local ->
        val merged = (remote + local).distinctBy { it.id }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(_localPublishedServices.value)
    )

    val blockedUsersState: StateFlow<UiState<List<BlockedUser>>> = combine(
        repository.observeBlockedUsers(currentUserId).catch { emit(emptyList()) },
        _localBlockedUsers
    ) { remote, local ->
        val merged = (local + remote).distinctBy { it.id }
        UiState.Success(merged)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(emptyList())
    )

    val filteredCities: StateFlow<List<CityInfo>> = _selectedCountry
        .map { country ->
            if (country == "الكل") LayaliCatalog.cities
            else LayaliCatalog.cities.filter { it.country == country }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LayaliCatalog.cities
        )

    val filteredVenues: StateFlow<List<Venue>> = combine(
        _selectedCountry,
        _selectedCity,
        _selectedCategory,
        _searchQuery,
        combine(
            _sortOption,
            _customVenuePrices,
            _customVenuePhotos,
            _deletedCatalogVenueIds
        ) { s, p, ph, del ->
            listOf(s, p, ph, del)
        }
    ) { country, city, category, query, extraList ->
        @Suppress("UNCHECKED_CAST")
        val sort = extraList[0] as SortOption
        @Suppress("UNCHECKED_CAST")
        val priceMap = extraList[1] as Map<String, Int>
        @Suppress("UNCHECKED_CAST")
        val photoMap = extraList[2] as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        val deletedIds = extraList[3] as Set<String>

        val trimmedQuery = query.trim()
        LayaliCatalog.venues
            .filterNot { deletedIds.contains(it.id) }
            .map { venue ->
                val updatedPrice = priceMap[venue.id] ?: venue.startingPriceUsd
                val updatedPhoto = photoMap[venue.id] ?: venue.visualPresetKey
                val updatedPackages = if (priceMap.containsKey(venue.id)) {
                    venue.packages.mapIndexed { idx, pkg ->
                        if (idx == 0) pkg.copy(priceUsd = updatedPrice) else pkg
                    }
                } else {
                    venue.packages
                }
                venue.copy(
                    startingPriceUsd = updatedPrice,
                    visualPresetKey = updatedPhoto,
                    packages = updatedPackages
                )
            }
            .filter { venue ->
                val matchesCountry = country == "الكل" || venue.country == country
                val matchesCity = city == null || venue.cityName == city
                val matchesCategory = category == "الكل" || venue.category == category
                val matchesQuery = trimmedQuery.isEmpty() ||
                    venue.name.contains(trimmedQuery, ignoreCase = true) ||
                    venue.cityName.contains(trimmedQuery, ignoreCase = true) ||
                    venue.neighborhood.contains(trimmedQuery, ignoreCase = true) ||
                    venue.shortDescription.contains(trimmedQuery, ignoreCase = true) ||
                    venue.packages.any { it.title.contains(trimmedQuery, ignoreCase = true) }
                matchesCountry && matchesCity && matchesCategory && matchesQuery
            }
            .let { list ->
                when (sort) {
                    SortOption.HIGHEST_RATED -> list.sortedByDescending { it.ratingAverage }
                    SortOption.MOST_REVIEWED -> list.sortedByDescending { it.reviewCount }
                    SortOption.LOWEST_PRICE -> list.sortedBy { it.startingPriceUsd }
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LayaliCatalog.venues
    )

    fun setSuperAdminSessionToken(token: String?) {
        _adminSessionToken.value = token
    }

    fun syncAuthenticatedAccount(
        authenticatedDisplayName: String,
        isSuperAdmin: Boolean
    ) {
        val current = _userProfile.value
        if (isSuperAdmin) {
            if (current.displayName.isBlank()) {
                _userProfile.value = current.copy(
                    displayName = authenticatedDisplayName.ifBlank { "Super Admin" },
                    telegramHandle = "@RamaNUR",
                    roleTitle = "المسؤول الرئيسي للنظام (Super Admin)"
                )
            }
        } else if (authenticatedDisplayName.isNotBlank()) {
            if (current.displayName.isBlank() || current.roleTitle.contains("Super Admin")) {
                _userProfile.value = UserProfile(
                    displayName = authenticatedDisplayName,
                    telegramHandle = "",
                    roleTitle = "",
                    bio = "",
                    cityName = "",
                    country = ""
                )
            }
        } else {
            // Unauthenticated / Guest: keep profile completely empty (no default account)
            _userProfile.value = UserProfile()
        }
    }

    fun clearAuthenticatedAccount() {
        _adminSessionToken.value = null
        _userProfile.value = UserProfile()
    }

    private fun enforceSuperAdminMiddleware(): Boolean {
        return when (val result = SuperAdminSecurityManager.requireSuperAdminMiddleware(_adminSessionToken.value)) {
            MiddlewareResult.Authorized -> true
            is MiddlewareResult.Forbidden403 -> {
                _snackbarMessage.value = result.message
                false
            }
        }
    }

    fun toggleThemeMode() {
        _isSoftRoseLightTheme.value = !_isSoftRoseLightTheme.value
    }

    fun updateUserProfile(
        displayName: String,
        bio: String,
        cityName: String,
        roleTitle: String,
        isAvailableNow: Boolean
    ) {
        _userProfile.value = _userProfile.value.copy(
            displayName = displayName.trim(),
            bio = bio.trim(),
            cityName = cityName.trim(),
            roleTitle = roleTitle.trim(),
            isAvailableNow = isAvailableNow
        )
        _snackbarMessage.value = "تم تحديث الملف الشخصي بنجاح!"
    }

    fun updateProfileAvatar(uriString: String) {
        _userProfile.value = _userProfile.value.copy(avatarUri = uriString)
        _snackbarMessage.value = "تم تحديث صورة الملف الشخصي!"
    }

    fun addCustomProfilePhoto(uriString: String) {
        val current = _userProfile.value.customPhotos
        _userProfile.value = _userProfile.value.copy(customPhotos = listOf(uriString) + current)
        _snackbarMessage.value = "تمت إضافة الصورة إلى ألبوم الملف الشخصي!"
    }

    fun updateVenueStartingPrice(venueId: String, newPriceUsd: Int) {
        if (!enforceSuperAdminMiddleware()) return
        val validPrice = newPriceUsd.coerceIn(5, 50000)
        _customVenuePrices.value = _customVenuePrices.value + (venueId to validPrice)
        _selectedVenue.value?.let { current ->
            if (current.id == venueId) {
                _selectedVenue.value = current.copy(
                    startingPriceUsd = validPrice,
                    packages = current.packages.mapIndexed { idx, pkg ->
                        if (idx == 0) pkg.copy(priceUsd = validPrice) else pkg
                    }
                )
            }
        }
        _snackbarMessage.value = "تم تحديث السعر المعتمد إلى $$validPrice بنجاح"
    }

    fun updateVenuePhoto(venueId: String, imageUri: String) {
        if (!enforceSuperAdminMiddleware()) return
        _customVenuePhotos.value = _customVenuePhotos.value + (venueId to imageUri)
        _selectedVenue.value?.let { current ->
            if (current.id == venueId) {
                _selectedVenue.value = current.copy(visualPresetKey = imageUri)
            }
        }
        _snackbarMessage.value = "تم تحديث صورة العرض بنجاح"
    }

    fun deleteCatalogVenue(venueId: String) {
        if (!enforceSuperAdminMiddleware()) return
        _deletedCatalogVenueIds.value = _deletedCatalogVenueIds.value + venueId
        if (_selectedVenue.value?.id == venueId) {
            _selectedVenue.value = null
        }
        _snackbarMessage.value = "تم حذف الخدمة من الدليل العام بنجاح"
    }

    fun restoreDeletedCatalogVenues() {
        if (!enforceSuperAdminMiddleware()) return
        _deletedCatalogVenueIds.value = emptySet()
        _snackbarMessage.value = "تم استعادة جميع مراكز الدليل المحذوفة"
    }

    fun topUpWallet(amountUsd: Int, bonusUsd: Int = 0, methodTitle: String = "شحن رصيد المحفظة") {
        val validAmount = amountUsd.coerceIn(1, 100000)
        val totalCredit = validAmount + bonusUsd
        _walletBalanceUsd.value += totalCredit
        val tx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = if (bonusUsd > 0) "$methodTitle (شامل +$$bonusUsd هدية مجانية)" else methodTitle,
            amountUsd = totalCredit,
            bonusUsd = bonusUsd,
            isCredit = true,
            timestampLabel = "الآن"
        )
        _walletTransactions.value = listOf(tx) + _walletTransactions.value
        _snackbarMessage.value = if (bonusUsd > 0) {
            "تم شحن المحفظة بمبلغ $$validAmount + $$bonusUsd مجاناً! رصيدك الحالي: $${_walletBalanceUsd.value}"
        } else {
            "تم شحن المحفظة بمبلغ $$validAmount بنجاح! رصيدك الحالي: $${_walletBalanceUsd.value}"
        }
    }

    fun adminAdjustWalletBalance(amountDeltaUsd: Int, reasonNote: String) {
        if (!enforceSuperAdminMiddleware()) return
        if (amountDeltaUsd == 0) return
        val nextBalance = (_walletBalanceUsd.value + amountDeltaUsd).coerceAtLeast(0)
        val isCredit = amountDeltaUsd > 0
        val absAmount = kotlin.math.abs(amountDeltaUsd)
        _walletBalanceUsd.value = nextBalance

        val tx = WalletTransaction(
            id = "admin_tx_${System.currentTimeMillis()}",
            title = if (isCredit) {
                "إضافة رصيد إداري (Super Admin): ${reasonNote.ifBlank { "شحن مباشر من الإدارة" }}"
            } else {
                "خصم/تسوية رصيد إداري (Super Admin): ${reasonNote.ifBlank { "تسوية رصيد" }}"
            },
            amountUsd = absAmount,
            bonusUsd = 0,
            isCredit = isCredit,
            timestampLabel = "بواسطة الإدارة"
        )
        _walletTransactions.value = listOf(tx) + _walletTransactions.value
        _snackbarMessage.value = "تم تحديث رصيد المحفظة من لوحة الإدارة إلى $$nextBalance"
    }

    fun deductWalletBalance(amountUsd: Int, orderTitle: String): Boolean {
        val current = _walletBalanceUsd.value
        if (current < amountUsd) {
            _snackbarMessage.value = "رصيد المحفظة الحالي ($$current) غير كافٍ لإتمام الطلب ($$amountUsd). يرجى شحن الرصيد أولاً."
            return false
        }
        _walletBalanceUsd.value = current - amountUsd
        val tx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            title = "دفع حجز: $orderTitle",
            amountUsd = amountUsd,
            bonusUsd = 0,
            isCredit = false,
            timestampLabel = "الآن"
        )
        _walletTransactions.value = listOf(tx) + _walletTransactions.value
        return true
    }

    fun consumeBroadcastAlert() {
        _incomingBroadcastAlert.value = null
    }

    fun selectCountry(country: String) {
        _selectedCountry.value = country
        val currentCity = _selectedCity.value
        if (country != "الكل" && currentCity != null) {
            val cityBelongs = LayaliCatalog.cities.any { it.nameAr == currentCity && it.country == country }
            if (!cityBelongs) {
                _selectedCity.value = null
            }
        }
    }

    fun selectCity(cityName: String?) {
        _selectedCity.value = if (_selectedCity.value == cityName) null else cityName
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun openVenueDetail(venue: Venue) {
        _selectedVenue.value = venue
    }

    fun closeVenueDetail() {
        _selectedVenue.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showInfoMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun publishNewService(
        title: String,
        cityName: String,
        country: String,
        category: String,
        description: String,
        priceUsd: Int,
        imageUrl: String,
        videoUrl: String,
        mediaType: String,
        onPublished: (PublishedService) -> Unit = {}
    ) {
        if (!enforceSuperAdminMiddleware()) return
        val localId = "local_pub_${System.currentTimeMillis()}"
        val publishedItem = PublishedService(
            id = localId,
            authorId = currentUserId,
            title = title,
            cityName = cityName,
            country = country,
            category = category,
            description = description,
            priceUsd = priceUsd.toLong(),
            imageUrl = imageUrl.ifBlank { "brand_dark_showcase" },
            videoUrl = videoUrl,
            mediaType = mediaType,
            status = "active",
            createdAt = Timestamp.now()
        )
        seenPublishedIds.add(localId)
        _localPublishedServices.value = listOf(publishedItem) + _localPublishedServices.value
        _snackbarMessage.value = "تم نشر الخدمة وإرسال إشعار أندرويد لجميع المستخدمين!"
        onPublished(publishedItem)

        viewModelScope.launch {
            val result = repository.publishService(
                title = title,
                cityName = cityName,
                country = country,
                category = category,
                description = description,
                priceUsd = priceUsd,
                imageUrl = imageUrl.ifBlank { "brand_dark_showcase" },
                videoUrl = videoUrl,
                mediaType = mediaType
            )
            result.onSuccess { remoteId ->
                seenPublishedIds.add(remoteId)
            }.onFailure { err ->
                Log.d(TAG, "Saved locally; remote sync skipped or failed: ${err.message}")
            }
        }
    }

    fun updatePublishedPrice(serviceId: String, newPriceUsd: Int) {
        if (!enforceSuperAdminMiddleware()) return
        val validPrice = newPriceUsd.coerceIn(5, 50000)
        _localPublishedServices.value = _localPublishedServices.value.map { item ->
            if (item.id == serviceId) item.copy(priceUsd = validPrice.toLong()) else item
        }
        _snackbarMessage.value = "تم تحديث السعر إلى $$validPrice بنجاح"

        viewModelScope.launch {
            repository.updatePublishedServicePrice(serviceId, validPrice)
        }
    }

    fun updatePublishedServiceDetails(
        serviceId: String,
        newTitle: String,
        newCityName: String,
        newCountry: String,
        newCategory: String,
        newPriceUsd: Int,
        newDescription: String
    ) {
        if (!enforceSuperAdminMiddleware()) return
        val validPrice = newPriceUsd.coerceIn(5, 50000)
        _localPublishedServices.value = _localPublishedServices.value.map { item ->
            if (item.id == serviceId) {
                item.copy(
                    title = newTitle.ifBlank { item.title },
                    cityName = newCityName.ifBlank { item.cityName },
                    country = newCountry.ifBlank { item.country },
                    category = newCategory.ifBlank { item.category },
                    priceUsd = validPrice.toLong(),
                    description = newDescription.ifBlank { item.description }
                )
            } else {
                item
            }
        }
        _snackbarMessage.value = "تم تعديل وحفظ بيانات الخدمة المنشورة بنجاح"
        viewModelScope.launch {
            repository.updatePublishedServicePrice(serviceId, validPrice)
        }
    }

    fun deletePublishedService(serviceId: String) {
        if (!enforceSuperAdminMiddleware()) return
        _localPublishedServices.value = _localPublishedServices.value.filterNot { it.id == serviceId }
        _snackbarMessage.value = "تم حذف العرض المنشور"
        viewModelScope.launch {
            repository.deletePublishedService(serviceId)
        }
    }

    fun blockUserAccount(identifier: String, reason: String, cityScope: String) {
        if (!enforceSuperAdminMiddleware()) return
        val cleanId = identifier.trim()
        if (cleanId.isBlank()) return
        val localBlock = BlockedUser(
            id = "block_${System.currentTimeMillis()}",
            userId = currentUserId,
            blockedIdentifier = cleanId,
            reason = reason.ifBlank { "مخالفة معايير الاستخدام" },
            cityScope = cityScope.ifBlank { "جميع المدن" },
            status = "active",
            createdAt = Timestamp.now()
        )
        _localBlockedUsers.value = listOf(localBlock) + _localBlockedUsers.value.filterNot {
            it.blockedIdentifier.equals(cleanId, ignoreCase = true)
        }
        _snackbarMessage.value = "تم حظر ($cleanId) وإخفاء محتواه فوراً"

        viewModelScope.launch {
            repository.blockUser(cleanId, reason, cityScope)
        }
    }

    fun toggleBlockUserStatus(blockedId: String, currentStatus: String) {
        if (!enforceSuperAdminMiddleware()) return
        val nextStatus = if (currentStatus == "active") "unblocked" else "active"
        _localBlockedUsers.value = _localBlockedUsers.value.map { item ->
            if (item.id == blockedId) item.copy(status = nextStatus) else item
        }
        _snackbarMessage.value = if (currentStatus == "active") {
            "تم رفع الحظر عن الحساب"
        } else {
            "تمت إعادة تفعيل الحظر"
        }
        viewModelScope.launch {
            repository.toggleBlockStatus(blockedId, currentStatus)
        }
    }

    fun deleteBlockedRecord(blockedId: String) {
        if (!enforceSuperAdminMiddleware()) return
        _localBlockedUsers.value = _localBlockedUsers.value.filterNot { it.id == blockedId }
        _snackbarMessage.value = "تم حذف سجل الحظر"
        viewModelScope.launch {
            repository.deleteBlockedUser(blockedId)
        }
    }

    fun createDirectBooking(
        venue: Venue,
        serviceTitle: String,
        unitPriceUsd: Int,
        bookingDate: String,
        timeSlot: String,
        guestCount: Int,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        val totalUsd = (unitPriceUsd * guestCount).coerceIn(5, 10000)
        val localBooking = Booking(
            id = "booking_${System.currentTimeMillis()}",
            userId = currentUserId,
            venueId = venue.id,
            venueName = venue.name,
            cityName = venue.cityName,
            country = venue.country,
            serviceTitle = serviceTitle,
            category = venue.category,
            bookingDate = bookingDate,
            timeSlot = timeSlot,
            guestCount = guestCount.toLong(),
            totalPriceUsd = totalUsd.toLong(),
            notes = notes,
            status = "confirmed",
            createdAt = Timestamp.now()
        )
        _localBookings.value = listOf(localBooking) + _localBookings.value
        _snackbarMessage.value = "تم تأكيد حجزك المباشر في ${venue.name} (${venue.cityName}) بنجاح!"
        onSuccess()

        viewModelScope.launch {
            repository.createBooking(
                venueId = venue.id,
                venueName = venue.name,
                cityName = venue.cityName,
                country = venue.country,
                serviceTitle = serviceTitle,
                category = venue.category,
                bookingDate = bookingDate,
                timeSlot = timeSlot,
                guestCount = guestCount,
                totalPriceUsd = totalUsd,
                notes = notes
            )
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        _localBookings.value = _localBookings.value.map { item ->
            if (item.id == bookingId) item.copy(status = newStatus) else item
        }
        val statusLabel = when (newStatus) {
            "cancelled" -> "تم إلغاء الحجز"
            "confirmed" -> "تم قبول وتأكيد الحجز بنجاح"
            else -> "تم تحديث حالة الحجز إلى مكتمل"
        }
        _snackbarMessage.value = statusLabel

        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, newStatus)
        }
    }

    fun adminUpdateBookingStatus(bookingId: String, newStatus: String) {
        if (!enforceSuperAdminMiddleware()) return
        updateBookingStatus(bookingId, newStatus)
    }

    fun rescheduleBooking(
        bookingId: String,
        newDate: String,
        newTimeSlot: String,
        newGuestCount: Int,
        newNotes: String
    ) {
        _localBookings.value = _localBookings.value.map { item ->
            if (item.id == bookingId) {
                item.copy(
                    bookingDate = newDate,
                    timeSlot = newTimeSlot,
                    guestCount = newGuestCount.toLong(),
                    notes = newNotes
                )
            } else {
                item
            }
        }
        _snackbarMessage.value = "تم تعديل موعد الحجز بنجاح"

        viewModelScope.launch {
            repository.rescheduleBooking(
                bookingId = bookingId,
                newDate = newDate,
                newTimeSlot = newTimeSlot,
                newGuestCount = newGuestCount,
                newNotes = newNotes
            )
        }
    }

    fun deleteBooking(bookingId: String) {
        _localBookings.value = _localBookings.value.filterNot { it.id == bookingId }
        _snackbarMessage.value = "تم حذف السجل من حجوزاتك"
        viewModelScope.launch {
            repository.deleteBooking(bookingId)
        }
    }

    fun submitCustomerReview(
        authorName: String,
        venue: Venue,
        serviceType: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit = {}
    ) {
        val localRev = CustomerReview(
            id = "rev_${System.currentTimeMillis()}",
            userId = currentUserId,
            authorName = authorName,
            venueId = venue.id,
            venueName = venue.name,
            cityName = venue.cityName,
            serviceType = serviceType,
            rating = rating.toLong(),
            comment = comment,
            createdAt = Timestamp.now()
        )
        _localReviews.value = listOf(localRev) + _localReviews.value
        _snackbarMessage.value = "شكراً لك! تم نشر تقييمك الموثق لـ ${venue.name}"
        onSuccess()

        viewModelScope.launch {
            repository.submitReview(
                authorName = authorName,
                venueId = venue.id,
                venueName = venue.name,
                cityName = venue.cityName,
                serviceType = serviceType,
                rating = rating,
                comment = comment
            )
        }
    }

    fun deleteCustomerReview(reviewId: String) {
        _localReviews.value = _localReviews.value.filterNot { it.id == reviewId }
        _snackbarMessage.value = "تم حذف التقييم"
        viewModelScope.launch {
            repository.deleteReview(reviewId)
        }
    }

    fun toggleFavorite(venue: Venue, isCurrentlyFav: Boolean) {
        if (isCurrentlyFav) {
            _localFavorites.value = _localFavorites.value.filterNot { it.venueId == venue.id }
            _snackbarMessage.value = "تمت إزالة ${venue.name} من المفضلة"
        } else {
            val fav = FavoriteVenue(
                id = venue.id,
                userId = currentUserId,
                venueId = venue.id,
                venueName = venue.name,
                cityName = venue.cityName,
                category = venue.category,
                createdAt = Timestamp.now()
            )
            _localFavorites.value = listOf(fav) + _localFavorites.value
            _snackbarMessage.value = "تمت إضافة ${venue.name} إلى المفضلة"
        }

        viewModelScope.launch {
            repository.toggleFavorite(
                venueId = venue.id,
                venueName = venue.name,
                cityName = venue.cityName,
                category = venue.category,
                currentlyFavorite = isCurrentlyFav
            )
        }
    }

    private companion object {
        const val TAG = "LayaliVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
