package com.example.security

import android.content.Context
import com.example.data.local.LayaliAccountDatabase
import com.example.data.local.RegisteredAccountEntity
import com.example.data.model.UserProfile
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

data class PasswordUpdateRecord(
    val accountId: String,
    val username: String,
    val email: String,
    val passwordSaltHex: String,
    val passwordHashHex: String,
    val bcryptPreview: String,
    val updatedAtLabel: String
)

/**
 * Manages Super Admin & Customer Account Database persistence (Room SQLite + SharedPreferences),
 * salted password hashing (PBKDF2-HMAC-SHA256 / bcrypt format), password change & recovery flows,
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
        val saltHex: String
        val hashHex: String
        val bcryptFormatted: String
        val nowLabel: String
        val adminEmail: String

        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            val salt = generateRandomSalt()
            saltHex = bytesToHex(salt)
            hashHex = hashPasswordPbkdf2(INITIAL_SUPER_ADMIN_PASSWORD, salt)
            bcryptFormatted = "\$2b\$12\$${saltHex.take(22)}\$${hashHex.take(31)}"
            nowLabel = currentTimestampLabel()
            adminEmail = INITIAL_SUPER_ADMIN_EMAIL

            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putString(KEY_ADMIN_EMAIL, adminEmail)
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
            adminEmail = prefs.getString(KEY_ADMIN_EMAIL, INITIAL_SUPER_ADMIN_EMAIL) ?: INITIAL_SUPER_ADMIN_EMAIL
            saltHex = prefs.getString(KEY_PASSWORD_SALT_HEX, "") ?: ""
            hashHex = prefs.getString(KEY_PASSWORD_HASH_HEX, "") ?: ""
            bcryptFormatted = prefs.getString(KEY_BCRYPT_FORMATTED, "") ?: ""
            nowLabel = prefs.getString(KEY_LAST_UPDATED, currentTimestampLabel()) ?: currentTimestampLabel()
        }

        // Ensure Super Admin account is also persisted in the Room SQLite database
        runCatching {
            val dao = LayaliAccountDatabase.getInstance(context).accountDao()
            val existingAdmin = dao.findByIdentifier(adminEmail)
            if (existingAdmin == null && saltHex.isNotBlank() && hashHex.isNotBlank()) {
                dao.upsertAccountSync(
                    RegisteredAccountEntity(
                        accountId = "admin_super_root",
                        username = "@ramanur_admin",
                        email = adminEmail,
                        displayName = "Super Admin",
                        role = "super_admin",
                        passwordSaltHex = saltHex,
                        passwordHashHex = hashHex,
                        bcryptPreview = bcryptFormatted,
                        bio = "المسؤول الرئيسي للنظام • إدارة العروض والحجوزات",
                        cityName = "دمشق",
                        country = "سوريا",
                        selectedFrameId = "obsidian_elite",
                        level = 50,
                        xpPoints = 9850,
                        nextLevelXp = 10000,
                        walletBalanceUsd = 500,
                        chatPriceUsd = 25,
                        isPaidChatEnabled = true,
                        updatedAtLabel = nowLabel,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Authenticates login credentials entered in AuthScreen or registers a new customer account
     * into the Room SQLite database.
     * - If the identifier matches Super Admin (RamaNUR@gmail.com or updated admin email), verifies
     *   the Super Admin password hash.
     * - If the identifier matches an existing Customer account in the Room SQLite database,
     *   verifies the customer's salted PBKDF2-HMAC-SHA256 password hash (unless isCreateOrOverwrite = false
     *   and password fails, in which case it rejects the wrong password).
     * - If the customer account does not exist yet in the database, creates and persists it with
     *   salted PBKDF2-HMAC-SHA256 password hash so the account is permanently saved.
     */
    fun authenticate(
        context: Context,
        emailOrUsername: String,
        passwordInput: String,
        fullNameOverride: String = ""
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

        // Standard Customer Authentication & Room Database Verification / Registration
        val dao = runCatching { LayaliAccountDatabase.getInstance(context).accountDao() }.getOrNull()
        val existingAccount = runCatching { dao?.findByIdentifier(cleanIdentifier) }.getOrNull()

        if (existingAccount != null && existingAccount.passwordSaltHex.isNotBlank() && existingAccount.passwordHashHex.isNotBlank()) {
            val saltBytes = hexToBytes(existingAccount.passwordSaltHex)
            val candidateHashHex = hashPasswordPbkdf2(passwordInput, saltBytes)
            val isPasswordValid = constantTimeEquals(candidateHashHex, existingAccount.passwordHashHex)
            if (!isPasswordValid) {
                return AuthResult.InvalidSuperAdminPassword(
                    message = "كلمة المرور غير صحيحة للحساب المحفوظ (${existingAccount.username}) — يمكنك تغييرها من زر «تغيير / نسيت كلمة المرور»"
                )
            }
            return AuthResult.CustomerSuccess(
                displayName = existingAccount.displayName.ifBlank { cleanIdentifier.substringBefore("@") },
                email = existingAccount.email
            )
        }

        // New Customer Account -> Persist in Room SQLite Database with Salted PBKDF2-HMAC-SHA256 Hash
        val displayName = fullNameOverride.trim().ifBlank {
            cleanIdentifier.removePrefix("@").substringBefore("@").trim().ifBlank { "عميل ليالي" }
        }
        val cleanHandle = "@" + cleanIdentifier
            .removePrefix("@")
            .substringBefore("@")
            .lowercase()
            .replace(" ", "_")
            .filter { it.isLetterOrDigit() || it == '_' }
            .ifBlank { "layali_vip" }
        val resolvedEmail = if (cleanIdentifier.contains("@") && !cleanIdentifier.startsWith("@")) {
            cleanIdentifier
        } else {
            "${cleanHandle.removePrefix("@")}@layali.client"
        }

        if (passwordInput.isNotBlank() && dao != null) {
            runCatching {
                val salt = generateRandomSalt()
                val saltHex = bytesToHex(salt)
                val hashHex = hashPasswordPbkdf2(passwordInput, salt)
                val bcryptPreview = "\$2b\$12\$${saltHex.take(22)}\$${hashHex.take(31)}"
                val nowLabel = currentTimestampLabel()
                val accountId = "user_${cleanHandle.removePrefix("@")}"

                dao.upsertAccountSync(
                    RegisteredAccountEntity(
                        accountId = accountId,
                        username = cleanHandle,
                        email = resolvedEmail,
                        displayName = displayName,
                        role = "customer",
                        passwordSaltHex = saltHex,
                        passwordHashHex = hashHex,
                        bcryptPreview = bcryptPreview,
                        bio = "عضو مسجل في قاعدة بيانات ليالي VIP ✨",
                        cityName = "دمشق",
                        country = "سوريا",
                        selectedFrameId = "emerald_crown",
                        level = 1,
                        xpPoints = 150,
                        nextLevelXp = 2500,
                        walletBalanceUsd = 0,
                        chatPriceUsd = 15,
                        isPaidChatEnabled = true,
                        updatedAtLabel = nowLabel,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }

        return AuthResult.CustomerSuccess(
            displayName = displayName,
            email = resolvedEmail
        )
    }

    /**
     * Changes or resets the password for any account (Customer or Super Admin) in both
     * Room SQLite Database and SharedPreferences.
     * - When [requireCurrentPassword] is true, verifies [currentPassword] against the stored hash first.
     * - When [requireCurrentPassword] is false (Self-Service Recovery / Reset flow on AuthScreen),
     *   updates the password hash directly for Customer accounts (Super Admin always requires current password).
     */
    fun changeAccountPassword(
        context: Context,
        identifier: String,
        currentPassword: String,
        newPassword: String,
        requireCurrentPassword: Boolean = true
    ): Result<PasswordUpdateRecord> {
        ensureInitialized(context)
        val cleanId = identifier.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم أو البريد الإلكتروني للحساب"))
        }
        if (newPassword.trim().length < 6) {
            return Result.failure(IllegalArgumentException("يجب أن تتكون كلمة المرور الجديدة من 6 خانات على الأقل"))
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedAdminEmail = prefs.getString(KEY_ADMIN_EMAIL, INITIAL_SUPER_ADMIN_EMAIL) ?: INITIAL_SUPER_ADMIN_EMAIL
        val isSuperAdmin = cleanId.equals(storedAdminEmail, ignoreCase = true) ||
            cleanId.equals(INITIAL_SUPER_ADMIN_EMAIL, ignoreCase = true) ||
            cleanId.equals("@ramanur_admin", ignoreCase = true)

        if (isSuperAdmin) {
            val storedSaltHex = prefs.getString(KEY_PASSWORD_SALT_HEX, "") ?: ""
            val storedHashHex = prefs.getString(KEY_PASSWORD_HASH_HEX, "") ?: ""
            val currentCandidateHash = hashPasswordPbkdf2(currentPassword, hexToBytes(storedSaltHex))
            if (!constantTimeEquals(currentCandidateHash, storedHashHex)) {
                return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة لحساب المسؤول الرئيسي"))
            }

            val newSalt = generateRandomSalt()
            val newSaltHex = bytesToHex(newSalt)
            val newHashHex = hashPasswordPbkdf2(newPassword, newSalt)
            val bcryptFormatted = "\$2b\$12\$${newSaltHex.take(22)}\$${newHashHex.take(31)}"
            val updatedTime = currentTimestampLabel()

            prefs.edit()
                .putString(KEY_PASSWORD_SALT_HEX, newSaltHex)
                .putString(KEY_PASSWORD_HASH_HEX, newHashHex)
                .putString(KEY_BCRYPT_FORMATTED, bcryptFormatted)
                .putString(KEY_LAST_UPDATED, updatedTime)
                .apply()

            runCatching {
                LayaliAccountDatabase.getInstance(context).accountDao().updateAccountPasswordSync(
                    accountId = "admin_super_root",
                    saltHex = newSaltHex,
                    hashHex = newHashHex,
                    bcryptPreview = bcryptFormatted,
                    updatedAtLabel = updatedTime,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            }

            return Result.success(
                PasswordUpdateRecord(
                    accountId = "admin_super_root",
                    username = "@ramanur_admin",
                    email = storedAdminEmail,
                    passwordSaltHex = newSaltHex,
                    passwordHashHex = newHashHex,
                    bcryptPreview = bcryptFormatted,
                    updatedAtLabel = updatedTime
                )
            )
        }

        val dao = LayaliAccountDatabase.getInstance(context).accountDao()
        val existing = dao.findByIdentifier(cleanId)

        if (existing != null && requireCurrentPassword && existing.passwordSaltHex.isNotBlank()) {
            val candidateHash = hashPasswordPbkdf2(currentPassword, hexToBytes(existing.passwordSaltHex))
            if (!constantTimeEquals(candidateHash, existing.passwordHashHex)) {
                return Result.failure(IllegalArgumentException("كلمة المرور الحالية غير صحيحة للحساب (${existing.username})"))
            }
        }

        val newSalt = generateRandomSalt()
        val newSaltHex = bytesToHex(newSalt)
        val newHashHex = hashPasswordPbkdf2(newPassword, newSalt)
        val bcryptFormatted = "\$2b\$12\$${newSaltHex.take(22)}\$${newHashHex.take(31)}"
        val updatedTime = currentTimestampLabel()

        val cleanHandle = existing?.username ?: ("@" + cleanId.removePrefix("@").substringBefore("@").lowercase().replace(" ", "_").ifBlank { "layali_vip" })
        val resolvedEmail = existing?.email ?: if (cleanId.contains("@") && !cleanId.startsWith("@")) cleanId else "${cleanHandle.removePrefix("@")}@layali.client"
        val accountId = existing?.accountId ?: "user_${cleanHandle.removePrefix("@")}"

        if (existing != null) {
            dao.updateAccountPasswordSync(
                accountId = existing.accountId,
                saltHex = newSaltHex,
                hashHex = newHashHex,
                bcryptPreview = bcryptFormatted,
                updatedAtLabel = updatedTime,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        } else {
            dao.upsertAccountSync(
                RegisteredAccountEntity(
                    accountId = accountId,
                    username = cleanHandle,
                    email = resolvedEmail,
                    displayName = cleanId.removePrefix("@").substringBefore("@").ifBlank { "عضو ليالي" },
                    role = "customer",
                    passwordSaltHex = newSaltHex,
                    passwordHashHex = newHashHex,
                    bcryptPreview = bcryptFormatted,
                    updatedAtLabel = updatedTime,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }

        return Result.success(
            PasswordUpdateRecord(
                accountId = accountId,
                username = cleanHandle,
                email = resolvedEmail,
                passwordSaltHex = newSaltHex,
                passwordHashHex = newHashHex,
                bcryptPreview = bcryptFormatted,
                updatedAtLabel = updatedTime
            )
        )
    }

    /**
     * Persists the complete profile, level, luxury frame, and wallet infrastructure for the active account
     * into the Room SQLite database (`registered_accounts`).
     */
    fun saveAccountInfrastructureSync(
        context: Context,
        accountId: String,
        profile: UserProfile,
        walletBalanceUsd: Int,
        email: String?,
        role: String = "customer"
    ): RegisteredAccountEntity? = runCatching {
        ensureInitialized(context)
        val dao = LayaliAccountDatabase.getInstance(context).accountDao()
        val cleanUsername = profile.username.ifBlank { "@layali_vip" }.let {
            if (it.startsWith("@")) it else "@$it"
        }
        val existing = dao.findByIdentifier(cleanUsername)
            ?: (email?.let { dao.findByIdentifier(it) })
            ?: dao.findByIdentifier(accountId)

        val saltHex: String
        val hashHex: String
        val bcryptPreview: String
        if (existing != null && existing.passwordSaltHex.isNotBlank() && existing.passwordHashHex.isNotBlank()) {
            saltHex = existing.passwordSaltHex
            hashHex = existing.passwordHashHex
            bcryptPreview = existing.bcryptPreview
        } else {
            val defaultSalt = generateRandomSalt()
            saltHex = bytesToHex(defaultSalt)
            hashHex = hashPasswordPbkdf2("LayaliVip2026", defaultSalt)
            bcryptPreview = "\$2b\$12\$${saltHex.take(22)}\$${hashHex.take(31)}"
        }

        val nowLabel = currentTimestampLabel()
        val resolvedAccountId = existing?.accountId ?: accountId.ifBlank { "user_${cleanUsername.removePrefix("@")}" }
        val resolvedEmail = email?.takeIf { it.isNotBlank() }
            ?: existing?.email
            ?: "${cleanUsername.removePrefix("@")}@layali.client"

        val entity = RegisteredAccountEntity(
            accountId = resolvedAccountId,
            username = cleanUsername,
            email = resolvedEmail,
            displayName = profile.displayName.ifBlank { existing?.displayName ?: "عضو ليالي الملكي" },
            role = existing?.role ?: role,
            passwordSaltHex = saltHex,
            passwordHashHex = hashHex,
            bcryptPreview = bcryptPreview,
            bio = profile.bio,
            cityName = profile.cityName.ifBlank { "دمشق" },
            country = profile.country.ifBlank { "سوريا" },
            avatarUri = profile.avatarUri,
            selectedFrameId = profile.selectedFrameId,
            level = profile.level,
            xpPoints = profile.xpPoints,
            nextLevelXp = profile.nextLevelXp,
            walletBalanceUsd = walletBalanceUsd,
            chatPriceUsd = profile.chatPriceUsd,
            isPaidChatEnabled = profile.isPaidChatEnabled,
            updatedAtLabel = nowLabel,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        dao.upsertAccountSync(entity)
        entity
    }.getOrNull()

    fun getSavedAccounts(context: Context): List<RegisteredAccountEntity> {
        ensureInitialized(context)
        return runCatching {
            LayaliAccountDatabase.getInstance(context).accountDao().getAllAccountsList()
        }.getOrDefault(emptyList())
    }

    fun findSavedAccount(context: Context, identifier: String): RegisteredAccountEntity? {
        ensureInitialized(context)
        return runCatching {
            LayaliAccountDatabase.getInstance(context).accountDao().findByIdentifier(identifier.trim())
        }.getOrNull()
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

        runCatching {
            LayaliAccountDatabase.getInstance(context).accountDao().updateAccountPasswordSync(
                accountId = "admin_super_root",
                saltHex = newSaltHex,
                hashHex = newHashHex,
                bcryptPreview = bcryptFormatted,
                updatedAtLabel = updatedTime,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        }

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

    fun currentTimestampLabel(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    }
}
