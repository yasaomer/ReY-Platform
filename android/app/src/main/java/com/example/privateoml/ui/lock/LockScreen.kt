package com.example.privateoml.ui.lock

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.PremiumButton
import com.example.privateoml.ui.components.PremiumTextField
import com.example.privateoml.utils.CryptoUtils
import com.example.privateoml.utils.KeyStoreHelper
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
  val activity = remember(context) { context as? FragmentActivity }
  val dbHelper = remember { DatabaseHelper(context) }
  var passwordInput by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var passwordVisible by remember { mutableStateOf(false) }
  
  val coroutineScope = rememberCoroutineScope()
  val shakeOffset = remember { Animatable(0f) }

  // Cooldown & locking logic state
  var remainingCooldownSeconds by remember { mutableStateOf(0L) }
  
  LaunchedEffect(Unit) {
    while (true) {
      val lockExpiration = dbHelper.getConfig("lock_expiration_time", "0").toLongOrNull() ?: 0L
      val currentTime = System.currentTimeMillis()
      if (lockExpiration > currentTime) {
        remainingCooldownSeconds = (lockExpiration - currentTime) / 1000L + 1
      } else {
        remainingCooldownSeconds = 0L
      }
      delay(1000)
    }
  }

  val isFingerprintEnabled = remember { dbHelper.getConfig("fingerprint_enabled", "false") == "true" }
  val isFingerprintTempDisabled = remember { dbHelper.getConfig("fingerprint_temp_disabled", "false") == "true" }
  val failedAttemptsCount = remember { dbHelper.getConfig("failed_attempts_count", "0").toIntOrNull() ?: 0 }

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

  fun showBiometricPrompt() {
    val act = activity ?: return
    val executor = ContextCompat.getMainExecutor(act)
    
    val biometricPrompt = BiometricPrompt(act, executor, object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Toast.makeText(act, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
      }

      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        
        // Reset lock state on successful authenticate
        dbHelper.saveConfig("failed_attempts_count", "0")
        dbHelper.saveConfig("lock_expiration_time", "0")
        dbHelper.saveConfig("fingerprint_temp_disabled", "false")
        
        // Background token refresh
        val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
        val username = dbHelper.getConfig("owner_username", "Rozuly")
        val password = dbHelper.getConfig("owner_password", "")

        coroutineScope.launch(Dispatchers.IO) {
          try {
            if (password.isNotEmpty()) {
              val loginPayload = JSONObject().apply {
                put("username", username)
                put("password", password)
              }
              val responseRaw = NetworkUtils.httpPost("$serverUrl/auth/login", loginPayload.toString())
              val res = JSONObject(responseRaw)
              if (res.getBoolean("success")) {
                val data = res.getJSONObject("data")
                val token = data.getString("token")
                dbHelper.saveConfig("session_token", token)
                dbHelper.saveConfig("is_ai_key_synced", "false")
                dbHelper.saveConfig("is_social_synced", "false")
              }
            }
          } catch (e: Exception) {
            android.util.Log.e("LockScreen", "Background session refresh failed: ${e.message}")
          }

          withContext(Dispatchers.Main) {
            onUnlock()
          }
        }
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        Toast.makeText(act, "Fingerprint not recognized", Toast.LENGTH_SHORT).show()
      }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Biometric Verification")
      .setSubtitle("Authenticate using your registered fingerprint")
      .setNegativeButtonText("Use Passcode")
      .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
      .build()

    biometricPrompt.authenticate(promptInfo)
  }

  // Trigger biometric auto-prompt on screen open
  LaunchedEffect(Unit) {
    if (isFingerprintEnabled && !isFingerprintTempDisabled && failedAttemptsCount < 10) {
      delay(500)
      showBiometricPrompt()
    }
  }

  fun attemptUnlock() {
    if (passwordInput.isEmpty()) {
      isError = true
      errorText = "Passcode is required."
      triggerShake()
      return
    }

    val lockExpiration = dbHelper.getConfig("lock_expiration_time", "0").toLongOrNull() ?: 0L
    if (System.currentTimeMillis() < lockExpiration) {
      isError = true
      errorText = "Passcode locked. Please wait for cooldown."
      triggerShake()
      return
    }
    
    isLoading = true
    isError = false
    
    coroutineScope.launch(Dispatchers.IO) {
      val saltEnc = dbHelper.getConfig("app_password_salt_encrypted", "")
      val hashEnc = dbHelper.getConfig("app_password_hash_encrypted", "")

      if (saltEnc.isEmpty() || hashEnc.isEmpty()) {
        withContext(Dispatchers.Main) {
          isLoading = false
          isError = true
          errorText = "System credentials error. Re-run setup wizard."
          triggerShake()
        }
        return@launch
      }

      val saltHex = KeyStoreHelper.decrypt(saltEnc)
      val expectedHashHex = KeyStoreHelper.decrypt(hashEnc)

      if (saltHex.isEmpty() || expectedHashHex.isEmpty()) {
        withContext(Dispatchers.Main) {
          isLoading = false
          isError = true
          errorText = "Keystore decryption error."
          triggerShake()
        }
        return@launch
      }

      // Convert salt hex string back to byte array
      val saltBytes = ByteArray(saltHex.length / 2)
      for (i in saltBytes.indices) {
        val index = i * 2
        val j = saltHex.substring(index, index + 2).toInt(16)
        saltBytes[i] = j.toByte()
      }

      val inputHashBytes = CryptoUtils.hashPassword(passwordInput.toCharArray(), saltBytes)
      val inputHashHex = inputHashBytes.joinToString("") { "%02x".format(it) }

      if (inputHashHex == expectedHashHex) {
        // Success
        dbHelper.saveConfig("failed_attempts_count", "0")
        dbHelper.saveConfig("lock_expiration_time", "0")
        dbHelper.saveConfig("fingerprint_temp_disabled", "false")

        val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
        val username = dbHelper.getConfig("owner_username", "Rozuly")
        val password = dbHelper.getConfig("owner_password", "")

        try {
          if (password.isNotEmpty()) {
            val loginPayload = JSONObject().apply {
              put("username", username)
              put("password", password)
            }
            val responseRaw = NetworkUtils.httpPost("$serverUrl/auth/login", loginPayload.toString())
            val res = JSONObject(responseRaw)
            if (res.getBoolean("success")) {
              val data = res.getJSONObject("data")
              val token = data.getString("token")
              dbHelper.saveConfig("session_token", token)
              dbHelper.saveConfig("is_ai_key_synced", "false")
              dbHelper.saveConfig("is_social_synced", "false")
            }
          }
        } catch (e: Exception) {
          android.util.Log.e("LockScreen", "Login server synch error: ${e.message}")
        }

        withContext(Dispatchers.Main) {
          isLoading = false
          onUnlock()
        }
      } else {
        // Failure
        val currentFailed = (dbHelper.getConfig("failed_attempts_count", "0").toIntOrNull() ?: 0) + 1
        dbHelper.saveConfig("failed_attempts_count", currentFailed.toString())

        var lockoutMessage = ""
        val now = System.currentTimeMillis()
        if (currentFailed >= 10) {
          dbHelper.saveConfig("fingerprint_temp_disabled", "true")
          lockoutMessage = "Fingerprint disabled. Passcode only."
        } else if (currentFailed >= 5) {
          dbHelper.saveConfig("lock_expiration_time", (now + 300_000).toString()) // 5 min lockout
          lockoutMessage = "Too many failures. Lockout: 5 minutes."
        } else if (currentFailed >= 3) {
          dbHelper.saveConfig("lock_expiration_time", (now + 30_000).toString()) // 30s lockout
          lockoutMessage = "Lockout: 30 seconds."
        }

        withContext(Dispatchers.Main) {
          isLoading = false
          isError = true
          errorText = if (lockoutMessage.isNotEmpty()) {
            lockoutMessage
          } else {
            "Incorrect password. Remaining attempts: ${10 - currentFailed}"
          }
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
        enabled = remainingCooldownSeconds <= 0L && !isLoading,
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
          .height(24.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        if (remainingCooldownSeconds > 0L) {
          Text(
            text = "Locked out. Try again in $remainingCooldownSeconds seconds.",
            color = ColorError,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        } else if (isError) {
          Text(
            text = errorText,
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
        // Fingerprint button (Only show if enabled and not locked out)
        if (isFingerprintEnabled && !isFingerprintTempDisabled && failedAttemptsCount < 10) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(BgCard)
              .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
              .clickable(enabled = remainingCooldownSeconds <= 0L && !isLoading) {
                showBiometricPrompt()
              },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = "Fingerprint Authentication",
              tint = Secondary,
              modifier = Modifier.size(28.dp)
            )
          }
        }

        // Primary Unlock Button
        PremiumButton(
          text = if (isLoading) "Unlocking..." else "Unlock",
          onClick = { attemptUnlock() },
          modifier = Modifier.weight(1f),
          enabled = !isLoading && remainingCooldownSeconds <= 0L
        )
      }
    }
  }
}
