package com.example.data.api

import java.io.File
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SecureSessionStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val origin = "https://layali.example"
    private val token = "test-opaque-session-token-that-is-not-a-real-credential-1234567890"

    @Test fun sessionRestoresAcrossClientInstancesWithoutPlaintext() = runBlocking {
        val key = key()
        val directory = temp.newFolder()
        SecureSessionStore.forTest(directory, origin, key).write(token)
        val encrypted = directory.listFiles()!!.single().readBytes()
        assertFalse(encrypted.toString(Charsets.ISO_8859_1).contains(token))
        val next = LayaliApi(origin, SecureSessionStore.forTest(directory, origin, key))
        assertTrue(next.restoreSession())
        assertEquals(token, next.token)
        next.forgetSession()
        assertFalse(LayaliApi(origin, SecureSessionStore.forTest(directory, origin, key)).restoreSession())
    }

    @Test fun alteredCiphertextAndInvalidatedKeyClearTheSession() {
        val directory = temp.newFolder()
        val key = key()
        val store = SecureSessionStore.forTest(directory, origin, key)
        store.write(token)
        val file = directory.listFiles()!!.single()
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        file.writeBytes(bytes)
        assertNull(store.read())
        store.write(token)
        assertNull(SecureSessionStore.forTest(directory, origin, this.key()).read())
    }

    @Test fun ciphertextCannotBeReusedForAnotherOrigin() {
        val key = key()
        val firstDir = temp.newFolder()
        val secondDir = temp.newFolder()
        val first = SecureSessionStore.forTest(firstDir, origin, key)
        val second = SecureSessionStore.forTest(secondDir, "https://other.example", key)
        first.write(token)
        second.write(token)
        secondDir.listFiles()!!.single().writeBytes(firstDir.listFiles()!!.single().readBytes())
        assertNull(second.read())
    }

    @Test fun encryptedOutboxMetadataSupportsLongMessagesWithoutChangingSessionBounds() {
        val key=key();val directory=temp.newFolder()
        val payload="{\"items\":[\""+"رسالة خاصة ".repeat(500)+"\"]}"
        val store=SecureSessionStore.forTest(directory,"$origin/outbox/1/2",key,200000)
        store.write(payload)
        assertEquals(payload,SecureSessionStore.forTest(directory,"$origin/outbox/1/2",key,200000).read())
        assertFalse(directory.listFiles()!!.single().readBytes().toString(Charsets.UTF_8).contains("رسالة خاصة"))
        val normal=SecureSessionStore.forTest(temp.newFolder(),origin,key)
        assertThrows(IllegalArgumentException::class.java){normal.write(payload)}
    }
}
