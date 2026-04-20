package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.*

@Composable
fun ErrorScreen(
    message: String = "Something went wrong",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = GoldSpark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextWhite,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Saffron)
            ) {
                Text("Retry")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun ErrorScreenPreview() {
    ErrorScreen(message = "Unable to connect to sacred servers", onRetry = {})
}

@Composable
fun ErrorScreen() {
    ErrorScreen(message = "Something went wrong", onRetry = null)
}

