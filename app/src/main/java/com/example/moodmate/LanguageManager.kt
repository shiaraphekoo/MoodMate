package com.example.moodmate

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.*

object LanguageManager {
    private const val PREFS_NAME = "AppPrefs"
    private const val LANGUAGE_KEY = "language_code"

    // Use lowercase codes consistent with Locale API
    const val ENGLISH_CODE = "en"
    const val ZULU_CODE = "zu" // ISO 639-1 code for Zulu

    private fun getPrefs(context: Context): SharedPreferences {
        // Use applicationContext to prevent memory leaks
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the selected language code (e.g., "en", "zu") to SharedPreferences.
     */
    fun saveLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit().putString(LANGUAGE_KEY, languageCode).apply()
    }

    /**
     * Retrieves the saved language code, defaulting to ENGLISH_CODE.
     */
    fun getSavedLanguage(context: Context): String {
        return getPrefs(context).getString(LANGUAGE_KEY, ENGLISH_CODE) ?: ENGLISH_CODE
    }

    /**
     * Updates the application's base context configuration with the saved locale.
     * This method must be called in attachBaseContext(newBase) of every Activity.
     * @return The new context with the updated configuration.
     */
    fun updateLanguage(context: Context): Context {
        val languageCode = getSavedLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale) // Correct API usage (Google Gemini, 2025)


        return context.createConfigurationContext(config) // Correct API usage (Google Gemini, 2025)
    }
}