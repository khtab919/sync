package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.functions.functions
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import com.example.R
import com.example.data.model.BlockedUser
import com.example.data.model.Booking
import com.example.data.model.ChatMessageItem
import com.example.data.model.CustomerReview
import com.example.data.model.FavoriteVenue
import com.example.data.model.PublishedService
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LayaliRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    /**
     * Only the server-maintained wallets collection is an authoritative balance.
     * A local SQLite account and /users walletBalanceUsd are NOT sources of funds.
     */
    fun observeVerifiedWalletBalance(userId: String = requireUserId()): Flow<Int> = flow {
        if (auth.currentUser?.uid != userId) {
            emit(0)
            return@flow
        }
        emitAll(db.collection("wallets").document(userId).snapshots().map { snapshot ->
            val balanceCents = snapshot.getLong("balanceCents") ?: 0L
            if (balanceCents < 0L) 0 else (balanceCents / 100L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        })
    }

    /** Accepted payments are always reconciled by the trusted Firebase callable endpoint. */
    suspend fun createVerifiedBookingRequest(
        serviceId: String, bookingDate: String, timeSlot: String,
        guestCount: Int, notes: String, payWithWallet: Boolean
    ): Result<Pair<String, String>> = runCatching {
        requireUserId()
        val payload = mapOf(
            "serviceId" to serviceId,
            "bookingDate" to bookingDate,
            "timeSlot" to timeSlot,
            "guestCount" to guestCount,
            "notes" to notes,
            "paymentMethod" to if (payWithWallet) "wallet" else "external"
        )
        @Suppress("UNCHECKED_CAST")
        val result = Firebase.functions("europe-west1")
            .getHttpsCallable("createBookingRequest").call(payload).await().data as Map<String, Any?>
        Pair(result["bookingId"] as String, result["status"] as String)
    }

    /**
     * Upload a private immutable receipt to Storage before queueing it for independent
     * manual payment verification. The server grants NO credit on submission.
     */
    suspend fun submitVerifiedReceipt(
        receiptUri: String, contentType: String, method: String, transactionRef: String,
        purpose: String, amountUsd: Int = 0, bookingId: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(contentType in setOf("image/jpeg", "image/png", "application/pdf")) { "صيغة الإيصال غير مدعومة" }
        val suffix = when (contentType) {
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> "jpg"
        }
        val id = UUID.randomUUID().toString().replace("-", "")
        val storagePath = "receipts/$uid/$id.$suffix"
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
        Firebase.storage.reference.child(storagePath).putFile(Uri.parse(receiptUri), metadata).await()
        val payload = mutableMapOf<String, Any>(
            "submissionId" to id,
            "storagePath" to storagePath,
            "method" to method,
            "transactionReference" to transactionRef,
            "purpose" to purpose
        )
        if (purpose == "wallet") payload["amountUsd"] = amountUsd
        if (purpose == "booking") payload["bookingId"] = checkNotNull(bookingId)
        @Suppress("UNCHECKED_CAST")
        val result = Firebase.functions("europe-west1")
            .getHttpsCallable("submitPaymentReceipt").call(payload).await().data as Map<String, Any?>
        result["receiptId"] as String
    }

    fun observeCreatorEarnings(userId: String = requireUserId()): Flow<Pair<Int, Int>> = flow {
        if (auth.currentUser?.uid != userId) {
            emit(0 to 0)
            return@flow
        }
        emitAll(db.collection("creator_wallets").document(userId).snapshots().map { doc ->
            val available = (doc.getLong("availableCents") ?: 0L).coerceAtLeast(0L) / 100L
            val reserved = (doc.getLong("reservedCents") ?: 0L).coerceAtLeast(0L) / 100L
            available.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() to
                reserved.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        })
    }

    suspend fun requestCreatorWithdrawal(amountUsd: Int, destination: String): Result<String> = runCatching {
        requireUserId()
        @Suppress("UNCHECKED_CAST")
        val result = Firebase.functions("europe-west1").getHttpsCallable("requestWithdrawal")
            .call(mapOf("amountUsd" to amountUsd, "destination" to destination))
            .await().data as Map<String, Any?>
        result["withdrawalId"] as String
    }


    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }


    data class CreatorChatQuote(val displayName: String, val username: String, val priceUsd: Int)

    suspend fun fetchCreatorChatQuote(creatorId: String): Result<CreatorChatQuote> = runCatching {
        requireUserId()
        @Suppress("UNCHECKED_CAST")
        val result = Firebase.functions("europe-west1").getHttpsCallable("getCreatorChatQuote")
            .call(mapOf("creatorId" to creatorId)).await().data as Map<String, Any?>
        CreatorChatQuote(
            displayName = result["displayName"] as String,
            username = result["username"] as String,
            priceUsd = (result["priceUsd"] as Number).toInt()
        )
    }

    suspend fun unlockServerPaidChat(creatorId: String, expectedPriceUsd: Int): Result<String> = runCatching {
        requireUserId()
        @Suppress("UNCHECKED_CAST")
        val result = Firebase.functions("europe-west1").getHttpsCallable("unlockPaidChat")
            .call(mapOf("creatorId" to creatorId, "expectedPriceUsd" to expectedPriceUsd)).await().data as Map<String, Any?>
        result["threadId"] as String
    }

    suspend fun sendServerChatMessage(threadId: String, message: String): Result<String> = runCatching {
        requireUserId()
        @Suppress("UNCHECKED_CAST")
        val result = Firebase.functions("europe-west1").getHttpsCallable("sendVerifiedMessage")
            .call(mapOf("threadId" to threadId, "message" to message)).await().data as Map<String, Any?>
        result["messageId"] as String
    }

    fun observeVerifiedChatMessages(threadId: String, memberId: String): Flow<List<ChatMessageItem>> = flow {
        val uid = requireUserId()
        require(threadId.matches(Regex("^[0-9a-f]{64}$")))
        emitAll(db.collection("chat_threads").document(threadId).collection("messages")
            .orderBy("createdAt").snapshots().map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    ChatMessageItem(
                        id = doc.id, memberId = memberId,
                        senderName = if (senderId == uid) "أنا" else "مقدم الخدمة",
                        senderUsername = "",
                        text = text, timestampLabel = "",
                        isFromCurrentUser = senderId == uid
                    )
                }
            })
    }

    private fun isSyntheticSessionId(userId: String): Boolean {
        return userId.isBlank() || userId == "guest_session" || userId.startsWith("user_")
    }

    fun observeBookings(userId: String = requireUserId()): Flow<List<Booking>> = flow {
        if (isSyntheticSessionId(userId)) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/bookings"
        emitAll(
            db.collection("users").document(userId).collection("bookings")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Booking::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserReviews(userId: String = requireUserId()): Flow<List<CustomerReview>> = flow {
        if (isSyntheticSessionId(userId)) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/reviews"
        emitAll(
            db.collection("users").document(userId).collection("reviews")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomerReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeFavorites(userId: String = requireUserId()): Flow<List<FavoriteVenue>> = flow {
        if (isSyntheticSessionId(userId)) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteVenue::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePublishedServices(): Flow<List<PublishedService>> = flow {
        if (auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "published_services"
        emitAll(
            db.collection("published_services")
                .whereEqualTo("status", "active")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PublishedService::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeBlockedUsers(userId: String = requireUserId()): Flow<List<BlockedUser>> = flow {
        if (isSyntheticSessionId(userId)) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/blocked_users"
        emitAll(
            db.collection("users").document(userId).collection("blocked_users")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BlockedUser::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun publishService(
        title: String,
        cityName: String,
        country: String,
        category: String,
        description: String,
        priceUsd: Int,
        imageUrl: String,
        videoUrl: String,
        mediaType: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val serviceId = "srv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("published_services").document(serviceId)
        val safeTitle = title.trim().let { if (it.length < 2) "$it عرض" else it.take(150) }
        val safeDesc = description.trim().let { if (it.length < 3) "$it تفاصيل العرض" else it.take(600) }
        val payload = mapOf(
            "authorId" to uid,
            "title" to safeTitle,
            "cityName" to cityName.trim().take(80),
            "country" to if (country in listOf("سوريا", "تركيا")) country else "سوريا",
            "category" to category.trim().take(80),
            "description" to safeDesc,
            "priceUsd" to priceUsd.coerceIn(5, 10000).toLong(),
            "imageUrl" to imageUrl.trim().take(500),
            "videoUrl" to videoUrl.trim().take(500),
            "mediaType" to if (mediaType in listOf("photo", "video", "both")) mediaType else "both",
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            serviceId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Publish a paid post preview while keeping the actual paid body unreadable
     * from the public feed. Payment verification and buyer entitlements must be
     * implemented server-side before buyers can access creator_post_bodies.
     */
    suspend fun publishPaidPost(
        title: String,
        preview: String,
        privateBody: String,
        priceUsd: Int,
        cityName: String,
        country: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(title.trim().length in 3..150) { "عنوان المنشور غير صالح" }
        require(preview.trim().length in 10..300) { "اكتب وصفاً قصيراً للمعاينة" }
        require(privateBody.trim().length in 20..5000) { "محتوى المنشور قصير جداً أو طويل جداً" }
        require(priceUsd in 1..1000) { "السعر يجب أن يكون بين 1 و1000 دولار" }

        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val publicRef = db.collection("published_services").document(postId)
        val privateRef = db.collection("creator_post_bodies").document(postId)
        val safeCity = cityName.trim().take(80).ifBlank { "غير محدد" }
        val safeCountry = if (country == "تركيا") "تركيا" else "سوريا"
        val publicPreview = mapOf(
            "authorId" to uid,
            "title" to title.trim().take(150),
            "cityName" to safeCity,
            "country" to safeCountry,
            "category" to "منشور مدفوع",
            "description" to preview.trim().take(300),
            "priceUsd" to priceUsd.toLong(),
            "imageUrl" to "",
            "videoUrl" to "",
            "mediaType" to "photo",
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val privateContent = mapOf(
            "authorId" to uid,
            "serviceId" to postId,
            "body" to privateBody.trim().take(5000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            db.batch().apply {
                set(publicRef, publicPreview)
                set(privateRef, privateContent)
            }.commit().await()
            postId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, privateRef.path)
            throw e
        }
    }

    suspend fun updatePublishedServicePrice(
        serviceId: String,
        newPriceUsd: Int
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("published_services").document(serviceId)
        val updates = mapOf(
            "priceUsd" to newPriceUsd.coerceIn(5, 10000).toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePublishedService(serviceId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("published_services").document(serviceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun blockUser(
        blockedIdentifier: String,
        reason: String,
        cityScope: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val blockedId = "blk_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("blocked_users").document(blockedId)
        val safeIdentifier = blockedIdentifier.trim().let { if (it.length < 2) "@user_$it" else it.take(120) }
        val safeReason = reason.trim().let { if (it.length < 2) "مخالفة سياسة الاستخدام" else it.take(300) }
        val payload = mapOf(
            "userId" to uid,
            "blockedIdentifier" to safeIdentifier,
            "reason" to safeReason,
            "cityScope" to cityScope.ifBlank { "جميع المدن" }.take(80),
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            blockedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleBlockStatus(blockedId: String, currentStatus: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("blocked_users").document(blockedId)
        val nextStatus = if (currentStatus == "active") "unblocked" else "active"
        val updates = mapOf(
            "status" to nextStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBlockedUser(blockedId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("blocked_users").document(blockedId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createBooking(
        venueId: String,
        venueName: String,
        cityName: String,
        country: String,
        serviceTitle: String,
        category: String,
        bookingDate: String,
        timeSlot: String,
        guestCount: Int,
        totalPriceUsd: Int,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val bookingId = "bk_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
        val payload = mapOf(
            "userId" to uid,
            "venueId" to venueId,
            "venueName" to venueName,
            "cityName" to cityName,
            "country" to country,
            "serviceTitle" to serviceTitle,
            "category" to category,
            "bookingDate" to bookingDate,
            "timeSlot" to timeSlot,
            "guestCount" to guestCount.toLong(),
            "totalPriceUsd" to totalPriceUsd.toLong(),
            "notes" to notes.trim().take(400),
            "status" to "confirmed",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            bookingId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getBookingById(ownerUserId: String, bookingId: String): Result<Booking?> = runCatching {
        val docRef = db.collection("users").document(ownerUserId).collection("bookings").document(bookingId)
        try {
            val snap = docRef.get().await()
            snap.toObject(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
        val updates = mapOf(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun rescheduleBooking(
        bookingId: String,
        newDate: String,
        newTimeSlot: String,
        newGuestCount: Int,
        newNotes: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
        val updates = mapOf(
            "bookingDate" to newDate,
            "timeSlot" to newTimeSlot,
            "guestCount" to newGuestCount.toLong(),
            "notes" to newNotes.trim().take(400),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBooking(bookingId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun submitReview(
        authorName: String,
        venueId: String,
        venueName: String,
        cityName: String,
        serviceType: String,
        rating: Int,
        comment: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val reviewId = "rv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("reviews").document(reviewId)
        val safeComment = comment.trim().let { if (it.length < 3) "$it..." else it.take(500) }
        val payload = mapOf(
            "userId" to uid,
            "authorName" to authorName.ifBlank { "عميل موثق" }.take(100),
            "venueId" to venueId,
            "venueName" to venueName,
            "cityName" to cityName,
            "serviceType" to serviceType,
            "rating" to rating.coerceIn(1, 5).toLong(),
            "comment" to safeComment,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            reviewId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("reviews").document(reviewId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavorite(
        venueId: String,
        venueName: String,
        cityName: String,
        category: String,
        currentlyFavorite: Boolean
    ): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("favorites").document(venueId)
        try {
            if (currentlyFavorite) {
                docRef.delete().await()
                false
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "venueId" to venueId,
                    "venueName" to venueName,
                    "cityName" to cityName,
                    "category" to category,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (currentlyFavorite) OperationType.DELETE else OperationType.CREATE,
                docRef.path
            )
            throw e
        }
    }

    suspend fun saveOrUpdateUserAccount(
        username: String,
        displayName: String,
        email: String,
        bio: String,
        cityName: String,
        country: String,
        avatarUri: String,
        selectedFrameId: String,
        level: Int,
        xpPoints: Int,
        nextLevelXp: Int,
        walletBalanceUsd: Int,
        chatPriceUsd: Int,
        isPaidChatEnabled: Boolean,
        passwordSaltHex: String,
        passwordHashHex: String,
        role: String = "customer"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeUsername = username.trim().let {
            val base = if (it.startsWith("@")) it else "@$it"
            if (base.length < 3) "${base}_vip" else base.take(50)
        }
        val safeDisplayName = displayName.trim().let {
            if (it.length < 2) "عضو ليالي الملكي" else it.take(100)
        }
        val safeEmail = email.trim().let {
            if (it.length < 3) "${safeUsername.removePrefix("@")}@layali.client" else it.take(150)
        }
        val safeSaltHex = passwordSaltHex.trim().ifBlank { "a1b2c3d4e5f60718293a4b5c6d7e8f90" }.take(128)
        val safeHashHex = passwordHashHex.trim().ifBlank {
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
        }.take(256)
        val safeCountry = if (country.trim() in listOf("سوريا", "تركيا")) country.trim() else "سوريا"

        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "username" to safeUsername,
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "bio" to bio.trim().take(300),
                    "cityName" to cityName.trim().ifBlank { "دمشق" }.take(80),
                    "country" to safeCountry,
                    "selectedFrameId" to selectedFrameId.trim().ifBlank { "emerald_crown" }.take(60),

                    "chatPriceUsd" to chatPriceUsd.coerceIn(1, 5000).toLong(),
                    "isPaidChatEnabled" to isPaidChatEnabled,
                    "passwordSaltHex" to safeSaltHex,
                    "passwordHashHex" to safeHashHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "username" to safeUsername,
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "bio" to bio.trim().take(300),
                    "cityName" to cityName.trim().ifBlank { "دمشق" }.take(80),
                    "country" to safeCountry,
                    "selectedFrameId" to selectedFrameId.trim().ifBlank { "emerald_crown" }.take(60),
                    "level" to level.coerceIn(1, 100).toLong(),
                    "xpPoints" to xpPoints.coerceIn(0, 10000000).toLong(),
                    "nextLevelXp" to nextLevelXp.coerceIn(100, 50000000).toLong(),
                    "walletBalanceUsd" to 0L,
                    "chatPriceUsd" to chatPriceUsd.coerceIn(1, 5000).toLong(),
                    "isPaidChatEnabled" to isPaidChatEnabled,
                    "passwordSaltHex" to safeSaltHex,
                    "passwordHashHex" to safeHashHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserAccountPasswordHash(
        newSaltHex: String,
        newHashHex: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mapOf(
            "passwordSaltHex" to newSaltHex.trim().take(128),
            "passwordHashHex" to newHashHex.trim().take(256),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun getUserAccountById(targetUserId: String): Result<com.example.data.model.UserAccount?> = runCatching {
        val docRef = db.collection("users").document(targetUserId)
        try {
            val snap = docRef.get().await()
            snap.toObject(
                com.example.data.model.UserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }
}
