package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMemberProfile
import com.example.data.model.ChatMessageItem
import com.example.data.model.UserProfile
import com.example.ui.components.FramedUserAvatar
import com.example.ui.components.LuxuryGradientButton
import com.example.ui.components.MandatoryReceiptAttachmentCard
import com.example.ui.components.ShamCashOfficialCard
import com.example.ui.components.UsdtBep20OfficialCard
import com.example.ui.theme.CharcoalNight
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ShamCashTeal
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SubtitleGrayText
import com.example.ui.theme.UsdtTetherGreen
import com.example.ui.theme.VioletBlueVip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    userProfile: UserProfile,
    walletBalanceUsd: Int,
    chatMembers: List<ChatMemberProfile>,
    unlockedMemberIds: Set<String>,
    activeConversationMemberId: String?,
    messagesByMember: Map<String, List<ChatMessageItem>>,
    onSelectConversationMember: (String?) -> Unit,
    onUpdateMyChatPrice: (priceUsd: Int, isEnabled: Boolean) -> Unit,
    onUnlockChatWithWallet: (ChatMemberProfile) -> Unit,
    onUnlockChatWithReceipt: (
        member: ChatMemberProfile,
        methodKey: String,
        receiptUri: String,
        txRef: String
    ) -> Unit,
    onSendChatMessage: (memberId: String, text: String) -> Unit,
    onOpenWalletTab: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var showMyChatPricingDialog by remember { mutableStateOf(false) }
    var memberPendingUnlock by remember { mutableStateOf<ChatMemberProfile?>(null) }

    val activeMember = remember(activeConversationMemberId, chatMembers) {
        chatMembers.find { it.id == activeConversationMemberId }
    }

    if (activeMember != null) {
        ChatConversationRoom(
            member = activeMember,
            userProfile = userProfile,
            messages = messagesByMember[activeMember.id].orEmpty(),
            onBack = { onSelectConversationMember(null) },
            onSendMessage = { text -> onSendChatMessage(activeMember.id, text) }
        )
        return
    }

    val filteredMembers = remember(chatMembers, unlockedMemberIds, searchQuery, selectedFilter) {
        val q = searchQuery.trim()
        chatMembers
            .map { m -> m.copy(isUnlocked = m.isUnlocked || unlockedMemberIds.contains(m.id)) }
            .filter { m ->
                val matchesSearch = q.isEmpty() ||
                    m.displayName.contains(q, ignoreCase = true) ||
                    m.username.contains(q, ignoreCase = true) ||
                    m.cityName.contains(q, ignoreCase = true) ||
                    m.category.contains(q, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    "متصل الآن" -> m.isOnline
                    "أعلى لفل VIP" -> m.level >= 20
                    "دردشاتي المفتوحة" -> m.isUnlocked
                    else -> true
                }
                matchesSearch && matchesFilter
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("chat_screen_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. My Chat Pricing Banner & User Handle Header ("اجعل بإمكان اي شخص تسعير الدردشه معه")
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("my_chat_pricing_banner"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CharcoalNight
                ),
                border = BorderStroke(1.2.dp, SovereignGold.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showMyChatPricingDialog = true },
                            color = SovereignGold,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("open_my_chat_price_dialog_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = CharcoalNight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تسعير دردشتي ($${userProfile.chatPriceUsd})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CharcoalNight,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = userProfile.displayName.ifBlank { "عضو ليالي VIP" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${userProfile.username.ifBlank { "@layali_vip" }} • Lv.${userProfile.level}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            FramedUserAvatar(
                                avatarUri = userProfile.avatarUri,
                                fallbackInitial = userProfile.displayName.ifBlank { "V" },
                                frameId = userProfile.selectedFrameId,
                                level = userProfile.level,
                                avatarSize = 52.dp,
                                isOnline = userProfile.isAvailableNow
                            )
                        }
                    }

                    Surface(
                        color = DarkCardSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, VioletBlueVip.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "رصيد محفظتك: $$walletBalanceUsd",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldVerified,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onOpenWalletTab() }
                            )

                            Text(
                                text = if (userProfile.isPaidChatEnabled) {
                                    "سعر فتح الدردشة معك: $${userProfile.chatPriceUsd} للجلسة"
                                } else {
                                    "الدردشة معك مجانية حالياً"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 2. Search & Filter Strip
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث بالاسم أو @اسم_المستخدم أو المدينة...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = SovereignGold
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_search_input")
            )
        }

        item {
            val filters = listOf("الكل", "متصل الآن", "أعلى لفل VIP", "دردشاتي المفتوحة")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SovereignGold,
                            selectedLabelColor = CharcoalNight
                        )
                    )
                }
            }
        }

        // 3. Members & Creators Chat Cards
        items(filteredMembers, key = { it.id }) { member ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (member.isUnlocked || member.chatPriceUsd <= 0) {
                            onSelectConversationMember(member.id)
                        } else {
                            memberPendingUnlock = member
                        }
                    }
                    .testTag("chat_member_card_${member.id}"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (member.isUnlocked) EmeraldVerified.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Chat Price / Unlocked Status Pill
                        Surface(
                            color = if (member.isUnlocked) {
                                EmeraldVerified.copy(alpha = 0.16f)
                            } else {
                                SovereignGold.copy(alpha = 0.16f)
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (member.isUnlocked) EmeraldVerified else SovereignGold
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (member.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (member.isUnlocked) EmeraldVerified else SovereignGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (member.isUnlocked) {
                                        "مفتوحة ✓"
                                    } else {
                                        "$${member.chatPriceUsd} / دردشة"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (member.isUnlocked) EmeraldVerified else SovereignGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Right: Framed Avatar + Name + @username + City
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f, fill = false)
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
                                        text = member.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${member.username} • ${member.cityName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = member.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SubtitleGrayText
                                )
                            }

                            FramedUserAvatar(
                                avatarUri = member.avatarPreset,
                                fallbackInitial = member.displayName,
                                frameId = member.frameId,
                                level = member.level,
                                avatarSize = 62.dp,
                                isOnline = member.isOnline
                            )
                        }
                    }

                    Text(
                        text = member.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    LuxuryGradientButton(
                        text = if (member.isUnlocked) {
                            "متابعة الدردشة المباشرة مع ${member.displayName}"
                        } else {
                            "فتح الدردشة الخاصة • $${member.chatPriceUsd}"
                        },
                        icon = Icons.Default.Chat,
                        isEmeraldStyle = member.isUnlocked,
                        onClick = {
                            if (member.isUnlocked || member.chatPriceUsd <= 0) {
                                onSelectConversationMember(member.id)
                            } else {
                                memberPendingUnlock = member
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "open_chat_button_${member.id}"
                    )
                }
            }
        }
    }

    // Dialog 1: Set My Own Chat Price ("اجعل بإمكان اي شخص تسعير الدردشه معه")
    if (showMyChatPricingDialog) {
        var priceInput by remember(userProfile.chatPriceUsd) {
            mutableStateOf(userProfile.chatPriceUsd.toString())
        }
        var paidEnabled by remember(userProfile.isPaidChatEnabled) {
            mutableStateOf(userProfile.isPaidChatEnabled)
        }
        val presetPrices = listOf(5, 10, 15, 25, 50, 100)

        AlertDialog(
            onDismissRequest = { showMyChatPricingDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "تسعير الدردشة الخاصة معك ($)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "حدد السعر الذي يدفعه أي مستخدم لفتح محادثة خاصة مباشرة معك (${userProfile.username}):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = paidEnabled,
                            onCheckedChange = { paidEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldVerified
                            )
                        )
                        Text(
                            text = "تفعيل الدردشة المدفوعة لحسابي",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetPrices.forEach { preset ->
                            val selected = priceInput == preset.toString()
                            FilterChip(
                                selected = selected,
                                onClick = { priceInput = preset.toString() },
                                label = { Text("$$preset", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold,
                                    selectedLabelColor = CharcoalNight
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { input -> priceInput = input.filter { it.isDigit() } },
                        label = { Text("سعر الدردشة المخصص بالدولار ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("my_chat_price_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = (priceInput.toIntOrNull() ?: 15).coerceIn(1, 5000)
                        onUpdateMyChatPrice(parsed, paidEnabled)
                        showMyChatPricingDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignGold,
                        contentColor = CharcoalNight
                    )
                ) {
                    Text("حفظ تسعيرة الدردشة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMyChatPricingDialog = false }) {
                    Text("إلغاء", color = SubtitleGrayText)
                }
            }
        )
    }

    // Dialog 2: Unlock Paid Chat with Sham Cash / USDT Receipt OR Wallet Balance
    memberPendingUnlock?.let { targetMember ->
        var selectedMethodKey by remember { mutableStateOf("SHAM_CASH") } // "SHAM_CASH", "USDT_BEP20", "WALLET"
        var receiptUri by remember { mutableStateOf("") }
        var txRef by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { memberPendingUnlock = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SovereignGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "$${targetMember.chatPriceUsd}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SovereignGold,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "فتح دردشة VIP خاصة",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = targetMember.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${targetMember.username} • Lv.${targetMember.level}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SovereignGold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            FramedUserAvatar(
                                avatarUri = targetMember.avatarPreset,
                                fallbackInitial = targetMember.displayName,
                                frameId = targetMember.frameId,
                                level = targetMember.level,
                                avatarSize = 54.dp,
                                isOnline = targetMember.isOnline
                            )
                        }
                    }

                    item {
                        Text(
                            text = "اختر وسيلة الدفع لفتح المحادثة ($$${targetMember.chatPriceUsd}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val methods = listOf(
                                "SHAM_CASH" to "شام كاش",
                                "USDT_BEP20" to "USDT BEP-20",
                                "WALLET" to "المحفظة ($$walletBalanceUsd)"
                            )
                            methods.forEach { (key, label) ->
                                val selected = selectedMethodKey == key
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedMethodKey = key },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SovereignGold,
                                        selectedLabelColor = CharcoalNight
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (selectedMethodKey == "SHAM_CASH") {
                        item {
                            ShamCashOfficialCard(
                                amountUsd = targetMember.chatPriceUsd,
                                onCopyMessage = onShowMessage
                            )
                        }
                        item {
                            MandatoryReceiptAttachmentCard(
                                receiptImageUri = receiptUri,
                                transactionRef = txRef,
                                onReceiptSelected = { receiptUri = it },
                                onTransactionRefChange = { txRef = it }
                            )
                        }
                    } else if (selectedMethodKey == "USDT_BEP20") {
                        item {
                            UsdtBep20OfficialCard(
                                amountUsd = targetMember.chatPriceUsd,
                                onCopyMessage = onShowMessage
                            )
                        }
                        item {
                            MandatoryReceiptAttachmentCard(
                                receiptImageUri = receiptUri,
                                transactionRef = txRef,
                                onReceiptSelected = { receiptUri = it },
                                onTransactionRefChange = { txRef = it }
                            )
                        }
                    } else {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "رصيد محفظتك الموثق: $$walletBalanceUsd",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (walletBalanceUsd >= targetMember.chatPriceUsd) EmeraldVerified else ErrorCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (walletBalanceUsd >= targetMember.chatPriceUsd) {
                                            "سيتم خصم $${targetMember.chatPriceUsd} من رصيدك الموثق وفتح الدردشة فوراً."
                                        } else {
                                            "رصيد المحفظة غير كافٍ. يمكنك الدفع مباشرة أعلاه عبر شام كاش أو USDT مع إرفاق الإيصال."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedMethodKey == "WALLET") {
                            onUnlockChatWithWallet(targetMember)
                            if (walletBalanceUsd >= targetMember.chatPriceUsd) {
                                memberPendingUnlock = null
                            }
                        } else {
                            if (receiptUri.isBlank()) {
                                onShowMessage("يرجى إرفاق صورة إيصال الدفع (شام كاش أو USDT) أولاً لفتح الدردشة!")
                            } else {
                                onUnlockChatWithReceipt(
                                    targetMember,
                                    selectedMethodKey,
                                    receiptUri,
                                    txRef
                                )
                                memberPendingUnlock = null
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignGold,
                        contentColor = CharcoalNight
                    )
                ) {
                    Text(
                        text = if (selectedMethodKey == "WALLET") {
                            "خصم $${targetMember.chatPriceUsd} وفتح الدردشة"
                        } else {
                            "إرسال الإيصال وفتح الدردشة"
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { memberPendingUnlock = null }) {
                    Text("إلغاء", color = SubtitleGrayText)
                }
            }
        )
    }
}

@Composable
private fun ChatConversationRoom(
    member: ChatMemberProfile,
    userProfile: UserProfile,
    messages: List<ChatMessageItem>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    var inputMessage by remember { mutableStateOf("") }

    val quickReplies = listOf(
        "مرحباً، أود تنسيق حجز VIP الليلة ✨",
        "هل الجناح الملكي متاح حالياً في ${member.cityName}؟",
        "تم إرفاق إيصال الدفع وتأكيد الموعد"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("chat_conversation_room")
    ) {
        // Conversation Header
        Surface(
            color = CharcoalNight,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCardSurface)
                        .testTag("chat_room_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
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
                                contentDescription = null,
                                tint = EmeraldVerified,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${member.username} • ${member.cityName} • Lv.${member.level}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SovereignGold
                        )
                    }

                    FramedUserAvatar(
                        avatarUri = member.avatarPreset,
                        fallbackInitial = member.displayName,
                        frameId = member.frameId,
                        level = member.level,
                        avatarSize = 48.dp,
                        isOnline = member.isOnline
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = SovereignGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "دردشة VIP مشفرة وموثقة مع ${member.displayName} (${member.username}) • تم تفعيل الجلسة بنجاح",
                        style = MaterialTheme.typography.labelMedium,
                        color = SovereignGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(messages, key = { it.id }) { msg ->
                val alignment = if (msg.isFromCurrentUser) Alignment.Start else Alignment.End
                val bubbleColor = if (msg.isFromCurrentUser) {
                    VioletBlueVip
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = if (msg.isFromCurrentUser) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = alignment
                ) {
                    Surface(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (msg.isFromCurrentUser) 4.dp else 18.dp,
                            bottomEnd = if (msg.isFromCurrentUser) 18.dp else 4.dp
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (msg.isFromCurrentUser) SovereignGold.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = "${msg.senderName} (${msg.senderUsername})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isFromCurrentUser) SovereignGold else VioletBlueVip,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.timestampLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Replies Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickReplies) { reply ->
                Surface(
                    onClick = { onSendMessage(reply) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = reply,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Message Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            onSendMessage(inputMessage.trim())
                            inputMessage = ""
                        }
                    },
                    enabled = inputMessage.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignGold,
                        contentColor = CharcoalNight
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("send_chat_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال", fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("اكتب رسالتك إلى ${member.displayName}...") },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
            }
        }
    }
}
