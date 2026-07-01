package com.aipoweredgita.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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

        // Background gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                0xFF1a0a2e.toInt(), 0xFF0d1b2a.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Gold accent paint
        val goldPaint = Paint().apply {
            color = 0xFFFFD700.toInt()
            textSize = 72f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val whitePaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }

        val dimPaint = Paint().apply {
            color = 0xAAFFFFFF.toInt()
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        var y = 300f

        // Title
        canvas.drawText("My Gita Journey", centerX, y, goldPaint)
        y += 120f

        // Yoga Level
        goldPaint.textSize = 56f
        canvas.drawText(yogaLevel, centerX, y, goldPaint)
        y += 80f
        canvas.drawText(yogaSanskritName, centerX, y, dimPaint)
        y += 160f

        // Stats
        val stats = listOf(
            "\uD83D\uDD25 $currentStreak Day Streak",
            "\uD83D\uDCD6 $versesRead Verses Read",
            "\uD83E\uDE99 $coinBalance Krishna Coins"
        )
        stats.forEach { stat ->
            canvas.drawText(stat, centerX, y, whitePaint)
            y += 100f
        }

        y += 100f
        dimPaint.textSize = 30f
        canvas.drawText("Bhagavad Gita AI", centerX, y, dimPaint)
        y += 60f
        canvas.drawText("Download on Play Store", centerX, y, dimPaint)

        return bitmap
    }

    fun shareAsImage(context: Context, bitmap: Bitmap) {
        val file = File(context.cacheDir, "progress_share.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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
        context.startActivity(Intent.createChooser(intent, "Share your progress"))
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

    Button(
        onClick = {
            val bitmap = ProgressShareCard.generateShareBitmap(
                context, yogaLevel, yogaSanskritName,
                currentStreak, versesRead, coinBalance
            )
            ProgressShareCard.shareAsImage(context, bitmap)
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Share Progress")
    }
}
