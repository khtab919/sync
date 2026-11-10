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
    fun accountPreviewHasPermanentReturnAndReadOnlyNotice() {
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
        compose.onNodeWithText("معاينة الحساب").assertIsDisplayed()
        compose.onNodeWithText("رجوع للحساب").assertIsDisplayed()
        compose.onNodeWithText("قراءة فقط · لا يمكن التصرف باسم المستخدم أو الاطلاع على رسائله الخاصة")
            .assertIsDisplayed()
        compose.onNodeWithText("رجوع للحساب").performClick()
        compose.runOnIdle { assertEquals(1, calls.get()) }
    }
}
