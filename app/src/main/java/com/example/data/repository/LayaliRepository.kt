package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BlockedUser
import com.example.data.model.Booking
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

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
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
}
