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
            var gender by remember { mutableStateOf("") }
            var adultConfirmed by remember { mutableStateOf(false) }
            var name by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            MyApplicationTheme {
                ReferenceLogin(register, false, "", name, password, email, adultConfirmed, { name = it }, { password = it }, { email = it }, { adultConfirmed = it }, { register = !register }, {}, gender=gender,onGender={gender=it})
            }
        }
        compose.onNodeWithText("أهلًا بعودتك").assertExists()
        compose.onNodeWithText("الجنس").assertDoesNotExist()
        screenshot("01-login")
        compose.onNodeWithText("جديد هنا؟ أنشئ حسابًا").performScrollTo().performClick()
        compose.onNodeWithText("Relaxia").assertExists()
        compose.onNodeWithText("البريد الإلكتروني").assertExists()
        compose.onNodeWithText("البريد الإلكتروني (اختياري)").assertDoesNotExist()
        compose.onNodeWithText("تاريخ الميلاد YYYY-MM-DD").assertDoesNotExist()
        compose.onNodeWithText("إنشاء حساب").performScrollTo().assertIsNotEnabled()
        compose.onNode(isToggleable() and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Checkbox)).performScrollTo().performClick()
        compose.onNodeWithText("إنشاء حساب").assertIsNotEnabled()
        compose.onNodeWithText("الاسم الظاهر").performScrollTo().performTextInput("نور")
        compose.onNodeWithText("كلمة المرور").performScrollTo().performTextInput("Sunrise8")
        compose.onNodeWithContentDescription("إظهار كلمة المرور").performClick()
        compose.onNodeWithContentDescription("إخفاء كلمة المرور").assertExists()
        compose.onNodeWithText("إنشاء حساب").assertIsNotEnabled()
        compose.onNodeWithText("البريد الإلكتروني").performScrollTo().performTextInput("member@example.com")
        compose.onNodeWithText("إنشاء حساب").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("أنثى").performScrollTo().performClick()
        compose.onNodeWithText("إنشاء حساب").performScrollTo().assertIsEnabled()
        screenshot("02-register")
    }

    @Test fun homeOverflowKeepsSecondaryActionsReachable() {
        var support=0;var wallet=0;var saved=0;var refresh=0
        compose.setContent { MyApplicationTheme { Surface { Row { HomeToolbarActions({},{wallet++},{saved++},{refresh++},{support++}) } } } }
        compose.onNodeWithText("المحفظة والرصيد").assertDoesNotExist()
        for(title in listOf("المحفظة والرصيد","المحفوظات","تحديث المحتوى","الدعم والمساعدة")) {
            compose.onNodeWithContentDescription("خيارات التصفح").performClick()
            compose.onNodeWithText(title).performClick()
        }
        org.junit.Assert.assertEquals(listOf(1,1,1,1),listOf(support,wallet,saved,refresh))
        compose.onNodeWithText("المحفظة والرصيد").assertDoesNotExist()
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
                    LazyColumn { item { RelaxiaFramePicker(0, "R", "", 10, setOf(0,110)) { chosen = it } } }
                }
            }
        }
        // The new 100-level catalog displays owned frames per tier. Older
        // tests used the deprecated list-only names, not the new compact cards.
        compose.onNodeWithText("Lv.10").assertExists()
        screenshot("04-frames")
        compose.onNodeWithText("Lv.10").performClick()
        org.junit.Assert.assertEquals(110, chosen)
        screenshot("05-frames-selected")
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
        compose.onNodeWithText("نشر الآن").performScrollTo().assertIsNotEnabled()
        screenshot("06-seller-upload")
        compose.onNodeWithText("ريلز").performScrollTo().performClick()
        compose.onNodeWithText("اختيار فيديو MP4 من الهاتف · حتى 20MB").performScrollTo().assertExists()
        compose.onNodeWithText("نشر الآن").assertIsNotEnabled()
        screenshot("07-reel-upload")
    }

    @Test fun walletCardMakesCreditPurchaseVisibleAndDoesNotInventBalance() {
        var bought = 0
        var opened = 0
        compose.setContent { MyApplicationTheme { Column(Modifier.fillMaxSize().padding(20.dp)) { WalletSummary(0L, false, "", { bought++ }, { opened++ }, {}) } } }
        compose.onNodeWithText("رصيدك").assertExists()
        compose.onNodeWithText("0 رصيد").assertExists()
        compose.onNodeWithText("متجر الرصيد").performClick()
        compose.onNodeWithContentDescription("المحفظة").performClick()
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
        compose.onNodeWithContentDescription("إرسال الرسالة").assertDoesNotExist()
        compose.onNodeWithContentDescription("تسجيل رسالة صوتية").assertExists()
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

    @Test fun chatStatusIconsReflectPendingServerAndRecipientStates() {
        compose.setContent { MyApplicationTheme { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            ChatBubble(JSONObject().put("id",1).put("body","بانتظار الشبكة").put("pending",true),true,{}, {})
            ChatBubble(JSONObject().put("id",2).put("body","أُرسلت إلى الخادم"),true,{}, {})
            ChatBubble(JSONObject().put("id",3).put("body","وصلت إلى جهاز المستلم").put("delivered_at","2026-11-06T12:00:00Z"),true,{}, {})
        } } } }
        compose.onNodeWithContentDescription("بانتظار الإرسال").assertExists()
        compose.onNodeWithContentDescription("تم إرسال الرسالة").assertExists()
        compose.onNodeWithContentDescription("وصلت إلى جهاز المستلم").assertExists()
        screenshot("11-message-delivery-states")
    }

    @Test fun coinPackageShowsPriceAndActualBonus() {
        var chosen=false
        compose.setContent { MyApplicationTheme { Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) { Column(Modifier.padding(22.dp)) { CoinPackage(2500,100,"25.00",true,null){chosen=true} } } } }
        screenshot("12-coin-package")
        compose.onNodeWithText(java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(2500)).assertExists()
        compose.onNodeWithText("+100 نقطة ترويجية").performClick()
        org.junit.Assert.assertTrue(chosen)
        screenshot("12-coin-package")
    }
    @Test fun chatReferenceLight() { referenceChat(false, "13-chat-reference-light") }
    @Test fun chatReferenceDark() { referenceChat(true, "14-chat-reference-dark") }
    private fun referenceChat(dark: Boolean, file: String) {
        var audio=0; var video=0
        compose.setContent { MyApplicationTheme(darkTheme=dark) {
            Surface(Modifier.fillMaxSize(),color=conversationColors().background) {
                Column {
                    ChatHeader("نور", {}, {audio++}, {}, true, handle="nour", video={video++}, verified=true)
                    Column(Modifier.weight(1f).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        ChatDayLabel("اليوم")
                        ChatBubble(JSONObject().put("body","مرحبًا 👋").put("created_at","2026-11-06T11:20:00Z"),false,{}, {}, peerName="نور")
                        ChatBubble(JSONObject().put("body","أهلًا نور 🌹").put("created_at","2026-11-06T11:22:00Z").put("delivered_at","2026-11-06T11:22:01Z"),true,{}, {})
                        ChatBubble(JSONObject().put("body","كيف حالك اليوم؟").put("created_at","2026-11-06T11:24:00Z"),false,{}, {}, peerName="نور")
                        ChatBubble(JSONObject().put("body","بخير، هل أنت متاحة الآن؟").put("created_at","2026-11-06T11:25:00Z"),true,{}, {})
                        ChatBubble(JSONObject().put("call_id","test-fixture").put("call_status","missed").put("call_mode","audio").put("created_at","2026-11-06T11:29:00Z"),true,{}, {})
                    }
                    ChatComposer("", {}, false, {}, {}, {})
                }
            }
        } }
        compose.onNodeWithContentDescription("بدء اتصال").performClick()
        compose.onNodeWithContentDescription("اتصال فيديو").performClick()
        org.junit.Assert.assertEquals(1,audio);org.junit.Assert.assertEquals(1,video)
        compose.onNodeWithContentDescription("حساب معتمد").assertExists()
        compose.onNodeWithText("لم يتم الرد").assertExists()
        compose.onNodeWithText("متصلة الآن").assertDoesNotExist()
        screenshot(file)
    }

    @Test fun readReceiptUsesDistinctServerReadState() {
        compose.setContent { MyApplicationTheme { Surface { ChatBubble(JSONObject().put("id",1).put("body","قُرئت").put("read_at","2026-11-06T12:00:00Z").put("delivered_at","2026-11-06T12:00:00Z"),true,{}, {}) } } }
        compose.onNodeWithContentDescription("قرأها المستلم").assertExists()
    }
    @Test fun recordingPauseHasRealStateLabel() {
        compose.setContent { MyApplicationTheme { Surface { RecordingIndicator(12,.4f,listOf(.1f,.3f,.6f,.2f),true) } } }
        compose.onNodeWithContentDescription("التسجيل متوقف مؤقتًا").assertExists()
        compose.onNodeWithText("0:12").assertExists()
    }

    @Test fun lockedMediaOffersExplicitPurchaseWithoutExposingAttachment() {
        var requested=false
        compose.setContent { MyApplicationTheme { Surface { ChatBubble(JSONObject().put("id",1).put("body","").put("media_locked",true).put("media_price_cents",100).put("attachment_mime","image/jpeg"),false,{}, {},unlock={requested=true}) } } }
        compose.onNodeWithText("فتح مقابل 100 رصيد").performClick()
        org.junit.Assert.assertTrue(requested)
        compose.onNodeWithContentDescription("صورة في المحادثة").assertDoesNotExist()
    }

    @Test fun liveCardShowsRealMetadataAndEntryAction(){
        var opened=false
        compose.setContent { MyApplicationTheme { Surface(Modifier.width(320.dp)) { Column(Modifier.padding(16.dp)) { LiveBroadcastCard(JSONObject().put("owner_name","حساب الاختبار").put("title","جلسة مباشرة").put("viewers",12).put("private",true).put("price_cents",100)){opened=true} } } } }
        compose.onNodeWithText("جلسة مباشرة").performClick();org.junit.Assert.assertTrue(opened)
        compose.onNodeWithText("12 مشاهد · مدفوع: 100").assertExists()
        screenshot("15-live-card-small-screen")
    }


    @Test @Config(qualifiers="ar-rSA-w320dp-h760dp-port-mdpi")
    fun emailLoginHasNoDisplayNameOrHandleField() {
        var submitted=false
        compose.setContent {
            var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
            MyApplicationTheme {ReferenceLogin(false,false,"","ignored-name",password,email,true,{}, {password=it},{email=it},{},{},{submitted=true})}
        }
        compose.onNodeWithText("الاسم الظاهر").assertDoesNotExist()
        compose.onNodeWithText("دخول").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("البريد الإلكتروني").performScrollTo().performTextInput("member@example.com")
        compose.onNodeWithText("كلمة المرور").performScrollTo().performTextInput("AccountPass!875")
        compose.onNodeWithText("دخول").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle{org.junit.Assert.assertTrue(submitted)}
        screenshot("16-petroleum-email-login-320")
    }

    @Test fun bothThemesUseTheRequestedBurgundyPalette() {
        var dark by mutableStateOf(false)
        var background=androidx.compose.ui.graphics.Color.Unspecified
        var primary=androidx.compose.ui.graphics.Color.Unspecified
        compose.setContent {MyApplicationTheme(darkTheme=dark){val colors=MaterialTheme.colorScheme;SideEffect{background=colors.background;primary=colors.primary};Surface{androidx.compose.material3.Text("Relaxia")}}}
        compose.runOnIdle{org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFFFAF6F2),background);org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFF792D45),primary);dark=true}
        compose.runOnIdle{org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFF1B1418),background);org.junit.Assert.assertEquals(androidx.compose.ui.graphics.Color(0xFFE9A8BA),primary)}
    }

    @Test fun profileDisplaysPublicNumberAndIndependentHandle() {
        val user=JSONObject().put("id",2).put("public_id","58321").put("display_name","اسم ظاهر").put("username","independent_handle")
        var dark by mutableStateOf(false)
        compose.setContent{MyApplicationTheme(darkTheme=dark) {Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {Column {ProfileHero(user,true,{},{},{})}}}}
        compose.onNodeWithText("@independent_handle").assertDoesNotExist()
        compose.onNodeWithText("ID: 58321").assertExists()
        compose.onNodeWithText("ID: 2").assertDoesNotExist()
        screenshot("17-petroleum-profile-identity")
        compose.runOnIdle { dark = true }
        compose.waitForIdle()
        screenshot("18-profile-dark")
    }
    @Test fun purchaseSubmissionShowsPendingStateWithoutCountdown() {
        compose.setContent { MyApplicationTheme { PurchaseStatusCard("test-order",100,"pending") } }
        compose.onNodeWithText("تم تقديم الطلب، انتظر قليلًا").assertExists()
        compose.onNodeWithText("مدة صلاحية طلب الشراء").assertDoesNotExist()
        screenshot("19-purchase-submitted")
    }
    @Test fun newFramePickerDoesNotOfferUnearnedFrames() {
        var chosen=-1
        compose.setContent { MyApplicationTheme { LazyColumn { item { RelaxiaFramePicker(0,"نور",""){chosen=it} } } } }
        compose.onNodeWithText("مقفل · مستوى 10").assertExists().performClick()
        org.junit.Assert.assertEquals(-1,chosen)
    }

    @Test fun retiredFramesAreNotOfferedEvenWhenServerReturnsOwnership() {
        compose.setContent { MyApplicationTheme { LazyColumn { item {
            RelaxiaFramePicker(5,"نور","",1,(0..100).toSet()) {}
        } } } }
        compose.onNodeWithText("Lv.100").performScrollTo().assertExists()
        compose.onNodeWithText("إطاراتك السابقة").assertDoesNotExist()
        for (id in 1..100) org.junit.Assert.assertFalse(isMilestoneFrame(id))
    }

    @Test fun allMilestoneOpeningsHaveConsistentPortraitAlignment() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MyApplicationTheme { Surface(color=MaterialTheme.colorScheme.background) {
            Column {
                (0..9).chunked(2).forEach { pair -> Row {
                    pair.forEach { index -> Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                        MilestoneAvatar("ن",110+index*10,128,"")
                        androidx.compose.material3.Text("Lv.${(index+1)*10}")
                    } }
                } }
            }
        } } }
        compose.mainClock.advanceTimeBy(100)
        screenshot("20-all-frame-alignment")
    }

}
