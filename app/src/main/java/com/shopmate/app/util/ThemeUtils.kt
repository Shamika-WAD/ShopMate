package com.shopmate.app.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Applies the light/dark choice saved in Settings.
 *  0 = follow the phone's setting (default)
 *  1 = always light
 *  2 = always dark
 */
object ThemeUtils {

    fun apply(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** Call as early as possible when the app starts. */
    fun applySaved(context: Context) {
        apply(Settings(context).themeMode)
    }
}