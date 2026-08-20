package com.aipoweredgita.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aipoweredgita.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ProgressShareCard {

    fun generateShareBitmap(
        context: Context,
        yogaLevel: String,
        yogaSanskritName: String,
        currentStreak: Int,
        versesRead: Int,
        coinBalance: Int
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF1a0a2e.toInt(), 0xFF0d1b2a.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD700.toInt()
            textSize = 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt() // stronger contrast than 0xAA
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val maxWidth = width - 120f
        var y = 280f

        y = drawWrappedText(canvas, "My Gita Journey", centerX, y, goldPaint, maxWidth, 88f)
        y += 40f
        goldPaint.textSize = 56f
        y = drawWrappedText(canvas, yogaLevel, centerX, y, goldPaint, maxWidth, 70f)
        y += 16f
        y = drawWrappedText(canvas, yogaSanskritName, centerX, y, dimPaint, maxWidth, 48f)
        y += 80f

        // ASCII-safe labels (avoid emoji tofu on Canvas)
        val stats = listOf(
            "$currentStreak Day Streak",
            "$versesRead Verses Read",
            "$coinBalance Krishna Coins"
        )
        stats.forEach { stat ->
            y = drawWrappedText(canvas, stat, centerX, y, whitePaint, maxWidth, 64f)
            y += 24f
        }

        y += 60f
        dimPaint.textSize = 30f
        y = drawWrappedText(canvas, "Bhagavad Gita AI", centerX, y, dimPaint, maxWidth, 40f)
        y += 12f
        drawWrappedText(canvas, "Download on Play Store", centerX, y, dimPaint, maxWidth, 40f)

        return bitmap
    }

    fun generateVerseShareBitmap(
        verseText: String,
        translation: String,
        chapter: Int,
        verseNo: Int,
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF1a0a2e.toInt(), 0xFF2a1500.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD700.toInt()
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val versePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF3E0.toInt()
            textSize = 42f
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val maxWidth = width - 140f
        var y = 220f
        y = drawWrappedText(canvas, "Bhagavad Gita $chapter:$verseNo", centerX, y, goldPaint, maxWidth, 56f)
        y += 48f
        y = drawWrappedText(canvas, verseText.replace('\n', ' '), centerX, y, versePaint, maxWidth, 56f)
        y += 40f
        y = drawWrappedText(canvas, translation.replace('\n', ' '), centerX, y, bodyPaint, maxWidth, 48f)
        y = (y + 80f).coerceAtMost(height - 120f)
        drawWrappedText(canvas, "AI Powered Gita", centerX, y, dimPaint, maxWidth, 36f)
        return bitmap
    }

    /**
     * Draws [text] wrapped to [maxWidth]; returns the y after the last line.
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float,
        lineHeight: Float,
    ): Float {
        if (text.isBlank()) return startY
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        val bounds = Rect()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            paint.getTextBounds(candidate, 0, candidate.length, bounds)
            if (bounds.width() > maxWidth && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()

        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, centerX, y, paint)
            y += lineHeight
        }
        return y
    }

    /**
     * Writes PNG off the caller's thread responsibility; returns a content Uri Intent or null on failure.
     */
    fun buildShareImageIntent(context: Context, bitmap: Bitmap, chooserTitle: String): Intent? {
        return try {
            val file = File(context.cacheDir, "progress_share_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)) {
                    return null
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(intent, chooserTitle)
        } catch (_: Exception) {
            null
        }
    }

    @Deprecated("Use buildShareImageIntent from a background dispatcher")
    fun shareAsImage(context: Context, bitmap: Bitmap) {
        val chooser = buildShareImageIntent(context, bitmap, "Share your progress") ?: return
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Missing share target / FileProvider misconfig — swallow to avoid crash
        }
    }
}

@Composable
fun ShareProgressButton(
    yogaLevel: String = "Karma Yogi",
    yogaSanskritName: String = "कर्म योगी",
    currentStreak: Int = 0,
    versesRead: Int = 0,
    coinBalance: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (isGenerating) return@Button
            isGenerating = true
            scope.launch {
                try {
                    val chooser = withContext(Dispatchers.IO) {
                        val bitmap = ProgressShareCard.generateShareBitmap(
                            context, yogaLevel, yogaSanskritName,
                            currentStreak, versesRead, coinBalance
                        )
                        try {
                            ProgressShareCard.buildShareImageIntent(
                                context, bitmap, context.getString(R.string.share_progress_chooser)
                            )
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    if (chooser != null) {
                        context.startActivity(chooser)
                    }
                } catch (_: Exception) {
                    // ignore — button just stops spinning
                } finally {
                    isGenerating = false
                }
            }
        },
        enabled = !isGenerating,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (isGenerating) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.share_progress))
    }
}
