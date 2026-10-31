package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LayaliRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBooking_andObserveBookings_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LayaliRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBooking(
                venueId = "damascus_royal_spa",
                venueName = "سبا قصر الياسمين الدمشقي",
                cityName = "دمشق",
                country = "سوريا",
                serviceTitle = "مساج ملكي بالأحجار الساخنة",
                category = "سبا ومساج",
                bookingDate = "2026-11-10",
                timeSlot = "08:00 مساءً",
                guestCount = 2,
                totalPriceUsd = 110,
                notes = "جلسة استرخاء مسائية"
            )
        }
        assertTrue("Expected booking creation to succeed", createResult.isSuccess)
        val bookingId = createResult.getOrThrow()

        val emittedBookings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBookings(uid).first { list -> list.any { it.id == bookingId } }
        }
        assertTrue(emittedBookings.any { it.id == bookingId && it.venueId == "damascus_royal_spa" })
    }

    @Test
    fun publishService_andBlockUser_authenticatedAdminFlow_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = LayaliRepository(firestore)

        val pubResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishService(
                title = "عرض مساج ملكي وسهرة VIP",
                cityName = "دمشق",
                country = "سوريا",
                category = "سبا ومساج",
                description = "صور وفيديو للأجنحة الملكية مع سعر خاص",
                priceUsd = 80,
                imageUrl = "preset://rose_gold_spa",
                videoUrl = "https://example.com/video.mp4",
                mediaType = "both"
            )
        }
        assertTrue("Expected publishService to succeed", pubResult.isSuccess)
        val serviceId = pubResult.getOrThrow()

        val emittedServices = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePublishedServices().first { list -> list.any { it.id == serviceId } }
        }
        assertTrue(emittedServices.any { it.id == serviceId && it.priceUsd == 80L })

        val blockResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.blockUser(
                blockedIdentifier = "@spam_account",
                reason = "مخالفة شروط الحجز",
                cityScope = "جميع المدن"
            )
        }
        assertTrue("Expected blockUser to succeed", blockResult.isSuccess)
        val blockId = blockResult.getOrThrow()

        val emittedBlocks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBlockedUsers(uid).first { list -> list.any { it.id == blockId } }
        }
        assertTrue(emittedBlocks.any { it.id == blockId && it.blockedIdentifier == "@spam_account" })
    }

    @Test
    fun getBookingById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LayaliRepository(firestore)
        val bookingId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBooking(
                venueId = "antalya_marina_club",
                venueName = "نادي ولاونج مارينا أنطاليا",
                cityName = "أنطاليا",
                country = "تركيا",
                serviceTitle = "طاولة سهرة VIP مطلة على البحر",
                category = "سهرات فنية",
                bookingDate = "2026-11-12",
                timeSlot = "10:00 مساءً",
                guestCount = 4,
                totalPriceUsd = 240,
                notes = "طاولة أمامية"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = LayaliRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getBookingById(aliceUid, bookingId)
        }
        assertTrue("Cross-user access must fail", crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeBookings_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = LayaliRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBookings(aliceUid).first()
            }
            fail("Expected unauthenticated observeBookings to throw PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun saveUserAccount_andUpdatePasswordHash_ownerSucceeds_crossUserBlocked() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = LayaliRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveOrUpdateUserAccount(
                username = "@layla_vip",
                displayName = "ليلى الملكية",
                email = ALICE_EMAIL,
                bio = "صانعة محتوى وسهرات VIP حصرية",
                cityName = "دمشق",
                country = "سوريا",
                avatarUri = "aleppo_palace_nights",
                selectedFrameId = "amethyst_vip",
                level = 1,
                xpPoints = 150,
                nextLevelXp = 2500,
                walletBalanceUsd = 250,
                chatPriceUsd = 20,
                isPaidChatEnabled = true,
                passwordSaltHex = "a1b2c3d4e5f60718293a4b5c6d7e8f90",
                passwordHashHex = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                role = "customer"
            )
        }
        assertTrue("Expected saveOrUpdateUserAccount to succeed", saveResult.isSuccess)

        val updatePassResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateUserAccountPasswordHash(
                newSaltHex = "f0e1d2c3b4a5968778695a4b3c2d1e0f",
                newHashHex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
        }
        assertTrue("Expected updateUserAccountPasswordHash to succeed", updatePassResult.isSuccess)

        val loadedAccount = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getUserAccountById(aliceUid).getOrThrow()
        }
        assertNotNull(loadedAccount)
        assertEquals("@layla_vip", loadedAccount?.username)
        assertEquals("f0e1d2c3b4a5968778695a4b3c2d1e0f", loadedAccount?.passwordSaltHex)

        // Cross-user read of Alice's UserAccount document (which contains email & passwordHashHex) must be blocked
        signInTestUser(BOB_EMAIL)
        val bobRepo = LayaliRepository(firestore)
        val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserAccountById(aliceUid)
        }
        assertTrue("Cross-user read of UserAccount must fail", crossRead.isFailure)
        val firestoreEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@layali.test"
        const val BOB_EMAIL = "bob@layali.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
