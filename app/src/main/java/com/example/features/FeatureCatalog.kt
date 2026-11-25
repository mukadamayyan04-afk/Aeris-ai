package com.example.features

import com.example.domain.model.FeatureStatus

enum class FeatureCategory(val code: String, val title: String, val shortLabel: String) {
    AI_INTELLIGENCE("A", "A. AI Intelligence", "AI Brain"),
    CREATIVE_STUDIO("B", "B. AI Creative Studio", "Creative"),
    VOICE_AUDIO("C", "C. Voice & Audio Studio", "Voice"),
    PRODUCTIVITY("D", "D. Productivity", "Productivity"),
    DEVICE_CONTROL("E", "E. Android Device Control", "Device"),
    GAME_COACH("F", "F. Game Coach", "Game Coach"),
    SYSTEM_MANAGEMENT("G", "G. System & AI Management", "System"),
    ADDITIONAL("H", "H. Additional Features", "Tools")
}

enum class FeatureRouteTarget {
    HOME,
    CHAT,
    CHAT_WITH_PROMPT,
    VOICE_ASSISTANT,
    VOICE_SELECTION,
    VOICE_SETTINGS,
    IMAGE_STUDIO,
    GAME_COACH,
    WORK_MODE,
    MODELS,
    AI_PROVIDERS,
    MEMORY,
    PROJECTS,
    NOTIFICATIONS,
    WEB_ASSISTANT,
    PERMISSIONS,
    PRIVACY_CENTER,
    LIBRARY,
    CUSTOMIZE,
    SETTINGS,
    TRACKER,
    FLOATING_ASSISTANT,
    TOGGLE_FLASHLIGHT,
    CHECK_BATTERY,
    CHECK_STORAGE,
    CHECK_DATETIME,
    SET_TIMER_60S,
    SET_ALARM_7AM
}

data class CatalogFeature(
    val id: String,
    val title: String,
    val description: String,
    val category: FeatureCategory,
    val status: FeatureStatus,
    val routeTarget: FeatureRouteTarget,
    val starterPrompt: String? = null,
    val permissionRequired: String? = null,
    val honestLimitation: String = "Runs inside AERIS native stack; cloud items depend on configured providers & connectivity."
)

/**
 * Stage 7 Searchable Catalog of 112 distinct AERIS capabilities across Categories A-H.
 * Every item has an honest status and connects to a real screen, prompt, or Android action.
 */
object FeatureCatalog {

    val allFeatures: List<CatalogFeature> = buildCatalog()

    fun search(query: String, category: FeatureCategory? = null): List<CatalogFeature> {
        val q = query.trim()
        return allFeatures.filter { item ->
            val catOk = category == null || item.category == category
            val qOk = q.isBlank() ||
                item.title.contains(q, ignoreCase = true) ||
                item.description.contains(q, ignoreCase = true) ||
                item.category.title.contains(q, ignoreCase = true) ||
                item.id.contains(q, ignoreCase = true)
            catOk && qOk
        }
    }

