package com.example.moodmate

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES

/**
 * Handles saving and applying the user's preferred theme.
 */
object ThemeManager {

    private const val PREFS_NAME = "ThemePrefs"
    private const val THEME_KEY = "current_theme"

    enum class Theme(val value: String, val nightMode: Int) {
        LIGHT("light", MODE_NIGHT_NO),
        DARK("dark", MODE_NIGHT_YES),
        SYSTEM("system", MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Saves the chosen theme preference and applies it.
     */
    fun saveTheme(context: Context, theme: Theme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_KEY, theme.value)
            .apply()
        applyTheme(theme)
    }

    /**
     * Applies the specified theme to the entire application.
     */
    fun applyTheme(theme: Theme) {
        AppCompatDelegate.setDefaultNightMode(theme.nightMode)
    }

    /**
     * Loads the saved theme from SharedPreferences and applies it.
     */
    fun loadAndApplyTheme(context: Context) {
        val savedThemeString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(THEME_KEY, Theme.LIGHT.value) ?: Theme.LIGHT.value

        val themeToApply = Theme.values().find { it.value == savedThemeString }
            ?: Theme.LIGHT // Default to light if string is not found

        applyTheme(themeToApply)
    }

    /**
     * Retrieves the current saved theme enum.
     */
    fun getCurrentTheme(context: Context): Theme {
        val savedThemeString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(THEME_KEY, Theme.LIGHT.value) ?: Theme.LIGHT.value

        return Theme.values().find { it.value == savedThemeString } ?: Theme.LIGHT
    }
}