package com.example.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Maintainable official API provider metadata & verified developer setup links.
 * Opens official developer dashboards cleanly via Android Custom Tabs extras (with browser fallback)
 * without scraping or handling external passwords inside AERIS.
 */
object ProviderDirectory {

    data class OfficialProviderMetadata(
        val providerType: AiProviderType,
        val shortName: String,
        val setupSummary: String,
        val officialKeyUrl: String,
        val officialPageLabel: String,
        val keyHint: String,
        val freeTierDisclosure: String
    )

    private val entries: Map<AiProviderType, OfficialProviderMetadata> = mapOf(
        AiProviderType.GEMINI_FIREBASE to OfficialProviderMetadata(
            providerType = AiProviderType.GEMINI_FIREBASE,
            shortName = "Google Gemini",
            setupSummary = "AERIS already connects to Google Gemini via Firebase AI Logic + App Check (no manual API key needed). You can also review official Gemini quotas in Google AI Studio.",
            officialKeyUrl = "https://aistudio.google.com/app/apikey",
            officialPageLabel = "Open Google AI Studio",
            keyHint = "Managed automatically via Firebase AI Logic",
            freeTierDisclosure = "Free-tier quotas apply per Firebase project; Gemini 4 Argon requires Blaze opt-in."
        ),
        AiProviderType.OPENAI_COMPATIBLE to OfficialProviderMetadata(
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            shortName = "OpenAI / Compatible",
            setupSummary = "Create an API key from OpenAI Platform or point the Base URL to your compatible server (/v1/chat/completions).",
            officialKeyUrl = "https://platform.openai.com/api-keys",
            officialPageLabel = "Open Official API Page",
            keyHint = "sk-...",
            freeTierDisclosure = "OpenAI cloud keys depend on your OpenAI account credits/limits; self-hosted servers depend on your host."
        ),
        AiProviderType.OPENROUTER to OfficialProviderMetadata(
            providerType = AiProviderType.OPENROUTER,
            shortName = "OpenRouter",
            setupSummary = "Sign in on OpenRouter, create an API key, and keep ':free' model suffixes while Free-Only Mode is ON.",
            officialKeyUrl = "https://openrouter.ai/settings/keys",
            officialPageLabel = "Open Official API Page",
            keyHint = "sk-or-v1-...",
            freeTierDisclosure = "Free-tier ':free' models enforce OpenRouter RPM/daily rate limits."
        ),
        AiProviderType.GROQ to OfficialProviderMetadata(
            providerType = AiProviderType.GROQ,
            shortName = "Groq",
            setupSummary = "Create a free API key in Groq Console for ultra-fast LPU inference with Llama and Qwen models.",
            officialKeyUrl = "https://console.groq.com/keys",
            officialPageLabel = "Open Official API Page",
            keyHint = "gsk_...",
            freeTierDisclosure = "Groq developer tier offers free RPM/TPM limits subject to Groq quota."
        ),
        AiProviderType.HUGGING_FACE to OfficialProviderMetadata(
            providerType = AiProviderType.HUGGING_FACE,
            shortName = "Hugging Face",
            setupSummary = "Generate an Access Token (Read / Inference Providers permission) on Hugging Face Settings.",
            officialKeyUrl = "https://huggingface.co/settings/tokens",
            officialPageLabel = "Open Official API Page",
            keyHint = "hf_...",
            freeTierDisclosure = "Hugging Face serverless inference has monthly free quota caps."
        ),
        AiProviderType.CLOUDFLARE_WORKERS_AI to OfficialProviderMetadata(
            providerType = AiProviderType.CLOUDFLARE_WORKERS_AI,
            shortName = "Cloudflare Workers AI",
            setupSummary = "Create a Workers AI API Token in Cloudflare Dashboard and enter your 32-char Account ID or full /ai/v1 endpoint URL.",
            officialKeyUrl = "https://dash.cloudflare.com/profile/api-tokens",
            officialPageLabel = "Open Official API Page",
            keyHint = "Cloudflare API Token",
            freeTierDisclosure = "Cloudflare Workers AI includes daily free Neurons allowance before billing applies."
        ),
        AiProviderType.GPT_OSS_HOSTED to OfficialProviderMetadata(
            providerType = AiProviderType.GPT_OSS_HOSTED,
            shortName = "GPT-OSS Hosted",
            setupSummary = "Point to any OpenAI-compatible GPT-OSS endpoint (vLLM, Together, Fireworks, or local LAN server).",
            officialKeyUrl = "https://platform.openai.com/docs/guides/open-models",
            officialPageLabel = "Open Official API Docs",
            keyHint = "Optional Bearer Token",
            freeTierDisclosure = "Depends on your chosen host or local server."
        ),
        AiProviderType.QWEN_HOSTED to OfficialProviderMetadata(
            providerType = AiProviderType.QWEN_HOSTED,
            shortName = "Qwen Hosted",
            setupSummary = "Use Alibaba Cloud Model Studio (DashScope compatible-mode /v1) or your own Qwen vLLM/Ollama endpoint.",
            officialKeyUrl = "https://bailian.console.alibabacloud.com/",
            officialPageLabel = "Open Official API Page",
            keyHint = "sk-... (or blank for local server)",
            freeTierDisclosure = "Depends on DashScope free quota or your self-hosted GPU endpoint."
        ),
        AiProviderType.LOCAL_GGUF to OfficialProviderMetadata(
            providerType = AiProviderType.LOCAL_GGUF,
            shortName = "Local GGUF Model",
            setupSummary = "Runs downloaded or imported GGUF models completely offline on Redmi 13 5G.",
            officialKeyUrl = "https://ai.google.dev/gemma",
            officialPageLabel = "Open Gemma Docs",
            keyHint = "No API key needed (100% on-device)",
            freeTierDisclosure = "100% free on-device; native token speed requires libllama.so."
        ),
        AiProviderType.ON_DEVICE_CORE to OfficialProviderMetadata(
            providerType = AiProviderType.ON_DEVICE_CORE,
            shortName = "AERIS On-Device Core",
            setupSummary = "Built-in offline rule, memory & intent core that works even without internet or external keys.",
            officialKeyUrl = "https://ai.google.dev/gemma",
            officialPageLabel = "Open Gemma Docs",
            keyHint = "No API key needed",
            freeTierDisclosure = "Always free and offline."
        )
    )

