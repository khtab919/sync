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
        assertTrue(script.contains("enterkeyhint"))
        assertTrue(script.contains("relaxiaLiveKeyboardFixAttached"))
        assertTrue(script.contains("measureVisibleRoom"))
        assertTrue(script.contains("positionNativeRoomSheet"))
        assertTrue(script.contains("relaxiaLiveDialogObserver"))
        assertTrue(script.contains("attributeFilter:['open']"))
        assertTrue(script.contains("maxHeight:h+'px'"))
        assertTrue(script.contains("bottom:'auto'"))
        assertTrue(script.contains("--room-footer-height"))
        assertTrue(script.contains("relaxiaLiveFooterObserver"))
        assertTrue(script.contains("ResizeObserver"))
        assertTrue(script.contains("chat-focused"))
        assertTrue(script.contains("#audioStage.seat-grid{display:none!important}"))
        assertTrue(script.contains("input.blur()"))
        assertFalse(script.contains("fetch("))
    }
}
