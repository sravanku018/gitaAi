package com.aipoweredgita.app.domain.usecase

import com.aipoweredgita.app.repository.StatsRepository
import javax.inject.Inject

/**
 * Use case to get the current coin balance
 * Encapsulates the business logic for fetching coin balance
 */
class GetCoinBalanceUseCase @Inject constructor(
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return try {
            val balance = statsRepository.getBalance()
            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
