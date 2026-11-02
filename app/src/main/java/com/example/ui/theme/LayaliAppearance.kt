package com.example.ui.theme

import android.content.Context

/** Shared appearance and first-run preferences, never account credentials. */
object LayaliAppearance {
    private const val STORE = "layali_appearance"
    private const val LIGHT = "light_mode"
    private const val WELCOME_SEEN = "welcome_seen"

    fun isLightTheme(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(LIGHT, false)

    fun setLightTheme(context: Context, enabled: Boolean) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putBoolean(LIGHT, enabled).apply()
    }

    fun hasSeenWelcome(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(WELCOME_SEEN, false)

    fun markWelcomeSeen(context: Context) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putBoolean(WELCOME_SEEN, true).apply()
    }
}
