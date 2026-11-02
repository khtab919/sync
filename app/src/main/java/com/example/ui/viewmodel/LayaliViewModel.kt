package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.LayaliCatalog
import com.example.data.local.RegisteredAccountEntity
import com.example.data.model.AvatarFrameCatalog
import com.example.data.model.BlockedUser
import com.example.data.model.Booking
import com.example.data.model.ChatMemberProfile
import com.example.data.model.ChatMessageItem
import com.example.data.model.CityInfo
import com.example.data.model.CustomerReview
import com.example.data.model.FavoriteVenue
import com.example.data.model.PaymentReceiptRecord
import com.example.data.model.PublishedService
import com.example.data.model.UserProfile
import com.example.data.model.Venue
import com.example.data.repository.LayaliRepository
import com.example.security.MiddlewareResult
import com.example.security.SuperAdminSecurityManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    val currentUserId: String,
    private val appContext: Context? = null
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

    // Default to Royal Dark Sapphire & Sovereign Gold theme
    private val appearancePreferences = appContext?.getSharedPreferences("layali_appearance", Context.MODE_PRIVATE)
    private val _isSoftRoseLightTheme = MutableStateFlow(appearancePreferences?.getBoolean("light_theme", true) ?: true)
    val isSoftRoseLightTheme: StateFlow<Boolean> = _isSoftRoseLightTheme.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            displayName = "",
            username = "",
            roleTitle = "",
            cityName = "دمشق",
            country = "سوريا",
            level = 1,
            xpPoints = 150,
            nextLevelXp = 2500,
            selectedFrameId = "emerald_crown",
            chatPriceUsd = 15,
            isPaidChatEnabled = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Wallet Balance, Transactions & Verified Payment Receipts
    private val _walletBalanceUsd = MutableStateFlow(0)
    val walletBalanceUsd: StateFlow<Int> = _walletBalanceUsd.asStateFlow()
    private val _creatorAvailableUsd = MutableStateFlow(0)
    val creatorAvailableUsd: StateFlow<Int> = _creatorAvailableUsd.asStateFlow()
    private val _creatorReservedUsd = MutableStateFlow(0)
    val creatorReservedUsd: StateFlow<Int> = _creatorReservedUsd.asStateFlow()

    private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

    private val _paymentReceipts = MutableStateFlow<List<PaymentReceiptRecord>>(emptyList())
    val paymentReceipts: StateFlow<List<PaymentReceiptRecord>> = _paymentReceipts.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<RegisteredAccountEntity>>(emptyList())
    val savedAccounts: StateFlow<List<RegisteredAccountEntity>> = _savedAccounts.asStateFlow()

    private val _lastInfrastructureSyncLabel = MutableStateFlow("مفعل ومتزامن مع SQLite + Cloud")
    val lastInfrastructureSyncLabel: StateFlow<String> = _lastInfrastructureSyncLabel.asStateFlow()

    init {
        appContext?.let { ctx ->
            _savedAccounts.value = SuperAdminSecurityManager.getSavedAccounts(ctx)
        }
        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.observeVerifiedWalletBalance(currentUserId)
                    .catch { error ->
                        Log.e(TAG, "Verified wallet stream unavailable", error)
                        emit(0)
                    }
                    .collect { verifiedBalance -> _walletBalanceUsd.value = verifiedBalance }
            }
            viewModelScope.launch {
                repository.observeCreatorEarnings(currentUserId)
                    .catch { error ->
                        Log.e(TAG, "Creator earnings stream unavailable", error)
                        emit(0 to 0)
                    }
                    .collect { (available, reserved) ->
                        _creatorAvailableUsd.value = available
                        _creatorReservedUsd.value = reserved
                    }
            }
        }
    }

    // Paid Chat Members, Unlocked Conversations & Message Threads
    private val _chatMembers = MutableStateFlow<List<ChatMemberProfile>>(emptyList())
    val chatMembers: StateFlow<List<ChatMemberProfile>> = _chatMembers.asStateFlow()

    private val _unlockedChatMemberIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedChatMemberIds: StateFlow<Set<String>> = _unlockedChatMemberIds.asStateFlow()

    private val _activeChatMemberId = MutableStateFlow<String?>(null)
    val activeChatMemberId: StateFlow<String?> = _activeChatMemberId.asStateFlow()

    private val _chatMessagesByMember = MutableStateFlow<Map<String, List<ChatMessageItem>>>(emptyMap())
    val chatMessagesByMember: StateFlow<Map<String, List<ChatMessageItem>>> = _chatMessagesByMember.asStateFlow()
    private val chatThreadByMember = mutableMapOf<String, String>()
    private val chatQuotesRequested = mutableSetOf<String>()
    private val subscribedThreadIds = mutableSetOf<String>()

    init {
        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.observeVerifiedChatThreads(currentUserId)
                    .catch { error ->
                        Log.e(TAG, "Verified chat membership unavailable", error)
                        emit(emptyList())
                    }
                    .collect { threads ->
                        threads.forEach { thread ->
                            subscribeToVerifiedThread(thread.peerId, thread.threadId)
                        }
                    }
            }
        }
    }

    private fun subscribeToVerifiedThread(peerId: String, threadId: String) {
        chatThreadByMember[peerId] = threadId
        _unlockedChatMemberIds.value = _unlockedChatMemberIds.value + peerId
        if (_chatMembers.value.none { it.id == peerId }) {
            viewModelScope.launch {
                repository.fetchCreatorChatQuote(peerId).onSuccess { quote ->
                    if (_chatMembers.value.none { it.id == peerId }) {
                        _chatMembers.value = _chatMembers.value + ChatMemberProfile(
                            id = peerId, displayName = quote.displayName,
                            username = quote.username, cityName = "", country = "",
                            category = "محادثة خاصة", bio = "", level = 1,
                            frameId = "emerald_crown", chatPriceUsd = 0,
                            isOnline = false, avatarPreset = "", isUnlocked = true
                        )
                    }
                }
            }
        }
        if (!subscribedThreadIds.add(threadId)) return
        viewModelScope.launch {
            repository.observeVerifiedChatMessages(threadId, peerId)
                .catch { error ->
                    Log.e(TAG, "Verified chat message stream error", error)
                    emit(emptyList())
                }.collect { messages ->
                    _chatMessagesByMember.value = _chatMessagesByMember.value + (peerId to messages)
                }
        }
    }



    // Admin custom price, photo, and deletion overrides for catalog venues
    private val _adminSessionToken = MutableStateFlow<String?>(null)
    val adminSessionToken: StateFlow<String?> = _adminSessionToken.asStateFlow()

    private val _customVenuePrices = MutableStateFlow<Map<String, Int>>(emptyMap())
    val customVenuePrices: StateFlow<Map<String, Int>> = _customVenuePrices.asStateFlow()

    private val _customVenuePhotos = MutableStateFlow<Map<String, String>>(emptyMap())
    val customVenuePhotos: StateFlow<Map<String, String>> = _customVenuePhotos.asStateFlow()

    private val _deletedCatalogVenueIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedCatalogVenueIds: StateFlow<Set<String>> = _deletedCatalogVenueIds.asStateFlow()

    // No fabricated showcase services in the production feed.
    private val _localPublishedServices = MutableStateFlow<List<PublishedService>>(emptyList())

    private val _localBookings = MutableStateFlow<List<Booking>>(emptyList())
    private val _localReviews = MutableStateFlow<List<CustomerReview>>(emptyList())
    private val _localFavorites = MutableStateFlow<List<FavoriteVenue>>(emptyList())
    private val _localBlockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())

    private val seenPublishedIds = mutableSetOf<String>()
    private var initialSnapshotLoaded = false

    private val _incomingBroadcastAlert = MutableStateFlow<PublishedService?>(null)
    val incomingBroadcastAlert: StateFlow<PublishedService?> = _incomingBroadcastAlert.asStateFlow()

    private fun hasFirebaseAuthForCurrentUser(): Boolean {
        val firebaseUid = try {
            Firebase.auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
        return !firebaseUid.isNullOrBlank() &&
            firebaseUid == currentUserId &&
            currentUserId != "guest_session" &&
            !currentUserId.startsWith("user_")
    }

    private fun hasFirebaseAuth(): Boolean {
        val firebaseUid = try {
            Firebase.auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
        return !firebaseUid.isNullOrBlank()
    }

    private val remoteBookingsFlow: Flow<List<Booking>> = flow {
        if (hasFirebaseAuthForCurrentUser()) {
            emitAll(repository.observeBookings(currentUserId).catch { emit(emptyList()) })
        } else {
            emit(emptyList())
        }
    }

    private val remoteReviewsFlow: Flow<List<CustomerReview>> = flow {
        if (hasFirebaseAuthForCurrentUser()) {
            emitAll(repository.observeUserReviews(currentUserId).catch { emit(emptyList()) })
        } else {
            emit(emptyList())
        }
    }

    private val remoteFavoritesFlow: Flow<List<FavoriteVenue>> = flow {
        if (hasFirebaseAuthForCurrentUser()) {
            emitAll(repository.observeFavorites(currentUserId).catch { emit(emptyList()) })
        } else {
            emit(emptyList())
        }
    }

    private val remotePublishedServicesFlow: Flow<List<PublishedService>> = flow {
        if (hasFirebaseAuth()) {
            emitAll(
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
                    .onEach { list -> syncRealChatOffers(list) }
                    .catch { emit(emptyList()) }
            )
        } else {
            emit(emptyList())
        }
    }

    private fun syncRealChatOffers(offers: List<PublishedService>) {
        if (!hasFirebaseAuthForCurrentUser()) return
        offers.filter { it.authorId.isNotBlank() && it.authorId != currentUserId }
            .distinctBy { it.authorId }.forEach { offer ->
                if (!chatQuotesRequested.add(offer.authorId)) return@forEach
                viewModelScope.launch {
                    repository.fetchCreatorChatQuote(offer.authorId)
                        .onSuccess { quote ->
                            val member = ChatMemberProfile(
                                id = offer.authorId,
                                displayName = quote.displayName,
                                username = quote.username,
                                cityName = offer.cityName,
                                country = offer.country,
                                category = offer.category,
                                bio = offer.description,
                                level = 1,
                                frameId = "emerald_crown",
                                chatPriceUsd = quote.priceUsd,
                                isOnline = false,
                                avatarPreset = offer.imageUrl
                            )
                            _chatMembers.value = (_chatMembers.value.filterNot { it.id == member.id } + member)
                        }.onFailure { error ->
                            chatQuotesRequested.remove(offer.authorId)
                            Log.w(TAG, "Creator chat quote unavailable: ${error.message}")
                        }
                }
            }
    }

    private val remoteBlockedUsersFlow: Flow<List<BlockedUser>> = flow {
        if (hasFirebaseAuthForCurrentUser()) {
            emitAll(repository.observeBlockedUsers(currentUserId).catch { emit(emptyList()) })
        } else {
            emit(emptyList())
        }
    }

    val bookingsState: StateFlow<UiState<List<Booking>>> = combine(
        remoteBookingsFlow,
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
        remoteReviewsFlow,
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
        remoteFavoritesFlow,
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
        remotePublishedServicesFlow,
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
        remoteBlockedUsersFlow,
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
        emptyList<Venue>()
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
        initialValue = emptyList()
    )

    fun setSuperAdminSessionToken(token: String?) {
        _adminSessionToken.value = token
    }

    fun syncAuthenticatedAccount(
        authenticatedDisplayName: String,
        isSuperAdmin: Boolean,
        emailOverride: String? = null
    ) {
        val current = _userProfile.value
        val ctx = appContext
        val existingSaved = if (ctx != null) {
            val lookupKey = when {
                isSuperAdmin -> SuperAdminSecurityManager.INITIAL_SUPER_ADMIN_EMAIL
                !emailOverride.isNullOrBlank() -> emailOverride
                authenticatedDisplayName.isNotBlank() -> authenticatedDisplayName
                else -> currentUserId
            }
            SuperAdminSecurityManager.findSavedAccount(ctx, lookupKey)
        } else {
            null
        }

        if (isSuperAdmin) {
            _userProfile.value = current.copy(
                displayName = authenticatedDisplayName.ifBlank { "Super Admin" },
                username = "@ramanur_admin",
                telegramHandle = "@RamaNUR",
                roleTitle = "المسؤول الرئيسي للنظام (Super Admin)",
                level = existingSaved?.level ?: 50,
                xpPoints = existingSaved?.xpPoints ?: 9850,
                nextLevelXp = existingSaved?.nextLevelXp ?: 10000,
                selectedFrameId = existingSaved?.selectedFrameId ?: "obsidian_elite"
            )
        } else if (authenticatedDisplayName.isNotBlank() || existingSaved != null) {
            val cleanHandle = existingSaved?.username ?: ("@" + authenticatedDisplayName
                .lowercase()
                .replace(" ", "_")
                .filter { it.isLetterOrDigit() || it == '_' }
                .ifBlank { "layali_vip" })
            _userProfile.value = current.copy(
                displayName = authenticatedDisplayName.ifBlank { existingSaved?.displayName ?: "عضو ليالي الملكي" },
                username = if (current.username.isBlank() || current.username == "@layali_vip") cleanHandle else current.username,
                bio = existingSaved?.bio?.takeIf { it.isNotBlank() } ?: current.bio,
                cityName = existingSaved?.cityName?.takeIf { it.isNotBlank() } ?: current.cityName,
                country = existingSaved?.country?.takeIf { it.isNotBlank() } ?: current.country,
                avatarUri = existingSaved?.avatarUri?.takeIf { it.isNotBlank() } ?: current.avatarUri,
                selectedFrameId = existingSaved?.selectedFrameId?.takeIf { it.isNotBlank() } ?: current.selectedFrameId,
                level = existingSaved?.level ?: current.level,
                xpPoints = existingSaved?.xpPoints ?: current.xpPoints,
                nextLevelXp = existingSaved?.nextLevelXp ?: current.nextLevelXp,
                chatPriceUsd = existingSaved?.chatPriceUsd ?: current.chatPriceUsd,
                isPaidChatEnabled = existingSaved?.isPaidChatEnabled ?: current.isPaidChatEnabled
            )
        }
        saveInfrastructureToDatabase(emailOverride = emailOverride, showSnackbar = false)
    }

    fun saveInfrastructureToDatabase(
        emailOverride: String? = null,
        showSnackbar: Boolean = true
    ) {
        val profile = _userProfile.value
        val role = if (_adminSessionToken.value != null) "super_admin" else "customer"
        val ctx = appContext
        val savedEntity = if (ctx != null) {
            SuperAdminSecurityManager.saveAccountInfrastructureSync(
                context = ctx,
                accountId = currentUserId,
                profile = profile,
                walletBalanceUsd = _walletBalanceUsd.value,
                email = emailOverride,
                role = role
            ).also {
                _savedAccounts.value = SuperAdminSecurityManager.getSavedAccounts(ctx)
            }
        } else {
            null
        }

        val nowLabel = SuperAdminSecurityManager.currentTimestampLabel()
        _lastInfrastructureSyncLabel.value = "محفوظ في SQLite + السحابة ($nowLabel)"

        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.saveOrUpdateUserAccount(
                    username = profile.username,
                    displayName = profile.displayName.ifBlank { "عضو ليالي الملكي" },
                    email = emailOverride ?: savedEntity?.email ?: "${profile.username.removePrefix("@")}@layali.client",
                    bio = profile.bio,
                    cityName = profile.cityName.ifBlank { "دمشق" },
                    country = profile.country.ifBlank { "سوريا" },
                    avatarUri = profile.avatarUri,
                    selectedFrameId = profile.selectedFrameId,
                    level = profile.level,
                    xpPoints = profile.xpPoints,
                    nextLevelXp = profile.nextLevelXp,
                    walletBalanceUsd = _walletBalanceUsd.value,
                    chatPriceUsd = profile.chatPriceUsd,
                    isPaidChatEnabled = profile.isPaidChatEnabled,
                    passwordSaltHex = savedEntity?.passwordSaltHex ?: "a1b2c3d4e5f60718293a4b5c6d7e8f90",
                    passwordHashHex = savedEntity?.passwordHashHex ?: "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                    role = role
                )
            }
        }

        if (showSnackbar) {
            _snackbarMessage.value = "تم حفظ البنية التحتية وبيانات الحساب (${profile.username} • LEVEL ${profile.level}) في قاعدة البيانات بنجاح ✓"
        }
    }

    fun changeAccountPassword(
        currentPassword: String,
        newPassword: String,
        requireCurrentPassword: Boolean = true,
        identifierOverride: String? = null
    ): Boolean {
        val ctx = appContext ?: return false
        val targetId = identifierOverride?.takeIf { it.isNotBlank() }
            ?: if (_adminSessionToken.value != null) SuperAdminSecurityManager.INITIAL_SUPER_ADMIN_EMAIL else _userProfile.value.username

        val result = SuperAdminSecurityManager.changeAccountPassword(
            context = ctx,
            identifier = targetId,
            currentPassword = currentPassword,
            newPassword = newPassword,
            requireCurrentPassword = requireCurrentPassword
        )
        return result.fold(
            onSuccess = { record ->
                _savedAccounts.value = SuperAdminSecurityManager.getSavedAccounts(ctx)
                _lastInfrastructureSyncLabel.value = "تم تحديث كلمة المرور المشفرة (${record.updatedAtLabel})"
                if (hasFirebaseAuthForCurrentUser()) {
                    viewModelScope.launch {
                        repository.updateUserAccountPasswordHash(
                            newSaltHex = record.passwordSaltHex,
                            newHashHex = record.passwordHashHex
                        )
                    }
                }
                _snackbarMessage.value = "تم تغيير وحفظ كلمة المرور المشفرة للحساب (${record.username}) في قاعدة البيانات بنجاح 🔒"
                true
            },
            onFailure = { err ->
                _snackbarMessage.value = err.message ?: "تعذر تغيير كلمة المرور"
                false
            }
        )
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
        appearancePreferences?.edit()?.putBoolean("light_theme", _isSoftRoseLightTheme.value)?.apply()
    }

    fun updateUserProfile(
        displayName: String,
        bio: String,
        cityName: String,
        roleTitle: String,
        isAvailableNow: Boolean
    ) {
        _userProfile.value = _userProfile.value.copy(
            displayName = displayName.trim().ifBlank { _userProfile.value.displayName },
            bio = bio.trim(),
            cityName = cityName.trim(),
            roleTitle = roleTitle.trim(),
            isAvailableNow = isAvailableNow
        )
        saveInfrastructureToDatabase(showSnackbar = false)
        _snackbarMessage.value = "تم تحديث الملف الشخصي وحفظه في قاعدة البيانات بنجاح!"
    }

    fun updateUsername(rawUsername: String) {
        val cleaned = rawUsername.trim().replace(" ", "_")
        if (cleaned.isBlank()) return
        val formatted = if (cleaned.startsWith("@")) cleaned else "@$cleaned"
        _userProfile.value = _userProfile.value.copy(username = formatted)
        saveInfrastructureToDatabase(showSnackbar = false)
        _snackbarMessage.value = "تم اعتماد اسم المستخدم الجديد ($formatted) وحفظه في قاعدة البيانات"
    }

    fun selectAvatarFrame(frameId: String) {
        val frame = AvatarFrameCatalog.findById(frameId)
        val currentLevel = _userProfile.value.level
        if (currentLevel < frame.minLevel && _adminSessionToken.value == null) {
            _snackbarMessage.value = "إطار «${frame.nameAr}» حصري ويتطلب الوصول إلى LEVEL ${frame.minLevel} (مستواك الحالي: LEVEL $currentLevel) 🔒"
            return
        }
        _userProfile.value = _userProfile.value.copy(selectedFrameId = frame.id)
        saveInfrastructureToDatabase(showSnackbar = false)
        _snackbarMessage.value = "تم تفعيل وحفظ الإطار الملكي الفاخر: ${frame.nameAr} ✨"
    }

    fun updateChatPricing(priceUsd: Int, isEnabled: Boolean) {
        val validPrice = priceUsd.coerceIn(1, 5000)
        _userProfile.value = _userProfile.value.copy(
            chatPriceUsd = validPrice,
            isPaidChatEnabled = isEnabled
        )
        saveInfrastructureToDatabase(showSnackbar = false)
        _snackbarMessage.value = if (isEnabled) {
            "تم ضبط وحفظ تسعيرة الدردشة معك على $$validPrice للجلسة!"
        } else {
            "تم جعل الدردشة معك مجانية حالياً"
        }
    }

    fun gainProfileXp(xpDelta: Int) {
        if (xpDelta <= 0) return
        val current = _userProfile.value
        val awardedXp = xpDelta.coerceAtMost(120)
        val newXp = current.xpPoints + awardedXp
        var newLevel = current.level
        var nextThreshold = current.nextLevelXp
        var leveledUp = false

        while (newXp >= nextThreshold) {
            newLevel += 1
            // Steep exponential XP progression so each level is a rare prestige achievement
            nextThreshold = (nextThreshold * 1.75).toInt() + (newLevel * 2000)
            leveledUp = true
        }

        _userProfile.value = current.copy(
            level = newLevel,
            xpPoints = newXp,
            nextLevelXp = nextThreshold
        )
        saveInfrastructureToDatabase(showSnackbar = false)
        if (leveledUp) {
            _snackbarMessage.value = "إنجاز نادر! ارتقى حسابك إلى المستوى الملكي LEVEL $newLevel 🎉"
        } else {
            val remaining = (nextThreshold - newXp).coerceAtLeast(0)
            _snackbarMessage.value = "تمت إضافة +$awardedXp XP • متبقي $remaining XP للوصول إلى LEVEL ${newLevel + 1}"
        }
    }

    fun updateProfileAvatar(uriString: String) {
        _userProfile.value = _userProfile.value.copy(avatarUri = uriString)
        gainProfileXp(15)
        _snackbarMessage.value = "تم تحديث وحفظ صورة الملف الشخصي (+15 XP)!"
    }

    fun addCustomProfilePhoto(uriString: String) {
        val current = _userProfile.value.customPhotos
        _userProfile.value = _userProfile.value.copy(customPhotos = listOf(uriString) + current)
        saveInfrastructureToDatabase(showSnackbar = false)
        _snackbarMessage.value = "تمت إضافة الصورة إلى ألبوم الملف الشخصي!"
    }

    // ========================================================================
    // REAL PAYMENT RECEIPT VERIFICATION (SHAM CASH & USDT BEP-20)
    // ========================================================================

    fun submitRealDepositReceipt(
        amountUsd: Int,
        bonusUsd: Int,
        methodKey: String,
        receiptImageUri: String,
        transactionRef: String
    ) {
        if (!hasFirebaseAuthForCurrentUser()) {
            _snackbarMessage.value = "سجل الدخول بحساب Firebase لرفع الإيصال."
            return
        }
        if (receiptImageUri.isBlank() || transactionRef.trim().length < 6) {
            _snackbarMessage.value = "أرفق إيصالاً وأدخل رقم تحويل صحيحاً (6 أحرف على الأقل)."
            return
        }
        val type = receiptContentType(receiptImageUri) ?: run {
            _snackbarMessage.value = "نوع الملف غير مدعوم. استخدم JPG أو PNG أو PDF."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                repository.submitVerifiedReceipt(
                    receiptUri = receiptImageUri,
                    contentType = type,
                    method = methodKey,
                    transactionRef = transactionRef,
                    purpose = "wallet",
                    amountUsd = amountUsd
                ).onSuccess {
                    _snackbarMessage.value = "تم إرسال الإيصال للمراجعة. الرصيد لن يزيد قبل تحقق الإدارة من التحويل."
                }.onFailure { error ->
                    _snackbarMessage.value = "فشل رفع الإيصال: ${error.localizedMessage ?: "خطأ في الخادم"}"
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun receiptContentType(uri: String): String? {
        val guessed = when (Uri.parse(uri).lastPathSegment?.substringAfterLast('.')?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            else -> null
        }
        val resolved = appContext?.contentResolver?.getType(Uri.parse(uri)) ?: guessed
        return resolved?.takeIf { it in setOf("image/jpeg", "image/png", "application/pdf") }
    }

    // ========================================================================
    // ========================================================================
    // PAID CHAT UNLOCK & MESSAGING
    // ========================================================================

    fun selectActiveChatMember(memberId: String?) {
        if (memberId == null) {
            _activeChatMemberId.value = null
            return
        }
        val member = _chatMembers.value.find { it.id == memberId } ?: return
        if (memberId in _unlockedChatMemberIds.value) {
            _activeChatMemberId.value = memberId
        } else if (member.chatPriceUsd == 0) {
            unlockChatWithWallet(member)
        } else {
            _snackbarMessage.value = "يلزم فتح الدردشة عبر الخادم أولاً."
        }
    }

    fun openOrRequestChatWithVenueProvider(venue: Venue, onNavigateToChatTab: () -> Unit) {
        // The creator is resolved from a real published offer, never by matching city.
        _activeChatMemberId.value = _chatMembers.value
            .firstOrNull { it.displayName == venue.providerDisplayName && it.id in _unlockedChatMemberIds.value }?.id
        onNavigateToChatTab()
    }

    fun unlockChatWithWallet(member: ChatMemberProfile) {
        if (!hasFirebaseAuthForCurrentUser()) {
            _snackbarMessage.value = "يلزم تسجيل الدخول قبل فتح المحادثة."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                repository.unlockServerPaidChat(member.id, member.chatPriceUsd)
                    .onSuccess { threadId ->
                        subscribeToVerifiedThread(member.id, threadId)
                        _activeChatMemberId.value = member.id
                        _snackbarMessage.value = "تم فتح المحادثة بواسطة الخادم."
                    }.onFailure { error ->
                        _snackbarMessage.value = "لم تُفتح الدردشة: ${error.localizedMessage ?: "رفض الخادم الطلب"}"
                    }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun unlockChatWithReceipt(
        member: ChatMemberProfile,
        methodKey: String,
        receiptUri: String,
        txRef: String
    ) {
        _snackbarMessage.value = "دفع المحادثة بإيصال خارجي غير مفعل حتى اكتمال مراجعة المدفوعات. استخدم محفظة موثقة."
    }

    fun sendChatMessage(memberId: String, text: String) {
        val threadId = chatThreadByMember[memberId]
        if (!hasFirebaseAuthForCurrentUser() || threadId == null || memberId !in _unlockedChatMemberIds.value) {
            _snackbarMessage.value = "هذه المحادثة غير مفتوحة من الخادم."
            return
        }
        val message = text.trim().take(2000)
        if (message.isBlank()) return
        viewModelScope.launch {
            repository.sendServerChatMessage(threadId, message)
                .onFailure { error ->
                    _snackbarMessage.value = "لم تُرسل الرسالة: ${error.localizedMessage ?: "تعذر الوصول للخادم"}"
                }
        }
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

    fun submitCreatorWithdrawal(amountUsd: Int, destination: String) {
        if (!hasFirebaseAuthForCurrentUser()) {
            _snackbarMessage.value = "سجل الدخول بحساب Firebase موثق لطلب السحب."
            return
        }
        if (amountUsd < 1 || destination.trim().length < 8) {
            _snackbarMessage.value = "أدخل مبلغاً صحيحاً ووجهة سحب صالحة."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                repository.requestCreatorWithdrawal(amountUsd, destination.trim())
                    .onSuccess { withdrawalId ->
                        _snackbarMessage.value = "تم حجز مبلغ السحب وإرسال الطلب ${withdrawalId} للمراجعة. لم يتم التحويل بعد."
                    }.onFailure { error ->
                        _snackbarMessage.value = "تعذر طلب السحب: ${error.localizedMessage ?: "رفض الخادم"}"
                    }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun topUpWallet(amountUsd: Int, bonusUsd: Int = 0, methodTitle: String = "شحن رصيد المحفظة") {
        _snackbarMessage.value = "شحن الرصيد متوقف لحين تأكيد الدفع من الخادم."
    }

    fun adminAdjustWalletBalance(amountDeltaUsd: Int, reasonNote: String) {
        _snackbarMessage.value = "لا يمكن تعديل الرصيد من الجهاز. يلزم تفويض الإدارة من الخادم وسجل تدقيق."
    }

    fun deductWalletBalance(amountUsd: Int, orderTitle: String): Boolean {
        _snackbarMessage.value = "تأكيد دفع المحفظة يتطلب خصماً موثقاً من الخادم."
        return false
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
        if (!hasFirebaseAuthForCurrentUser()) {
            _snackbarMessage.value = "يلزم حساب Firebase فعلي لنشر الخدمة."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                repository.publishService(
                    title = title,
                    cityName = cityName,
                    country = country,
                    category = category,
                    description = description,
                    priceUsd = priceUsd,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl,
                    mediaType = mediaType
                ).onSuccess { remoteId ->
                    seenPublishedIds.add(remoteId)
                    _snackbarMessage.value = "تم نشر الخدمة في قاعدة البيانات بنجاح."
                    onPublished(
                        PublishedService(
                            id = remoteId, authorId = currentUserId, title = title,
                            cityName = cityName, country = country, category = category,
                            description = description, priceUsd = priceUsd.toLong(),
                            imageUrl = imageUrl, videoUrl = videoUrl,
                            mediaType = mediaType, status = "active"
                        )
                    )
                }.onFailure { error ->
                    _snackbarMessage.value = "تعذر نشر الخدمة: ${error.localizedMessage ?: "خطأ بالخادم"}"
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /** Creator publication requires real Firebase authentication and a successful write. */
    fun publishCreatorPaidPost(title: String, preview: String, body: String, priceUsd: Int) {
        if (!hasFirebaseAuth()) {
            _snackbarMessage.value = "يجب تسجيل الدخول بحساب متصل قبل نشر منشور مدفوع"
            return
        }
        if (title.trim().length < 3 || preview.trim().length < 10 || body.trim().length < 20 || priceUsd !in 1..1000) {
            _snackbarMessage.value = "تحقق من العنوان والوصف والمحتوى والسعر (1–1000 دولار)"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                repository.publishPaidPost(
                    title = title,
                    preview = preview,
                    privateBody = body,
                    priceUsd = priceUsd,
                    cityName = _userProfile.value.cityName,
                    country = _userProfile.value.country
                ).onSuccess { postId ->
                    val created = PublishedService(
                        id = postId,
                        authorId = currentUserId,
                        title = title.trim(),
                        cityName = _userProfile.value.cityName.ifBlank { "غير محدد" },
                        country = _userProfile.value.country,
                        category = "منشور مدفوع",
                        description = preview.trim(),
                        priceUsd = priceUsd.toLong(),
                        imageUrl = "",
                        videoUrl = "",
                        mediaType = "photo",
                        status = "active",
                        createdAt = Timestamp.now()
                    )
                    seenPublishedIds.add(postId)
                    _localPublishedServices.value = listOf(created) + _localPublishedServices.value
                    _snackbarMessage.value = "تم نشر المعاينة وحفظ المحتوى الخاص بشكل محمي. الشراء متوقف حتى توثيق الدفع من الخادم."
                }.onFailure { error ->
                    Log.e(TAG, "Failed to publish private paid post", error)
                    _snackbarMessage.value = "تعذر نشر المنشور المدفوع. تحقق من الاتصال وصلاحيات الحساب."
                }
            } finally {
                _isSubmitting.value = false
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

        if (hasFirebaseAuth()) {
            viewModelScope.launch {
                repository.updatePublishedServicePrice(serviceId, validPrice)
            }
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
        if (hasFirebaseAuth()) {
            viewModelScope.launch {
                repository.updatePublishedServicePrice(serviceId, validPrice)
            }
        }
    }

    fun deletePublishedService(serviceId: String) {
        if (!enforceSuperAdminMiddleware()) return
        _localPublishedServices.value = _localPublishedServices.value.filterNot { it.id == serviceId }
        _snackbarMessage.value = "تم حذف العرض المنشور"
        if (hasFirebaseAuth()) {
            viewModelScope.launch {
                repository.deletePublishedService(serviceId)
            }
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

        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.blockUser(cleanId, reason, cityScope)
            }
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
        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.toggleBlockStatus(blockedId, currentStatus)
            }
        }
    }

    fun deleteBlockedRecord(blockedId: String) {
        if (!enforceSuperAdminMiddleware()) return
        _localBlockedUsers.value = _localBlockedUsers.value.filterNot { it.id == blockedId }
        _snackbarMessage.value = "تم حذف سجل الحظر"
        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.deleteBlockedUser(blockedId)
            }
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
        paymentMethodTitle: String = "شام كاش / USDT",
        receiptImageUri: String = "",
        transactionReference: String = "",
        onSuccess: () -> Unit = {}
    ) {
        if (!hasFirebaseAuthForCurrentUser()) {
            _snackbarMessage.value = "يلزم تسجيل الدخول بحساب Firebase قبل طلب الحجز."
            return
        }
        val payWithWallet = transactionReference == "WALLET_DEDUCT"
        if (!payWithWallet && (receiptImageUri.isBlank() || transactionReference.trim().length < 6)) {
            _snackbarMessage.value = "أرفق إيصال التحويل ورقم معاملته لإرسال طلب الحجز."
            return
        }
        val contentType = if (payWithWallet) null else receiptContentType(receiptImageUri)
        if (!payWithWallet && contentType == null) {
            _snackbarMessage.value = "صيغة الإيصال غير مدعومة."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                repository.createVerifiedBookingRequest(
                    serviceId = venue.id,
                    bookingDate = bookingDate,
                    timeSlot = timeSlot,
                    guestCount = guestCount,
                    notes = notes,
                    payWithWallet = payWithWallet
                ).onSuccess { (bookingId, status) ->
                    if (payWithWallet) {
                        _snackbarMessage.value = "تم الحجز وخصم الرصيد من الخادم بنجاح."
                        onSuccess()
                    } else {
                        repository.submitVerifiedReceipt(
                            receiptUri = receiptImageUri,
                            contentType = checkNotNull(contentType),
                            method = if (paymentMethodTitle.contains("شام")) "SHAM_CASH" else "USDT_BEP20",
                            transactionRef = transactionReference,
                            purpose = "booking",
                            bookingId = bookingId
                        ).onSuccess {
                            _snackbarMessage.value = "تم تسجيل طلب الحجز وإيصال الدفع. الحالة: بانتظار مراجعة التحويل."
                            onSuccess()
                        }.onFailure { error ->
                            _snackbarMessage.value = "تم إنشاء الحجز ${bookingId} بحالة انتظار؛ تعذر إرسال الإيصال: ${error.localizedMessage}"
                        }
                    }
                }.onFailure { error ->
                    _snackbarMessage.value = "لم يتم إنشاء الحجز: ${error.localizedMessage ?: "تعذر الاتصال بالخادم"}"
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        _snackbarMessage.value = "تغيير حالة الحجز يتطلب مراجعة الخادم. لم يتم تعديل الحجز."
    }

    fun adminUpdateBookingStatus(bookingId: String, newStatus: String) {
        // Admin workflow uses a Firebase-authenticated claim, not a local session token.
        _snackbarMessage.value = "تغيير حالات الحجوزات لا ينفذ من الجهاز قبل تحقق تفويض الإدارة من الخادم."
    }

    fun rescheduleBooking(
        bookingId: String,
        newDate: String,
        newTimeSlot: String,
        newGuestCount: Int,
        newNotes: String
    ) {
        _snackbarMessage.value = "إعادة الجدولة تتطلب تعديل السعة والحجز معاً من الخادم. لم يُعدّل الموعد."
    }

    fun deleteBooking(bookingId: String) {
        _snackbarMessage.value = "لا يمكن حذف سجل مالي من الجهاز. يمكن طلب الإلغاء من الإدارة."
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
        gainProfileXp(15)
        _snackbarMessage.value = "شكراً لك! تم نشر تقييمك الموثق لـ ${venue.name} (+15 XP)"
        onSuccess()

        if (hasFirebaseAuthForCurrentUser()) {
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
    }

    fun deleteCustomerReview(reviewId: String) {
        _localReviews.value = _localReviews.value.filterNot { it.id == reviewId }
        _snackbarMessage.value = "تم حذف التقييم"
        if (hasFirebaseAuthForCurrentUser()) {
            viewModelScope.launch {
                repository.deleteReview(reviewId)
            }
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

        if (hasFirebaseAuthForCurrentUser()) {
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
    }

    private companion object {
        const val TAG = "LayaliVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
