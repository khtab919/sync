package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ar-rSA-w360dp-h760dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RelaxiaHomeDashboardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun screenshot(name: String) {
        val output = File("build/reference-previews/$name.png")
        output.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
    }

    @Test fun compactHomeKeepsEveryRealNavigationActionReachable() {
        var profile = 0
        var wallet = 0
        var live = 0
        var audio = 0
        var reels = 0
        var store = 0
        compose.setContent {
            val search = remember { mutableStateOf("") }
            MyApplicationTheme {
                Surface(Modifier.fillMaxSize()) {
                    RelaxiaHomeDashboard(
                        profile = JSONObject().put("display_name", "سارة"),
                        balanceCents = 0,
                        query = search.value,
                        onSearch = { search.value = it },
                        onProfile = { profile++ },
                        onWallet = { wallet++ },
                        onLive = { live++ },
                        onAudio = { audio++ },
                        onReels = { reels++ },
                        onStore = { store++ }
                    )
                }
            }
        }
        screenshot("18-home-rebuild-light")
        compose.onNodeWithText("سارة").performClick()
        compose.onNodeWithText("الرصيد").performClick()
        compose.onNodeWithText("البث المباشر").performClick()
        compose.onNodeWithText("الغرف الصوتية").performClick()
        compose.onNodeWithText("الريلز").performClick()
        compose.onNodeWithText("المتجر والحجز").performClick()
        assertEquals(listOf(1, 1, 1, 1, 1, 1), listOf(profile, wallet, live, audio, reels, store))
        compose.onNodeWithText("$0.00").assertExists()
    }

    @Test fun homeDoesNotShowInventedBalanceWhenWalletHasNotLoaded() {
        compose.setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize()) {
                    RelaxiaHomeDashboard(
                        profile = JSONObject().put("display_name", "حساب جديد"),
                        balanceCents = null,
                        query = "",
                        onSearch = {},
                        onProfile = {},
                        onWallet = {},
                        onLive = {},
                        onAudio = {},
                        onReels = {},
                        onStore = {}
                    )
                }
            }
        }
        screenshot("19-home-rebuild-dark")
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("$0.00").assertDoesNotExist()
        compose.onNodeWithText("المتجر والحجز").assertExists()
    }
}
