package com.aipoweredgita.app.di

import android.content.Context
import com.aipoweredgita.app.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GitaDatabase {
        return GitaDatabase.getDatabase(context)
    }


    @Provides
    fun provideFavoriteVerseDao(database: GitaDatabase): FavoriteVerseDao {
        return database.favoriteVerseDao()
    }

    @Provides
    fun provideCachedVerseDao(database: GitaDatabase): CachedVerseDao {
        return database.cachedVerseDao()
    }

    @Provides
    fun provideUserStatsDao(database: GitaDatabase): UserStatsDao {
        return database.userStatsDao()
    }

    @Provides
    fun provideQuizAttemptDao(database: GitaDatabase): QuizAttemptDao {
        return database.quizAttemptDao()
    }

    @Provides
    fun provideReadVerseDao(database: GitaDatabase): ReadVerseDao {
        return database.readVerseDao()
    }

    @Provides
    fun provideDailyActivityDao(database: GitaDatabase): DailyActivityDao {
        return database.dailyActivityDao()
    }

    @Provides
    fun provideLearningPatternDao(database: GitaDatabase): LearningPatternDao {
        return database.learningPatternDao()
    }

    @Provides
    fun provideQuestionPerformanceDao(database: GitaDatabase): QuestionPerformanceDao {
        return database.questionPerformanceDao()
    }

    @Provides
    fun provideUserPreferencesDao(database: GitaDatabase): UserPreferencesDao {
        return database.userPreferencesDao()
    }

    @Provides
    fun provideRecommendationDataDao(database: GitaDatabase): RecommendationDataDao {
        return database.recommendationDataDao()
    }

    @Provides
    fun provideLearningInsightsDao(database: GitaDatabase): LearningInsightsDao {
        return database.learningInsightsDao()
    }

    @Provides
    fun provideQuizQuestionBankDao(database: GitaDatabase): QuizQuestionBankDao {
        return database.quizQuestionBankDao()
    }

    @Provides
    fun provideStudyGuideDao(database: GitaDatabase): StudyGuideDao {
        return database.studyGuideDao()
    }

    @Provides
    fun provideFlashcardDao(database: GitaDatabase): FlashcardDao {
        return database.flashcardDao()
    }

    @Provides
    fun provideBookmarkDao(database: GitaDatabase): BookmarkDao {
        return database.bookmarkDao()
    }



    @Provides
    fun provideSpacedRepetitionDao(database: GitaDatabase): SpacedRepetitionDao {
        return database.spacedRepetitionDao()
    }

    @Provides
    fun provideLearningStyleDao(database: GitaDatabase): LearningStyleDao {
        return database.learningStyleDao()
    }

    @Provides
    fun provideYogaProgressionDao(database: GitaDatabase): YogaProgressionDao {
        return database.yogaProgressionDao()
    }

    @Provides
    fun provideRandomVerseHistoryDao(database: GitaDatabase): RandomVerseHistoryDao {
        return database.randomVerseHistoryDao()
    }

    @Provides
    fun provideVoiceChatMessageDao(database: GitaDatabase): VoiceChatMessageDao {
        return database.voiceChatMessageDao()
    }

    @Provides
    fun provideTranslationCacheDao(database: GitaDatabase): TranslationCacheDao {
        return database.translationCacheDao()
    }

    @Provides
    fun provideChatSummaryDao(database: GitaDatabase): ChatSummaryDao {
        return database.chatSummaryDao()
    }

    @Provides
    fun providePendingSyncEventDao(database: GitaDatabase): PendingSyncEventDao {
        return database.pendingSyncEventDao()
    }

    @Provides
    fun provideVerseNoteDao(database: GitaDatabase): VerseNoteDao {
        return database.verseNoteDao()
    }

    @Provides
    fun provideStudyPlanDao(database: GitaDatabase): StudyPlanDao {
        return database.studyPlanDao()
    }

    @Provides
    fun provideVerseSearchDao(database: GitaDatabase): VerseSearchDao {
        return database.verseSearchDao()
    }
}
