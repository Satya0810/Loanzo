package com.loanzo.app.util

import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationHelper @Inject constructor() {

    companion object {
        private const val TAG = "TranslationHelper"
    }

    // Fast HTTP client with reasonable timeouts and automatic retry
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // In-memory LRU cache to store up to 2000 translated sentences (0ms instant lookup)
    private val translationCache = LruCache<String, String>(2000)

    // Active ML Kit Translators cache & downloaded status
    private val mlKitTranslators = java.util.concurrent.ConcurrentHashMap<String, com.google.mlkit.nl.translate.Translator>()
    private val downloadedModels = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val _modelDownloadedState = MutableStateFlow<Set<String>>(emptySet())
    val modelDownloadedState: StateFlow<Set<String>> = _modelDownloadedState.asStateFlow()

    init {
        checkDownloadedModelsOnDisk()
    }

    private fun getTranslateLanguage(langCode: String): String? = when (langCode.lowercase()) {
        "hi" -> com.google.mlkit.nl.translate.TranslateLanguage.HINDI
        "mr" -> com.google.mlkit.nl.translate.TranslateLanguage.MARATHI
        "bn" -> com.google.mlkit.nl.translate.TranslateLanguage.BENGALI
        "te" -> com.google.mlkit.nl.translate.TranslateLanguage.TELUGU
        "ta" -> com.google.mlkit.nl.translate.TranslateLanguage.TAMIL
        "gu" -> com.google.mlkit.nl.translate.TranslateLanguage.GUJARATI
        "kn" -> com.google.mlkit.nl.translate.TranslateLanguage.KANNADA
        "ur" -> com.google.mlkit.nl.translate.TranslateLanguage.URDU
        else -> null
    }

    private fun checkDownloadedModelsOnDisk() {
        try {
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            listOf("hi", "mr", "bn", "te", "ta", "gu", "kn", "ur").forEach { lang ->
                val target = getTranslateLanguage(lang)
                if (target != null) {
                    val model = com.google.mlkit.nl.translate.TranslateRemoteModel.Builder(target).build()
                    modelManager.isModelDownloaded(model)
                        .addOnSuccessListener { isDownloaded ->
                            if (isDownloaded) {
                                downloadedModels.add(lang)
                                _modelDownloadedState.value = downloadedModels.toSet()
                                Log.i(TAG, "Detected pre-downloaded ML Kit model on disk for $lang")
                            }
                        }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkDownloadedModelsOnDisk note: ${e.message}")
        }
    }

    fun isModelDownloaded(targetLang: String): Boolean {
        return downloadedModels.contains(targetLang.lowercase())
    }

    fun downloadModelIfNeeded(targetLang: String, onStatus: ((Boolean) -> Unit)? = null) {
        val target = getTranslateLanguage(targetLang)
        if (target == null) {
            onStatus?.invoke(false)
            return
        }

        try {
            val options = com.google.mlkit.nl.translate.TranslatorOptions.Builder()
                .setSourceLanguage(com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH)
                .setTargetLanguage(target)
                .build()
            val translator = com.google.mlkit.nl.translate.Translation.getClient(options)
            mlKitTranslators[targetLang.lowercase()] = translator

            val conditions = com.google.mlkit.common.model.DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    Log.i(TAG, "ML Kit offline model for $targetLang successfully downloaded and ready for internal translation.")
                    downloadedModels.add(targetLang.lowercase())
                    _modelDownloadedState.value = downloadedModels.toSet()
                    onStatus?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit model download note for $targetLang: ${e.message}")
                    onStatus?.invoke(false)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize ML Kit translator for $targetLang: ${e.message}")
            onStatus?.invoke(false)
        }
    }

    private suspend fun translateWithMlKit(translator: com.google.mlkit.nl.translate.Translator, text: String): String? =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            translator.translate(text)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) continuation.resumeWith(Result.success(result))
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resumeWith(Result.success(null))
                }
        }

    // =========================================================================
    // Comprehensive Domain Vocabulary (0ms Instant Offline UI Translation)
    // =========================================================================
    private val offlineGlossaryHi = mapOf(
        "hello" to "नमस्ते",
        "hi" to "नमस्ते",
        "welcome to loanzo" to "लोनज़ो में आपका स्वागत है",
        "thank you" to "धन्यवाद",
        "thanks" to "धन्यवाद",
        "yes" to "हाँ",
        "no" to "नहीं",
        "ok" to "ठीक है",
        "okay" to "ठीक है",
        "member" to "सदस्य",
        "user" to "उपयोगकर्ता",
        "field agent" to "फ़ील्ड एजेंट",
        "agent" to "एजेंट",
        "master admin" to "मास्टर एडमिन",
        "app owner" to "ऐप ओनर",
        "borrower" to "उधारकर्ता",
        "lender" to "ऋणदाता",
        "guarantor" to "गारंटर",
        "in-app role switcher" to "इन-ऐप भूमिका स्विचर",
        "trust score" to "विश्वास स्कोर",
        "digilocker" to "डिजिलॉकर",
        "vault" to "वॉल्ट",
        "verified" to "सत्यापित",
        "pending" to "लंबित",
        "approved" to "स्वीकृत",
        "rejected" to "अस्वीकृत",
        "completed" to "पूर्ण",
        "sanctioned" to "स्वीकृत",
        "disbursed" to "संवितरित",
        "repaid" to "चुकाया",
        "overdue" to "अतिदेय",
        "active" to "सक्रिय",
        "closed" to "समाप्त",
        "unlocked" to "अनलॉक 🔓",
        "locked" to "लॉक 🔒",
        "on duty" to "ड्यूटी पर",
        "off duty" to "ऑफ ड्यूटी",
        "home" to "होम",
        "loans" to "ऋण",
        "alerts" to "अलर्ट",
        "profile" to "प्रोफ़ाइल",
        "marketplace" to "बाज़ार",
        "visits" to "विज़िट",
        "platform" to "प्लेटफ़ॉर्म",
        "inspect" to "निरीक्षण",
        "dispatch" to "डिस्पैच",
        "post" to "पोस्ट",
        "portfolio overview" to "पोर्टफोलियो अवलोकन",
        "real-time capital balance" to "वास्तविक समय पूंजी शेष",
        "active portfolio value" to "सक्रिय पोर्टफोलियो मूल्य",
        "lent out" to "दिया गया ऋण",
        "borrowed" to "लिया गया ऋण",
        "active loans" to "सक्रिय ऋण",
        "manage all loans" to "सभी ऋण प्रबंधित करें",
        "recent activity" to "हाल की गतिविधि",
        "quick actions" to "त्वरित क्रियाएं",
        "apply for loan" to "ऋण के लिए आवेदन करें",
        "explore marketplace" to "बाज़ार देखें",
        "community loan wall" to "सामुदायिक ऋण मंच",
        "all offers" to "सभी ऑफ़र",
        "lenders" to "ऋणदाता",
        "borrowers" to "उधारकर्ता",
        "my posts" to "मेरी पोस्ट",
        "settings" to "सेटिंग्स",
        "app language" to "ऐप भाषा",
        "change language" to "भाषा बदलें",
        "select app language" to "ऐप भाषा चुनें",
        "appearance" to "उपस्थिति",
        "sign out" to "साइन आउट",
        "logout" to "लॉग आउट",
        "push demo data" to "डेमो डेटा लोड करें",
        "clear demo data" to "डेमो डेटा साफ़ करें",
        "kyc verification" to "केवाईसी सत्यापन",
        "bank details" to "बैंक विवरण",
        "bank accounts" to "बैंक खाते",
        "security & biometrics" to "सुरक्षा और बायोमेट्रिक्स",
        "guide" to "गाइड",
        "interactive guide" to "इंटरैक्टिव गाइड",
        "chat" to "चैट",
        "report" to "रिपोर्ट",
        "simulator" to "सिम्युलेटर",
        "help & support" to "सहायता और समर्थन",
        "loan details" to "ऋण विवरण",
        "loan amount" to "ऋण राशि",
        "interest rate" to "ब्याज दर",
        "tenure" to "कार्यकाल",
        "outstanding balance" to "बकाया राशि",
        "repayment amount" to "चुकौती राशि",
        "monthly emi" to "मासिक ईएमआई",
        "principal" to "मूलधन",
        "interest" to "ब्याज",
        "save" to "सहेजें",
        "cancel" to "रद्द करें",
        "clear" to "साफ़ करें",
        "submit" to "जमा करें",
        "next" to "आगे बढ़ें",
        "back" to "पीछे",
        "confirm" to "पुष्टि करें",
        "search" to "खोजें",
        "edit" to "संपादित करें",
        "delete" to "हटाएं",
        "upload" to "अपलोड करें",
        "download" to "डाउनलोड करें",
        "sign in" to "साइन इन करें",
        "sign in to your account" to "अपने खाते में साइन इन करें",
        "welcome back" to "वापसी पर स्वागत है",
        "username" to "उपयोगकर्ता नाम",
        "password" to "पासवर्ड",
        "forgot password?" to "पासवर्ड भूल गए?",
        "continue with google" to "Google के साथ जारी रखें",
        "payment received" to "भुगतान प्राप्त हुआ",
        "proposal accepted" to "प्रस्ताव स्वीकार कर लिया गया!",
        "inspection" to "भौतिक सत्यापन"
    )

    private val offlineGlossaryMr = mapOf(
        "hello" to "नमस्कार",
        "hi" to "नमस्कार",
        "welcome to loanzo" to "लोनझो मध्ये आपले स्वागत आहे",
        "thank you" to "धन्यवाद",
        "thanks" to "धन्यवाद",
        "yes" to "होय",
        "no" to "नाही",
        "member" to "सदस्य",
        "field agent" to "फील्ड एजंट",
        "master admin" to "मास्टर ॲडमिन",
        "borrower" to "कर्जदार",
        "lender" to "कर्जदाता",
        "trust score" to "विश्वास स्कोअर",
        "digilocker" to "डिजीलॉकर",
        "vault" to "वॉल्ट",
        "verified" to "सत्यापित",
        "pending" to "प्रलंबित",
        "approved" to "मंजूर",
        "active" to "सक्रिय",
        "home" to "घर",
        "loans" to "कर्ज",
        "alerts" to "सूचना",
        "profile" to "प्रोफाइल",
        "portfolio overview" to "पोर्टफोलिओ आढावा",
        "quick actions" to "जलद कृती",
        "app language" to "ॲप भाषा",
        "sign out" to "साइन आउट",
        "push demo data" to "डेमो डेटा जोडा",
        "clear demo data" to "डेमो डेटा हटवा"
    )

    fun getOfflineTranslation(text: String, targetLang: String): String? {
        val trimmed = text.trim().lowercase()
        return when (targetLang.lowercase()) {
            "hi" -> offlineGlossaryHi[trimmed]
            "mr" -> offlineGlossaryMr[trimmed]
            else -> null
        }
    }

    fun translateSync(text: String, targetLang: String): String {
        if (targetLang.equals("en", ignoreCase = true) || text.isBlank()) return text
        val cacheKey = "en_${targetLang.lowercase()}_${text.trim()}"
        val cached = translationCache.get(cacheKey)
        if (cached != null) return cached

        // 1. Check instant offline domain glossary (0ms latency)
        val offline = getOfflineTranslation(text, targetLang)
        if (offline != null) {
            translationCache.put(cacheKey, offline)
            return offline
        }

        // 2. Queue background translation via on-device ML Kit model
        CoroutineScope(Dispatchers.IO).launch {
            translateText(text, targetLang)
        }

        return text
    }

    suspend fun translate(text: String, targetLang: String = "hi"): String? = translateText(text, targetLang)

    suspend fun translateText(text: String, targetLang: String, sourceLang: String = "auto"): String? = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@withContext text

        val cacheKey = "${sourceLang}_${targetLang}_$trimmed"
        val cached = translationCache.get(cacheKey)
        if (cached != null) {
            return@withContext cached
        }

        // TIER 0: Offline Domain Vocabulary (Instant 0ms sync lookup)
        val offline = getOfflineTranslation(trimmed, targetLang)
        if (offline != null) {
            translationCache.put(cacheKey, offline)
            return@withContext offline
        }

        val encodedQuery = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            trimmed.replace(" ", "%20")
        }

        // TIER 1: Google ML Kit On-Device Internal Translation (Fast & Offline)
        try {
            val key = targetLang.lowercase()
            val translator = mlKitTranslators[key] ?: run {
                val target = getTranslateLanguage(key)
                if (target != null) {
                    val options = com.google.mlkit.nl.translate.TranslatorOptions.Builder()
                        .setSourceLanguage(com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH)
                        .setTargetLanguage(target)
                        .build()
                    com.google.mlkit.nl.translate.Translation.getClient(options).also {
                        mlKitTranslators[key] = it
                    }
                } else null
            }
            if (translator != null) {
                val mlResult = translateWithMlKit(translator, trimmed)
                if (!mlResult.isNullOrBlank()) {
                    translationCache.put(cacheKey, mlResult)
                    return@withContext mlResult
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Tier 1 (ML Kit on-device) note: ${e.message}")
        }

        // TIER 2: Google Translate Chrome Extension Client (Bypasses 429 Bot Blocks)
        try {
            val url = "https://translate.googleapis.com/translate_a/single?client=dict-chrome-ex&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank() && responseBody.startsWith("[")) {
                        val rootArray = JSONArray(responseBody)
                        val sentences = rootArray.optJSONArray(0)
                        if (sentences != null) {
                            val sb = StringBuilder()
                            for (i in 0 until sentences.length()) {
                                val part = sentences.optJSONArray(i)
                                if (part != null) {
                                    sb.append(part.optString(0, ""))
                                }
                            }
                            val result = sb.toString().trim()
                            if (result.isNotBlank()) {
                                translationCache.put(cacheKey, result)
                                return@withContext result
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 (Google dict-chrome-ex) translation note: ${e.message}")
        }

        // TIER 3: MyMemory Open Translation API
        try {
            val src = if (sourceLang == "auto") "en" else sourceLang
            val myMemoryUrl = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=$src|$targetLang"
            val request = Request.Builder()
                .url(myMemoryUrl)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank() && responseBody.startsWith("{")) {
                        val json = JSONObject(responseBody)
                        val responseData = json.optJSONObject("responseData")
                        val translatedText = responseData?.optString("translatedText", "") ?: ""
                        if (translatedText.isNotBlank() && !translatedText.contains("MYMEMORY WARNING")) {
                            val cleaned = cleanHtmlEntities(translatedText)
                            translationCache.put(cacheKey, cleaned)
                            return@withContext cleaned
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 3 (MyMemory) translation note: ${e.message}")
        }

        return@withContext null
    }

    private fun cleanHtmlEntities(input: String): String {
        return input
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }

    @Composable
    fun rememberTranslated(text: String, targetLang: String): String {
        if (targetLang.equals("en", ignoreCase = true) || text.isBlank()) return text

        val downloadedModels by modelDownloadedState.collectAsState()
        var translated by remember(text, targetLang, downloadedModels) {
            mutableStateOf(translateSync(text, targetLang))
        }

        LaunchedEffect(text, targetLang, downloadedModels) {
            val result = translateText(text, targetLang)
            if (!result.isNullOrBlank() && result != text) {
                translated = result
            }
        }

        return translated
    }
}
