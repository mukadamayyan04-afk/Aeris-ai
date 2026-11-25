package com.example.voice

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import java.io.File

data class LocalSpeechCapabilityReport(
    val isAndroidSttAvailable: Boolean,
    val isAndroidOnDeviceSttAvailable: Boolean,
    val isSherpaNativeLibPresent: Boolean,
    val isSherpaKwsModelPresent: Boolean,
    val isSherpaSttModelPresent: Boolean,
    val isVoskModelPresent: Boolean,
    val kwsModelDirectory: String,
    val sttModelDirectory: String,
    val missingKwsReason: String?
)

/**
 * Inspects actual on-device native libraries and local model files without faking availability.
 */
object LocalSpeechModelDetector {

    fun inspect(context: Context): LocalSpeechCapabilityReport {
        val voiceModelsRoot = File(context.filesDir, "voice_models").apply { mkdirs() }
        val kwsDir = File(voiceModelsRoot, "kws").apply { mkdirs() }
        val sttDir = File(voiceModelsRoot, "stt").apply { mkdirs() }

        val androidSttAvailable = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }

        val androidOnDeviceSttAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }

        val sherpaNativePresent = try {
            System.loadLibrary("sherpa-onnx-jni")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        } catch (_: Throwable) {
            false
        }

        val kwsFiles = kwsDir.listFiles()?.map { it.name.lowercase() }?.toSet() ?: emptySet()
        val hasKeywordsTxt = kwsFiles.contains("keywords.txt")
        val hasTokensTxt = kwsFiles.contains("tokens.txt")
        val hasOnnxFile = kwsFiles.any { it.endsWith(".onnx") }
        val sherpaKwsModelPresent = hasKeywordsTxt && hasTokensTxt && hasOnnxFile

        val sttFiles = sttDir.listFiles()?.map { it.name.lowercase() }?.toSet() ?: emptySet()
        val sherpaSttModelPresent = sttFiles.any { it.endsWith(".onnx") } && sttFiles.contains("tokens.txt")
        val voskModelPresent = File(sttDir, "vosk-model").let { it.exists() && it.isDirectory && (it.listFiles()?.isNotEmpty() == true) }

        val missingReason = when {
            !sherpaNativePresent -> "Missing native library: libsherpa-onnx-jni.so (not bundled in APK)"
            !sherpaKwsModelPresent -> "Missing KWS model files (encoder/decoder/joiner .onnx, tokens.txt, keywords.txt) in ${kwsDir.absolutePath}"
            else -> null
        }

        return LocalSpeechCapabilityReport(
            isAndroidSttAvailable = androidSttAvailable,
            isAndroidOnDeviceSttAvailable = androidOnDeviceSttAvailable,
            isSherpaNativeLibPresent = sherpaNativePresent,
            isSherpaKwsModelPresent = sherpaKwsModelPresent,
            isSherpaSttModelPresent = sherpaSttModelPresent,
            isVoskModelPresent = voskModelPresent,
            kwsModelDirectory = kwsDir.absolutePath,
            sttModelDirectory = sttDir.absolutePath,
            missingKwsReason = missingReason
        )
    }
}
