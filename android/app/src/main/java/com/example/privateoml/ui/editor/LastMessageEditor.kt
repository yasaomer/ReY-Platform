package com.example.privateoml.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastMessageEditor(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val scope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var publishStatus by remember { mutableStateOf("") }

    // Load initial message
    LaunchedEffect(Unit) {
        val currentContent = dbHelper.getConfig("last_message_content", "")
        messageText = currentContent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish Message Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // 1. Save locally
                                    dbHelper.saveConfig("last_message_content", messageText)
                                    
                                    // 2. Publish to API gateway
                                    val serverUrl = dbHelper.getConfig("server_url", "http://localhost:8787/api/v1")
                                    val token = dbHelper.getConfig("session_token", "")

                                    if (token.isNotEmpty()) {
                                        val payload = JSONObject().apply {
                                            put("message_content", messageText)
                                        }
                                        val responseRaw = NetworkUtils.httpPost("$serverUrl/sync/last-message", payload.toString(), token)
                                        val res = JSONObject(responseRaw)
                                        if (res.getBoolean("success")) {
                                            publishStatus = "Message published successfully!"
                                        } else {
                                            publishStatus = "Publish error: ${res.getString("message")}"
                                        }
                                    } else {
                                        publishStatus = "Saved locally. Log in to sync."
                                    }
                                } catch (e: Exception) {
                                    publishStatus = "Error: ${e.message}"
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Publish")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Compose status message (HTML formatting supported):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("<h1>Hello World</h1><p>This is a status update...</p>") }
            )

            // Formatting hints bar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { messageText += "<b></b>" }, label = { Text("Bold") })
                AssistChip(onClick = { messageText += "<i></i>" }, label = { Text("Italic") })
                AssistChip(onClick = { messageText += "<blockquote></blockquote>" }, label = { Text("Quote") })
                AssistChip(onClick = { messageText += "<hr/>" }, label = { Text("Divider") })
            }

            if (publishStatus.isNotEmpty()) {
                Text(publishStatus, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
        }
    }
}
