package com.example.privateoml

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.PrivateOMLTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val dbHelper = DatabaseHelper(this)
    val currentUrl = dbHelper.getConfig("server_url", "")
    if (currentUrl.isEmpty() || currentUrl.contains("localhost") || currentUrl.contains("127.0.0.1")) {
        dbHelper.saveConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
    }

    enableEdgeToEdge()

    // Auto-start ecosystem sync service
    val serviceIntent = android.content.Intent(this, com.example.privateoml.services.SyncPullWorker::class.java)
    try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          startForegroundService(serviceIntent)
      } else {
          startService(serviceIntent)
      }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Failed to start sync service on start: ${e.message}")
    }

    setContent {
      PrivateOMLTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
