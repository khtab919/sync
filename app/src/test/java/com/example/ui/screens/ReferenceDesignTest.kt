package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
    @get:Rule val compose = createComposeRule()

    private fun screenshot(name: String) {
        val output = File("build/reference-previews/$name.png")
        output.parentFile?.mkdirs()
        output.outputStream().use { stream ->
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    @Test fun loginSwitchesToRealRegistrationFields() {
        compose.setContent {
            var register by remember { mutableStateOf(false) }
            MyApplicationTheme {
                ReferenceLogin(register, false, "", "", "", "", "", {}, {}, {}, {}, { register = !register }, {})
            }
        }
        compose.onNodeWithText("أهلًا بعودتك").assertExists()
        screenshot("01-login")
        compose.onNodeWithText("جديد هنا؟ أنشئ حسابًا").performScrollTo().performClick()
        compose.onNodeWithText("البريد الإلكتروني").assertExists()
        compose.onNodeWithText("تاريخ الميلاد YYYY-MM-DD").assertExists()
        screenshot("02-register")
    }

    @Test fun profileActionsRemainAvailableWithServerCounters() {
        var edits = 0
        var frames = 0
        val person = JSONObject().put("id", 42).put("display_name", "حساب اختبار").put("selected_frame", 2).put("identity_verified", true).put("followers_count", 0).put("following_count", 0).put("service_count", 0).put("reel_count", 0)
        compose.setContent { MyApplicationTheme { ProfileHero(person, true, {}, { edits++ }, { frames++ }) } }
        compose.onNodeWithText("تعديل الملف").performClick()
        compose.onNodeWithText("إطاراتي").performClick()
        org.junit.Assert.assertEquals(1, edits)
        org.junit.Assert.assertEquals(1, frames)
        screenshot("03-profile")
    }
}
