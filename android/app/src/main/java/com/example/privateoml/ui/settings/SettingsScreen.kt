package com.example.privateoml.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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

    val biometricManager = remember { BiometricManager.from(context) }
    var biometricStatusText by remember { mutableStateOf("Checking status...") }
    var isHardwareAvailable by remember { mutableStateOf(false) }
    var isFingerprintRegistered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
        geminiKey = dbHelper.getConfig("gemini_api_key", "")
        recoveryPhone = dbHelper.getConfig("recovery_phone_number", "")
        usernameVal = dbHelper.getConfig("owner_username", "Rozuly")
        passwordVal = dbHelper.getConfig("owner_password", "")

        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricStatusText = "Active (Registered)"
            isHardwareAvailable = true
            isFingerprintRegistered = true
        } else if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            biometricStatusText = "Supported (No fingerprints enrolled)"
            isHardwareAvailable = true
            isFingerprintRegistered = false
        } else {
            biometricStatusText = "Unsupported or Sensor Unavailable"
            isHardwareAvailable = false
            isFingerprintRegistered = false
        }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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

            Spacer(modifier = Modifier.height(4.dp))
            Text("AI Provider Keys", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

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

            Spacer(modifier = Modifier.height(4.dp))
            Text("Security & Biometrics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Text("Biometric Status: $biometricStatusText", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            var fingerprintEnabled by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                fingerprintEnabled = dbHelper.getConfig("fingerprint_enabled", "false") == "true"
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Fingerprint Authentication", fontSize = 14.sp)
                Switch(
                    checked = fingerprintEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            val status = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            if (status == BiometricManager.BIOMETRIC_SUCCESS) {
                                fingerprintEnabled = true
                                dbHelper.saveConfig("fingerprint_enabled", "true")
                                Toast.makeText(context, "Fingerprint verification enabled", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Cannot enable: Please enroll fingerprints in system settings first.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            fingerprintEnabled = false
                            dbHelper.saveConfig("fingerprint_enabled", "false")
                            Toast.makeText(context, "Fingerprint verification disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val act = context as? FragmentActivity
                        if (act != null) {
                            val executor = ContextCompat.getMainExecutor(act)
                            val prompt = BiometricPrompt(act, executor, object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    Toast.makeText(context, "Verification test succeeded! Fingerprint works correctly.", Toast.LENGTH_LONG).show()
                                }
                                override fun onAuthenticationFailed() {
                                    super.onAuthenticationFailed()
                                    Toast.makeText(context, "Verification test failed: Fingerprint not recognized.", Toast.LENGTH_LONG).show()
                                }
                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    Toast.makeText(context, "Test error: $errString", Toast.LENGTH_LONG).show()
                                }
                            })
                            val info = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Verify Biometrics")
                                .setSubtitle("Biometric hardware validation test")
                                .setNegativeButtonText("Cancel")
                                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                .build()
                            prompt.authenticate(info)
                        }
                    },
                    enabled = isFingerprintRegistered && fingerprintEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify", fontSize = 12.sp)
                }
                
                OutlinedButton(
                    onClick = {
                        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            }
                        } else {
                            Intent(Settings.ACTION_SECURITY_SETTINGS)
                        }
                        try {
                            context.startActivity(enrollIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Re-register", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
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
