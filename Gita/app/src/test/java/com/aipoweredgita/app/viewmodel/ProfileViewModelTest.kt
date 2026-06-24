package com.aipoweredgita.app.viewmodel

import com.aipoweredgita.app.domain.model.ProfileEvent
import com.aipoweredgita.app.domain.model.ProfileSideEffect
import com.aipoweredgita.app.domain.usecase.GenerateBadgesUseCase
import com.aipoweredgita.app.domain.usecase.GetCoinBalanceUseCase
import com.aipoweredgita.app.domain.usecase.LoadDashboardUseCase
import com.aipoweredgita.app.domain.usecase.UpdateProfileUseCase
import com.aipoweredgita.app.repository.ContentRepository
import com.aipoweredgita.app.repository.StatsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        every { statsRepository.getUserStatsFlow() } returns flowOf(null)
        every { statsRepository.coinBalance } returns MutableStateFlow(0)
        every { contentRepo.getActiveRecommendations() } returns flowOf(emptyList())

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
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(null, state.stats)
        assertEquals(0, state.coinBalance)
        assertFalse(state.isLoading)
    }

    @Test
    fun `refreshCoinBalance should update coin balance`() = runTest {
        coEvery { getCoinBalanceUseCase() } returns Result.success(100)

        viewModel.onEvent(ProfileEvent.RefreshCoins)
        advanceUntilIdle()

        assertEquals(100, viewModel.uiState.value.coinBalance)
    }

    @Test
    fun `refreshCoinBalance should call use case`() = runTest {
        coEvery { getCoinBalanceUseCase() } returns Result.success(100)

        viewModel.onEvent(ProfileEvent.RefreshCoins)
        advanceUntilIdle()

        coVerify { getCoinBalanceUseCase() }
    }

    @Test
    fun `setCoinBalance should update state`() = runTest {
        viewModel.onEvent(ProfileEvent.SetCoinBalance(500))
        advanceUntilIdle()

        assertEquals(500, viewModel.uiState.value.coinBalance)
    }

    @Test
    fun `setCoinBalance with zero`() = runTest {
        viewModel.onEvent(ProfileEvent.SetCoinBalance(0))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.coinBalance)
    }

    @Test
    fun `setCoinBalance with large value`() = runTest {
        viewModel.onEvent(ProfileEvent.SetCoinBalance(9999))
        advanceUntilIdle()

        assertEquals(9999, viewModel.uiState.value.coinBalance)
    }

    @Test
    fun `updateProfile success should call use case`() = runTest {
        coEvery { updateProfileUseCase(any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(ProfileEvent.UpdateProfile("Krishna", "2000-01-01"))
        advanceUntilIdle()

        coVerify { updateProfileUseCase("Krishna", "2000-01-01") }
    }

    @Test
    fun `updateProfile failure should emit error`() = runTest {
        coEvery { updateProfileUseCase(any(), any()) } returns Result.failure(Exception("Update failed"))

        viewModel.onEvent(ProfileEvent.UpdateProfile("Krishna", "2000-01-01"))
        advanceUntilIdle()

        coVerify { updateProfileUseCase("Krishna", "2000-01-01") }
    }

    @Test
    fun `multiple coin balance updates should reflect latest`() = runTest {
        coEvery { getCoinBalanceUseCase() } returnsMany listOf(
            Result.success(100),
            Result.success(200)
        )

        viewModel.onEvent(ProfileEvent.RefreshCoins)
        advanceUntilIdle()
        assertEquals(100, viewModel.uiState.value.coinBalance)

        viewModel.onEvent(ProfileEvent.RefreshCoins)
        advanceUntilIdle()
        assertEquals(200, viewModel.uiState.value.coinBalance)
    }
}
