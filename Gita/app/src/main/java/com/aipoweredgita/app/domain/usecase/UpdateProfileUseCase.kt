package com.aipoweredgita.app.domain.usecase

import com.aipoweredgita.app.database.UserStatsDao
import javax.inject.Inject

/**
 * Use case to update user profile
 * Encapsulates the business logic for profile updates
 */
class UpdateProfileUseCase @Inject constructor(
    private val userStatsDao: UserStatsDao
) {
    suspend operator fun invoke(name: String, dob: String): Result<Unit> {
        return try {
            userStatsDao.updateProfile(name, dob)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
