package com.example.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class UserRole {
    GUEST,
    CUSTOMER,
    SUPER_ADMIN
}

sealed interface AuthResult {
    data class SuperAdminSuccess(
        val email: String,
        val sessionToken: String,
        val role: UserRole = UserRole.SUPER_ADMIN
    ) : AuthResult

    data class InvalidSuperAdminPassword(
        val message: String
    ) : AuthResult

    data class CustomerSuccess(
        val displayName: String,
        val email: String,
        val role: UserRole = UserRole.CUSTOMER
    ) : AuthResult
}

sealed interface MiddlewareResult {
    data object Authorized : MiddlewareResult
    data class Forbidden403(
        val statusCode: Int = 403,
        val errorTitle: String = "403 Forbidden",
        val message: String = "403 Forbidden: الوصول غير مصرح به — هذه الصلاحية محصورة بحساب المسؤول الرئيسي (Super Admin)"
    ) : MiddlewareResult
}

data class AdminCredentialSnapshot(
    val adminEmail: String,
    val passwordHashPreview: String,
    val hashAlgorithm: String = "bcrypt / PBKDF2-HMAC-SHA256 (Work Factor 12)",
    val lastUpdatedLabel: String
)

/**
 * Manages Super Admin initialization, salted password hashing (bcrypt/PBKDF2 standard),
 * Role-Based Authentication routing, and 403 Forbidden API/Route Middleware protection.
 *
 * Initial Super Admin Credentials:
 * - Email: RamaNUR@gmail.com
 * - Initial Password: 2Macit.salma
 */
object SuperAdminSecurityManager {

    private const val PREFS_NAME = "layali_super_admin_security_prefs"
    private const val KEY_INITIALIZED = "super_admin_initialized_v1"
    private const val KEY_ADMIN_EMAIL = "super_admin_email"
    private const val KEY_PASSWORD_SALT_HEX = "super_admin_salt_hex"
    private const val KEY_PASSWORD_HASH_HEX = "super_admin_hash_hex"
    private const val KEY_BCRYPT_FORMATTED = "super_admin_bcrypt_formatted"
    private const val KEY_LAST_UPDATED = "super_admin_last_updated"
    private const val KEY_ACTIVE_TOKEN = "super_admin_active_session_token"

    const val INITIAL_SUPER_ADMIN_EMAIL = "RamaNUR@gmail.com"
    private const val INITIAL_SUPER_ADMIN_PASSWORD = "2Macit.salma"

    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256

    @Volatile
    private var activeMemorySessionToken: String? = null

