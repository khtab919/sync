package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.AuthResult
import com.example.security.MiddlewareResult
import com.example.security.SuperAdminSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Layali Lounge", appName)
    }

    @Test
    fun `super admin initial credentials and 403 middleware verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Unauthorized access without session token returns 403 Forbidden
        val unauthMiddleware = SuperAdminSecurityManager.requireSuperAdminMiddleware(null)
        assertTrue(unauthMiddleware is MiddlewareResult.Forbidden403)
        val forbidden = unauthMiddleware as MiddlewareResult.Forbidden403
        assertEquals(403, forbidden.statusCode)

        // 2. Wrong password for Super Admin email fails
        val wrongPassResult = SuperAdminSecurityManager.authenticate(
            context = context,
            emailOrUsername = "RamaNUR@gmail.com",
            passwordInput = "wrong_password"
        )
        assertTrue(wrongPassResult is AuthResult.InvalidSuperAdminPassword)

        // 3. Correct initial Super Admin credentials authenticate and pass middleware
        val successResult = SuperAdminSecurityManager.authenticate(
            context = context,
            emailOrUsername = "RamaNUR@gmail.com",
            passwordInput = "2Macit.salma"
        )
        assertTrue(successResult is AuthResult.SuperAdminSuccess)
        val adminSuccess = successResult as AuthResult.SuperAdminSuccess
        val authMiddleware = SuperAdminSecurityManager.requireSuperAdminMiddleware(adminSuccess.sessionToken)
        assertTrue(authMiddleware is MiddlewareResult.Authorized)

        // 4. Stored password hash uses cryptographic salted hashing ($2b$12$)
        val snapshot = SuperAdminSecurityManager.getCredentialSnapshot(context)
        assertTrue(snapshot.passwordHashPreview.startsWith("\$2b\$12\$"))
    }
}