    private fun ai(
        num: Int,
        title: String,
        desc: String,
        prompt: String,
        status: FeatureStatus = FeatureStatus.COMPLETED,
        target: FeatureRouteTarget = FeatureRouteTarget.CHAT_WITH_PROMPT,
        limitation: String = "Works online with configured AI providers or offline standby rules."
    ) = CatalogFeature("A-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.AI_INTELLIGENCE, status, target, prompt, null, limitation)

    private fun creative(
        num: Int,
        title: String,
        desc: String,
        prompt: String? = null,
        target: FeatureRouteTarget = FeatureRouteTarget.IMAGE_STUDIO,
        status: FeatureStatus = FeatureStatus.INTEGRATED,
        limitation: String = "Image generation requires Firebase Blaze opt-in or OpenAI-compatible /images/generations endpoint."
    ) = CatalogFeature("B-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.CREATIVE_STUDIO, status, target, prompt, null, limitation)

    private fun voice(
        num: Int,
        title: String,
        desc: String,
        target: FeatureRouteTarget = FeatureRouteTarget.VOICE_ASSISTANT,
        status: FeatureStatus = FeatureStatus.COMPLETED,
        perm: String? = "android.permission.RECORD_AUDIO",
        limitation: String = "Uses installed Android STT/TTS engines; offline STT requires downloaded Android language packs."
    ) = CatalogFeature("C-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.VOICE_AUDIO, status, target, null, perm, limitation)

    private fun prod(
        num: Int,
        title: String,
        desc: String,
        target: FeatureRouteTarget = FeatureRouteTarget.CHAT_WITH_PROMPT,
        prompt: String? = null,
        status: FeatureStatus = FeatureStatus.COMPLETED
    ) = CatalogFeature("D-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.PRODUCTIVITY, status, target, prompt, null, "Stored locally in Room SQLite v4.")

    private fun device(
        num: Int,
        title: String,
        desc: String,
        target: FeatureRouteTarget,
        perm: String? = null,
        status: FeatureStatus = FeatureStatus.COMPLETED,
        limitation: String = "Requires supported Android hardware/intent handler and user confirmation for sensitive actions."
    ) = CatalogFeature("E-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.DEVICE_CONTROL, status, target, null, perm, limitation)

    private fun coach(
        num: Int,
        title: String,
        desc: String,
        prompt: String? = null,
        target: FeatureRouteTarget = FeatureRouteTarget.GAME_COACH,
        status: FeatureStatus = FeatureStatus.INTEGRATED
    ) = CatalogFeature("F-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.GAME_COACH, status, target, prompt, "MediaProjection (Per-Session Consent)", "Live frame analysis requires per-session MediaProjection consent & active vision provider.")

    private fun sys(
        num: Int,
        title: String,
        desc: String,
        target: FeatureRouteTarget,
        status: FeatureStatus = FeatureStatus.COMPLETED,
        limitation: String = "Local Android telemetry & KeyStore encryption."
    ) = CatalogFeature("G-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.SYSTEM_MANAGEMENT, status, target, null, null, limitation)

    private fun extra(
        num: Int,
        title: String,
        desc: String,
        target: FeatureRouteTarget,
        prompt: String? = null,
        status: FeatureStatus = FeatureStatus.COMPLETED,
        limitation: String = "Runs in-app without ads or external telemetry."
    ) = CatalogFeature("H-${num.toString().padStart(2, '0')}", title, desc, FeatureCategory.ADDITIONAL, status, target, prompt, null, limitation)

    private fun buildCatalog(): List<CatalogFeature> = listOf(
        // A. AI Intelligence (18 features)
        ai(1, "Multi-Model AI Chat", "Stream answers via Gemini, OpenRouter, Groq, HF, Qwen, GPT-OSS, or local core.", "Hello AERIS, help me brainstorm cleanly.", target = FeatureRouteTarget.CHAT),
        ai(2, "Step-by-Step Reasoning", "Structured logical breakdown using AERIS Reasoning & Planning Engine.", "Break this problem down step by step: "),
        ai(3, "Kotlin & Compose Code Helper", "Write, refactor, and debug Jetpack Compose & Kotlin coroutines.", "Help me write clean Jetpack Compose code for: "),
        ai(4, "Python & Script Debugger", "Identify tracebacks, off-by-one bugs, and clean fixes.", "Debug this script step by step: "),
        ai(5, "SQL & Room Query Architect", "Design Room DAO queries, indexes, and SQLite migrations.", "Design a Room SQLite query and migration for: "),
        ai(6, "Roman Hinglish Companion", "Natural Roman-script Hinglish + English replies with zero Devanagari.", "Sir ji, please reply in friendly Roman Hinglish about: "),
        ai(7, "Text Summarizer", "Condense long articles, logs, or transcripts into bullet points.", "Summarize the key points concisely:\n"),
        ai(8, "Document & Log Explainer", "Attach .txt/.log/.json files via + button for grounded Q&A.", "Explain the attached file and list any issues found."),
        ai(9, "Study Tutor & Flashcards", "Generate clear explanations, mnemonics, and practice questions.", "Teach me this topic like a patient tutor with a quick quiz: "),
        ai(10, "Translation Studio", "Translate clearly between English, Roman Hinglish, Spanish, French, etc.", "Translate accurately while preserving tone: "),
        ai(11, "Compare Two Ideas / Tradeoffs", "Structured pros/cons matrix before making engineering or product calls.", "Compare pros, cons, and risks between: "),
        ai(12, "Grammar & Clarity Polisher", "Rewrite rough drafts into crisp, polite, or executive English.", "Polish this message for clarity without losing my voice:\n"),
        ai(13, "SQL/JSON Schema Validator", "Spot malformed fields, edge cases, and missing null checks.", "Inspect this JSON/schema for errors:\n"),
        ai(14, "Technical Interview Prep", "Mock coding & system design Q&A tailored to Android or backend.", "Ask me one Android senior engineer interview question and evaluate my answer."),
        ai(15, "ELI12 Concept Simplifier", "Explain complex math, physics, or CS topics with simple analogies.", "Explain this concept in simple everyday analogies: "),
        ai(16, "Email & Reply Drafter", "Draft respectful client, professor, or team emails quickly.", "Draft a concise, polite email about: "),
        ai(17, "Offline Core Fallback", "Guaranteed deterministic local guidance when offline or rate-limited.", "Check offline readiness and local models", status = FeatureStatus.COMPLETED, target = FeatureRouteTarget.MODELS, limitation = "Local offline rules run anytime; neural GGUF token inference requires downloaded weights + runtime."),
        ai(18, "Learned Feedback Adaptation", "Thumbs up/down rules adapt AERIS prompt directives cleanly.", "Adjust personality & rules", target = FeatureRouteTarget.CUSTOMIZE),

        // B. AI Creative Studio (14 features)
        creative(1, "Nano Banana Image Studio", "Generate custom artwork with Gemini Nano Banana 2.1 or OpenAI endpoint."),
        creative(2, "AI Prompt Enhancer", "Expand a short 4-word idea into a detailed lighting & composition prompt."),
        creative(3, "Square 1:1 Avatar Maker", "Synthesize clean 1:1 profile and holographic concept icons."),
        creative(4, "Widescreen 16:9 Wallpaper", "Generate cyberpunk & landscape banner compositions."),
        creative(5, "Portrait 9:16 Poster Art", "Vertical phone wallpapers & poster layouts."),
        creative(6, "Save to Pictures/AERIS", "Export generated PNG artwork directly into Android MediaStore Gallery."),
        creative(7, "Native Image Share Sheet", "Share studio creations securely via Android FileProvider."),
        creative(8, "UI/UX Wireframe Brainstormer", "Sketch mobile screen layouts & copy structures in Chat.", "Design a clean mobile screen wireframe and copy for: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED, limitation = "Text wireframe layout."),
        creative(9, "Youtube / Video Script Hook", "Generate engaging intros, scene beats, and voiceover scripts.", "Write a tight 60-second video script with hooks on: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        creative(10, "Social Caption Generator", "Create clean, non-cringe captions and hashtags.", "Write 5 concise captions for: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        creative(11, "Story & Worldbuilding Co-Writer", "Plot characters, lore rules, and interactive scenes.", "Help me create a sci-fi story scene featuring: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        creative(12, "Image Connection Tester", "Verify live availability of Gemini Blaze / image endpoints cleanly."),
        creative(13, "Brand Naming Workshop", "Brainstorm memorable product names and taglines.", "Suggest 10 crisp product names and taglines for: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        creative(14, "Artwork Library Archive", "Browse saved & referenced studio files in unified Library.", target = FeatureRouteTarget.LIBRARY, status = FeatureStatus.COMPLETED),

        // C. Voice & Audio Studio (14 features)
        voice(1, "Full-Screen Holographic Voice HUD", "Animated orbital voice core with RMS equalizer and state rings."),
        voice(2, "Push-to-Talk & Tap-to-Talk", "Switch between hold-to-speak and tap toggle seamlessly.", FeatureRouteTarget.VOICE_SETTINGS),
        voice(3, "Live Partial Transcripts", "See speech-to-text captions appear in real time while speaking."),
        voice(4, "Edit Transcript Before Send", "Inspect and edit voice transcripts in the HUD before sending to AI."),
        voice(5, "Female Neural Voice Priority", "Automatically selects clear installed female TTS voices first.", FeatureRouteTarget.VOICE_SELECTION, perm = null),
        voice(6, "Multi-Voice & Language Picker", "Switch installed voices across en-IN, en-US, and system TTS packs.", FeatureRouteTarget.VOICE_SELECTION, perm = null),
        voice(7, "Installed TTS Engine Switcher", "Switch between Google TTS, Samsung TTS, or third-party engines.", FeatureRouteTarget.VOICE_SETTINGS, perm = null),
        voice(8, "Chunked TTS Pause & Resume", "Pause mid-sentence and resume cleanly from the next speech chunk."),
        voice(9, "Speech Rate & Pitch Calibration", "Tune speaking speed (0.5x–2.0x) and pitch in Voice Settings.", FeatureRouteTarget.VOICE_SETTINGS, perm = null),
        voice(10, "Auto-Play Chat Replies", "Optional hands-free vocal readout of incoming chat messages.", FeatureRouteTarget.VOICE_SETTINGS, perm = null),
        voice(11, "Voice Mode Orientation Commands", "Say 'AERIS, switch to Work Mode' or 'Exit Work Mode' hands-free."),
        voice(12, "Audio Focus Ducking", "Respectfully pauses/ducks background media during TTS or STT.", perm = null),
        voice(13, "Opt-In Wake Word Service", "Foreground microphone keyword listener ('Hey AERIS') with Stop action.", FeatureRouteTarget.VOICE_SETTINGS, FeatureStatus.DEVELOPED, limitation = "Requires external libsherpa-onnx-jni.so + KWS model weights for offline spotting."),
        voice(14, "Room Voice Task Audit Log", "Inspect recognized voice commands, durations, and errors in Voice Settings.", FeatureRouteTarget.VOICE_SETTINGS, perm = null),
        voice(15, "Voice Assistant", "Open the full-screen AERIS voice assistant with push-to-talk, editable transcripts, and spoken responses."),

        // D. Productivity (14 features)
        prod(1, "Project Workspaces", "Organize scoped chats, notes, task checklists, and files per project.", FeatureRouteTarget.PROJECTS),
        prod(2, "Project Task Checklists", "Track real completion % automatically across project tasks.", FeatureRouteTarget.PROJECTS),
        prod(3, "Project Notes Vault", "Store structured technical specs and quick notes per project.", FeatureRouteTarget.PROJECTS),
        prod(4, "Scoped Project File Context", "Attach text/code files directly inside project workspaces.", FeatureRouteTarget.PROJECTS),
        prod(5, "Persistent Long-Term Memory", "Save preferences, device facts, and context across chats in Room.", FeatureRouteTarget.MEMORY),
        prod(6, "Sensitive Memory Guard", "Flags potential tokens/credentials and asks confirmation before storing.", FeatureRouteTarget.MEMORY),
        prod(7, "Rolling Conversation Continuity", "Maintains context summaries cleanly even when switching AI models.", FeatureRouteTarget.CHAT),
        prod(8, "Chat History Search", "Search across past conversations and messages stored locally.", FeatureRouteTarget.LIBRARY),
        prod(9, "Unified Library Archive", "Browse Chats, Project Files, and Image generations in one place.", FeatureRouteTarget.LIBRARY),
        prod(10, "Landscape Command Center", "Real multi-pane horizontal Work Mode for coding & multitasking.", FeatureRouteTarget.WORK_MODE),
        prod(11, "Meeting & Lecture Notes Cleaner", "Turn messy bullet notes into structured action items.", prompt = "Convert these rough notes into Summary, Decisions, and Action Items:\n"),
        prod(12, "Daily Focus Planner", "Time-block your study or build session cleanly.", prompt = "Help me plan a realistic 3-hour study/build schedule for: "),
        prod(13, "Markdown & One-Tap Code Copy", "Renders clean formatted code blocks with instant clipboard copy.", FeatureRouteTarget.CHAT),
        prod(14, "JSON Backup Exporter", "Export conversations, memories, reminders, and projects to JSON.", FeatureRouteTarget.PRIVACY_CENTER),

        // E. Android Device Control (16 features)
        device(1, "Hardware Flashlight Torch", "Instant CameraManager back-LED torch toggle from HUD, Work Mode, or Voice.", FeatureRouteTarget.TOGGLE_FLASHLIGHT, "android.hardware.camera.flash"),
        device(2, "Native Android Clock Timer", "Dispatches AlarmClock.ACTION_SET_TIMER directly to system Clock.", FeatureRouteTarget.SET_TIMER_60S, "com.android.alarm.permission.SET_ALARM"),
        device(3, "Native Android Alarm Creator", "Opens system alarm confirmation with preset hour/minute.", FeatureRouteTarget.SET_ALARM_7AM, "com.android.alarm.permission.SET_ALARM"),
        device(4, "Smart WorkManager + Calendar Reminders", "Creates reliable local reminder alerts & optional Calendar drafts.", FeatureRouteTarget.NOTIFICATIONS, "android.permission.POST_NOTIFICATIONS"),
        device(5, "Battery Level & Temperature", "Reads live BatteryManager percentage, charging state & temp.", FeatureRouteTarget.CHECK_BATTERY),
        device(6, "Internal Storage Inspector", "Checks live StatFs free/total gigabytes on internal storage.", FeatureRouteTarget.CHECK_STORAGE),
        device(7, "Local Date, Time & Timezone", "Reads exact system clock date, seconds, and active timezone.", FeatureRouteTarget.CHECK_DATETIME),
        device(8, "Installed App Launcher", "Queries launchable apps and opens chosen apps after confirmation.", FeatureRouteTarget.WORK_MODE),
        device(9, "Android Settings Deep-Links", "Opens Wi-Fi, Bluetooth, Battery, Overlay, Storage, or App Info safely.", FeatureRouteTarget.PERMISSIONS),
        device(10, "Media Volume Step Control", "Raises/lowers Android STREAM_MUSIC with native volume slider UI.", FeatureRouteTarget.WORK_MODE),
        device(11, "Haptic Feedback Pulse", "Tactile VibratorManager feedback on voice/composer triggers.", FeatureRouteTarget.CUSTOMIZE),
        device(12, "Floating Assistant Overlay Orb", "Optional draggable overlay orb for quick Chat/Voice access.", FeatureRouteTarget.FLOATING_ASSISTANT, "android.permission.SYSTEM_ALERT_WINDOW"),
        device(13, "Notification Listener Announcer", "Speaks notifications from user-selected apps via NotificationListenerService.", FeatureRouteTarget.NOTIFICATIONS, "BIND_NOTIFICATION_LISTENER_SERVICE"),
        device(14, "OTP & Banking Privacy Redactor", "Automatically hides sensitive codes & financial alerts from spoken announcements.", FeatureRouteTarget.NOTIFICATIONS),
        device(15, "Standby Boot Opt-In Notification", "Posts a compliant resume notification on boot if explicitly enabled.", FeatureRouteTarget.VOICE_SETTINGS, "RECEIVE_BOOT_COMPLETED"),
        device(16, "Action Lifecycle Safety Guard", "Tracks Requested/Permission/Confirm/Executing/Completed/Failed states.", FeatureRouteTarget.WORK_MODE),

        // F. Game Coach (10 features)
        coach(1, "Consented Screen Observer", "Android 14+ MediaProjection session with visible active notification."),
        coach(2, "One-Tap Stop Observation", "Immediately closes VirtualDisplay and stops projection on demand."),
        coach(3, "Live Gemini 3.8 Flash Callouts", "Opt-in visual strategy tips when screen sharing is active."),
        coach(4, "Local Frame Sampler Privacy", "Keeps sampled frames strictly off-cloud unless Live Coach toggle is ON."),
        coach(5, "Fair-Play Tactical Q&A", "Ask strategy, loadout, or map positioning questions cleanly.", prompt = "Give me fair-play strategy and positioning tips for: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        coach(6, "Aim & Sensitivity Math Helper", "Convert eDPI and sensitivity values between games.", prompt = "Help me calculate consistent sensitivity / eDPI for: ", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        coach(7, "FPS & Thermal Optimization Tips", "Tune Redmi 13 5G (Snapdragon 4 Gen 2) thermal settings.", prompt = "Best FPS vs battery settings for Snapdragon 4 Gen 2 on Android?", target = FeatureRouteTarget.CHAT_WITH_PROMPT, status = FeatureStatus.COMPLETED),
        coach(8, "Voice Strategy Mode", "Listen to tactical tips hands-free via Voice HUD.", target = FeatureRouteTarget.VOICE_ASSISTANT, status = FeatureStatus.COMPLETED),
        coach(9, "Build / Loadout Planner", "Save game loadouts or patch notes in Project Workspaces.", target = FeatureRouteTarget.PROJECTS, status = FeatureStatus.COMPLETED),
        coach(10, "Session Advice History Feed", "Review recent visual coaching callouts cleanly inside Game Coach."),
        coach(11, "Game Coach", "Open the consent-based gameplay coaching workspace and session controls."),

        // G. System & AI Management (14 features)
        sys(1, "AI Operating Mode Switcher", "Switch cleanly between Hybrid, Online-Only, and Local-Only AI modes.", FeatureRouteTarget.AI_PROVIDERS),
        sys(2, "Free-Only Policy Shield", "Default ON guard preventing accidental paid/billing model calls.", FeatureRouteTarget.MODELS),
        sys(3, "Multi-Provider Router & Failover", "Automatic health tracking + cooldown switching across 8 adapters.", FeatureRouteTarget.AI_PROVIDERS),
        sys(4, "KeyStore AES-GCM Credential Vault", "Hardware-backed encrypted API key storage; keys never shown in plaintext.", FeatureRouteTarget.AI_PROVIDERS),
        sys(5, "Live Provider Connection Tester", "Honest HTTP /models & completion probes before marking Connected.", FeatureRouteTarget.AI_PROVIDERS),
        sys(6, "Gemma 3 1B Q4 Download & Resume", "Recommended local GGUF download with .part resume and cancel.", FeatureRouteTarget.MODELS),
        sys(7, "SHA-256 Weight Verifier", "Cryptographic digest check for downloaded/imported .gguf files.", FeatureRouteTarget.MODELS),
        sys(8, "SAF External GGUF Import", "Import .gguf model files from device storage without broad permissions.", FeatureRouteTarget.MODELS),
        sys(9, "1500 MB RAM Safety Guard", "Prevents loading heavy weights when free RAM risks OOM crash.", FeatureRouteTarget.MODELS),
        sys(10, "TTFT & Tok/s Speed Telemetry", "Measures real Time-To-First-Token & throughput across models.", FeatureRouteTarget.MODELS),
        sys(11, "Redmi 13 5G Hardware Diagnostics", "Real RAM, storage, ABI, Android version & battery readout.", FeatureRouteTarget.WORK_MODE),
        sys(12, "Interactive Permission Manager", "Inspects Granted/Denied/Restricted states with ON_RESUME auto-refresh.", FeatureRouteTarget.PERMISSIONS),
        sys(13, "Battery-Saver Hologram Switch", "Reduces orbital canvas animations to save battery on demand.", FeatureRouteTarget.CUSTOMIZE),
        sys(14, "Feature & Module Diagnostics Tracker", "Full engineering audit of every AERIS module and limitation.", FeatureRouteTarget.TRACKER),

        // H. Additional Features (12 features)
        extra(1, "AERIS Side Navigation Drawer", "Swipe/menu drawer for Chats, Projects, Library, Customize & Tools.", FeatureRouteTarget.HOME),
        extra(2, "Personality & Tone Customizer", "Choose Calm Professional, Concise Technical, Friendly Companion, or Deep Reasoning.", FeatureRouteTarget.CUSTOMIZE),
        extra(3, "Custom Persistent Instructions", "Author personal preferences applied to every AERIS Brain prompt.", FeatureRouteTarget.CUSTOMIZE),
        extra(4, "Privacy & Data Control Center", "Inspect stored counts, export JSON, or wipe conversations/memories.", FeatureRouteTarget.PRIVACY_CENTER),
        extra(5, "Guided HTTPS Web Assistant", "Zero-password Custom Tabs / HTTPS browser with Google account list helper.", FeatureRouteTarget.WEB_ASSISTANT),
        extra(6, "Zero-Permission Photo Attachment", "Uses Android Photo Picker (PickVisualMedia) with zero broad storage permissions.", FeatureRouteTarget.CHAT),
        extra(7, "Official Provider Key Setup Links", "One-tap official console URLs for Gemini, OpenRouter, Groq, HF & Cloudflare.", FeatureRouteTarget.AI_PROVIDERS),
        extra(8, "WorkManager Task Retry Queue", "Inspect background sweeps and retry failed tasks in Notifications Center.", FeatureRouteTarget.NOTIFICATIONS),
        extra(9, "Chat Message Regenerate & Retry", "Regenerate clean answers or retry failed network turns in one tap.", FeatureRouteTarget.CHAT),
        extra(10, "Dual Normal / Work Orientation Lock", "Explicitly switches Android Activity orientation between Portrait & Landscape.", FeatureRouteTarget.WORK_MODE),
        extra(11, "Edge-to-Edge IME Composer Sync", "Smooth keyboard avoidance across Home, Chat & Landscape Work Mode.", FeatureRouteTarget.CHAT),
        extra(12, "Zero-Ads Free-First Architecture", "No ads, no telemetry trackers, no fake Pro paywalls anywhere in AERIS.", FeatureRouteTarget.PRIVACY_CENTER)
    )
}
