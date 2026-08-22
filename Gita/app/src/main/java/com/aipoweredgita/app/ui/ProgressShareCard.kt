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
        val maxWidth = width - 160f
        val centerX = width / 2f

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFD700.toInt()
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val versePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFF8E7.toInt()
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFD700.toInt()
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val slokaLines = mutableListOf<String>()
        verseText.split("\n").forEach { rawLine ->
            val cleanLine = rawLine.trim()
            if (cleanLine.isNotEmpty()) {
                slokaLines.addAll(wrapLineToMaxWidth(cleanLine, versePaint, maxWidth))
            }
        }

        val headerHeight = 60f
        val verseLineHeight = 64f
        val slokaTotalHeight = (slokaLines.size * verseLineHeight).coerceAtLeast(64f)
        val footerHeight = 40f
        val topPadding = 70f
        val bottomPadding = 70f
        val gap1 = 40f
        val gap2 = 50f

        val totalHeight = (topPadding + headerHeight + gap1 + slokaTotalHeight + gap2 + footerHeight + bottomPadding).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, totalHeight.toFloat(),
                0xFF1A0E26.toInt(), 0xFF2D1600.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x44FFD700.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(20f, 20f, width - 20f, totalHeight - 20f, 28f, 28f, borderPaint)

        var y = topPadding + 40f
        canvas.drawText("Bhagavad Gita  •  Chapter $chapter, Verse $verseNo", centerX, y, goldPaint)

        y += gap1 + 44f
        slokaLines.forEach { line ->
            canvas.drawText(line, centerX, y, versePaint)
            y += verseLineHeight
        }

        y = totalHeight - bottomPadding
        canvas.drawText("AI Powered Gita", centerX, y, footerPaint)

        return bitmap
    }

    private fun wrapLineToMaxWidth(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        val bounds = Rect()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            paint.getTextBounds(candidate, 0, candidate.length, bounds)
            if (bounds.width() > maxWidth && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

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
        val lines = wrapLineToMaxWidth(text, paint, maxWidth)
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
