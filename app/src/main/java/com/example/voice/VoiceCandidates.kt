package com.example.voice

import android.content.Context

/**
 * Local offline voice engine registry and live capability inspection.
 * Reports exact runtime status without faking unavailable native dependencies.
 */
data class VoiceEngineInfo(
    val id: String,
    val name: String,
    val type: VoiceEngineType,
    val isAvailableLocally: Boolean,
    val license: String,
    val supportedModels: List<String>,
    val notes: String
)

enum class VoiceEngineType {
    STT, TTS, WAKEWORD
}

object VoiceEngineRegistry {

    fun getAvailableEngines(context: Context? = null): List<VoiceEngineInfo> {
        val report = context?.let { LocalSpeechModelDetector.inspect(it) }
        val androidSttAvailable = report?.isAndroidSttAvailable ?: true
        val sherpaSttAvailable = (report?.isSherpaNativeLibPresent == true && report.isSherpaSttModelPresent)
        val sherpaKwsAvailable = (report?.isSherpaNativeLibPresent == true && report.isSherpaKwsModelPresent)
        val voskAvailable = report?.isVoskModelPresent == true

        return listOf(
            VoiceEngineInfo(
                id = "android-builtin-tts",
                name = "Android System TTS (Installed Engines)",
                type = VoiceEngineType.TTS,
                isAvailableLocally = true,
                license = "AOSP / Installed TTS Engine",
                supportedModels = listOf("Installed System Voice Packs"),
                notes = "Active local TTS synthesis supporting engine/voice switching, speed & pitch."
            ),
            VoiceEngineInfo(
                id = "android-builtin-stt",
                name = "Android SpeechRecognizer (Offline Preferred)",
                type = VoiceEngineType.STT,
                isAvailableLocally = androidSttAvailable,
                license = "AOSP / Device Speech Service",
                supportedModels = listOf("On-Device Offline Language Packs"),
                notes = if (report?.isAndroidOnDeviceSttAvailable == true) {
                    "Android on-device SpeechRecognizer available on this device."
                } else {
                    "Uses EXTRA_PREFER_OFFLINE; offline pack availability depends on device speech provider."
                }
            ),
            VoiceEngineInfo(
                id = "sherpa-onnx-stt",
                name = "Sherpa-ONNX Local Streaming STT",
                type = VoiceEngineType.STT,
                isAvailableLocally = sherpaSttAvailable,
                license = "Apache 2.0",
                supportedModels = listOf("sherpa-onnx-whisper-tiny", "sherpa-onnx-streaming-zipformer"),
                notes = if (sherpaSttAvailable) {
                    "libsherpa-onnx-jni.so and local STT model detected."
                } else {
                    "Not bundled: requires libsherpa-onnx-jni.so and ONNX STT files in voice_models/stt/."
                }
            ),
            VoiceEngineInfo(
                id = "sherpa-onnx-kws",
                name = "Sherpa-ONNX Local Wake-Word Spotting",
                type = VoiceEngineType.WAKEWORD,
                isAvailableLocally = sherpaKwsAvailable,
                license = "Apache 2.0",
                supportedModels = listOf("sherpa-onnx-kws-zipformer-gigaspeech"),
                notes = report?.missingKwsReason ?: "Local KWS model active."
            ),
            VoiceEngineInfo(
                id = "vosk-offline-stt",
                name = "Vosk Offline Kaldi STT",
                type = VoiceEngineType.STT,
                isAvailableLocally = voskAvailable,
                license = "Apache 2.0",
                supportedModels = listOf("vosk-model-small-en-us", "vosk-model-small-en-in"),
                notes = if (voskAvailable) {
                    "Local Vosk model detected."
                } else {
                    "Not bundled: requires Vosk AAR/native libs and model folder in voice_models/stt/vosk-model/."
                }
            )
        )
    }
}
