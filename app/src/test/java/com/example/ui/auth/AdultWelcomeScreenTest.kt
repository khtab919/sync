package com.example.ui.auth

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultWelcomeScreenTest {
    private fun today() = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.NOVEMBER, 6, 12, 0)
    }

    @Test fun eighteenthBirthdayIsAllowed() {
        assertTrue(isAdultOnDate(2008, Calendar.NOVEMBER, 6, today()))
        assertFalse(isAdultOnDate(2008, Calendar.NOVEMBER, 7, today()))
    }

    @Test fun invalidAndFutureDatesAreRejected() {
        assertFalse(isAdultOnDate(2027, Calendar.JANUARY, 1, today()))
        assertFalse(isAdultOnDate(2000, Calendar.FEBRUARY, 30, today()))
    }
}
