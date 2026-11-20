package com.example.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ar-rSA-w390dp-h844dp-port-mdpi")
class AdminAccountPreviewScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun accountPreviewLooksLikeUserAccountWithPermanentAdminReturn() {
        val calls = AtomicInteger(0)
        val snapshot = JSONObject(
            """{"read_only":true,"profile":{"public_id":"58321",
                "username":"example","display_name":"مستخدم تجريبي",
                "level":31,"vip_tier":2},"services":[],"benefits":[],
                "bookings":[],"communication":{"sent":0,"received":0}}"""
        )
        compose.setContent {
            MaterialTheme {
                AdminAccountPreviewScreen(snapshot) { calls.incrementAndGet() }
            }
        }
        compose.onNodeWithText("معاينة للقراءة فقط").assertIsDisplayed()
        compose.onNodeWithText("مستخدم تجريبي").assertIsDisplayed()
        compose.onNodeWithText("العودة للإدارة").assertIsDisplayed()
        compose.onNodeWithText("الريلز").performClick()
        compose.onNodeWithText("العودة للإدارة").assertIsDisplayed()
        compose.onNodeWithText("المحفظة · عرض فقط").assertDoesNotExist()
        compose.onNodeWithText("العودة للإدارة").performClick()
        compose.runOnIdle { assertEquals(1, calls.get()) }
    }
}
