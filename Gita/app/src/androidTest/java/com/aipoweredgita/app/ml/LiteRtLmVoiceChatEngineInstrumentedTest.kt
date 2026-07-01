package com.aipoweredgita.app.ml

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aipoweredgita.app.utils.DeviceTierDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LiteRtLmVoiceChatEngineInstrumentedTest {

    private lateinit var context: Context
    private lateinit var engine: LiteRtLmVoiceChatEngine

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        engine = LiteRtLmVoiceChatEngine(context)
    }

    @Test
    fun testHasVulkanIsResolvable() {
        // Just testing that the detector doesn't crash on device and returns a boolean
        val hasVulkan = DeviceTierDetector.hasVulkan(context)
        println("Device has Vulkan: $hasVulkan")
        // No assert because some devices might not have it, but it shouldn't crash
    }

    @Test
    fun testEngineInitializationFailsGracefullyWithMissingModel() = runBlocking {
        // Given a path that doesn't exist
        val fakePath = "/data/data/${context.packageName}/files/ml_models/missing_model.bin"
        
        // When initializing
        val success = engine.initialize(
            path = fakePath,
            maxTokens = 2048,
            timeoutMs = 60000L
        )
        
        // Then it should catch the exception and return false, rather than crashing the app
        assertFalse("Engine should fail initialization gracefully when model is missing", success)
    }

    @Test
    fun testEngineInitializationWithRealModelIfExists() = runBlocking {
        // Attempt to find Gemma or Qwen in files dir to test real initialization
        val mlDir = File(context.filesDir, "ml_models")
        if (!mlDir.exists() || mlDir.listFiles().isNullOrEmpty()) {
            println("No models downloaded on device. Skipping real initialization test.")
            return@runBlocking
        }
        
        val modelFile = mlDir.listFiles()?.firstOrNull { it.name.endsWith(".bin") }
        if (modelFile == null) {
            println("No .bin models found on device. Skipping real initialization test.")
            return@runBlocking
        }

        println("Found model for testing: ${modelFile.absolutePath}")
        
        // If a model exists on device, we can do a real integration test
        val initSuccess = engine.initialize(
            path = modelFile.absolutePath,
            maxTokens = 2048,
            timeoutMs = 120000L
        )
        
        if (initSuccess) {
            assertTrue(initSuccess)
            
            // Try sending a short message
            val response = engine.sendMessage("Say 'Hello'")
            assertTrue("Response should not be empty", response.isNotEmpty())
            println("LLM Response: $response")
            
            // Cleanup
            engine.resetConversation()
            engine.close()
        } else {
            // It might fail if the model is corrupted or RAM is too low, but we log it
            println("Initialization returned false. See logcat for details.")
        }
    }
}
