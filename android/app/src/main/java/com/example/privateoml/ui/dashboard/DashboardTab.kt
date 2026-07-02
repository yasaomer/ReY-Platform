package com.example.privateoml.ui.dashboard

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardTab(
  serverUrl: String,
  lastSyncTime: String,
  onRefresh: () -> Unit
) {
  val context = LocalContext.current
  val dbHelper = remember { DatabaseHelper(context) }
  val coroutineScope = rememberCoroutineScope()

  // Live Auto-updating Time and Date
  var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
  LaunchedEffect(key1 = true) {
    while (true) {
      currentTime = LocalDateTime.now()
      delay(1000)
    }
  }

  val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  // Mock live values that fluctuate for a premium "active monitor" look
  var pingValue by remember { mutableIntStateOf(42) }
  var speedValue by remember { mutableStateOf("12.4 Mbps") }
  var cpuUsage by remember { mutableIntStateOf(14) }
  var ramUsage by remember { mutableIntStateOf(48) }

  LaunchedEffect(key1 = true) {
    while (true) {
      delay(3000)
      pingValue = (35..55).random()
      cpuUsage = (8..28).random()
      ramUsage = (45..53).random()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgBase)
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    
    // ==========================================
    // ANIMATED HEADER
    // ==========================================
    PremiumCard {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Private OML",
              color = Color.White,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = currentTime.format(dateFormatter), color = TextSecondary, fontSize = 12.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = currentTime.format(timeFormatter),
                color = Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Owner Avatar Placeholder
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(CircleShape)
              .background(Primary.copy(alpha = 0.2f))
              .border(2.dp, Primary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "YO",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color(0x1AFFFFFF))
        Spacer(modifier = Modifier.height(16.dp))

        // Connection Indicators Horizontal Flow
        Text(text = "Gateway Integrations", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val indicators = listOf(
            "Sync" to ColorSuccess,
            "Net" to ColorSuccess,
            "GDrive" to ColorSuccess,
            "Cloudflare" to ColorSuccess,
            "Web" to ColorSuccess,
            "AI" to ColorSuccess
          )
          
          indicators.forEach { (label, color) ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(BgBase)
                .padding(vertical = 4.dp, horizontal = 6.dp),
              horizontalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(color, CircleShape)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = label,
                color = TextPrimary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    // ==========================================
    // SYSTEM HEALTH SECTION
    // ==========================================
    Text(text = "System Health Logs", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    
    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val healths = listOf(
          Triple("Website Platform", "Online", ColorSuccess),
          Triple("Serverless API Gateway", "Healthy", ColorSuccess),
          Triple("SQLite Cache DB", "Healthy", ColorSuccess),
          Triple("Google Drive", "Healthy", ColorSuccess),
          Triple("Cloudflare Edge Network", "Online", ColorSuccess),
          Triple("Sync Service Daemon", "Healthy", ColorSuccess),
          Triple("AI Gemini Provider", "Healthy", ColorSuccess),
          Triple("GPS Location Service", "Warning", ColorWarning)
        )

        healths.forEach { (name, status, color) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = name, color = TextPrimary, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // ==========================================
    // QUICK STATUS CARDS (GRID LAYOUT)
    // ==========================================
    Text(text = "Administrative Statistics", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      val statsList1 = listOf(
        Triple("Website Visits", "1,842", Icons.Default.Language),
        Triple("Gallery Views", "541", Icons.Default.RemoveRedEye),
        Triple("Images Uploaded", "942", Icons.Default.CloudUpload),
        Triple("Waiting Upload", "0", Icons.Default.HourglassEmpty)
      )
      val statsList2 = listOf(
        Triple("AI Conversations", "48", Icons.Default.Assistant),
        Triple("AI Messages Today", "12", Icons.Default.Message),
        Triple("Location Requests", "28", Icons.Default.MyLocation),
        Triple("Message Opens", "198", Icons.Default.MailOutline)
      )
      val statsList3 = listOf(
        Triple("Social Visits", "4,021", Icons.Default.Share),
        Triple("Upload Queue", "0", Icons.Default.List),
        Triple("KB Documents", "142", Icons.Default.Book),
        Triple("Storage Used", "94%", Icons.Default.Storage)
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statsList1.take(2).forEach { (title, valStr, icon) ->
          StatsCard(title = title, value = valStr, icon = icon, modifier = Modifier.weight(1f))
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statsList1.drop(2).forEach { (title, valStr, icon) ->
          StatsCard(title = title, value = valStr, icon = icon, modifier = Modifier.weight(1f))
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statsList2.take(2).forEach { (title, valStr, icon) ->
          StatsCard(title = title, value = valStr, icon = icon, modifier = Modifier.weight(1f))
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statsList2.drop(2).forEach { (title, valStr, icon) ->
          StatsCard(title = title, value = valStr, icon = icon, modifier = Modifier.weight(1f))
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statsList3.take(2).forEach { (title, valStr, icon) ->
          StatsCard(title = title, value = valStr, icon = icon, modifier = Modifier.weight(1f))
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        statsList3.drop(2).forEach { (title, valStr, icon) ->
          StatsCard(title = title, value = valStr, icon = icon, modifier = Modifier.weight(1f))
        }
      }
    }

    // ==========================================
    // UPLOAD MONITOR
    // ==========================================
    Text(text = "Active Upload Monitor", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Current Item", color = TextSecondary, fontSize = 11.sp)
            Text("IMG_20260702_1509.png", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
          Text("Active", color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Linear Progress bar
        LinearProgressIndicator(
          progress = { 0.65f },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = Primary,
          trackColor = BgBase
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Speed: $speedValue", color = TextSecondary, fontSize = 11.sp)
          Text("ETA: 12 seconds", color = TextSecondary, fontSize = 11.sp)
        }
      }
    }

    // ==========================================
    // LIVE CONNECTION PANEL
    // ==========================================
    Text(text = "Live Connection Speeds", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val connections = listOf(
          Triple("Ping Latency", "$pingValue ms", Secondary),
          Triple("Backend Response", "48 ms", Secondary),
          Triple("Cloudflare Edge CDN", "12 ms", Secondary),
          Triple("Google Drive Speed", "8.4 MB/s", Secondary),
          Triple("Current Queue Size", "0 items", TextSecondary)
        )

        connections.forEach { (name, value, color) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(name, color = TextPrimary, fontSize = 13.sp)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // ==========================================
    // AI PANEL
    // ==========================================
    Text(text = "AI Engine Parameters", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val aiParams = listOf(
          "Model Engine" to "Gemini 1.5 Flash",
          "Temperature" to "0.7 (Factual/Creative)",
          "Tokens Limit" to "1,000,000 max",
          "Today's Requests" to "12/100 messages",
          "Knowledge Base Context" to "142 files loaded"
        )

        aiParams.forEach { (name, value) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(name, color = TextPrimary, fontSize = 13.sp)
            Text(value, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // ==========================================
    // LOCATION PANEL
    // ==========================================
    Text(text = "GPS Diagnostics", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val gpsParams = listOf(
          "Tracker Status" to "Active (Service running)",
          "Background Permission" to "Granted (Always)",
          "Last Update Time" to lastSyncTime,
          "Location Accuracy" to "12.0 meters",
          "Current Coordinates" to "45.4642° N, 9.1900° E"
        )

        gpsParams.forEach { (name, value) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(name, color = TextPrimary, fontSize = 13.sp)
            Text(value, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // ==========================================
    // GOOGLE DRIVE PANEL
    // ==========================================
    Text(text = "Google Drive Cloud Bounds", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val driveParams = listOf(
          "Account Connected" to "yasaomer@gmail.com",
          "Backup Folder Path" to "My Drive/Private_OML_Backups",
          "Files Synchronized" to "1,842 files",
          "Used Storage space" to "12.4 GB",
          "Remaining space" to "987.6 GB"
        )

        driveParams.forEach { (name, value) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(name, color = TextPrimary, fontSize = 13.sp)
            Text(value, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // ==========================================
    // PERFORMANCE PANEL
    // ==========================================
    Text(text = "Hardware Performance Gauges", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // CPU Usage
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("CPU Usage", color = TextPrimary, fontSize = 13.sp)
            Text("$cpuUsage%", color = Secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { cpuUsage.toFloat() / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = Secondary,
            trackColor = BgBase
          )
        }

        // RAM Usage
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("RAM allocation", color = TextPrimary, fontSize = 13.sp)
            Text("$ramUsage%", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { ramUsage.toFloat() / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = Primary,
            trackColor = BgBase
          )
        }

        // Services Status
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Battery Optimization State", color = TextPrimary, fontSize = 13.sp)
          Text("Optimized", color = ColorSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Background Service Workers", color = TextPrimary, fontSize = 13.sp)
          Text("Active threads (1)", color = Secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // ==========================================
    // ANALYTICS INTEGRATIONS (CANVAS CHARTS)
    // ==========================================
    Text(text = "Historical Daily Telemetry", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Daily Platform Traffic (Visits vs Uploads)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        PremiumLineChart(modifier = Modifier.height(180.dp))
      }
    }

    // ==========================================
    // RECENT ACTIVITY LOGS TIMELINE
    // ==========================================
    Text(text = "System Activity Logs Timeline", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

    PremiumCard {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        val activities = listOf(
          Triple("Image Uploaded", "IMG_20260702_1509.png successfully backed up to Google Drive.", "10 mins ago"),
          Triple("AI Question Received", "User queried knowledge base for vector contexts matching 'health checks'.", "25 mins ago"),
          Triple("GPS Location Update", "Location broadcast coordinates updated to API server successfully.", "40 mins ago"),
          Triple("Website Login Check", "Admin session verified from IP 192.168.1.83 via Secure OTP check.", "1 hour ago"),
          Triple("Backup Completed", "Local SQLite configurations seeder exported to cloud folder successfully.", "3 hours ago")
        )

        activities.forEachIndexed { index, (action, detail, time) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
          ) {
            // Timeline line & node
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.width(20.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(Primary, CircleShape)
              )
              if (index < activities.size - 1) {
                Box(
                  modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(Color(0x33FFFFFF))
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(text = action, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = time, color = TextSecondary, fontSize = 11.sp)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = detail, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }
      }
    }
  }
}
