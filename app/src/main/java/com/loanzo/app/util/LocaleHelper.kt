package com.loanzo.app.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "loanzo_locale_prefs"
    private const val KEY_LANGUAGE = "app_language"
    const val DEFAULT_LANGUAGE = "en"

    fun getPersistedLanguage(context: Context): String {
        return try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        } catch (_: Exception) {
            DEFAULT_LANGUAGE
        }
    }

    fun persistLanguage(context: Context, languageCode: String) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        } catch (_: Exception) {}
    }

    fun applyLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        persistLanguage(context, languageCode)

        // 1. Android 13+ (API 33+) native LocaleManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(languageCode)
            } catch (e: Exception) {
                android.util.Log.w("LocaleHelper", "LocaleManager note: ${e.message}")
            }
        } else {
            // 2. AppCompatDelegate for API < 33
            try {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(languageCode)
                )
            } catch (e: Exception) {
                android.util.Log.w("LocaleHelper", "AppCompatDelegate note: ${e.message}")
            }
        }

        // 3. Update configuration directly for immediate in-process resources
        try {
            val res = context.resources
            val config = Configuration(res.configuration)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            config.setLayoutDirection(locale)

            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)

            val appRes = context.applicationContext?.resources
            if (appRes != null && appRes != res) {
                @Suppress("DEPRECATION")
                appRes.updateConfiguration(config, appRes.displayMetrics)
            }
        } catch (_: Exception) {}
    }

    fun createLocalizedContext(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        try {
            val appRes = context.applicationContext?.resources
            if (appRes != null && appRes != res) {
                @Suppress("DEPRECATION")
                appRes.updateConfiguration(config, appRes.displayMetrics)
            }
        } catch (_: Exception) {}

        return context.createConfigurationContext(config)
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getPersistedLanguage(context)
        return createLocalizedContext(context, languageCode)
    }
}
