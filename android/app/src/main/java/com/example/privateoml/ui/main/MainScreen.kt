package com.example.privateoml.ui.main

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.privateoml.ui.dashboard.DashboardTab
import com.example.privateoml.ui.gallery.GalleryTab
import com.example.privateoml.ui.ai.AiTab
import com.example.privateoml.ui.knowledge.KnowledgeBasePanel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.privateoml.LastMessage
import com.example.privateoml.Settings
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val dbHelper = remember { DatabaseHelper(context) }
  val coroutineScope = rememberCoroutineScope()

  var selectedTab by remember { mutableIntStateOf(0) }
  
  // Sheet states for More tab details
  var activeSheet by remember { mutableStateOf<String?>(null) }

  // App configurations state
  var serverUrl by remember { mutableStateOf(dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")) }
  var geminiKey by remember { mutableStateOf(dbHelper.getConfig("gemini_api_key", "Not Set")) }
  var folderPath by remember { mutableStateOf(dbHelper.getConfig("gallery_folder_path", "Not Set")) }
  var lastSyncTime by remember { mutableStateOf(dbHelper.getConfig("last_sync_time", "Never")) }

  val tabs = listOf(
    Triple("Home", Icons.Default.Home, 0),
    Triple("Gallery", Icons.Default.Folder, 1),
    Triple("Composer", Icons.Default.Edit, 2),
    Triple("AI Portal", Icons.Default.Info, 3),
    Triple("GPS Map", Icons.Default.LocationOn, 4),
    Triple("More", Icons.Default.Settings, 5)
  )

  Scaffold(
    modifier = modifier.background(BgBase),
    bottomBar = {
      NavigationBar(
        containerColor = BgSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      ) {
        tabs.forEach { (label, icon, index) ->
          val selected = selectedTab == index
          NavigationBarItem(
            selected = selected,
            onClick = { selectedTab = index },
            icon = {
              Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Secondary else TextSecondary
              )
            },
            label = {
              Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextPrimary else TextSecondary
              )
            },
            colors = NavigationBarItemDefaults.colors(
              indicatorColor = Primary.copy(alpha = 0.2f)
            )
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(BgBase)
        .padding(innerPadding)
    ) {
      // Content dynamic transitions
      AnimatedContent(
        targetState = selectedTab,
        transitionSpec = {
          fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "tabContent"
      ) { targetTab ->
        when (targetTab) {
          0 -> DashboardTab(
            serverUrl = serverUrl,
            lastSyncTime = lastSyncTime,
            onRefresh = {
              coroutineScope.launch {
                val now = java.time.LocalDateTime.now().toString()
                dbHelper.saveConfig("last_sync_time", now)
                lastSyncTime = now
                Toast.makeText(context, "Telemetry synced successfully", Toast.LENGTH_SHORT).show()
              }
            }
          )
          1 -> GalleryTab(dbHelper = dbHelper)
          2 -> LastMessageTab(dbHelper = dbHelper)
          3 -> AiTab(dbHelper = dbHelper)
          4 -> LocationTab(dbHelper = dbHelper)
          5 -> MoreTab(
            onItemClick = onItemClick,
            onOpenSheet = { sheetName -> activeSheet = sheetName }
          )
        }
      }

      // Render Bottom Sheet detailing "More" actions
      if (activeSheet != null) {
        PremiumBottomSheet(
          title = activeSheet!!,
          onDismiss = { activeSheet = null }
        ) {
          when (activeSheet) {
            "Google Drive" -> GoogleDrivePanel(dbHelper)
            "Logs" -> LogsPanel()
            "Developer" -> DeveloperPanel(serverUrl) { serverUrl = it; dbHelper.saveConfig("server_url", it) }
            "Social Media" -> SocialMediaPanel()
            "Knowledge Base" -> KnowledgeBasePanel(dbHelper)
            "Synchronization" -> SyncPanel(lastSyncTime)
            "Backup" -> BackupPanel()
            "About" -> AboutPanel()
          }
        }
      }
    }
  }
}

// ==========================================
// TABS IMPLEMENTATION
// ==========================================





@Composable
fun LastMessageTab(
  dbHelper: DatabaseHelper
) {
  val context = LocalContext.current
  var statusInput by remember { mutableStateOf(dbHelper.getConfig("last_written_status", "")) }
  var isSending by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Compose Status",
      color = Color.White,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold
    )

    PremiumCard {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Broadcast message to ReY website banner directly.", color = TextSecondary, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
          value = statusInput,
          onValueChange = { statusInput = it },
          placeholder = { Text("What's on your mind?", color = TextSecondary) },
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = BgBase,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${statusInput.length} characters",
            color = TextSecondary,
            fontSize = 11.sp
          )
          
          PremiumButton(
            text = if (isSending) "Posting..." else "Post Status",
            onClick = {
              if (statusInput.isEmpty()) return@PremiumButton
              isSending = true
              coroutineScope.launch {
                delay(1000) // Aesthetic network simulation
                dbHelper.saveConfig("last_written_status", statusInput)
                isSending = false
                Toast.makeText(context, "Status updated online", Toast.LENGTH_SHORT).show()
              }
            },
            enabled = !isSending
          )
        }
      }
    }
    
    // Status preview card
    Text("Online Preview", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    PremiumCard {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .background(BgBase, RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        Text(
          text = if (statusInput.isNotEmpty()) statusInput else "No status configured yet. Type above to preview...",
          color = if (statusInput.isNotEmpty()) TextPrimary else TextSecondary,
          fontSize = 14.sp,
          fontStyle = if (statusInput.isNotEmpty()) androidx.compose.ui.text.font.FontStyle.Normal else androidx.compose.ui.text.font.FontStyle.Italic
        )
      }
    }
  }
}



