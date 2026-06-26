package com.aipoweredgita.app.di

import android.content.Context
import com.aipoweredgita.app.database.*
import com.aipoweredgita.app.repository.*
import com.aipoweredgita.app.utils.QuizPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideQuizPreferences(
        @ApplicationContext context: Context
    ): QuizPreferences {
        return QuizPreferences(context)
    }

    @Provides
    @Singleton
    fun provideStatsRepository(
        userStatsDao: UserStatsDao,
        dailyActivityDao: DailyActivityDao,
        pendingSyncEventDao: PendingSyncEventDao,
        @ApplicationContext context: Context
    ): StatsRepository {
        return StatsRepository(
            userStatsDao = userStatsDao,
            dailyActivityDao = dailyActivityDao,
            appContext = context,
            pendingSyncEventDao = pendingSyncEventDao
        )
    }

    @Provides
    @Singleton
    fun provideContentRepository(
        recommendationDataDao: RecommendationDataDao
    ): ContentRepository {
        return ContentRepository(recommendationDataDao)
    }

    @Provides
    @Singleton
    fun provideReadingRepository(
        readVerseDao: ReadVerseDao,
        cachedVerseDao: CachedVerseDao
    ): ReadingRepository {
        return ReadingRepository(readVerseDao, cachedVerseDao)
    }

    @Provides
    @Singleton
    fun provideDailyActivityRepository(
        dailyActivityDao: DailyActivityDao
    ): DailyActivityRepository {
        return DailyActivityRepository(dailyActivityDao)
    }

    @Provides
    @Singleton
    fun provideQuizStatsRepository(
        quizAttemptDao: QuizAttemptDao
    ): QuizStatsRepository {
        return QuizStatsRepository(quizAttemptDao)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        voiceChatMessageDao: VoiceChatMessageDao
    ): ChatRepository {
        return ChatRepository(voiceChatMessageDao)
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(
        bookmarkDao: BookmarkDao
    ): BookmarkRepository {
        return BookmarkRepository(bookmarkDao)
    }

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        favoriteVerseDao: FavoriteVerseDao
    ): FavoriteRepository {
        return FavoriteRepository(favoriteVerseDao)
    }

    @Provides
    @Singleton
    fun provideFlashcardRepository(
        flashcardDao: FlashcardDao
    ): FlashcardRepository {
        return FlashcardRepository(flashcardDao)
    }

    @Provides
    @Singleton
    fun provideGitaRepository(): GitaRepository {
        return GitaRepository()
    }

    @Provides
    @Singleton
    fun provideLearningRepository(
        learningInsightsDao: LearningInsightsDao,
        learningPatternDao: LearningPatternDao,
        learningStyleDao: LearningStyleDao
    ): LearningRepository {
        return LearningRepository(
            learningInsightsDao,
            learningPatternDao,
            learningStyleDao
        )
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao
    ): NoteRepository {
        return NoteRepository(noteDao)
    }

    @Provides
    @Singleton
    fun provideOfflineCacheRepository(
        cachedVerseDao: CachedVerseDao
    ): OfflineCacheRepository {
        return OfflineCacheRepository(cachedVerseDao)
    }

    @Provides
    @Singleton
    fun provideQuizQuestionRepository(
        quizQuestionBankDao: QuizQuestionBankDao,
        questionPerformanceDao: QuestionPerformanceDao
    ): QuizQuestionRepository {
        return QuizQuestionRepository(quizQuestionBankDao, questionPerformanceDao)
    }

    @Provides
    @Singleton
    fun provideYogaProgressionRepository(
        yogaProgressionDao: YogaProgressionDao
    ): YogaProgressionRepository {
        return YogaProgressionRepository(yogaProgressionDao)
    }

    @Provides
    @Singleton
    fun provideQuizRepository(
        database: GitaDatabase,
        quizAttemptDao: QuizAttemptDao,
        questionPerformanceDao: QuestionPerformanceDao,
        translationCacheDao: TranslationCacheDao,
        statsRepository: StatsRepository,
        yogaProgressionRepository: YogaProgressionRepository
    ): QuizRepository {
        return QuizRepositoryImpl(
            database = database,
            quizAttemptDao = quizAttemptDao,
            questionPerformanceDao = questionPerformanceDao,
            translationCacheDao = translationCacheDao,
            statsRepository = statsRepository,
            yogaProgressionRepository = yogaProgressionRepository
        )
    }

    @Provides
    @Singleton
    fun provideRandomVerseRepository(
        randomVerseHistoryDao: RandomVerseHistoryDao
    ): RandomVerseRepository {
        return RandomVerseRepository(randomVerseHistoryDao)
    }

    @Provides
    @Singleton
    fun provideSpacedRepetitionRepository(
        spacedRepetitionDao: SpacedRepetitionDao
    ): SpacedRepetitionRepository {
        return SpacedRepetitionRepository(spacedRepetitionDao)
    }

    @Provides
    @Singleton
    fun provideSpiritualPathRepository(
        readVerseDao: ReadVerseDao
    ): SpiritualPathRepository {
        return SpiritualPathRepository(readVerseDao)
    }

    @Provides
    @Singleton
    fun provideStudyGuideRepository(
        studyGuideDao: StudyGuideDao
    ): StudyGuideRepository {
        return StudyGuideRepository(studyGuideDao)
    }

    @Provides
    @Singleton
    fun provideTranslationCacheRepository(
        translationCacheDao: TranslationCacheDao
    ): TranslationCacheRepository {
        return TranslationCacheRepository(translationCacheDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        userPreferencesDao: UserPreferencesDao
    ): UserPreferencesRepository {
        return UserPreferencesRepository(userPreferencesDao)
    }
}
