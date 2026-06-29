package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.aipoweredgita.app.database.ChatSummaryDao
import com.aipoweredgita.app.ml.LiteRtLmVoiceChatEngine
import com.aipoweredgita.app.repository.ChatRepository
import com.aipoweredgita.app.repository.ContentRepository
import com.aipoweredgita.app.repository.StatsRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class VoiceChatViewModelTest {

    private lateinit var application: Application
    private val chatRepo = mockk<ChatRepository>(relaxed = true)
    private val summaryDao = mockk<ChatSummaryDao>(relaxed = true)
    private val statsRepository = mockk<StatsRepository>(relaxed = true)
    private val contentRepository = mockk<ContentRepository>(relaxed = true)
    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val voiceChatEngine = mockk<LiteRtLmVoiceChatEngine>(relaxed = true)

    private lateinit var viewModel: VoiceChatViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = org.robolectric.RuntimeEnvironment.getApplication()
        
        every { statsRepository.coinBalance } returns kotlinx.coroutines.flow.MutableStateFlow(0)
        every { contentRepository.getActiveRecommendations() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        
        // Mock preferences if needed, or rely on real shared prefs in Robolectric
        // We'll let Robolectric handle shared prefs naturally

        // Mock LiteRtLmVoiceChatEngine constructor
        mockkConstructor(LiteRtLmVoiceChatEngine::class)
        coEvery { anyConstructed<LiteRtLmVoiceChatEngine>().initialize(any(), any(), any(), any()) } returns true

        viewModel = VoiceChatViewModel(
            application = application,
            chatRepo = chatRepo,
            summaryDao = summaryDao,
            statsRepository = statsRepository,
            contentRepository = contentRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is correctly set`() = runTest {
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(false, state.isListening)
        assertEquals(false, state.isSpeaking)
        assertEquals(false, state.isThinking)
    }

    // Additional tests for coroutine state emission would require injecting
    // aiDispatcher instead of the hardcoded Dispatchers.Default.
}
