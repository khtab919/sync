package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PublishedService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private data class PendingBookingReview(val id: String, val title: String, val buyerId: String, val price: Long)

/** The only booking operation in the marketplace: submit for trusted admin review. */
suspend fun submitMarketplaceRequest(
    db: FirebaseFirestore,
    service: PublishedService
): Result<Unit> = runCatching {
    val uid = Firebase.auth.currentUser?.uid
        ?: error("يجب تسجيل الدخول بحساب Firebase موثّق قبل طلب الحجز")
    require(service.id.isNotBlank() && service.authorId.isNotBlank() && service.priceUsd > 0)
    val id = "req_" + UUID.randomUUID().toString().replace("-", "")
    val payload = mapOf(
        "userId" to uid,
        "serviceId" to service.id,
        "creatorId" to service.authorId,
        "serviceTitle" to service.title,
        "priceUsd" to service.priceUsd,
        "status" to "pending",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    db.collection("service_booking_requests").document(id).set(payload).await()
    Unit
}

/** Only Firebase rule-authorized moderators can list or review this live inbox. */
@Composable
fun AdminModerationInbox() {
    val context = LocalContext.current
    val db = remember(context) { FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var services by remember { mutableStateOf<List<PublishedService>>(emptyList()) }
    var requests by remember { mutableStateOf<List<PendingBookingReview>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val uid = Firebase.auth.currentUser?.uid

    DisposableEffect(db, uid) {
        if (uid == null) {
            errorMessage = "مراجعة الطلبات تحتاج حساب Firebase مخوّلاً بالإدارة، وليس جلسة إدارة محلية فقط."
            onDispose { }
        } else {
            val servicesListener = db.collection("published_services")
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) errorMessage = "تعذر تحميل العروض: ${error.localizedMessage}"
                    else if (snapshot != null) services = snapshot.toObjects(PublishedService::class.java)
                }
            val requestsListener = db.collection("service_booking_requests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) errorMessage = "تعذر تحميل الحجوزات: ${error.localizedMessage}"
                    else if (snapshot != null) requests = snapshot.documents.map { doc ->
                        PendingBookingReview(
                            id = doc.id,
                            title = doc.getString("serviceTitle").orEmpty(),
                            buyerId = doc.getString("userId").orEmpty(),
                            price = doc.getLong("priceUsd") ?: 0L
                        )
                    }
                }
            onDispose {
                servicesListener.remove()
                requestsListener.remove()
            }
        }
    }
    Surface(
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("مراجعة الإدارة", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(
                "العروض والحجوزات الواردة مباشرة من Firestore. مراجعة الحجز لا تعني تأكيد الدفع.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (errorMessage != null) Text(errorMessage!!, color = colors.error)
            if (feedback != null) Text(feedback!!, color = colors.onSurfaceVariant)
            if (services.isEmpty() && requests.isEmpty() && errorMessage == null)
                Text("لا توجد طلبات معلّقة", color = colors.onSurfaceVariant)
            services.forEach { service ->
                ModerationRow(
                    title = "عرض: ${service.title}",
                    subtitle = "سعر ${service.priceUsd}$",
                    busy = processingId == service.id,
                    approve = "نشر",
                    onApprove = {
                        scope.launch {
                            processingId = service.id
                            try {
                                db.collection("published_services").document(service.id)
                                    .update("status", "active", "updatedAt", FieldValue.serverTimestamp()).await()
                                feedback = "تم اعتماد العرض في قاعدة البيانات"
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { errorMessage = "فشل اعتماد العرض: ${e.localizedMessage}" }
                            finally { processingId = null }
                        }
                    },
                    onReject = {
                        scope.launch {
                            processingId = service.id
                            try {
                                db.collection("published_services").document(service.id)
                                    .update("status", "archived", "updatedAt", FieldValue.serverTimestamp()).await()
                                feedback = "تم رفض العرض"
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { errorMessage = "تعذر رفض العرض: ${e.localizedMessage}" }
                            finally { processingId = null }
                        }
                    }
                )
            }
            requests.forEach { request ->
                ModerationRow(
                    title = "طلب: ${request.title}",
                    subtitle = "المستخدم ${request.buyerId.take(12)} • ${request.price}$",
                    busy = processingId == request.id,
                    approve = "تمت المراجعة",
                    onApprove = {
                        scope.launch {
                            processingId = request.id
                            try {
                                db.collection("service_booking_requests").document(request.id)
                                    .update("status", "reviewed", "updatedAt", FieldValue.serverTimestamp()).await()
                                feedback = "تمت مراجعة طلب الحجز؛ لم يتم تحصيل أي دفعة"
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { errorMessage = "فشلت مراجعة الحجز: ${e.localizedMessage}" }
                            finally { processingId = null }
                        }
                    },
                    onReject = {
                        scope.launch {
                            processingId = request.id
                            try {
                                db.collection("service_booking_requests").document(request.id)
                                    .update("status", "rejected", "updatedAt", FieldValue.serverTimestamp()).await()
                                feedback = "تم رفض الطلب"
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { errorMessage = "تعذر رفض الطلب: ${e.localizedMessage}" }
                            finally { processingId = null }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModerationRow(
    title: String,
    subtitle: String,
    busy: Boolean,
    approve: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onApprove, enabled = !busy, modifier = Modifier.testTag("moderation_approve")) { Text(approve) }
            OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.testTag("moderation_reject")) { Text("رفض") }
        }
    }
}
