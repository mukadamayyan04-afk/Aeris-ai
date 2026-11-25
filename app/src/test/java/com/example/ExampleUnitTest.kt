package com.example

import com.example.ai.PromptFormatter
import com.example.domain.model.FeatureStatus
import com.example.domain.model.MemoryItem
import com.example.features.FeatureCatalog
import com.example.features.FeatureTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun feature_tracker_matrix_contains_all_capabilities() {
        val features = FeatureTracker.features
        assertTrue("Tracker must cover Stage 1-8 modules", features.size >= 17)

        val aiBrain = features.find { it.id == "F-02-AI-BRAIN-11" }
        assertNotNull(aiBrain)
        assertEquals(FeatureStatus.COMPLETED, aiBrain?.status)

        val freeOnly = features.find { it.id == "F-03-FREE-ONLY-POLICY" }
        assertNotNull(freeOnly)
        assertEquals(FeatureStatus.COMPLETED, freeOnly?.status)

        val gemini = features.find { it.id == "F-05-GEMINI-FIREBASE" }
        assertNotNull(gemini)
        assertEquals(FeatureStatus.INTEGRATED, gemini?.status)

        val wakeWord = features.find { it.id == "F-10-WAKE-WORD-SERVICE" }
        assertNotNull(wakeWord)
        assertEquals(FeatureStatus.DEVELOPED, wakeWord?.status)

        val avatar = features.find { it.id == "F-11-HOLO-AVATAR-6STATE" }
        assertNotNull(avatar)
        assertEquals(FeatureStatus.COMPLETED, avatar?.status)

        val imageStudio = features.find { it.id == "F-13-IMAGE-STUDIO" }
        assertNotNull(imageStudio)
        assertEquals(FeatureStatus.INTEGRATED, imageStudio?.status)

        val gameCoach = features.find { it.id == "F-14-GAME-COACH" }
        assertNotNull(gameCoach)
        assertEquals(FeatureStatus.INTEGRATED, gameCoach?.status)

        val notif = features.find { it.id == "F-15-NOTIF-ANNOUNCER" }
        assertNotNull(notif)
        assertEquals(FeatureStatus.COMPLETED, notif?.status)

        val permMgr = features.find { it.id == "F-16-PERMISSION-MGR" }
        assertNotNull(permMgr)
        assertEquals(FeatureStatus.COMPLETED, permMgr?.status)
    }

    @Test
    fun prompt_formatter_embeds_local_memory_and_sir_ji_address() {
        val memory = MemoryItem(id = 1, key = "UserName", content = "Ayyan")
        val prompt = PromptFormatter.format("hello", emptyList(), listOf(memory))
        assertTrue("Formatted prompt must contain memory", prompt.contains("UserName: Ayyan"))
        assertTrue("Formatted prompt must contain AERIS system prompt", prompt.contains("AERIS AI"))
        assertTrue("Formatted prompt must mandate addressing user respectfully as Sir ji", prompt.contains("Sir ji"))
        assertTrue("Formatted prompt must mandate honest reporting", prompt.contains("100% honest"))
    }

    @Test
    fun library_features_catalog_contains_core_capabilities() {
        val catalog = FeatureCatalog.allFeatures
        assertTrue("Catalog should contain 100+ Stage 1-8 capabilities", catalog.size >= 100)
        val voice = catalog.find { it.title.contains("Voice Assistant", ignoreCase = true) }
        assertNotNull(voice)

        val studio = catalog.find { it.title.contains("Image", ignoreCase = true) }
        assertNotNull(studio)

        val coach = catalog.find { it.title.contains("Game Coach", ignoreCase = true) }
        assertNotNull(coach)

        val webAss = catalog.find { it.title.contains("Web", ignoreCase = true) }
        assertNotNull(webAss)
    }
}