@Composable
fun LocationTab(
  dbHelper: DatabaseHelper
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "GPS Coordinate Tracker",
      color = Color.White,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold
    )

    PremiumCard {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("GPS Tracker Service", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Switch(
            checked = true,
            onCheckedChange = {},
            colors = SwitchDefaults.colors(checkedThumbColor = Secondary, checkedTrackColor = Primary)
          )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text("Coordinates: 45.4642° N, 9.1900° E (Milan, Italy)", color = TextSecondary, fontSize = 13.sp)
      }
    }

    // Compass diagnostics
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      StatsCard(
        title = "Speed",
        value = "0.0 km/h",
        icon = Icons.Default.CompassCalibration,
        modifier = Modifier.weight(1f)
      )
      StatsCard(
        title = "Altitude",
        value = "120 m",
        icon = Icons.Default.LocationOn,
        modifier = Modifier.weight(1f)
      )
    }

    // Map logs list
    Text("Recent Location Broadcast Logs", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      items(
        listOf(
          "Broadcast success - 45.4642, 9.1900" to "10 mins ago",
          "Broadcast success - 45.4641, 9.1899" to "20 mins ago",
          "Broadcast success - 45.4640, 9.1898" to "30 mins ago"
        )
      ) { (log, time) ->
        PremiumCard {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(log, color = TextPrimary, fontSize = 13.sp)
            Text(time, color = TextSecondary, fontSize = 11.sp)
          }
        }
      }
    }
  }
}

