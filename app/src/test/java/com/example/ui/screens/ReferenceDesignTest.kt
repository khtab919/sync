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

}
