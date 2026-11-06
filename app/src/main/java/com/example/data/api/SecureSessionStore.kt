package com.example.data.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface SessionStore {
    fun read(): String?
    fun write(token: String)
    fun clear()
}

/** Only an encrypted opaque session token is stored, in Android's no-backup directory. */
internal class SecureSessionStore private constructor(
    directory: File,
    private val origin: String,
    private val keyProvider: () -> SecretKey
) : SessionStore {
    private val storage = AtomicFile(File(directory, "layali-session-${digest(origin)}.bin"))

    constructor(context: Context, origin: String) : this(context.noBackupFilesDir, origin, {
        val alias = "layali-session-${digest(origin)}"
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    })

    @Synchronized override fun read(): String? {
        if (!storage.baseFile.exists() && !File(storage.baseFile.path + ".bak").exists()) return null
        return try {
            val bytes = storage.openRead().use { it.readBytes() }
            require(bytes.size in 30..4096 && bytes[0] == 1.toByte())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            cipher.updateAAD(origin.toByteArray(Charsets.UTF_8))
            cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8).also { require(it.length in 32..256) }
        } catch (_: Exception) {
            storage.delete()
            null
        }
    }

    @Synchronized override fun write(token: String) {
        require(token.length in 32..256)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        cipher.updateAAD(origin.toByteArray(Charsets.UTF_8))
        val bytes = byteArrayOf(1) + cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val stream = storage.startWrite()
        try { stream.write(bytes); storage.finishWrite(stream) }
        catch (e: Exception) { storage.failWrite(stream); throw e }
    }
    @Synchronized override fun clear() { storage.delete() }

    companion object {
        private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
        internal fun forTest(directory: File, origin: String, key: SecretKey) = SecureSessionStore(directory, origin) { key }
    }
}
