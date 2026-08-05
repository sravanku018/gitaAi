package com.aipoweredgita.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.util.GitaConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var feedbackType by remember { mutableStateOf("Feedback") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val goldColor = Color(0xFFF59E0B)
    val maxChars = 200

    // Theme-aware colors
    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Help Centre",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (message.isBlank()) {
                            Toast.makeText(context, "Please enter your message", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val userId = com.aipoweredgita.app.utils.AuthPreferences.getInstance(context).userId ?: "guest"
                            val nowStr = java.text.SimpleDateFormat("yyyy-MM-DD HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            val json = JSONObject().apply {
                                put("user_id", userId)
                                put("type", feedbackType.lowercase())
                                put("subject", subject.ifBlank { "General $feedbackType" })
                                put("message", message.trim().take(maxChars))
                                put("client_timestamp", nowStr)
                            }

                            val client = OkHttpClient()
                            val req = Request.Builder()
                                .url("${GitaConstants.COIN_API_BASE_URL}feedback")
                                .post(json.toString().toRequestBody("application/json".toMediaType()))
                                .build()

                            try {
                                val resp = client.newCall(req).execute()
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    if (resp.isSuccessful) {
                                        Toast.makeText(context, "$feedbackType submitted successfully! 🙏", Toast.LENGTH_LONG).show()
                                        subject = ""
                                        message = ""
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "Failed to submit. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSubmitting = false
                                    Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "SUBMIT ${feedbackType.uppercase()}",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "We value your input! Share feedback or report an issue directly to the admin (Max 200 characters).",
                color = onSurfaceMuted,
                fontSize = 14.sp
            )

            // Feedback Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Feedback", "Complaint").forEach { type ->
                    val isSelected = feedbackType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) goldColor else surfaceColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { feedbackType = type }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == "Feedback") "💡 Feedback" else "🚨 Complaint",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else onSurfaceColor
                        )
                    }
                }
            }

            // Subject Input
            OutlinedTextField(
                value = subject,
                onValueChange = { if (it.length <= 100) subject = it },
                label = { Text("Subject (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = goldColor,
                    unfocusedBorderColor = onSurfaceMuted,
                    focusedLabelColor = goldColor,
                    unfocusedLabelColor = onSurfaceMuted,
                    focusedTextColor = onSurfaceColor,
                    unfocusedTextColor = onSurfaceColor
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Message Input with 200 Character Limit
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= maxChars) message = it },
                    label = { Text("Describe your feedback or issue...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goldColor,
                        unfocusedBorderColor = onSurfaceMuted,
                        focusedLabelColor = goldColor,
                        unfocusedLabelColor = onSurfaceMuted,
                        focusedTextColor = onSurfaceColor,
                        unfocusedTextColor = onSurfaceColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 8
                )
                Text(
                    text = "${message.length} / $maxChars characters",
                    color = if (message.length >= maxChars) Color(0xFFEF4444) else onSurfaceMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
