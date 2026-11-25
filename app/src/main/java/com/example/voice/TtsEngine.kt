package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.domain.model.AerisVoice
import com.example.domain.model.TtsEngineMetadata
import com.example.domain.model.VoiceGender
import com.example.domain.model.VoiceQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface TtsEngine : TextToSpeechManager {
    val isSpeakingFlow: Flow<Boolean>
    val isPausedFlow: StateFlow<Boolean>
    val isReadyFlow: Flow<Boolean>
    val availableEngines: StateFlow<List<TtsEngineMetadata>>
    val activeEnginePackage: StateFlow<String>
    val ttsError: StateFlow<String?>
    val lastSpokenText: StateFlow<String?>

    fun setLanguage(locale: Locale): Boolean
    fun switchEngine(packageName: String)
    fun pauseSpeaking()
    fun resumeSpeaking(): Boolean
    fun replayLastSpoken(): Boolean
    fun clearError()
    fun shutdown()
}

internal fun splitIntoSpeechChunks(text: String, maxChunkChars: Int = 240): List<String> {
    val normalized = text.trim()
    if (normalized.isEmpty()) return emptyList()
    val rawSentences = normalized
        .split(Regex("(?<=[.!?])\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (rawSentences.isEmpty()) return listOf(normalized)

    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    for (sentence in rawSentences) {
        if (sentence.length > maxChunkChars) {
            if (current.isNotEmpty()) {
                chunks.add(current.toString().trim())
                current.clear()
            }
            var idx = 0
            while (idx < sentence.length) {
                val end = (idx + maxChunkChars).coerceAtMost(sentence.length)
                chunks.add(sentence.substring(idx, end).trim())
                idx = end
            }
        } else if (current.isEmpty()) {
            current.append(sentence)
        } else if (current.length + 1 + sentence.length <= maxChunkChars) {
            current.append(' ').append(sentence)
        } else {
            chunks.add(current.toString().trim())
            current.clear()
            current.append(sentence)
        }
    }
    if (current.isNotEmpty()) {
        chunks.add(current.toString().trim())
    }
    return chunks
}

/**
 * AndroidTtsEngine:
 * Real Android TextToSpeech backend with:
 * - Installed TTS engine enumeration & switching
 * - Real voice enumeration & reliable metadata-based gender classification
 * - AudioSessionManager audio focus coordination
 * - Saved voice restoration with safe fallback notice
 * - Replay last spoken text & error reporting
 */
class AndroidTtsEngine(
    private val context: Context,
    private val audioSessionManager: AerisAudioSessionManager? = null
) : TtsEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var requestedEnginePackage: String? = null
    private var pendingVoiceId: String? = null
    private var fallbackListener: ((String?) -> Unit)? = null

    private var pendingChunks: List<String> = emptyList()
    private var currentChunkIndex: Int = 0
    private var pausedAtChunkIndex: Int? = null
    private var activeSessionTag: String = ""

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeakingFlow: Flow<Boolean> = _isSpeaking.asStateFlow()
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    override val isPausedFlow: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReadyFlow: Flow<Boolean> = _isReady.asStateFlow()
    override val isEngineReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<AerisVoice>>(emptyList())
    override val availableVoices: StateFlow<List<AerisVoice>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<AerisVoice?>(null)
    override val selectedVoice: StateFlow<AerisVoice?> = _selectedVoice.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    override val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    override val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _availableEngines = MutableStateFlow<List<TtsEngineMetadata>>(emptyList())
    override val availableEngines: StateFlow<List<TtsEngineMetadata>> = _availableEngines.asStateFlow()

    private val _activeEnginePackage = MutableStateFlow("")
    override val activeEnginePackage: StateFlow<String> = _activeEnginePackage.asStateFlow()

    private val _ttsError = MutableStateFlow<String?>(null)
    override val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String?>(null)
    override val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

    init {
        initTts(enginePackage = null)
    }

    private fun initTts(enginePackage: String?) {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        _isReady.value = false
        _isSpeaking.value = false
        requestedEnginePackage = enginePackage
        tts = try {
            if (!enginePackage.isNullOrBlank()) {
                TextToSpeech(context.applicationContext, this, enginePackage)
            } else {
                TextToSpeech(context.applicationContext, this)
            }
        } catch (e: Exception) {
            _ttsError.value = "Failed to start Android TTS engine: ${e.message}"
            null
        }
    }

    fun setOnVoiceFallbackListener(listener: (String?) -> Unit) {
        fallbackListener = listener
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _ttsError.value = null
            val engine = tts ?: return
            try {
                engine.language = Locale.US
            } catch (_: Exception) {
            }
            engine.setSpeechRate(_speechRate.value)
            engine.setPitch(_speechPitch.value)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _isPaused.value = false
                    _ttsError.value = null
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null && activeSessionTag.isNotBlank() && utteranceId.startsWith(activeSessionTag) && !_isPaused.value) {
                        val nextIdx = currentChunkIndex + 1
                        if (nextIdx < pendingChunks.size) {
                            speakChunkAt(nextIdx)
                            return
                        }
                    }
                    pendingChunks = emptyList()
                    pausedAtChunkIndex = null
                    _isSpeaking.value = false
                    audioSessionManager?.abandonAudioFocus()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _isPaused.value = false
                    audioSessionManager?.abandonAudioFocus()
                    _ttsError.value = "TTS playback error on current voice"
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _isPaused.value = false
                    audioSessionManager?.abandonAudioFocus()
                    _ttsError.value = "TTS synthesis failed (code $errorCode)"
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _isSpeaking.value = false
                    if (!_isPaused.value) {
                        audioSessionManager?.abandonAudioFocus()
                    }
                }
            })

            enumerateInstalledEngines(engine)
            _isReady.value = true
            refreshVoices()
        } else {
            _isReady.value = false
            _ttsError.value = "Android TTS initialization failed (status $status). Check Text-to-Speech in System Settings."
        }
    }

    private fun enumerateInstalledEngines(engine: TextToSpeech) {
        val defaultPkg = try {
            engine.defaultEngine.orEmpty()
        } catch (_: Exception) {
            ""
        }
        val activePkg = requestedEnginePackage?.takeIf { it.isNotBlank() } ?: defaultPkg
        _activeEnginePackage.value = activePkg

        val enginesList = try {
            engine.engines.orEmpty().map { info ->
                TtsEngineMetadata(
                    packageName = info.name,
                    label = info.label?.takeIf { it.isNotBlank() } ?: info.name,
                    isDefault = info.name == defaultPkg
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        _availableEngines.value = enginesList
    }

    override fun switchEngine(packageName: String) {
        if (packageName.isBlank()) return
        if (packageName == _activeEnginePackage.value && _isReady.value) return
        _selectedVoice.value = null
        initTts(packageName)
    }

    override fun refreshVoices() {
        val engine = tts ?: return
        enumerateInstalledEngines(engine)

        val rawVoices = try {
            engine.voices
        } catch (_: Exception) {
            null
        }

        val activePkg = _activeEnginePackage.value.ifBlank { engine.defaultEngine ?: "Android System TTS" }
        val activeLabel = _availableEngines.value.firstOrNull { it.packageName == activePkg }?.label ?: activePkg

        if (!rawVoices.isNullOrEmpty()) {
            val mapped = rawVoices.map { v ->
                val features = v.features ?: emptySet()
                val gender = classifyVoiceGender(v.name, features)

                val quality = when (v.quality) {
                    Voice.QUALITY_VERY_HIGH -> VoiceQuality.VERY_HIGH
                    Voice.QUALITY_HIGH -> VoiceQuality.HIGH
                    Voice.QUALITY_NORMAL -> VoiceQuality.NORMAL
                    Voice.QUALITY_LOW -> VoiceQuality.LOW
                    else -> VoiceQuality.NORMAL
                }

                val isNetworkReq = try {
                    v.isNetworkConnectionRequired
                } catch (_: Exception) {
                    false
                }

                AerisVoice(
                    id = v.name,
                    name = cleanVoiceName(v.name, v.locale),
                    locale = v.locale,
                    languageDisplayName = v.locale.displayLanguage.ifBlank { "English" },
                    countryDisplayName = v.locale.displayCountry.ifBlank { "System" },
                    gender = gender,
                    quality = quality,
                    isNetworkRequired = isNetworkReq,
                    isInstalledLocally = !isNetworkReq,
                    engineName = activeLabel,
                    features = features
                )
            }.sortedWith(
                compareBy(
                    { it.gender != VoiceGender.FEMALE },
                    { !it.isInstalledLocally },
                    { it.languageDisplayName },
                    { it.name }
                )
            )

            _availableVoices.value = mapped

            val targetId = pendingVoiceId ?: _selectedVoice.value?.id
            if (targetId != null) {
                val notice = selectVoiceWithFallback(targetId)
                fallbackListener?.invoke(notice)
                pendingVoiceId = null
            } else if (mapped.isNotEmpty()) {
                val preferred = mapped.firstOrNull {
                    it.gender == VoiceGender.FEMALE && it.locale.language == "en" && it.isInstalledLocally
                } ?: mapped.firstOrNull {
                    it.gender == VoiceGender.FEMALE && it.locale.language == "en"
                } ?: mapped.first()
                setVoice(preferred)
            }
        } else {
            val defaultLocale = Locale.getDefault()
            val fallbackVoice = AerisVoice(
                id = "system-default-voice",
                name = "System Default Voice (${defaultLocale.displayName})",
                locale = defaultLocale,
                languageDisplayName = defaultLocale.displayLanguage.ifBlank { "English" },
                countryDisplayName = defaultLocale.displayCountry.ifBlank { "Default" },
                gender = VoiceGender.UNSPECIFIED,
                quality = VoiceQuality.NORMAL,
                isNetworkRequired = false,
                isInstalledLocally = true,
                engineName = activeLabel,
                features = setOf("system_default")
            )
            _availableVoices.value = listOf(fallbackVoice)
            if (pendingVoiceId != null && pendingVoiceId != fallbackVoice.id) {
                fallbackListener?.invoke("Saved voice unavailable; using System Default Voice.")
                pendingVoiceId = null
            }
            _selectedVoice.value = fallbackVoice
        }
    }

    /**
     * Classifies gender ONLY when reliable engine metadata or explicit voice naming tokens indicate it.
     * Carefully checks for "female" before "male" so "female" never matches as "male".
     */
    internal fun classifyVoiceGender(voiceName: String, features: Set<String>): VoiceGender {
        val normalizedFeatures = features.map { it.lowercase(Locale.ROOT) }
        val lowerName = voiceName.lowercase(Locale.ROOT)

        val hasFemaleFeature = normalizedFeatures.any { f ->
            f == "female" || f.contains("gender=female") || f.contains("gender:female") || f.contains("#female")
        }
        val hasMaleFeature = normalizedFeatures.any { f ->
            f == "male" || f.contains("gender=male") || f.contains("gender:male") || f.contains("#male")
        }

        if (hasFemaleFeature) return VoiceGender.FEMALE
        if (hasMaleFeature) return VoiceGender.MALE

        val hasFemaleToken = Regex("""(^|[^a-z])(female|f)([^a-z]|$)""").containsMatchIn(
            lowerName.replace("#", " ").replace("_", " ").replace("-", " ")
        ) || lowerName.contains("#female") || lowerName.contains("-f-") || lowerName.contains("_f_")

        if (hasFemaleToken) return VoiceGender.FEMALE

        val cleanedForMale = lowerName.replace("female", "")
        val hasMaleToken = Regex("""(^|[^a-z])(male|m)([^a-z]|$)""").containsMatchIn(
            cleanedForMale.replace("#", " ").replace("_", " ").replace("-", " ")
        ) || cleanedForMale.contains("#male") || cleanedForMale.contains("-m-") || cleanedForMale.contains("_m_")

        if (hasMaleToken) return VoiceGender.MALE

        return VoiceGender.UNSPECIFIED
    }

    private fun cleanVoiceName(rawName: String, locale: Locale): String {
        val simplified = rawName
            .replace("en-", "EN ")
            .replace("-local", "")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
        return if (simplified.length > 28) {
            "Voice ${locale.toLanguageTag().uppercase()} (${rawName.takeLast(6)})"
        } else {
            simplified
        }
    }

    override fun setVoice(voice: AerisVoice) {
        _selectedVoice.value = voice
        val engine = tts ?: return
        try {
            val rawVoices = engine.voices
            val matching = rawVoices?.find { it.name == voice.id }
            if (matching != null) {
                engine.voice = matching
            } else {
                engine.language = voice.locale
            }
        } catch (_: Exception) {
            try {
                engine.language = voice.locale
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Attempts to select a saved voice ID. If voices aren't loaded yet, caches it as pendingVoiceId.
     * If voices are loaded and the saved ID is missing, falls back cleanly and returns a notice string.
     */
    fun selectVoiceWithFallback(preferredVoiceId: String?): String? {
        if (preferredVoiceId.isNullOrBlank()) return null
        val voices = _availableVoices.value
        if (voices.isEmpty()) {
            pendingVoiceId = preferredVoiceId
            return null
        }

        val exactMatch = voices.find { it.id == preferredVoiceId }
        if (exactMatch != null) {
            setVoice(exactMatch)
            return null
        }

        val fallback = voices.firstOrNull {
            it.gender == VoiceGender.FEMALE && it.locale.language == "en" && it.isInstalledLocally
        } ?: voices.firstOrNull {
            it.isInstalledLocally
        } ?: voices.first()

        setVoice(fallback)
        return "Saved voice \"$preferredVoiceId\" is unavailable on this TTS engine; switched to ${fallback.name}."
    }

    fun setVoiceById(voiceId: String): Boolean {
        val found = _availableVoices.value.find { it.id == voiceId }
        return if (found != null) {
            setVoice(found)
            true
        } else {
            pendingVoiceId = voiceId
            false
        }
    }

    override fun previewVoice(voice: AerisVoice, sampleText: String) {
        if (!_isReady.value) {
            _ttsError.value = "TTS engine is still initializing. Please try again in a moment."
            return
        }
        setVoice(voice)
        speak(sampleText, "preview_${System.currentTimeMillis()}")
    }

    override fun speak(text: String, utteranceId: String) {
        if (text.isBlank()) return
        if (!_isReady.value || tts == null) {
            _ttsError.value = "TTS engine is not ready yet."
            return
        }

        _lastSpokenText.value = text
        _isPaused.value = false
        pausedAtChunkIndex = null
        pendingChunks = splitIntoSpeechChunks(text)
        activeSessionTag = "${utteranceId}_${System.nanoTime()}"
        audioSessionManager?.requestAudioFocusForPlayback()

        if (pendingChunks.isNotEmpty()) {
            speakChunkAt(0)
        }
    }

    private fun speakChunkAt(index: Int) {
        val chunk = pendingChunks.getOrNull(index) ?: return
        currentChunkIndex = index
        val chunkUtteranceId = "${activeSessionTag}_$index"
        try {
            val result = tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, chunkUtteranceId)
            if (result == TextToSpeech.ERROR) {
                _isSpeaking.value = false
                audioSessionManager?.abandonAudioFocus()
                _ttsError.value = "TTS engine rejected playback request."
            }
        } catch (e: Exception) {
            _isSpeaking.value = false
            audioSessionManager?.abandonAudioFocus()
            _ttsError.value = "TTS playback error: ${e.message}"
        }
    }

    override fun pauseSpeaking() {
        if (!_isSpeaking.value && pendingChunks.isEmpty()) return
        pausedAtChunkIndex = currentChunkIndex
        _isPaused.value = true
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
        audioSessionManager?.abandonAudioFocus()
    }

    override fun resumeSpeaking(): Boolean {
        val resumeIndex = pausedAtChunkIndex ?: return false
        if (!_isReady.value || tts == null || resumeIndex >= pendingChunks.size) {
            _isPaused.value = false
            pausedAtChunkIndex = null
            return false
        }
        _isPaused.value = false
        pausedAtChunkIndex = null
        audioSessionManager?.requestAudioFocusForPlayback()
        speakChunkAt(resumeIndex)
        return true
    }

    override fun replayLastSpoken(): Boolean {
        val text = _lastSpokenText.value
        if (text.isNullOrBlank()) return false
        speak(text, "replay_${System.currentTimeMillis()}")
        return true
    }

    override fun stop() {
        stopSpeaking()
    }

    override fun stopSpeaking() {
        _isPaused.value = false
        pausedAtChunkIndex = null
        pendingChunks = emptyList()
        currentChunkIndex = 0
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
        audioSessionManager?.abandonAudioFocus()
    }

    override fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.5f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    override fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _speechPitch.value = clamped
        tts?.setPitch(clamped)
    }

    override fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun clearError() {
        _ttsError.value = null
    }

    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
        audioSessionManager?.abandonAudioFocus()
    }
}
