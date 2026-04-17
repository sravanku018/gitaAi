package com.aipoweredgita.app.ml

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ModelLoadingTest {

    private lateinit var context: Context
    private lateinit var modelAvailability: ModelAvailability
    private lateinit var llmEngine: LlmInferenceEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        modelAvailability = ModelAvailability.getInstance(context)
        llmEngine = LlmInferenceEngine(context)
    }

    @Test
    fun testResolvedPathLogic() {
        // Mock preferences
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // 1. Test Qwen3 preference
        prefs.edit().putString("selected_ai_model", "Qwen3 0.6B").apply()
        val pathQwen = modelAvailability.getResolvedTextModelPath("Qwen3 0.6B")
        
        // 2. Test Gemma 4 preference
        prefs.edit().putString("selected_ai_model", "Gemma 4 2B").apply()
        val pathGemma = modelAvailability.getResolvedTextModelPath("Gemma 4 2B")
        
        // Since we don't have actual files in the test environment usually, 
        // we check if the logic correctly returns null if files are missing 
        // or the correct path if they existed.
        
        // Verify that it doesn't crash
        println("Resolved Qwen path: $pathQwen")
        println("Resolved Gemma path: $pathGemma")
    }

    @Test
    fun testEngineInitializationWithFakeModelFailsGracefully() = runBlocking {
        val fakePath = File(context.filesDir, "fake_model.litertlm")
        fakePath.writeText("Not a real model file but enough to trigger size check if needed")
        
        val success = llmEngine.initialize(fakePath.absolutePath)
        assertFalse("Engine should NOT initialize with a fake text file", success)
        
        fakePath.delete()
    }
    
    @Test
    fun testModelAvailabilityDetection() {
        val modelsDir = File(context.filesDir, "ml_models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        
        val qwenFile = File(modelsDir, "qwen3-0.6b-int4.litertlm")
        
        // Test: File doesn't exist
        qwenFile.delete()
        assertFalse("Qwen3 should not be available if file is missing", modelAvailability.isQwen3Available())
        
        // Test: File exists but too small
        qwenFile.writeText("short")
        assertFalse("Qwen3 should not be available if file is too small", modelAvailability.isQwen3Available())
        
        // Clean up
        qwenFile.delete()
    }
}
