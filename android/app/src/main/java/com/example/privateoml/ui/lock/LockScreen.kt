package com.example.privateoml.ui.lock

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
import com.example.privateoml.utils.CryptoUtils

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var passwordInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Private OML", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Administrative Control Center Locked", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { 
                passwordInput = it
                isError = false
            },
            label = { Text("Enter App Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Incorrect password, please try again", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val expectedHash = dbHelper.getConfig("app_password_hash", "")
                val inputHash = CryptoUtils.sha256(passwordInput)
                if (inputHash == expectedHash) {
                    onUnlock()
                } else {
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
    }
}
