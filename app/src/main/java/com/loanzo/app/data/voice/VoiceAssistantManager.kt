package com.loanzo.app.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class VoiceState {
    object Idle : VoiceState()
    data class Listening(val partialText: String = "", val rmsDb: Float = 0f) : VoiceState()
    data class Thinking(val prompt: String) : VoiceState()
    data class Speaking(
        val spokenText: String,
        val displayText: String,
        val actionTags: List<String> = emptyList(),
        val isAudioPlaying: Boolean = true
    ) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

@Singleton
class VoiceAssistantManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceAssistantManager"
        private const val UTTERANCE_ID_ASSISTANT = "loanzo_voice_assistant_utterance"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechOnTtsReady: String? = null
    private var onSpeechResultCallback: ((String) -> Unit)? = null
    private var isIntentionallyStopping = false

    init {
        mainHandler.post {
            initTts()
        }
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    val tts = textToSpeech
                    if (tts != null) {
                        // Set Indian English if available, with US fallback
                        val indianLocale = Locale("en", "IN")
                        val result = tts.setLanguage(indianLocale)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts.language = Locale.US
                        }
                        tts.setPitch(1.05f) // Warm, friendly pitch
                        tts.setSpeechRate(0.98f) // Clear, accessible rate for ease of comprehension

                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                Log.d(TAG, "TTS playback started: $utteranceId")
                                mainHandler.post {
                                    val current = _voiceState.value
                                    if (current is VoiceState.Speaking) {
                                        _voiceState.value = current.copy(isAudioPlaying = true)
                                    }
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                Log.d(TAG, "TTS playback finished: $utteranceId")
                                mainHandler.post {
                                    val current = _voiceState.value
                                    if (current is VoiceState.Speaking) {
                                        // Keep response text and action pills visible on screen!
                                        _voiceState.value = current.copy(isAudioPlaying = false)
                                    }
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                Log.w(TAG, "TTS playback error: $utteranceId")
                                mainHandler.post {
                                    val current = _voiceState.value
                                    if (current is VoiceState.Speaking) {
                                        _voiceState.value = current.copy(isAudioPlaying = false)
                                    }
                                }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                Log.w(TAG, "TTS playback error: $utteranceId, code: $errorCode")
                                mainHandler.post {
                                    val current = _voiceState.value
                                    if (current is VoiceState.Speaking) {
                                        _voiceState.value = current.copy(isAudioPlaying = false)
                                    }
                                }
                            }
                        })
                    }

                    pendingSpeechOnTtsReady?.let { text ->
                        pendingSpeechOnTtsReady = null
                        speak(text)
                    }
                } else {
                    Log.e(TAG, "Failed to initialize TextToSpeech engine (status: $status)")
                    isTtsInitialized = false
                    mainHandler.post {
                        val pending = pendingSpeechOnTtsReady
                        if (pending != null) {
                            pendingSpeechOnTtsReady = null
                            _voiceState.value = VoiceState.Speaking(
                                spokenText = VoiceTextSanitizer.cleanForSpeech(pending),
                                displayText = VoiceTextSanitizer.cleanForDisplay(pending),
                                actionTags = VoiceTextSanitizer.extractActions(pending),
                                isAudioPlaying = false
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing TTS", e)
        }
    }

    private var lastRecognizedPartialText = ""
    private var stopWatchdogRunnable: Runnable? = null

    /**
     * Starts listening for user voice input.
     * Enforces creation and execution on the Main UI Looper to prevent crashes across Android versions.
     */
    fun startListening(onResult: (String) -> Unit) {
        mainHandler.post {
            // Cancel any pending stop watchdog
            stopWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
            stopWatchdogRunnable = null

            // Halt any active speech output to avoid microphone audio loopback
            stopSpeaking()

            onSpeechResultCallback = onResult
            lastRecognizedPartialText = ""

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceState.value = VoiceState.Error("Speech recognition is not available on this device.")
                return@post
            }

            try {
                // Safely destroy existing recognizer if any
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer: Ready for speech")
                        _voiceState.value = VoiceState.Listening(partialText = "", rmsDb = 0f)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: Beginning of speech")
                        _voiceState.value = VoiceState.Listening(partialText = "Listening...", rmsDb = 2f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS dB (typically -2 to 10) for UI soundwave visualizer
                        val normalizedRms = rmsdB.coerceIn(0f, 10f)
                        val current = _voiceState.value
                        if (current is VoiceState.Listening) {
                            _voiceState.value = current.copy(rmsDb = normalizedRms)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: End of speech")
                    }

                    override fun onError(error: Int) {
                        stopWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
                        stopWatchdogRunnable = null

                        // If user intentionally stopped listening
                        if (error == SpeechRecognizer.ERROR_CLIENT && isIntentionallyStopping) {
                            isIntentionallyStopping = false
                            if (lastRecognizedPartialText.isNotBlank()) {
                                val captured = lastRecognizedPartialText
                                lastRecognizedPartialText = ""
                                _voiceState.value = VoiceState.Thinking(prompt = captured)
                                onSpeechResultCallback?.invoke(captured)
                            } else {
                                _voiceState.value = VoiceState.Idle
                            }
                            return
                        }

                        // If speech timed out or no match was found, BUT we already caught partial text
                        if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) &&
                            lastRecognizedPartialText.isNotBlank()
                        ) {
                            val captured = lastRecognizedPartialText
                            lastRecognizedPartialText = ""
                            Log.d(TAG, "Recovering partial speech on code $error: $captured")
                            _voiceState.value = VoiceState.Thinking(prompt = captured)
                            onSpeechResultCallback?.invoke(captured)
                            return
                        }

                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch any speech. Tap microphone to speak."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out. Tap microphone to try again."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Voice recognition issue (Code: $error). Tap to retry."
                        }
                        Log.w(TAG, "SpeechRecognizer error: $error ($message)")
                        _voiceState.value = VoiceState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        stopWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
                        stopWatchdogRunnable = null

                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        var text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isBlank() && lastRecognizedPartialText.isNotBlank()) {
                            text = lastRecognizedPartialText
                        }
                        lastRecognizedPartialText = ""
                        Log.d(TAG, "SpeechRecognizer results: $text")

                        if (text.isNotBlank()) {
                            _voiceState.value = VoiceState.Thinking(prompt = text)
                            onSpeechResultCallback?.invoke(text)
                        } else {
                            _voiceState.value = VoiceState.Idle
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            lastRecognizedPartialText = partial
                            val currentRms = (_voiceState.value as? VoiceState.Listening)?.rmsDb ?: 2f
                            _voiceState.value = VoiceState.Listening(partialText = partial, rmsDb = currentRms)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val defaultLangTag = Locale.getDefault().toLanguageTag()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, defaultLangTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, defaultLangTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    // High stability pause thresholds: prevents Android from cutting off user mid-sentence
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                }

                isIntentionallyStopping = false
                speechRecognizer?.startListening(intent)
                _voiceState.value = VoiceState.Listening(partialText = "", rmsDb = 0f)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition", e)
                _voiceState.value = VoiceState.Error("Could not start microphone: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Halts speech recording with a safety watchdog to eliminate infinite hangs.
     */
    fun stopListening() {
        mainHandler.post {
            isIntentionallyStopping = true
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping SpeechRecognizer", e)
            }

            if (_voiceState.value is VoiceState.Listening) {
                if (lastRecognizedPartialText.isNotBlank()) {
                    _voiceState.value = VoiceState.Thinking(prompt = lastRecognizedPartialText)
                } else {
                    _voiceState.value = VoiceState.Thinking("Processing speech...")
                }
            }

            // Watchdog timer: If SpeechRecognizer fails to emit onResults within 1500ms, recover automatically
            stopWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
            stopWatchdogRunnable = Runnable {
                if (_voiceState.value is VoiceState.Thinking) {
                    if (lastRecognizedPartialText.isNotBlank()) {
                        val captured = lastRecognizedPartialText
                        lastRecognizedPartialText = ""
                        _voiceState.value = VoiceState.Thinking(prompt = captured)
                        onSpeechResultCallback?.invoke(captured)
                    } else {
                        _voiceState.value = VoiceState.Idle
                    }
                }
            }
            mainHandler.postDelayed(stopWatchdogRunnable!!, 1500L)
        }
    }

    /**
     * Synthesizes and speaks text out loud via Android TextToSpeech.
     * Sanitizes markdown, symbols, and action tags so the speech sounds conversational and human.
     */
    fun speak(rawText: String, fullDisplayResponse: String? = null) {
        val displayResponse = fullDisplayResponse ?: rawText
        val cleanSpoken = VoiceTextSanitizer.cleanForSpeech(rawText)
        val cleanDisplay = VoiceTextSanitizer.cleanForDisplay(displayResponse)
        val actionTags = VoiceTextSanitizer.extractActions(rawText)

        mainHandler.post {
            if (!isTtsInitialized) {
                pendingSpeechOnTtsReady = rawText
                // Immediately display the response text and action pills so user is never stuck on Thinking
                _voiceState.value = VoiceState.Speaking(
                    spokenText = cleanSpoken,
                    displayText = cleanDisplay,
                    actionTags = actionTags,
                    isAudioPlaying = false
                )
                return@post
            }

            try {
                textToSpeech?.stop()
                if (cleanSpoken.isBlank()) {
                    _voiceState.value = VoiceState.Speaking(
                        spokenText = "",
                        displayText = cleanDisplay,
                        actionTags = actionTags,
                        isAudioPlaying = false
                    )
                    return@post
                }

                _voiceState.value = VoiceState.Speaking(
                    spokenText = cleanSpoken,
                    displayText = cleanDisplay,
                    actionTags = actionTags,
                    isAudioPlaying = true
                )

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID_ASSISTANT)
                }
                textToSpeech?.speak(cleanSpoken, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID_ASSISTANT)
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering TTS playback", e)
                _voiceState.value = VoiceState.Speaking(
                    spokenText = cleanSpoken,
                    displayText = cleanDisplay,
                    actionTags = actionTags,
                    isAudioPlaying = false
                )
            }
        }
    }

    /**
     * Stops any currently playing audio speech while keeping the text and action pills visible.
     */
    fun stopSpeaking() {
        mainHandler.post {
            try {
                if (textToSpeech?.isSpeaking == true) {
                    textToSpeech?.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping TTS", e)
            }
            val current = _voiceState.value
            if (current is VoiceState.Speaking) {
                _voiceState.value = current.copy(isAudioPlaying = false)
            }
        }
    }

    /**
     * Resets assistant state to Idle.
     */
    fun resetState() {
        mainHandler.post {
            stopWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
            stopWatchdogRunnable = null
            lastRecognizedPartialText = ""
            onSpeechResultCallback = null
            isIntentionallyStopping = true
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling SpeechRecognizer", e)
            }
            stopSpeaking()
            _voiceState.value = VoiceState.Idle
        }
    }

    /**
     * Lifecycle release for Clean Architecture cleanup.
     */
    fun destroy() {
        mainHandler.post {
            stopWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
            stopWatchdogRunnable = null
            lastRecognizedPartialText = ""
            onSpeechResultCallback = null
            isIntentionallyStopping = true
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying SpeechRecognizer", e)
            }

            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
                isTtsInitialized = false
            } catch (e: Exception) {
                Log.w(TAG, "Error shutting down TextToSpeech", e)
            }
        }
    }
}
