package com.example.privateoml.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
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
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var recoveryPhone by remember { mutableStateOf("") }
    var usernameVal by remember { mutableStateOf("Rozuly") }
    var passwordVal by remember { mutableStateOf("") }
    var authStatus by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
        geminiKey = dbHelper.getConfig("gemini_api_key", "")
        recoveryPhone = dbHelper.getConfig("recovery_phone_number", "")
        usernameVal = dbHelper.getConfig("owner_username", "Rozuly")
        passwordVal = dbHelper.getConfig("owner_password", "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Ecosystem Servers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { 
                    serverUrl = it
                    dbHelper.saveConfig("server_url", it)
                },
                label = { Text("Server API Endpoint") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = recoveryPhone,
                onValueChange = { 
                    recoveryPhone = it
                    dbHelper.saveConfig("recovery_phone_number", it)
                },
                label = { Text("SMS Recovery Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("AI provider keys", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = geminiKey,
                onValueChange = { 
                    geminiKey = it
                    dbHelper.saveConfig("gemini_api_key", it)
                    dbHelper.saveConfig("is_ai_key_synced", "false")
                },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Website Login (Rozuly)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = usernameVal,
                onValueChange = { 
                    usernameVal = it 
                    dbHelper.saveConfig("owner_username", it)
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = passwordVal,
                onValueChange = { 
                    passwordVal = it 
                    dbHelper.saveConfig("owner_password", it)
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            authStatus = "Contacting server..."
                            val payload = JSONObject().apply {
                                put("username", usernameVal)
                                put("password", passwordVal)
                            }
                            val responseRaw = NetworkUtils.httpPost("$serverUrl/auth/login", payload.toString())
                            val res = JSONObject(responseRaw)
                            if (res.getBoolean("success")) {
                                val token = res.getJSONObject("data").getString("token")
                                dbHelper.saveConfig("session_token", token)
                                authStatus = "Linked successfully! Token cached."
                            } else {
                                authStatus = "Link failed: ${res.getString("message")}"
                            }
                        } catch (e: Exception) {
                            authStatus = "Error: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Authenticate Viewer Profile")
            }

            if (authStatus.isNotEmpty()) {
                Text(authStatus, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            }
        }
    }
}
