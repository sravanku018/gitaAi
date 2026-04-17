package com.aipoweredgita.app.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteVerse::class, CachedVerse::class, UserStats::class, QuizAttempt::class,
        ReadVerse::class, DailyActivity::class, LearningPattern::class, QuestionPerformance::class,
        UserPreferences::class, RecommendationData::class, LearningInsights::class, QuizQuestionBank::class,
        StudyGuide::class, Flashcard::class, Bookmark::class, Note::class,
        SpacedRepetitionItem::class, LearningStyle::class, YogaProgression::class, RandomVerseHistory::class,
        VoiceChatMessage::class
    ],
    version = 22,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GitaDatabase : RoomDatabase() {

    abstract fun favoriteVerseDao(): FavoriteVerseDao
    abstract fun cachedVerseDao(): CachedVerseDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun readVerseDao(): ReadVerseDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun learningPatternDao(): LearningPatternDao
    abstract fun questionPerformanceDao(): QuestionPerformanceDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun recommendationDataDao(): RecommendationDataDao
    abstract fun learningInsightsDao(): LearningInsightsDao
    abstract fun quizQuestionBankDao(): QuizQuestionBankDao
    abstract fun studyGuideDao(): StudyGuideDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao
    abstract fun spacedRepetitionDao(): SpacedRepetitionDao
    abstract fun learningStyleDao(): LearningStyleDao
    abstract fun yogaProgressionDao(): YogaProgressionDao
    abstract fun randomVerseHistoryDao(): RandomVerseHistoryDao
    abstract fun voiceChatMessageDao(): VoiceChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: GitaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create cached_verses table without affecting favorite_verses
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_verses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapterNo INTEGER NOT NULL,
                        verseNo INTEGER NOT NULL,
                        chapterName TEXT NOT NULL,
                        verse TEXT NOT NULL,
                        translation TEXT NOT NULL,
                        meaning TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_cached_verses_chapterNo_verseNo
                    ON cached_verses (chapterNo, verseNo)
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create user_stats table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_stats (
                        id INTEGER PRIMARY KEY NOT NULL,
                        totalQuizzesTaken INTEGER NOT NULL,
                        totalQuestionsAnswered INTEGER NOT NULL,
                        totalCorrectAnswers INTEGER NOT NULL,
                        bestScore INTEGER NOT NULL,
                        bestScoreOutOf INTEGER NOT NULL,
                        versesRead INTEGER NOT NULL,
                        chaptersCompleted INTEGER NOT NULL,
                        totalTimeSpentSeconds INTEGER NOT NULL,
                        normalModeTimeSeconds INTEGER NOT NULL,
                        quizModeTimeSeconds INTEGER NOT NULL,
                        lastActiveTimestamp INTEGER NOT NULL,
                        totalFavorites INTEGER NOT NULL,
                        currentStreak INTEGER NOT NULL,
                        longestStreak INTEGER NOT NULL,
                        lastActiveDate TEXT NOT NULL,
                        firstOpenTimestamp INTEGER NOT NULL
                    )
                """)
                // Initialize with default values
                database.execSQL("""
                    INSERT INTO user_stats (id, totalQuizzesTaken, totalQuestionsAnswered,
                        totalCorrectAnswers, bestScore, bestScoreOutOf, versesRead,
                        chaptersCompleted, totalTimeSpentSeconds, normalModeTimeSeconds,
                        quizModeTimeSeconds, lastActiveTimestamp,
                        totalFavorites, currentStreak, longestStreak, lastActiveDate,
                        firstOpenTimestamp)
                    VALUES (1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ${System.currentTimeMillis()}, 0, 0, 0, '', ${System.currentTimeMillis()})
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create quiz_attempts table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS quiz_attempts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        score INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        timeSpentSeconds INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        date TEXT NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add userName and dateOfBirth columns to user_stats table
                database.execSQL("ALTER TABLE user_stats ADD COLUMN userName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE user_stats ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add distinctVersesRead column
                database.execSQL("ALTER TABLE user_stats ADD COLUMN distinctVersesRead INTEGER NOT NULL DEFAULT 0")
                // Create read_verses table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS read_verses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapterNo INTEGER NOT NULL,
                        verseNo INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """
                )
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_read_verses_chapterNo_verseNo_date
                    ON read_verses (chapterNo, verseNo, date)
                    """
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_read_verses_chapterNo_verseNo
                    ON read_verses (chapterNo, verseNo)
                    """
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS daily_activity (date TEXT PRIMARY KEY NOT NULL, normalSeconds INTEGER NOT NULL, quizSeconds INTEGER NOT NULL, versesRead INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS learning_patterns (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, averageTimePerQuestion LONG NOT NULL, fastestTimePerQuestion LONG NOT NULL, slowestTimePerQuestion LONG NOT NULL, preferredDifficulty INTEGER NOT NULL, bestStudyHour INTEGER NOT NULL, learningStyle INTEGER NOT NULL, focusAreas TEXT NOT NULL, peakPerformanceDay TEXT NOT NULL, peakPerformanceTime INTEGER NOT NULL, studyConsistencyScore REAL NOT NULL, weeklyStudyDays INTEGER NOT NULL, learningEfficiency REAL NOT NULL, lastUpdated LONG NOT NULL, createdAt LONG NOT NULL)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS question_performance (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, questionId TEXT NOT NULL, topicCategory TEXT NOT NULL, questionType TEXT NOT NULL, timesAttempted INTEGER NOT NULL, timesCorrect INTEGER NOT NULL, successRate REAL NOT NULL, perceivedDifficulty INTEGER NOT NULL, averageTimeSpent LONG NOT NULL, userFeedback TEXT NOT NULL, commonMistakes TEXT NOT NULL, learningValue REAL NOT NULL, recommendationScore REAL NOT NULL, lastAttempted LONG NOT NULL, createdAt LONG NOT NULL)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS user_preferences (userId INTEGER PRIMARY KEY NOT NULL, preferredQuestionTypes TEXT NOT NULL, preferredDifficultyMin INTEGER NOT NULL, preferredDifficultyMax INTEGER NOT NULL, preferredYogaLevel INTEGER NOT NULL, preferredChapters TEXT NOT NULL, preferredStudyMode TEXT NOT NULL, questionsPerSession INTEGER NOT NULL, languagePreference TEXT NOT NULL, enableAIRecommendations INTEGER NOT NULL, enableDifficultyAdaptation INTEGER NOT NULL, enablePersonalizedPaths INTEGER NOT NULL, enableSpacedRepetition INTEGER NOT NULL, dailyReminderTime TEXT NOT NULL, enableDailyReminders INTEGER NOT NULL, reminderFrequency TEXT NOT NULL, darkModeEnabled INTEGER NOT NULL, fontSize INTEGER NOT NULL, showHints INTEGER NOT NULL, hideCompletedTopics INTEGER NOT NULL, showOnlyWeakAreas INTEGER NOT NULL, lastUpdated LONG NOT NULL, createdAt LONG NOT NULL)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS recommendation_data (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, recommendationType TEXT NOT NULL, recommendationId TEXT NOT NULL, recommendationTitle TEXT NOT NULL, priority INTEGER NOT NULL, confidenceScore REAL NOT NULL, relevanceScore REAL NOT NULL, reason TEXT NOT NULL, baseReason TEXT NOT NULL, userWeakness REAL NOT NULL, expectedBenefit REAL NOT NULL, urgencyLevel TEXT NOT NULL, status TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt LONG NOT NULL, viewedAt LONG NOT NULL, completedAt LONG NOT NULL, dismissedAt LONG NOT NULL)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS learning_insights (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, insightType TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, detail TEXT NOT NULL, dataPoints TEXT NOT NULL, confidenceLevel REAL NOT NULL, relatedYogaLevel INTEGER NOT NULL, relatedTopics TEXT NOT NULL, suggestedAction TEXT NOT NULL, actionPriority TEXT NOT NULL, actionUrl TEXT NOT NULL, impactScore REAL NOT NULL, timelineValue TEXT NOT NULL, isViewed INTEGER NOT NULL, isDismissed INTEGER NOT NULL, createdAt LONG NOT NULL, viewedAt LONG NOT NULL, expiresAt LONG NOT NULL)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS quiz_question_bank (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, questionHash TEXT NOT NULL, questionType TEXT NOT NULL, difficulty INTEGER NOT NULL, question TEXT NOT NULL, chapter INTEGER NOT NULL, verse INTEGER NOT NULL, yogaLevel INTEGER NOT NULL, optionA TEXT NOT NULL, optionB TEXT NOT NULL, optionC TEXT NOT NULL, optionD TEXT NOT NULL, correctAnswer TEXT NOT NULL, explanation TEXT NOT NULL, keywords TEXT NOT NULL, topics TEXT NOT NULL, generatedBy TEXT NOT NULL, generationMethod TEXT NOT NULL, modelVersion TEXT NOT NULL, qualityScore REAL NOT NULL, relevanceScore REAL NOT NULL, isVerified INTEGER NOT NULL, isApproved INTEGER NOT NULL, usageCount INTEGER NOT NULL, usersAttempted INTEGER NOT NULL, averageSuccessRate REAL NOT NULL, lastUsed LONG NOT NULL, isActive INTEGER NOT NULL, isArchived INTEGER NOT NULL, createdAt LONG NOT NULL, updatedAt LONG NOT NULL)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS study_guides (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, chapterNo INTEGER NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, keyPoints TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS flashcards (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, topic TEXT NOT NULL, frontText TEXT NOT NULL, backText TEXT NOT NULL, chapterNo INTEGER NOT NULL, verseNo INTEGER NOT NULL, timesShown INTEGER NOT NULL, timesCorrect INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create bookmarks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapter INTEGER NOT NULL,
                        verse INTEGER NOT NULL,
                        verseText TEXT NOT NULL,
                        bookmarkType TEXT NOT NULL,
                        difficulty INTEGER NOT NULL DEFAULT 5,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        lastReviewedAt INTEGER,
                        reviewCount INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_chapter_verse ON bookmarks (chapter, verse)")

                // Create notes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapter INTEGER NOT NULL,
                        verse INTEGER NOT NULL,
                        noteTitle TEXT NOT NULL,
                        noteContent TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        mood TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isPrivate INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_chapter_verse ON notes (chapter, verse)")

                // Create spaced_repetition_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS spaced_repetition_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapter INTEGER NOT NULL,
                        verse INTEGER NOT NULL,
                        easeFactor REAL NOT NULL DEFAULT 2.5,
                        interval INTEGER NOT NULL DEFAULT 1,
                        repetition INTEGER NOT NULL DEFAULT 0,
                        lastReviewedAt INTEGER NOT NULL,
                        nextReviewAt INTEGER NOT NULL,
                        quality INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_spaced_repetition_next_review ON spaced_repetition_items (nextReviewAt)")

                // Create learning_style table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS learning_style (
                        id INTEGER PRIMARY KEY NOT NULL,
                        visualScore REAL NOT NULL DEFAULT 0,
                        auditoryScore REAL NOT NULL DEFAULT 0,
                        readingScore REAL NOT NULL DEFAULT 0,
                        kinestheticScore REAL NOT NULL DEFAULT 0,
                        preferredSessionLength INTEGER NOT NULL DEFAULT 15,
                        preferredStudyTime TEXT NOT NULL DEFAULT 'morning',
                        questionTypePreference TEXT NOT NULL DEFAULT 'mixed',
                        lastUpdated INTEGER NOT NULL,
                        confidence REAL NOT NULL DEFAULT 0
                    )
                """)
                // Initialize learning style record
                database.execSQL("""
                    INSERT OR IGNORE INTO learning_style
                    (id, visualScore, auditoryScore, readingScore, kinestheticScore, preferredSessionLength, preferredStudyTime, questionTypePreference, lastUpdated, confidence)
                    VALUES (1, 0, 0, 0, 0, 15, 'morning', 'mixed', ${System.currentTimeMillis()}, 0)
                """)
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add separated verse tracking columns to cached_verses table
                database.execSQL("ALTER TABLE cached_verses ADD COLUMN wasSeparated INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE cached_verses ADD COLUMN originalCombinedGroup TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create yoga_progression table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS yoga_progression (
                        id INTEGER PRIMARY KEY NOT NULL,
                        yogaLevel INTEGER NOT NULL DEFAULT 0,
                        progressionPercentage REAL NOT NULL DEFAULT 0,
                        lastActivityDate TEXT NOT NULL DEFAULT '',
                        consecutiveDays INTEGER NOT NULL DEFAULT 0,
                        quizAccuracy REAL NOT NULL DEFAULT 0,
                        readingConsistency REAL NOT NULL DEFAULT 0,
                        totalQuizzesTaken INTEGER NOT NULL DEFAULT 0,
                        totalVersesRead INTEGER NOT NULL DEFAULT 0,
                        lastUpdated INTEGER NOT NULL
                    )
                """)
                // Initialize with default values
                database.execSQL("""
                    INSERT INTO yoga_progression (id, yogaLevel, progressionPercentage, lastActivityDate, consecutiveDays, quizAccuracy, readingConsistency, totalQuizzesTaken, totalVersesRead, lastUpdated)
                    VALUES (1, 0, 0, '', 0, 0, 0, 0, 0, ${System.currentTimeMillis()})
                """)
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create random_verse_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS random_verse_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        chapterNo INTEGER NOT NULL,
                        verseNo INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create voice_chat_messages table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS voice_chat_messages (
                        id TEXT PRIMARY KEY NOT NULL,
                        text TEXT NOT NULL,
                        isUser INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add cooldown tracking + user rating columns to quiz_question_bank
                database.execSQL("ALTER TABLE quiz_question_bank ADD COLUMN lastAskedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE quiz_question_bank ADD COLUMN userRating REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE quiz_question_bank ADD COLUMN ratingCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Recreate user_stats to remove qaModeTimeSeconds and fix schema
                database.execSQL("DROP TABLE IF EXISTS `user_stats_new`")
                database.execSQL("""
                    CREATE TABLE `user_stats_new` (
                        `id` INTEGER NOT NULL, 
                        `userName` TEXT NOT NULL, 
                        `dateOfBirth` TEXT NOT NULL, 
                        `totalQuizzesTaken` INTEGER NOT NULL, 
                        `totalQuestionsAnswered` INTEGER NOT NULL, 
                        `totalCorrectAnswers` INTEGER NOT NULL, 
                        `bestScore` INTEGER NOT NULL, 
                        `bestScoreOutOf` INTEGER NOT NULL, 
                        `versesRead` INTEGER NOT NULL, 
                        `distinctVersesRead` INTEGER NOT NULL, 
                        `chaptersCompleted` INTEGER NOT NULL, 
                        `totalTimeSpentSeconds` INTEGER NOT NULL, 
                        `normalModeTimeSeconds` INTEGER NOT NULL, 
                        `quizModeTimeSeconds` INTEGER NOT NULL, 
                        `voiceStudioTimeSeconds` INTEGER NOT NULL, 
                        `lastActiveTimestamp` INTEGER NOT NULL, 
                        `totalFavorites` INTEGER NOT NULL, 
                        `currentStreak` INTEGER NOT NULL, 
                        `longestStreak` INTEGER NOT NULL, 
                        `lastActiveDate` TEXT NOT NULL, 
                        `firstOpenTimestamp` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO `user_stats_new` (
                        `id`, `userName`, `dateOfBirth`, `totalQuizzesTaken`, `totalQuestionsAnswered`, 
                        `totalCorrectAnswers`, `bestScore`, `bestScoreOutOf`, `versesRead`, 
                        `distinctVersesRead`, `chaptersCompleted`, `totalTimeSpentSeconds`, 
                        `normalModeTimeSeconds`, `quizModeTimeSeconds`, `voiceStudioTimeSeconds`, 
                        `lastActiveTimestamp`, `totalFavorites`, `currentStreak`, `longestStreak`, 
                        `lastActiveDate`, `firstOpenTimestamp`
                    )
                    SELECT 
                        `id`, `userName`, `dateOfBirth`, `totalQuizzesTaken`, `totalQuestionsAnswered`, 
                        `totalCorrectAnswers`, `bestScore`, `bestScoreOutOf`, `versesRead`, 
                        `distinctVersesRead`, `chaptersCompleted`, `totalTimeSpentSeconds`, 
                        `normalModeTimeSeconds`, `quizModeTimeSeconds`, 0, 
                        `lastActiveTimestamp`, `totalFavorites`, `currentStreak`, `longestStreak`, 
                        `lastActiveDate`, `firstOpenTimestamp`
                    FROM `user_stats`
                """)
                
                database.execSQL("DROP TABLE `user_stats`")
                database.execSQL("ALTER TABLE `user_stats_new` RENAME TO `user_stats`")

                // 2. Recreate daily_activity to remove qaSeconds and fix schema
                database.execSQL("DROP TABLE IF EXISTS `daily_activity_new`")
                database.execSQL("""
                    CREATE TABLE `daily_activity_new` (
                        `date` TEXT NOT NULL, 
                        `normalSeconds` INTEGER NOT NULL, 
                        `quizSeconds` INTEGER NOT NULL, 
                        `voiceStudioTimeSeconds` INTEGER NOT NULL, 
                        `versesRead` INTEGER NOT NULL, 
                        PRIMARY KEY(`date`)
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO `daily_activity_new` (`date`, `normalSeconds`, `quizSeconds`, `voiceStudioTimeSeconds`, `versesRead`)
                    SELECT `date`, `normalSeconds`, `quizSeconds`, 0, `versesRead` FROM `daily_activity`
                """)
                
                database.execSQL("DROP TABLE `daily_activity`")
                database.execSQL("ALTER TABLE `daily_activity_new` RENAME TO `daily_activity`")
            }
        }

        fun getDatabase(context: Context): GitaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GitaDatabase::class.java,
                    "gita_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
