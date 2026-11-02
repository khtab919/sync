package com.example.platform

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.UUID

@Composable
internal fun LiveReels(vm: PlatformViewModel, state: PlatformState) {
    if (state.reels.isEmpty()) {
        EmptyPanel(
            "لا توجد ريلز منشورة بعد",
            "المشاهدة متاحة دون إثبات هوية. حدّث الصفحة للاطلاع على المنشورات الجديدة.",
        )
        return
    }
    val pager = rememberPagerState(pageCount = { state.reels.size })
    VerticalPager(
        state = pager,
        modifier = Modifier.fillMaxSize(),
        key = { state.reels[it].str("id") },
    ) { index ->
        val reel = state.reels[index]
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row {
                RoyalAvatar(reel.str("creatorName"), reel.str("frameId"), true)
                Column {
                    Text(reel.str("creatorName"), style = MaterialTheme.typography.titleLarge)
                    Text(reel.str("title"), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (index == pager.currentPage) {
                var url by remember(reel.str("id")) { mutableStateOf("") }
                var error by remember(reel.str("id")) { mutableStateOf<String?>(null) }
                LaunchedEffect(reel.str("id")) {
                    try {
                        url = vm.call("reelPlayback", mapOf("id" to reel.str("id"))).str("url")
                    } catch (e: Exception) {
                        error = "تعذر تشغيل الفيديو؛ حدّث الصفحة"
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (error != null) Text(error!!)
                    else if (url.isBlank()) CircularProgressIndicator() else VideoPlayer(url)
                }
            } else Spacer(Modifier.weight(1f))
            var gift by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { gift = true },
                enabled =
                    state.signedIn &&
                        !state.busy &&
                        reel.str("ownerId") != state.profile.str("uid"),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text("إرسال هدية")
            }
            if (!state.signedIn)
                Text(
                    "سجل الدخول من حسابي لإرسال الهدايا",
                    style = MaterialTheme.typography.bodyMedium,
                )
            if (gift) {
                var amount by remember { mutableStateOf("5.00") }
                val requestId = remember { UUID.randomUUID().toString() }
                AlertDialog(
                    onDismissRequest = { if (!state.busy) gift = false },
                    title = { Text("هدية للمذيعة") },
                    text = {
                        Column {
                            MoneyField(amount, { amount = it })
                            Text("يصل للمذيعة 80%، وحصة المنصة 20%. يُخصم المبلغ من رصيدك.")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !state.busy,
                            onClick = {
                                val cents = parseCents(amount)
                                if (cents == null) vm.fail("أدخل مبلغًا صحيحًا")
                                else
                                    vm.perform(
                                        "sendGift",
                                        mapOf(
                                            "creatorId" to reel.str("ownerId"),
                                            "amountCents" to cents,
                                            "requestId" to requestId,
                                        ),
                                        "تم إرسال الهدية",
                                        after = { gift = false },
                                    )
                            },
                        ) {
                            Text("تأكيد الإرسال")
                        }
                    },
                    dismissButton = {
                        TextButton(enabled = !state.busy, onClick = { gift = false }) {
                            Text("إلغاء")
                        }
                    },
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player =
        remember(url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }
        }
    DisposableEffect(player, lifecycle) {
        player.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                layoutParams = ViewGroup.LayoutParams(-1, -1)
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize(),
    )
}
