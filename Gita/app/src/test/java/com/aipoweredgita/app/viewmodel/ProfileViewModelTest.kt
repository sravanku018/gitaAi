package com.aipoweredgita.app.viewmodel

import com.aipoweredgita.app.domain.model.ProfileEvent
import com.aipoweredgita.app.domain.model.ProfileSideEffect
import com.aipoweredgita.app.domain.usecase.GenerateBadgesUseCase
import com.aipoweredgita.app.domain.usecase.GetCoinBalanceUseCase
import com.aipoweredgita.app.domain.usecase.LoadDashboardUseCase
import com.aipoweredgita.app.domain.usecase.UpdateProfileUseCase
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.UserLevel
import com.aipoweredgita.app.repository.ContentRepository
import com.aipoweredgita.app.repository.StatsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfileViewModel
    private lateinit var statsRepository: StatsRepository
    private lateinit var contentRepo: ContentRepository
    private lateinit var getCoinBalanceUseCase: GetCoinBalanceUseCase
    private lateinit var loadDashboardUseCase: LoadDashboardUseCase
    private lateinit var generateBadgesUseCase: GenerateBadgesUseCase
    private lateinit var updateProfileUseCase: UpdateProfileUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        statsRepository = mockk(relaxed = true)
        contentRepo = mockk(relaxed = true)
        getCoinBalanceUseCase = mockk(relaxed = true)
        loadDashboardUseCase = mockk(relaxed = true)
        generateBadgesUseCase = mockk(relaxed = true)
        updateProfileUseCase = mockk(relaxed = true)

        viewModel = ProfileViewModel(
            statsRepository = statsRepository,
            contentRepo = contentRepo,
            getCoinBalanceUseCase = getCoinBalanceUseCase,
            loadDashboardUseCase = loadDashboardUseCase,
            generateBadgesUseCase = generateBadgesUseCase,
            updateProfileUseCase = updateProfileUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(null, state.stats)
        assertTrue(state.badges.isEmpty())
        assertEquals(null, state.level)
        assertEquals(0, state.coinBalance)
        assertFalse(state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `refreshCoinBalance should update coin balance`() = runTest {
        coEvery { getCoinBalanceUseCase() } returns Result.success(100)

        viewModel.onEvent(ProfileEvent.RefreshCoins)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(100, state.coinBalance)
    }

    @Test
    fun `refreshCoinBalance should emit error on failure`() = runTest {
        coEvery { getCoinBalanceUseCase() } returns Result.failure(Exception("Network error"))

        viewModel.onEvent(ProfileEvent.RefreshCoins)
        advanceUntilIdle()

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ProfileSideEffect.ShowError)
        assertEquals("Network error", (effect as ProfileSideEffect.ShowError).message)
    }

    @Test
    fun `setCoinBalance should update state`() = runTest {
        viewModel.onEvent(ProfileEvent.SetCoinBalance(500))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(500, state.coinBalance)
    }

    @Test
    fun `updateProfile should emit success toast`() = runTest {
        coEvery { updateProfileUseCase(any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(ProfileEvent.UpdateProfile("Krishna", "2000-01-01"))
        advanceUntilIdle()

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ProfileSideEffect.ShowToast)
        assertEquals("Profile updated successfully", (effect as ProfileSideEffect.ShowToast).message)
    }

    @Test
    fun `updateProfile should emit error on failure`() = runTest {
        coEvery { updateProfileUseCase(any(), any()) } returns Result.failure(Exception("Update failed"))

        viewModel.onEvent(ProfileEvent.UpdateProfile("Krishna", "2000-01-01"))
        advanceUntilIdle()

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ProfileSideEffect.ShowError)
        assertEquals("Update failed", (effect as ProfileSideEffect.ShowError).message)
    }
}