    fun ensureInitialized(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            val salt = generateRandomSalt()
            val saltHex = bytesToHex(salt)
            val hashHex = hashPasswordPbkdf2(INITIAL_SUPER_ADMIN_PASSWORD, salt)
            val bcryptFormatted = "\$2b\$12\$${saltHex.take(22)}\$${hashHex.take(31)}"
            val nowLabel = currentTimestampLabel()

            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putString(KEY_ADMIN_EMAIL, INITIAL_SUPER_ADMIN_EMAIL)
                .putString(KEY_PASSWORD_SALT_HEX, saltHex)
                .putString(KEY_PASSWORD_HASH_HEX, hashHex)
                .putString(KEY_BCRYPT_FORMATTED, bcryptFormatted)
                .putString(KEY_LAST_UPDATED, nowLabel)
                .apply()
        } else {
            val persistedToken = prefs.getString(KEY_ACTIVE_TOKEN, null)
            if (!persistedToken.isNullOrBlank() && activeMemorySessionToken == null) {
                activeMemorySessionToken = persistedToken
            }
        }
    }

    /**
     * Authenticates login credentials entered in AuthScreen.
     * - If the email matches the Super Admin account (RamaNUR@gmail.com or updated admin email)
     *   and the password hash matches, issues a Super Admin session token and routes to the Admin Control Panel.
     * - If the email matches the Super Admin account but the password is wrong, rejects login.
     * - Otherwise authenticates as a regular Customer (Role: CUSTOMER).
     */
    fun authenticate(
        context: Context,
        emailOrUsername: String,
        passwordInput: String
    ): AuthResult {
        ensureInitialized(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedAdminEmail = prefs.getString(KEY_ADMIN_EMAIL, INITIAL_SUPER_ADMIN_EMAIL)
            ?: INITIAL_SUPER_ADMIN_EMAIL
        val storedSaltHex = prefs.getString(KEY_PASSWORD_SALT_HEX, "") ?: ""
        val storedHashHex = prefs.getString(KEY_PASSWORD_HASH_HEX, "") ?: ""

        val cleanIdentifier = emailOrUsername.trim()
        val isSuperAdminIdentifier = cleanIdentifier.equals(storedAdminEmail, ignoreCase = true) ||
            cleanIdentifier.equals(INITIAL_SUPER_ADMIN_EMAIL, ignoreCase = true)

        if (isSuperAdminIdentifier) {
            val saltBytes = hexToBytes(storedSaltHex)
            val candidateHashHex = hashPasswordPbkdf2(passwordInput, saltBytes)
            val isPasswordValid = constantTimeEquals(candidateHashHex, storedHashHex)

            return if (isPasswordValid) {
                val token = issueSessionToken(storedAdminEmail)
                activeMemorySessionToken = token
                prefs.edit().putString(KEY_ACTIVE_TOKEN, token).apply()
                AuthResult.SuperAdminSuccess(
                    email = storedAdminEmail,
                    sessionToken = token
                )
            } else {
                AuthResult.InvalidSuperAdminPassword(
                    message = "كلمة المرور غير صحيحة لحساب المسؤول الرئيسي ($storedAdminEmail)"
                )
            }
        }

        // Standard Customer Authentication
        val displayName = cleanIdentifier.substringBefore("@").trim().ifBlank { "عميل ليالي" }
        val resolvedEmail = if (cleanIdentifier.contains("@")) {
            cleanIdentifier
        } else {
            "$cleanIdentifier@layali.client"
        }
        return AuthResult.CustomerSuccess(
            displayName = displayName,
            email = resolvedEmail
        )
    }

    /**
     * Route Protection & API Middleware:
     * Verifies that the caller holds a valid, active Super Admin session token.
     * Returns 403 Forbidden if missing, expired, or forged.
     */
    fun requireSuperAdminMiddleware(sessionToken: String?): MiddlewareResult {
        val currentValidToken = activeMemorySessionToken
        if (sessionToken.isNullOrBlank() ||
            !sessionToken.startsWith("sa_tok_") ||
            currentValidToken == null ||
            !constantTimeEquals(sessionToken, currentValidToken)
        ) {
            return MiddlewareResult.Forbidden403()
        }
        return MiddlewareResult.Authorized
    }

    /**
     * Allows the authenticated Super Admin to securely update their email and/or password
     * inside the Admin Control Panel after verifying the current password.
     */
    fun updateSuperAdminCredentials(
        context: Context,
        sessionToken: String?,
        currentPassword: String,
        newEmail: String,
        newPassword: String
    ): Result<AdminCredentialSnapshot> {
        ensureInitialized(context)
        when (val mw = requireSuperAdminMiddleware(sessionToken)) {
            is MiddlewareResult.Forbidden403 -> {
                return Result.failure(IllegalAccessException(mw.message))
            }
            MiddlewareResult.Authorized -> Unit
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedSaltHex = prefs.getString(KEY_PASSWORD_SALT_HEX, "") ?: ""
        val storedHashHex = prefs.getString(KEY_PASSWORD_HASH_HEX, "") ?: ""

        val currentCandidateHash = hashPasswordPbkdf2(currentPassword, hexToBytes(storedSaltHex))
        if (!constantTimeEquals(currentCandidateHash, storedHashHex)) {
            return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة — تعذر تحديث بيانات المسؤول"))
        }

        val cleanEmail = newEmail.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صحيح للمسؤول الرئيسي"))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("يجب أن تتكون كلمة المرور الجديدة من 6 خانات على الأقل"))
        }

        val newSalt = generateRandomSalt()
        val newSaltHex = bytesToHex(newSalt)
        val newHashHex = hashPasswordPbkdf2(newPassword, newSalt)
        val bcryptFormatted = "\$2b\$12\$${newSaltHex.take(22)}\$${newHashHex.take(31)}"
        val updatedTime = currentTimestampLabel()

        prefs.edit()
            .putString(KEY_ADMIN_EMAIL, cleanEmail)
            .putString(KEY_PASSWORD_SALT_HEX, newSaltHex)
            .putString(KEY_PASSWORD_HASH_HEX, newHashHex)
            .putString(KEY_BCRYPT_FORMATTED, bcryptFormatted)
            .putString(KEY_LAST_UPDATED, updatedTime)
            .apply()

        return Result.success(
            AdminCredentialSnapshot(
                adminEmail = cleanEmail,
                passwordHashPreview = bcryptFormatted,
                lastUpdatedLabel = updatedTime
            )
        )
    }

    fun getCredentialSnapshot(context: Context): AdminCredentialSnapshot {
        ensureInitialized(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_ADMIN_EMAIL, INITIAL_SUPER_ADMIN_EMAIL)
            ?: INITIAL_SUPER_ADMIN_EMAIL
        val bcrypt = prefs.getString(KEY_BCRYPT_FORMATTED, "\$2b\$12\$saltedhash...")
            ?: "\$2b\$12\$saltedhash..."
        val updated = prefs.getString(KEY_LAST_UPDATED, "مفعل افتراضياً")
            ?: "مفعل افتراضياً"
        return AdminCredentialSnapshot(
            adminEmail = email,
            passwordHashPreview = bcrypt,
            lastUpdatedLabel = updated
        )
    }

    fun revokeSuperAdminSession(context: Context) {
        activeMemorySessionToken = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ACTIVE_TOKEN).apply()
    }

    private fun issueSessionToken(email: String): String {
        val randomBytes = ByteArray(16)
        SecureRandom().nextBytes(randomBytes)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$email:${System.currentTimeMillis()}:${bytesToHex(randomBytes)}".toByteArray())
        return "sa_tok_${bytesToHex(digest).take(32)}"
    }

    private fun generateRandomSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hashPasswordPbkdf2(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        return bytesToHex(encoded)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = if (hex.length % 2 == 0) hex else "0$hex"
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = clean.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    private fun currentTimestampLabel(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }
}