    fun metadataFor(providerType: AiProviderType): OfficialProviderMetadata {
        return entries[providerType] ?: entries.getValue(AiProviderType.OPENAI_COMPATIBLE)
    }

    fun allCloudSetupTargets(): List<AiProviderType> = listOf(
        AiProviderType.GEMINI_FIREBASE,
        AiProviderType.OPENROUTER,
        AiProviderType.GROQ,
        AiProviderType.HUGGING_FACE,
        AiProviderType.CLOUDFLARE_WORKERS_AI,
        AiProviderType.OPENAI_COMPATIBLE,
        AiProviderType.GPT_OSS_HOSTED,
        AiProviderType.QWEN_HOSTED
    )

    fun buildCloudflareBaseUrl(accountIdOrUrl: String): String {
        val clean = accountIdOrUrl.trim().trimEnd('/')
        if (clean.startsWith("https://", ignoreCase = true) || clean.startsWith("http://", ignoreCase = true)) {
            return clean
        }
        return if (clean.isBlank()) {
            AiProviderType.CLOUDFLARE_WORKERS_AI.defaultBaseUrl
        } else {
            "https://api.cloudflare.com/client/v4/accounts/$clean/ai/v1"
        }
    }

    fun openOfficialApiPage(context: Context, providerType: AiProviderType): Result<String> {
        val meta = metadataFor(providerType)
        val url = meta.officialKeyUrl
        if (!url.startsWith("https://", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Unsupported URL scheme for official API page."))
        }
        return try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                // Standard Android Custom Tabs extras so Chrome/browsers open as an in-app Custom Tab
                // while falling back cleanly to the default browser if Custom Tabs isn't supported.
                putExtras(
                    Bundle().apply {
                        putBinder("android.support.customtabs.extra.SESSION", null)
                        putInt("android.support.customtabs.extra.TOOLBAR_COLOR", 0xFF050811.toInt())
                    }
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
