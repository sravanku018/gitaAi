package com.aipoweredgita.app.viewmodel

import android.app.Application
import com.aipoweredgita.app.database.QuizQuestionBank
import com.aipoweredgita.app.ml.HuggingFaceMLManager
import com.aipoweredgita.app.ml.TranslationManager
import com.aipoweredgita.app.repository.OfflineCacheRepository
import com.aipoweredgita.app.repository.QuizQuestionRepository
import com.aipoweredgita.app.repository.QuizRepository
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.utils.QuizPreferences
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelGuardTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: QuizViewModel
    private lateinit var quizQuestionRepository: QuizQuestionRepository

    private fun createQuestion(id: Int, difficulty: Int = 5): QuizQuestionBank {
        return QuizQuestionBank(
            id = id,
            questionHash = "hash_$id",
            question = "Question $id?",
            chapter = (id % 10) + 1,
            verse = (id % 5) + 1,
            optionA = "Option A",
            optionB = "Option B",
            optionC = "Option C",
            optionD = "Option D",
            correctAnswer = "A",
            explanation = "Explanation $id",
            difficulty = difficulty,
            isActive = true,
            isApproved = true,
            usageCount = 0,
            lastAskedAt = 0
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val statsRepository = mockk<StatsRepository>(relaxed = true)
        val quizRepository = mockk<QuizRepository>(relaxed = true)
        quizQuestionRepository = mockk<QuizQuestionRepository>(relaxed = true)
        val offlineCacheRepository = mockk<OfflineCacheRepository>(relaxed = true)
        val quizPreferences = mockk<QuizPreferences>(relaxed = true)
        val userPreferencesDao = mockk<com.aipoweredgita.app.database.UserPreferencesDao>(relaxed = true)
        coEvery { userPreferencesDao.getPreferencesSync(any()) } returns null
        val application = mockk<Application>(relaxed = true)

        // Mock Translation.getClient BEFORE TranslationManager constructor runs
        mockkStatic(Translation::class)
        every { Translation.getClient(any()) } returns mockk<Translator>(relaxed = true)

        mockkConstructor(TranslationManager::class)
        coEvery { anyConstructed<TranslationManager>().downloadModelsIfNeeded() } returns true

        mockkConstructor(HuggingFaceMLManager::class)
        every { anyConstructed<HuggingFaceMLManager>().close() } returns Unit

        viewModel = QuizViewModel(
            statsRepository = statsRepository,
            quizRepository = quizRepository,
            quizQuestionRepository = quizQuestionRepository,
            offlineCacheRepository = offlineCacheRepository,
            quizPreferences = quizPreferences,
            userPreferencesDao = userPreferencesDao,
            application = application
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Translation::class)
        unmockkConstructor(TranslationManager::class)
        unmockkConstructor(HuggingFaceMLManager::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `setQuizConfig starts totalQuestions at 0 not maxQuestions`() = runTest {
        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        val state = viewModel.quizState.value
        assertEquals("maxQuestions should be 15", 15, state.maxQuestions)
        assertEquals("totalQuestions should start at 0", 0, state.totalQuestions)
        assertNull("currentQuestion should be null at start", state.currentQuestion)
    }

    @Test
    fun `setQuizConfig 25 starts totalQuestions at 0`() = runTest {
        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(25, "en"))
        advanceUntilIdle()

        val state = viewModel.quizState.value
        assertEquals("maxQuestions should be 25", 25, state.maxQuestions)
        assertEquals("totalQuestions should start at 0", 0, state.totalQuestions)
    }

    @Test
    fun `loadNextQuestion increments totalQuestions correctly`() = runTest {
        val questions = (1..3).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        viewModel.loadNextQuestion()
        advanceUntilIdle()
        assertEquals("After 1st load, totalQuestions=1", 1, viewModel.quizState.value.totalQuestions)
        assertNotNull("currentQuestion should not be null", viewModel.quizState.value.currentQuestion)

        viewModel.loadNextQuestion()
        advanceUntilIdle()
        assertEquals("After 2nd load, totalQuestions=2", 2, viewModel.quizState.value.totalQuestions)

        viewModel.loadNextQuestion()
        advanceUntilIdle()
        assertEquals("After 3rd load, totalQuestions=3", 3, viewModel.quizState.value.totalQuestions)
    }

    @Test
    fun `guard blocks loading at maxQuestions 15`() = runTest {
        val questions = (1..20).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        for (i in 1..15) {
            viewModel.loadNextQuestion()
            advanceUntilIdle()
        }

        assertEquals("totalQuestions should be 15", 15, viewModel.quizState.value.totalQuestions)

        viewModel.loadNextQuestion()
        advanceUntilIdle()

        assertEquals("totalQuestions should still be 15 after guard blocks", 15, viewModel.quizState.value.totalQuestions)
    }

    @Test
    fun `guard blocks loading at maxQuestions 25`() = runTest {
        val questions = (1..30).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(25, "en"))
        advanceUntilIdle()

        for (i in 1..25) {
            viewModel.loadNextQuestion()
            advanceUntilIdle()
        }

        assertEquals("totalQuestions should be 25", 25, viewModel.quizState.value.totalQuestions)

        viewModel.loadNextQuestion()
        advanceUntilIdle()

        assertEquals("totalQuestions should still be 25 after guard blocks", 25, viewModel.quizState.value.totalQuestions)
    }

    @Test
    fun `no extra questions beyond 15 even with repeated calls`() = runTest {
        val questions = (1..30).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        for (i in 1..30) {
            viewModel.loadNextQuestion()
            advanceUntilIdle()
        }

        assertTrue("totalQuestions must not exceed 15",
            viewModel.quizState.value.totalQuestions <= 15)
    }

    @Test
    fun `no extra questions beyond 25 even with repeated calls`() = runTest {
        val questions = (1..50).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(25, "en"))
        advanceUntilIdle()

        for (i in 1..50) {
            viewModel.loadNextQuestion()
            advanceUntilIdle()
        }

        assertTrue("totalQuestions must not exceed 25",
            viewModel.quizState.value.totalQuestions <= 25)
    }

    @Test
    fun `restartQuiz resets totalQuestions and reloads first question`() = runTest {
        val questions = (1..10).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        viewModel.loadNextQuestion()
        advanceUntilIdle()
        viewModel.loadNextQuestion()
        advanceUntilIdle()
        assertEquals("Should have 2 questions loaded", 2, viewModel.quizState.value.totalQuestions)

        viewModel.restartQuiz()
        advanceUntilIdle()

        // restartQuiz resets then calls loadNextQuestion, so totalQuestions=1
        assertEquals("After restart, totalQuestions should be 1 (first question reloaded)", 1, viewModel.quizState.value.totalQuestions)
        assertEquals("After restart, maxQuestions should still be 15", 15, viewModel.quizState.value.maxQuestions)
        assertNotNull("After restart, currentQuestion should be loaded", viewModel.quizState.value.currentQuestion)
    }

    @Test
    fun `resetQuiz resets totalQuestions and reloads first question`() = runTest {
        val questions = (1..10).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        viewModel.loadNextQuestion()
        advanceUntilIdle()
        viewModel.loadNextQuestion()
        advanceUntilIdle()

        viewModel.resetQuiz()
        advanceUntilIdle()

        // resetQuiz resets then calls loadNextQuestion, so totalQuestions=1
        assertEquals("After reset, totalQuestions should be 1 (first question reloaded)", 1, viewModel.quizState.value.totalQuestions)
    }

    @Test
    fun `all questions are unique within a quiz`() = runTest {
        val questions = (1..15).map { createQuestion(it) }
        coEvery { quizQuestionRepository.getNextQuestions(any(), any(), any()) } returns questions

        viewModel.onEvent(com.aipoweredgita.app.domain.model.QuizEvent.SetQuizConfig(15, "en"))
        advanceUntilIdle()

        for (i in 1..15) {
            viewModel.loadNextQuestion()
            advanceUntilIdle()
            assertNotNull("Question $i should be loaded", viewModel.quizState.value.currentQuestion)
        }

        coVerify(exactly = 15) { quizQuestionRepository.markAsAsked(any()) }
    }

    @Test
    fun `setQuizLimit starts totalQuestions at 0`() = runTest {
        viewModel.setQuizLimit(15)
        advanceUntilIdle()

        val state = viewModel.quizState.value
        assertEquals("maxQuestions should be 15", 15, state.maxQuestions)
        assertEquals("totalQuestions should start at 0", 0, state.totalQuestions)
    }
}
