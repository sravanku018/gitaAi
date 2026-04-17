package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.QuizQuestionBankDao

/**
 * Wrapper around DatasetIngestionPipeline for backward compatibility.
 * Delegates all work to the production pipeline.
 */
class BhagavadGitaQAImporter(
    context: Context,
    questionBankDao: QuizQuestionBankDao
) {
    private val pipeline = DatasetIngestionPipeline(context, questionBankDao)

    suspend fun importDataset(
        language: String = "english",
        batchSize: Int = 500,
        onProgress: (imported: Int, total: Int) -> Unit = { _, _ -> }
    ): Int {
        return pipeline.ingestDataset(language, batchSize, onProgress)
    }

    suspend fun hasQuestions(): Boolean = pipeline.hasQuestions()
}
