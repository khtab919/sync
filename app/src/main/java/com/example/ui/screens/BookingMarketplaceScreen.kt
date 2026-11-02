package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PublishedService
import com.example.ui.components.SmartServiceMediaView

/**
 * A storefront for actual active Firestore creator services, not synthetic venues.
 * The booking CTA submits a pending request; no payment or approval is implied.
 */
@Composable
fun BookingMarketplaceScreen(
    services: List<PublishedService>,
    loading: Boolean,
    error: String?,
    submittingId: String?,
    onRequestBooking: (PublishedService) -> Unit,
    onViewService: (PublishedService) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var category by remember { mutableStateOf("الكل") }
    val available = remember(services) {
        services.filter {
            it.id.isNotBlank() &&
                it.authorId.isNotBlank() &&
                it.status == "active" &&
                it.category != "منشور مدفوع" &&
                it.priceUsd > 0
        }
    }
    val categories = remember(available) {
        listOf("الكل") + available.map { it.category }.filter { it.isNotBlank() }.distinct()
    }
    val filtered = remember(available, category) {
        if (category == "الكل") available else available.filter { it.category == category }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("booking_marketplace_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الحجز", style = MaterialTheme.typography.headlineLarge, color = colors.onBackground)
                Text(
                    "اكتشف خدمات المذيعات والأسعار، وأرسل طلبك للمراجعة قبل تأكيد الحجز.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item) },
                        modifier = Modifier.testTag("marketplace_filter_${item}")
                    )
                }
            }
        }
        when {
            loading -> item { Text("جارٍ تحميل الخدمات من قاعدة البيانات…", color = colors.onSurfaceVariant) }
            error != null -> item { Text(error, color = colors.error) }
            filtered.isEmpty() -> item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = colors.onSurfaceVariant)
                    Text("لا توجد خدمات منشورة في هذا القسم حاليًا", color = colors.onSurfaceVariant)
                }
            }
            else -> items(filtered.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { service ->
                        Card(
                            modifier = Modifier.weight(1f).testTag("marketplace_service_${service.id}"),
                            shape = RoundedCornerShape(17.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.65f))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmartServiceMediaView(
                                    imageUrl = service.imageUrl,
                                    category = service.category,
                                    accentHex = 0xFF45788CL,
                                    secondaryHex = 0xFF182D3EL,
                                    modifier = Modifier.fillMaxWidth().height(135.dp)
                                )
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 3.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        service.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        service.cityName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        "${service.priceUsd}$",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = { onRequestBooking(service) },
                                        enabled = submittingId != service.id,
                                        shape = RoundedCornerShape(11.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("request_booking_${service.id}"),
                                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                                    ) {
                                        Text(if (submittingId == service.id) "جارٍ الإرسال…" else "طلب حجز")
                                    }
                                    androidx.compose.material3.TextButton(
                                        onClick = { onViewService(service) },
                                        modifier = Modifier.fillMaxWidth().testTag("view_service_${service.id}")
                                    ) { Text("التفاصيل", color = colors.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
