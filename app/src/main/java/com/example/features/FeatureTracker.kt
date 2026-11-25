package com.example.features

import com.example.domain.model.FeatureItem
import com.example.domain.model.FeatureStatus

object FeatureTracker {

    val features: List<FeatureItem> = listOf(
        FeatureItem(
            id = "F-01-IDENTITY",
            name = "Project Identity & Packaging",
            description = "AERIS AI native Android foundation, API 26+ target, applicationId com.aeris.ai, Redmi 13 5G optimized.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("AndroidManifest.xml", "build.gradle.kts", "settings.gradle.kts"),
            dependencies = listOf("Android SDK 36", "Kotlin 2.2.10"),
            testStatus = "Verified via manifest & build config",
            knownLimitations = "Targets arm64-v8a Android 8.0+ devices."
        ),
        FeatureItem(
            id = "F-02-AI-BRAIN-11",
            name = "11-Engine Independent AI Brain",
            description = "Personality, Reasoning/Planning, Multi-Model Router, Provider Manager, Health/Quota Manager, Prompt Creation, Tool Execution, Continuity, Memory, Feedback Preferences & Image Manager.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("AerisAiEngine.kt", "MultiModelRouter.kt", "ProviderIntegrationManager.kt", "GeminiFirebaseProviderAdapter.kt", "OpenAiCompatibleProviderAdapter.kt"),
            dependencies = listOf("Firebase AI Logic + App Check", "OkHttp SSE", "Coroutines"),
            testStatus = "Free-Only Mode default, auto-switch, SSE parser, and offline fallback tested",
            knownLimitations = "Cloud inference requires connectivity; free tiers enforce external quotas."
        ),
        FeatureItem(
            id = "F-03-FREE-ONLY-POLICY",
            name = "Free-Only Default Routing Policy",
            description = "Free-Only Mode enabled by default; paid/billing models (e.g. Gemini 4 Argon / Nano Banana Blaze) remain blocked until user explicitly opts in.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("MultiModelRouter.kt", "SettingsRepository.kt"),
            dependencies = listOf("DataStore Preferences"),
            testStatus = "Unit-tested blocking of billing models while Free-Only Mode is ON",
            knownLimitations = "Google Gemini free tier still subject to Firebase Spark/App Check quotas."
        ),
        FeatureItem(
            id = "F-04-DUAL-MODES",
            name = "Dual Normal & Landscape Work Mode",
            description = "Portrait HUD and true Landscape Work Mode sharing live AI state, health readout, summary, tasks, and voice triggers.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("HomeScreen.kt", "WorkModeScreen.kt", "MainActivity.kt"),
            dependencies = listOf("Jetpack Compose M3", "ActivityInfo Orientation"),
            testStatus = "Orientation switching and testTags verified in Robolectric",
            knownLimitations = "Multi-window desktop mode may restrict programmatic orientation lock."
        ),
        FeatureItem(
            id = "F-05-GEMINI-FIREBASE",
            name = "Google Gemini via Firebase AI Logic",
            description = "Supported Gemini 3.8 Flash & Gemini 3.5 Flash-Lite streaming + vision via App Check without hardcoded API keys.",
            status = FeatureStatus.INTEGRATED,
            sourceFiles = listOf("GeminiFirebaseProviderAdapter.kt", "AppCheckInitializer.kt"),
            dependencies = listOf("com.google.firebase:firebase-ai", "Firebase App Check"),
            testStatus = "App Check wiring + lazy model calls + error handling implemented",
            knownLimitations = "Live server inference requires network + valid App Check token on device."
        ),
        FeatureItem(
            id = "F-06-OPENAI-QWEN-GGUF",
            name = "OpenRouter, Groq, HF, OpenAI-Compat, GPT-OSS, Qwen & GGUF",
            description = "Real pooled OkHttp SSE streaming & /models discovery for OpenRouter, Groq, Hugging Face, OpenAI-compat, GPT-OSS, and Qwen with KeyStore-encrypted tokens plus GGUF management.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("OpenAiCompatibleProviderAdapter.kt", "CredentialVault.kt", "PerformanceTelemetryTracker.kt", "ModelManagerScreen.kt"),
            dependencies = listOf("OkHttp 4.10.0", "Android KeyStore AES-GCM", "OpenDocument SAF"),
            testStatus = "SSE streaming, /models discovery, 401/429/503 status mapping, TTFT telemetry, and GGUF manager verified",
            knownLimitations = "On-device GGUF C++ token inference requires bundling libllama.so."
        ),
        FeatureItem(
            id = "F-07-VOICE-FEMALE-SYSTEM",
            name = "Female Holographic Voice & Speech System",
            description = "Prioritizes installed Female TTS voice by default, Push-to-Talk/Tap STT, installed engine switcher, and Room voice task logs.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("TtsEngine.kt", "SttEngine.kt", "VoiceSelectionScreen.kt", "VoiceSettingsScreen.kt"),
            dependencies = listOf("Android TextToSpeech", "SpeechRecognizer", "AudioManager"),
            testStatus = "Voice gender classification, permission guards, and audio focus verified",
            knownLimitations = "Voice gender metadata depends on the installed device TTS engine."
        ),
        FeatureItem(
            id = "F-08-MEMORY-ROOM-V3",
            name = "Persistent Room v3 Memory, Continuity & Feedback",
            description = "Non-destructive Room v1->v2->v3 database for conversations, continuity summaries, project tags, sensitive-memory confirmation, reminders, and user feedback rules.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("AerisDatabase.kt", "Entities.kt", "Daos.kt", "MemoryScreen.kt"),
            dependencies = listOf("Room 2.7.0", "KSP"),
            testStatus = "Migrations 1->2 and 2->3 verified via Robolectric SQLite tests",
            knownLimitations = "Data remains device-local unless exported."
        ),
        FeatureItem(
            id = "F-09-DEVICE-ACTIONS",
            name = "Permission-Based Android Device Control",
            description = "Flashlight hardware toggle, Clock Timers/Alarms, Calendar/WorkManager Reminders, Installed App Launcher, Settings deep-links, Volume & Haptics with sensitive confirmation.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("DeviceActionHandler.kt", "ReminderWorker.kt"),
            dependencies = listOf("CameraManager", "AlarmClock", "CalendarContract", "WorkManager"),
            testStatus = "Intent dispatch and confirmation flow integrated",
            knownLimitations = "Requires device hardware flash / installed Clock & Calendar apps."
        ),
        FeatureItem(
            id = "F-10-WAKE-WORD-SERVICE",
            name = "Continuous Opt-In Wake Word ('Hey AERIS' / 'Wake up AERIS')",
            description = "Foreground microphone service with privacy & battery guidance, voice-loop trigger, and honest KWS dependency inspection.",
            status = FeatureStatus.DEVELOPED,
            sourceFiles = listOf("AerisWakeWordService.kt", "WakeWordManager.kt", "LocalSpeechModelDetector.kt"),
            dependencies = listOf("ForegroundService Microphone", "sherpa-onnx KWS (External)"),
            testStatus = "Permission guard, battery notice, and missing-KWS reporting verified",
            knownLimitations = "Continuous offline keyword spotting requires external libsherpa-onnx-jni.so + .onnx weights."
        ),
        FeatureItem(
            id = "F-11-HOLO-AVATAR-6STATE",
            name = "6-State Female Holographic Avatar",
            description = "IDLE, LISTENING, THINKING, RESPONDING, SPEAKING (RMS equalizer bar), OFFLINE, ERROR states with lifecycle animation pause.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("HolographicAvatar.kt", "HomeScreen.kt"),
            dependencies = listOf("Compose Canvas", "LifecycleEventObserver"),
            testStatus = "All states rendered cleanly in Compose tests",
            knownLimitations = "Uses battery-friendly vector rings & bundled avatar artwork."
        ),
        FeatureItem(
            id = "F-12-CHAT-PRO",
            name = "Streaming Chat, Markdown, Code Copy, Retry & Attachments",
            description = "File attachment extraction into prompt context, JetBrains Mono code block copy, Stop generation, Regenerate, Retry, Thumbs feedback, and Model Handoff Banner.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("ChatScreen.kt", "MarkdownText.kt", "ChatHistoryScreen.kt"),
            dependencies = listOf("OpenDocument", "ClipboardManager", "Room"),
            testStatus = "Composer IME padding, attachment preview, feedback, and markdown tested",
            knownLimitations = "Binary PDFs/images require text extraction or multimodal Vision studio."
        ),
        FeatureItem(
            id = "F-13-IMAGE-STUDIO",
            name = "Image Generation Studio (Gemini Nano Banana 2.1)",
            description = "Dedicated Image Studio with Prompt Enhancer, 30–60s shimmer loading, real error reporting, MediaStore Gallery save, and FileProvider share.",
            status = FeatureStatus.INTEGRATED,
            sourceFiles = listOf("ImageGenerationManager.kt", "ImageStudioScreen.kt"),
            dependencies = listOf("Firebase.ai (gemini-nano-banana-2.1)", "MediaStore", "FileProvider"),
            testStatus = "Route, state machine, and FileProvider configured",
            knownLimitations = "Gemini image models require a Firebase Blaze plan or custom /images/generations endpoint."
        ),
        FeatureItem(
            id = "F-14-GAME-COACH",
            name = "Consented Game Coach (MediaProjection + Vision)",
            description = "Android 14+ MediaProjection.Callback + throttled VirtualDisplay sampling analyzed by Gemini 3.8 Flash vision for fair-play callouts.",
            status = FeatureStatus.INTEGRATED,
            sourceFiles = listOf("GameCoachService.kt", "GameCoachScreen.kt"),
            dependencies = listOf("MediaProjectionManager", "ImageReader", "Gemini Vision"),
            testStatus = "Foreground service contract & UI controls verified",
            knownLimitations = "Requires explicit user screen-capture consent per session."
        ),
        FeatureItem(
            id = "F-15-NOTIF-ANNOUNCER",
            name = "Notification Listener Announcer & Task Center",
            description = "NotificationListenerService integration with per-app allow-list, OTP/banking redaction privacy filter, quick disable, plus background Task Retry controls.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("AerisNotificationListener.kt", "NotificationCenterScreen.kt", "AerisTaskManager.kt"),
            dependencies = listOf("NotificationListenerService", "Coroutines"),
            testStatus = "Sensitive OTP/bank redaction formatter unit-tested",
            knownLimitations = "Requires manual toggle in Android Notification Access Settings."
        ),
        FeatureItem(
            id = "F-16-PERMISSION-MGR",
            name = "Interactive Stage 8 Permission Manager",
            description = "Classifies GRANTED, DENIED, RESTRICTED, and NOT_REQUESTED states with runtime launchers and Android Settings shortcuts.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("PermissionStatusInspector.kt", "SettingsScreen.kt"),
            dependencies = listOf("PackageManager", "PowerManager", "ActivityResultContracts"),
            testStatus = "Permission state inspection tested via Robolectric",
            knownLimitations = "Install-time permissions (INTERNET) are displayed read-only."
        ),
        FeatureItem(
            id = "F-17-WEB-ASSISTANT",
            name = "Web Assistant & Guided Gmail Selector",
            description = "Guided HTTPS browser asking 'Sir ji, kis Gmail account se login karna hai?' with zero password capture.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("WebAssistantScreen.kt"),
            dependencies = listOf("Android WebView", "AccountManager"),
            testStatus = "HTTPS enforcement & zero-credential handling verified",
            knownLimitations = "Uses device Google accounts available to Android AccountManager."
        ),
        FeatureItem(
            id = "F-18-SEARCH-100PLUS",
            name = "112-Capability Searchable Feature Hub",
            description = "Categorized A-H capability catalog with usage tracking, real routing, and honest limitations.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("FeatureCatalog.kt", "FeaturesScreen.kt"),
            dependencies = listOf("DataStore Preferences", "Jetpack Compose"),
            testStatus = "100+ feature search & category filtering verified",
            knownLimitations = "Cloud AI & external hardware capabilities depend on device/provider availability."
        ),
        FeatureItem(
            id = "F-19-FLOATING-OVERLAY",
            name = "User-Controlled Floating Assistant Orb",
            description = "Optional draggable WindowManager orb gated by SYSTEM_ALERT_WINDOW with quick Chat/Voice links and in-app fallback.",
            status = FeatureStatus.IMPLEMENTED,
            sourceFiles = listOf("FloatingAssistantService.kt", "PermissionManagerScreen.kt"),
            dependencies = listOf("SYSTEM_ALERT_WINDOW", "WindowManager"),
            testStatus = "Overlay permission check and service lifecycle verified",
            knownLimitations = "Requires explicit 'Display over other apps' approval on device."
        ),
        FeatureItem(
            id = "F-20-BOOT-STANDBY",
            name = "Opt-In Boot Standby Reminder",
            description = "Android 14+-safe BOOT_COMPLETED receiver that posts a quiet tap-to-resume standby notification only when user opts in.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("BootCompletedReceiver.kt", "AerisNotificationManager.kt"),
            dependencies = listOf("RECEIVE_BOOT_COMPLETED", "NotificationManager"),
            testStatus = "Manifest registration & opt-in DataStore gates verified",
            knownLimitations = "Does not auto-start microphone foreground services on boot per Android 14+ policy."
        ),
        FeatureItem(
            id = "F-21-MOBILE-SHELL-V2",
            name = "Drawer, Library, Customize & Privacy Center",
            description = "ChatGPT-inspired mobile ergonomics on pure AERIS cyan/purple theme: Side Drawer, Rounded Composer, Library archive, Custom Instructions, and JSON backup.",
            status = FeatureStatus.COMPLETED,
            sourceFiles = listOf("AerisSideDrawer.kt", "AerisRoundedComposer.kt", "LibraryScreen.kt", "CustomizeScreen.kt", "PrivacyCenterScreen.kt"),
            dependencies = listOf("ModalNavigationDrawer", "PickVisualMedia", "Room SQLite"),
            testStatus = "Drawer, Composer, Library, Customize & Privacy UI verified",
            knownLimitations = "JSON backups are stored via user-selected Android Storage Access Framework location."
        )
    )

    fun getSummary(): String {
        val total = features.size
        val integrated = features.count { it.status == FeatureStatus.INTEGRATED || it.status == FeatureStatus.COMPLETED || it.status == FeatureStatus.IMPLEMENTED }
        val developed = features.count { it.status == FeatureStatus.DEVELOPED }
        val planned = features.count { it.status == FeatureStatus.PLANNED }
        val catalogCount = FeatureCatalog.allFeatures.size
        return "$integrated Ready ($catalogCount Searchable Capabilities), $developed Developed, $planned Planned out of $total Modules."
    }
}
