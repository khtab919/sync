package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ar-rSA-w390dp-h844dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun screenshot(name: String) {
        val output = File("build/reference-previews/$name.png")
        output.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.findViewById<View>(android.R.id.content)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun loginSwitchesToRealRegistrationFields() {
        compose.setContent {
            var register by remember { mutableStateOf(false) }
            var adultConfirmed by remember { mutableStateOf(false) }
            var name by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            MyApplicationTheme {
                ReferenceLogin(register, false, "", name, password, email, adultConfirmed, { name = it }, { password = it }, { email = it }, { adultConfirmed = it }, { register = !register }, {})
            }
        }
        compose.onNodeWithText("أهلًا بعودتك").assertExists()
        screenshot("01-login")
        compose.onNodeWithText("جديد هنا؟ أنشئ حسابًا").performScrollTo().performClick()
        compose.onNodeWithText("Relaxia").assertExists()
        compose.onNodeWithText("البريد الإلكتروني").assertExists()
        compose.onNodeWithText("البريد الإلكتروني (اختياري)").assertDoesNotExist()
        compose.onNodeWithText("تاريخ الميلاد YYYY-MM-DD").assertDoesNotExist()
        compose.onNodeWithText("إنشاء حساب").performScrollTo().assertIsNotEnabled()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("إنشاء حساب").assertIsNotEnabled()
        compose.onNodeWithText("الاسم").performScrollTo().performTextInput("نور")
        compose.onNodeWithText("كلمة المرور").performScrollTo().performTextInput("Sunrise8")
        compose.onNodeWithContentDescription("إظهار كلمة المرور").performClick()
        compose.onNodeWithContentDescription("إخفاء كلمة المرور").assertExists()
        compose.onNodeWithText("إنشاء حساب").assertIsNotEnabled()
        compose.onNodeWithText("البريد الإلكتروني").performScrollTo().performTextInput("member@example.com")
        compose.onNodeWithText("إنشاء حساب").assertIsEnabled()
        screenshot("02-register")
    }

    @Test fun profileActionsRemainAvailableWithServerCounters() {
        var edits = 0
        var frames = 0
        val person = JSONObject().put("id", 42).put("display_name", "حساب اختبار").put("selected_frame", 2).put("identity_verified", false).put("followers_count", 0).put("following_count", 0).put("service_count", 0).put("reel_count", 0)
        compose.setContent { MyApplicationTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column { ProfileHero(person, true, {}, { edits++ }, { frames++ }) } } } }
        compose.onNodeWithText("تعديل الملف").performClick()
        compose.onNodeWithText("إطاراتي").performClick()
        org.junit.Assert.assertEquals(1, edits)
        org.junit.Assert.assertEquals(1, frames)
        compose.onNodeWithText("بانتظار التوثيق").assertDoesNotExist()
        screenshot("03-profile")
    }
    @Test fun tenDimensionalFramesCanBeSelected() {
        var chosen = 0
        compose.setContent {
            MyApplicationTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LazyColumn { item { RelaxiaFramePicker(0, "R", "") { chosen = it } } }
                }
            }
        }
        compose.onNodeWithText("وردة الياقوت").assertExists()
        screenshot("04-frames")
        compose.onNodeWithText("التاج الملكي").performScrollTo().performClick()
        org.junit.Assert.assertEquals(10, chosen)
        screenshot("05-frames-last")
    }


    @Test fun supportChooserOffersBothRequestedChannels() {
        compose.setContent { MyApplicationTheme { SupportFloatingButton() } }
        compose.onNodeWithText("الدعم").performClick()
        compose.onNodeWithText("واتساب").assertExists()
        compose.onNodeWithText("تلجرام").assertExists()
    }

    @Test fun sellerUploadsUsePhonePickerInsteadOfUrlField() {
        compose.setContent { MyApplicationTheme { SellerPublishScreen(com.example.data.api.LayaliApi("https://example.invalid"), {}) } }
        compose.onNodeWithText("مساحة النشر").assertExists()
        compose.onNodeWithText("رفع صورة من الهاتف · حتى 8MB").performScrollTo().assertExists()
        compose.onNodeWithText("رابط صورة HTTPS · اختياري").assertDoesNotExist()
        compose.onNodeWithText("إرسال للمراجعة").performScrollTo().assertIsNotEnabled()
        screenshot("06-seller-upload")
        compose.onNodeWithText("ريلز").performScrollTo().performClick()
        compose.onNodeWithText("اختيار فيديو MP4 من الهاتف · حتى 20MB").performScrollTo().assertExists()
        compose.onNodeWithText("إرسال للمراجعة").assertIsNotEnabled()
        screenshot("07-reel-upload")
    }

    @Test fun walletCardMakesCreditPurchaseVisibleAndDoesNotInventBalance() {
        var bought = 0
        var opened = 0
        compose.setContent { MyApplicationTheme { Column(Modifier.fillMaxSize().padding(20.dp)) { WalletBalanceCard(0L, false, "", { bought++ }, { opened++ }, {}) } } }
        compose.onNodeWithText("بطاقة رصيدك").assertExists()
        compose.onNodeWithText("0.00 USD").assertExists()
        compose.onNodeWithText("شراء رصيد").performClick()
        compose.onNodeWithText("المحفظة").performClick()
        org.junit.Assert.assertEquals(1, bought)
        org.junit.Assert.assertEquals(1, opened)
        screenshot("08-wallet-card")
    }

    @Test fun chatComposerAndBothMessageDirectionsAreReadable() {
        var sent = 0
        val incoming = JSONObject().put("id",1).put("body","أهلًا، متى يناسبك موعد الجلسة؟").put("created_at","2026-11-06T12:00:00Z")
        val outgoing = JSONObject().put("id",2).put("body","أهلًا بك، يناسبني المساء. سأرسل لك التفاصيل هنا.").put("created_at","2026-11-06T12:01:00Z")
        compose.setContent { MyApplicationTheme {
            var draft by remember { mutableStateOf("") }
            Column(Modifier.fillMaxSize()) {
                ChatHeader("حساب الاختبار", {}, {}, {}, true)
                Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { ChatBubble(incoming, false, {}, {}); ChatBubble(outgoing, true, {}, {}) }
                ChatComposer(draft, { draft = it }, false, {}, {}) { sent++; draft = "" }
            }
        } }
        compose.onNodeWithContentDescription("إرسال الرسالة").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("رسالة جديدة")
        compose.onNodeWithContentDescription("إرسال الرسالة").performClick()
        org.junit.Assert.assertEquals(1, sent)
        compose.onNodeWithContentDescription("إرسال صورة").assertExists()
        compose.onNodeWithContentDescription("تسجيل رسالة صوتية").assertExists()
        compose.onNodeWithText("شراء رصيد").assertExists()
        screenshot("09-chat-layout")
    }

    @Test fun conversationRowsKeepNamePreviewAndOpenActionClear() {
        var opened = false
        val person = JSONObject().put("id", 10).put("display_name", "حساب الاختبار").put("last_message", "شكرًا، وصلني الموعد والتفاصيل.").put("created_at", "2026-11-06T12:00:00Z")
        compose.setContent { MyApplicationTheme { Column(Modifier.fillMaxSize().padding(20.dp)) { ConversationTile(person) { opened = true } } } }
        compose.onNodeWithText("حساب الاختبار").assertExists()
        compose.onNodeWithText("شكرًا، وصلني الموعد والتفاصيل.").performClick()
        org.junit.Assert.assertTrue(opened)
        screenshot("10-conversation-row")
    }

    @Test fun callCaptureIsRestrictedToTheApplicationServer() {
        org.junit.Assert.assertTrue(trustedCallOrigin("https://165-227-128-113.sslip.io", "https://165-227-128-113.sslip.io"))
        org.junit.Assert.assertFalse(trustedCallOrigin("http://165-227-128-113.sslip.io", "https://165-227-128-113.sslip.io"))
        org.junit.Assert.assertFalse(trustedCallOrigin("https://evil.example", "https://165-227-128-113.sslip.io"))
        org.junit.Assert.assertFalse(trustedCallOrigin("https://165-227-128-113.sslip.io:444", "https://165-227-128-113.sslip.io"))
    }
}
