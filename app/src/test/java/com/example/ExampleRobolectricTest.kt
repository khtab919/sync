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
        assertEquals("Relaxia", appName)
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

    @Test
    fun `customer account database registration and password change verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Register a customer account in Room SQLite Database
        val initialLogin = SuperAdminSecurityManager.authenticate(
            context = context,
            emailOrUsername = "@layla_royal",
            passwordInput = "SecretPass123",
            fullNameOverride = "ليلى الملكية"
        )
        assertTrue(initialLogin is AuthResult.CustomerSuccess)

        // 2. Wrong password for the saved customer account is rejected
        val wrongCustomerPass = SuperAdminSecurityManager.authenticate(
            context = context,
            emailOrUsername = "@layla_royal",
            passwordInput = "WrongPass999"
        )
        assertTrue(wrongCustomerPass is AuthResult.InvalidSuperAdminPassword)

        // 3. Change customer password with current password verification
        val changeResult = SuperAdminSecurityManager.changeAccountPassword(
            context = context,
            identifier = "@layla_royal",
            currentPassword = "SecretPass123",
            newPassword = "NewRoyalPass2026",
            requireCurrentPassword = true
        )
        assertTrue(changeResult.isSuccess)

        // 4. Old password now fails, new password succeeds
        val oldPassAttempt = SuperAdminSecurityManager.authenticate(
            context = context,
            emailOrUsername = "@layla_royal",
            passwordInput = "SecretPass123"
        )
        assertTrue(oldPassAttempt is AuthResult.InvalidSuperAdminPassword)

        val newPassAttempt = SuperAdminSecurityManager.authenticate(
            context = context,
            emailOrUsername = "@layla_royal",
            passwordInput = "NewRoyalPass2026"
        )
        assertTrue(newPassAttempt is AuthResult.CustomerSuccess)

        // 5. Saved accounts list in Room SQLite includes the registered account
        val savedAccounts = SuperAdminSecurityManager.getSavedAccounts(context)
        assertTrue(savedAccounts.any { it.username == "@layla_royal" })
    }
}