@Composable
fun MoreTab(
  onItemClick: (NavKey) -> Unit,
  onOpenSheet: (String) -> Unit
) {
  val menuItems = listOf(
    Triple("Social Media", "Configure profiles sync rules", Icons.Default.Share),
    Triple("Knowledge Base", "Add vector search reference docs", Icons.Default.Info),
    Triple("Synchronization", "Manage background timers", Icons.Default.Refresh),
    Triple("Google Drive", "Cloud backup folder config", Icons.Default.CloudQueue),
    Triple("Logs", "View console diagnostics", Icons.Default.List),
    Triple("Backup", "Manual SQLite export tools", Icons.Default.Backup),
    Triple("Developer", "Toggle debug parameters", Icons.Default.BugReport),
    Triple("About", "Portal specifications version", Icons.Default.Help)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Control Hub",
      color = Color.White,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold
    )

    // Main Settings row (navigates to original Settings page)
    PremiumCard {
      Row(
        modifier = Modifier
          .clickable { onItemClick(Settings) }
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Global System Settings", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Manage passwords, API endpoints, folder bounds", color = TextSecondary, fontSize = 11.sp)
        }
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
      }
    }

    // Secondary cards grid mapping to bottom sheets panels
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f)
    ) {
      items(menuItems) { (title, desc, icon) ->
        PremiumCard {
          Column(
            modifier = Modifier
              .clickable { onOpenSheet(title) }
              .padding(12.dp)
              .fillMaxWidth()
          ) {
            Icon(imageVector = icon, contentDescription = title, tint = Secondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 12.sp)
          }
        }
      }
    }
  }
}

// ==========================================
// SUB-PANELS BOTTOM SHEETS IMPLEMENTATION
// ==========================================

@Composable
fun GoogleDrivePanel(dbHelper: DatabaseHelper) {
  var isLinked by remember { mutableStateOf(dbHelper.getConfig("drive_linked", "false") == "true") }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Status: ${if (isLinked) "Linked to Drive" else "Not Connected"}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
    Text("Synchronizes locally stored items automatically into drive backups folder structure.", color = TextSecondary, fontSize = 13.sp)
    PremiumButton(
      text = if (isLinked) "Unlink Account" else "Connect Google Account",
      onClick = {
        isLinked = !isLinked
        dbHelper.saveConfig("drive_linked", isLinked.toString())
      },
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun LogsPanel() {
  val mockLogs = listOf(
    "18:42:01 - SyncWorker started execution",
    "18:42:03 - Fetched pending task checklist (0 items)",
    "18:42:05 - Scan folder check complete",
    "18:43:00 - Battery state: 85% healthy",
    "18:43:10 - GPS tracking coordinates update triggered",
    "18:43:12 - Telemetry package uploaded success"
  )
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.height(200.dp)
  ) {
    items(mockLogs) { log ->
      Text(text = log, color = TextSecondary, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
  }
}

@Composable
fun DeveloperPanel(currentUrl: String, onUpdate: (String) -> Unit) {
  var urlInput by remember { mutableStateOf(currentUrl) }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    PremiumTextField(
      value = urlInput,
      onValueChange = { urlInput = it },
      label = "Server Endpoint URL"
    )
    PremiumButton(
      text = "Apply Changes",
      onClick = { onUpdate(urlInput) },
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun SocialMediaPanel() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Active Sync Integrations", color = TextPrimary, fontWeight = FontWeight.Bold)
    Text("• Instagram feed synchronization: Active", color = TextSecondary, fontSize = 13.sp)
    Text("• Facebook status broadcast: Inactive", color = TextSecondary, fontSize = 13.sp)
    Text("• Telegram logs dispatcher: Active", color = TextSecondary, fontSize = 13.sp)
  }
}



@Composable
fun SyncPanel(lastSyncTime: String) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Background Scheduler configurations", color = TextPrimary, fontWeight = FontWeight.Bold)
    Text("• Interval: 15 minutes", color = TextSecondary, fontSize = 13.sp)
    Text("• Execution mode: Foreground service thread", color = TextSecondary, fontSize = 13.sp)
    Text("• Last execution time: $lastSyncTime", color = TextSecondary, fontSize = 13.sp)
  }
}

@Composable
fun BackupPanel() {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Manual local backups data manager", color = TextSecondary, fontSize = 13.sp)
    PremiumButton(
      text = "Export SQLite Database",
      onClick = {},
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun AboutPanel() {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Private OML Portal App", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Text("Version: 2.1.0-Premium", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    Text("System: Google Antigravity Suite", color = TextSecondary, fontSize = 12.sp)
    Text("Architecture: Jetpack Compose Navigation 3 with SQLite open caching database.", color = TextSecondary, fontSize = 12.sp)
  }
}
