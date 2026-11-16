package com.example.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveControlsRecoveryTest {

    @Test fun legacyFooterIsRearrangedWithoutReplacingControlsOrTouchingMedia() {
        val script = relaxiaLiveFooterRepairScript()
        assertTrue(script.contains("bottom.insertBefore(actions, bottom.firstChild)"))
        assertTrue(script.contains("actions.appendChild(button)"))
        assertTrue(script.contains("bottom.appendChild(composer)"))
        assertTrue(script.contains("document.getElementById('relaxia-native-live-controls-repair')"))
        assertTrue(script.contains("style.textContent"))
        assertTrue(script.contains("flex-direction:column!important"))
        assertTrue(script.contains("input::placeholder"))
        assertTrue(script.contains("color:#FFFFFF!important"))
        assertTrue(script.contains("html[data-theme=\\\"light\\\"]") || script.contains("html[data-theme=\"light\"]"))
        assertFalse(script.contains("room.disconnect("))
        assertFalse(script.contains("localParticipant"))
        assertFalse(script.contains("fetch("))
    }
}
