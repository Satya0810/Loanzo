package com.loanzo.app.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.loanzo.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("loanzo_ai_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LLM7_KEY = "llm7_api_key"
        private const val KEY_LLM7_MODEL = "llm7_model"
        private const val KEY_SAMBANOVA_KEY = "sambanova_api_key"
        private const val KEY_SAMBANOVA_MODEL = "sambanova_model"
        private const val KEY_CLOUDFLARE_TOKEN = "cloudflare_token"
        private const val KEY_CLOUDFLARE_ACCOUNT_ID = "cloudflare_account_id"
        private const val KEY_CLOUDFLARE_MODEL = "cloudflare_model"
        private const val KEY_GEMINI_KEY = "gemini_api_key"

        // Defaults loaded securely from BuildConfig (injected via local.properties or CI)
        val DEFAULT_LLM7_KEY: String get() = BuildConfig.LLM7_API_KEY
        const val DEFAULT_LLM7_MODEL = "default"

        val DEFAULT_SAMBANOVA_KEY: String get() = BuildConfig.SAMBANOVA_API_KEY
        const val DEFAULT_SAMBANOVA_MODEL = "Meta-Llama-3.3-70B-Instruct"

        val DEFAULT_CLOUDFLARE_TOKEN: String get() = BuildConfig.CLOUDFLARE_TOKEN
        val DEFAULT_CLOUDFLARE_ACCOUNT_ID: String get() = BuildConfig.CLOUDFLARE_ACCOUNT_ID
        const val DEFAULT_CLOUDFLARE_MODEL = "@cf/meta/llama-3.1-8b-instruct"
    }

    var llm7ApiKey: String
        get() {
            val v = prefs.getString(KEY_LLM7_KEY, null)
            return if (!v.isNullOrBlank()) v else DEFAULT_LLM7_KEY
        }
        set(value) = prefs.edit().putString(KEY_LLM7_KEY, value.trim()).apply()

    var llm7Model: String
        get() = prefs.getString(KEY_LLM7_MODEL, DEFAULT_LLM7_MODEL) ?: DEFAULT_LLM7_MODEL
        set(value) = prefs.edit().putString(KEY_LLM7_MODEL, value.trim()).apply()

    var sambaNovaApiKey: String
        get() {
            val v = prefs.getString(KEY_SAMBANOVA_KEY, null)
            return if (!v.isNullOrBlank()) v else DEFAULT_SAMBANOVA_KEY
        }
        set(value) = prefs.edit().putString(KEY_SAMBANOVA_KEY, value.trim()).apply()

    var sambaNovaModel: String
        get() = prefs.getString(KEY_SAMBANOVA_MODEL, DEFAULT_SAMBANOVA_MODEL) ?: DEFAULT_SAMBANOVA_MODEL
        set(value) = prefs.edit().putString(KEY_SAMBANOVA_MODEL, value.trim()).apply()

    var cloudflareToken: String
        get() {
            val v = prefs.getString(KEY_CLOUDFLARE_TOKEN, null)
            return if (!v.isNullOrBlank()) v else DEFAULT_CLOUDFLARE_TOKEN
        }
        set(value) = prefs.edit().putString(KEY_CLOUDFLARE_TOKEN, value.trim()).apply()

    var cloudflareAccountId: String
        get() {
            val v = prefs.getString(KEY_CLOUDFLARE_ACCOUNT_ID, null)
            return if (!v.isNullOrBlank()) v else DEFAULT_CLOUDFLARE_ACCOUNT_ID
        }
        set(value) = prefs.edit().putString(KEY_CLOUDFLARE_ACCOUNT_ID, value.trim()).apply()

    var cloudflareModel: String
        get() = prefs.getString(KEY_CLOUDFLARE_MODEL, DEFAULT_CLOUDFLARE_MODEL) ?: DEFAULT_CLOUDFLARE_MODEL
        set(value) = prefs.edit().putString(KEY_CLOUDFLARE_MODEL, value.trim()).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value.trim()).apply()
}
