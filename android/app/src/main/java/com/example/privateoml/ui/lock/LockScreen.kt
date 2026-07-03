package com.example.privateoml.ui.lock

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.PremiumButton
import com.example.privateoml.ui.components.PremiumTextField
import com.example.privateoml.utils.CryptoUtils
import com.example.privateoml.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun LockScreen(
  onUnlock: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val dbHelper = remember { DatabaseHelper(context) }
  var passwordInput by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var passwordVisible by remember { mutableStateOf(false) }
  
  val coroutineScope = rememberCoroutineScope()
  
  // Shake offset animation for errors
  val shakeOffset = remember { Animatable(0f) }

  val shakeSpec = keyframes {
    durationMillis = 500
    0f at 0
    -20f at 100
    20f at 200
    -20f at 300
    20f at 400
    0f at 500
  }

  fun triggerShake() {
    coroutineScope.launch {
      shakeOffset.animateTo(0f, animationSpec = shakeSpec)
    }
  }

  fun attemptUnlock() {
    if (passwordInput.isEmpty()) {
      isError = true
      triggerShake()
      return
    }
    
    isLoading = true
    isError = false
    
    coroutineScope.launch(Dispatchers.IO) {
      val expectedHash = dbHelper.getConfig("app_password_hash", "")
      val inputHash = CryptoUtils.sha256(passwordInput)

      if (inputHash == expectedHash) {
        // Step 1: Local auth passed. Now get a real backend session token.
        val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
        val username = dbHelper.getConfig("owner_username", "Rozuly")

        try {
          val loginPayload = JSONObject().apply {
            put("username", username)
            put("password", passwordInput)
          }
          val responseRaw = NetworkUtils.httpPost("$serverUrl/auth/login", loginPayload.toString())
          val res = JSONObject(responseRaw)
          if (res.getBoolean("success")) {
            val data = res.getJSONObject("data")
            val token = data.getString("token")
            dbHelper.saveConfig("session_token", token)
            dbHelper.saveConfig("is_ai_key_synced", "false")  // Force re-sync AI key
            dbHelper.saveConfig("is_social_synced", "false")   // Force re-sync social
          }
        } catch (e: Exception) {
          // Backend login failed (offline?) - still let them in locally
          android.util.Log.w("LockScreen", "Backend login failed (offline?): ${e.message}")
        }

        withContext(Dispatchers.Main) {
          isLoading = false
          onUnlock()
        }
      } else {
        withContext(Dispatchers.Main) {
          isLoading = false
          isError = true
          triggerShake()
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(BgBase, Color(0xFF0D0E15))
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp)
        .offset(x = shakeOffset.value.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Header Section
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(Primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Shield",
          tint = Primary,
          modifier = Modifier.size(44.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Private OML",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Enter passcode to open control portal",
        color = TextSecondary,
        fontSize = 14.sp
      )

      Spacer(modifier = Modifier.height(48.dp))

      // Password input field
      PremiumTextField(
        value = passwordInput,
        onValueChange = {
          passwordInput = it
          isError = false
        },
        label = "Passcode",
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Password",
            tint = if (isError) ColorError else Primary
          )
        },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Toggle Visibility",
              tint = TextSecondary
            )
          }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
      )

      // Error message layout
      Spacer(modifier = Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(20.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        if (isError) {
          Text(
            text = "Incorrect password. Please try again.",
            color = ColorError,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Authentication action buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Fingerprint button
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .clickable {
              coroutineScope.launch {
                Toast.makeText(context, "Scanning fingerprint...", Toast.LENGTH_SHORT).show()
                delay(1200)
                // Simulate biometric approval
                onUnlock()
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric Lock",
            tint = Secondary,
            modifier = Modifier.size(28.dp)
          )
        }

        // Primary Unlock Button
        PremiumButton(
          text = if (isLoading) "Unlocking..." else "Unlock",
          onClick = { attemptUnlock() },
          modifier = Modifier.weight(1f),
          enabled = !isLoading
        )
      }
    }
  }
}
