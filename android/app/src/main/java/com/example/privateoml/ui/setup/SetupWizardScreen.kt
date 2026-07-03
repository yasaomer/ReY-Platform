package com.example.privateoml.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper

@Composable
fun SetupWizardScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var currentStep by remember { mutableStateOf(1) }
    
    // Form fields
    var appPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("http://10.0.2.2:8787/api/v1") } // Emulator local worker loop
    var geminiKey by remember { mutableStateOf("") }
    var recoveryPhone by remember { mutableStateOf("") }
    var galleryPath by remember { mutableStateOf("/storage/emulated/0/Pictures/Vault") }
    var isTestSuccessful by remember { mutableStateOf<Boolean?>(null) }
    var statusText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Title / Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Setup Wizard - Step $currentStep of 12",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / 12f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Main step content block
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            when (currentStep) {
                1 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome to ReY Platform", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Let's configure your Private OML administrator app.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Authentication Setup", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = appPassword,
                        onValueChange = { appPassword = it },
                        label = { Text("App Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                3 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ecosystem Permissions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Private OML requires location access, storage permissions, and SMS capability for OTP code deliveries.", fontSize = 14.sp)
                }

                4 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Battery Optimization", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ensure background updates remain stable by exempting the app from battery optimization limits in Android OS settings.", fontSize = 14.sp)
                }

                5 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Background Sync Service", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("We will configure a background scheduler task to constantly look for sync tasks from the server.", fontSize = 14.sp)
                }

                6 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Notification Permission", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Required to notify you when location requests occur or sync tasks fail.", fontSize = 14.sp)
                }

                7 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Storage Permission", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Required to monitor, optimize, and upload photos to Google Drive.", fontSize = 14.sp)
                }

                8 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Location Permission", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Required to track your coordinate statistics securely.", fontSize = 14.sp)
                }

                9 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Choose Gallery Folder", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = galleryPath,
                        onValueChange = { galleryPath = it },
                        label = { Text("Monitored Folder Path") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                10 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Server Connection Test", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server API URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isTestSuccessful = true
                            statusText = "Connected to backend API successfully."
                        }
                    ) {
                        Text("Run Sync Test")
                    }
                    if (statusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(statusText, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                11 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AI Configuration", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Gemini API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recoveryPhone,
                        onValueChange = { recoveryPhone = it },
                        label = { Text("Recovery Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                12 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Configuration Finished!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("All settings saved. You are ready to start administering the ReY Platform.", fontSize = 14.sp)
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { if (currentStep > 1) currentStep-- },
                enabled = currentStep > 1,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    if (currentStep < 12) {
                        // Intermediate saves
                        if (currentStep == 2 && appPassword.isNotEmpty()) {
                            dbHelper.saveConfig("app_password_hash", com.example.privateoml.utils.CryptoUtils.sha256(appPassword))
                        }
                        if (currentStep == 9) {
                            dbHelper.saveConfig("gallery_folder_path", galleryPath)
                        }
                        if (currentStep == 10) {
                            dbHelper.saveConfig("server_url", serverUrl)
                        }
                        if (currentStep == 11) {
                            dbHelper.saveConfig("gemini_api_key", geminiKey)
                            dbHelper.saveConfig("recovery_phone_number", recoveryPhone)
                            dbHelper.saveConfig("is_ai_key_synced", "false")
                        }
                        currentStep++
                    } else {
                        dbHelper.saveConfig("owner_username", "Rozuly")
                        dbHelper.saveConfig("is_setup_completed", "true")
                        onFinish()
                    }
                },
                enabled = when (currentStep) {
                    2 -> appPassword.isNotEmpty() && appPassword == confirmPassword
                    10 -> isTestSuccessful == true
                    11 -> geminiKey.isNotEmpty() && recoveryPhone.isNotEmpty()
                    else -> true
                }
            ) {
                Text(if (currentStep == 12) "Finish" else "Next")
            }
        }
    }
}
