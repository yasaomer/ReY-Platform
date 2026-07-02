package com.example.privateoml.ui.sync

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import com.example.privateoml.utils.NetworkUtils
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────

enum class TaskPriority(val label: String, val color: Color) {
    CRITICAL("Critical", Color(0xFFFF5252)),
    HIGH("High",     Color(0xFFFF9800)),
    NORMAL("Normal", Color(0xFF9B5DE5)),
    LOW("Low",       Color(0xFF00BBF9)),
    BACKGROUND("Background", Color(0xFF9499B3))
}

enum class TaskStatus(val label: String, val color: Color) {
    PENDING("Pending",       Color(0xFFFFD166)),
    RUNNING("Running",       Color(0xFF00BBF9)),
    COMPLETED("Completed",   Color(0xFF00E676)),
    FAILED("Failed",         Color(0xFFFF5252)),
    RETRYING("Retrying",     Color(0xFFFF9800)),
    CANCELLED("Cancelled",   Color(0xFF9499B3))
}

enum class ModuleStatus(val label: String, val color: Color) {
    ONLINE("Online",           Color(0xFF00E676)),
    OFFLINE("Offline",         Color(0xFFFF5252)),
    SYNCING("Syncing",         Color(0xFF00BBF9)),
    WAITING("Waiting",         Color(0xFFFFD166)),
    RETRYING("Retrying",       Color(0xFFFF9800)),
    COMPLETED("Completed",     Color(0xFF00E676)),
    ERROR("Error",             Color(0xFFFF5252))
}

data class SyncTask(
    val id: String,
    val module: String,
    val priority: TaskPriority,
    var status: TaskStatus,
    val created: String,
    var started: String = "—",
    var finished: String = "—",
    var retryCount: Int = 0,
    var errorMsg: String = ""
)

data class ModuleSync(
    val name: String,
    val icon: ImageVector,
    var status: ModuleStatus,
    var lastSync: String = "Never"
)

data class LogEntry(
    val time: String,
    val category: String,
    val level: String,    // INFO / WARN / ERROR
    val message: String
)

// ─────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────

private fun now(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

private fun nowFull(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

// ─────────────────────────────────────────────────────────────
//  MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncManagementPanel(dbHelper: DatabaseHelper) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Settings ──────────────────────────────────────────────
    var autoSync         by remember { mutableStateOf(dbHelper.getConfig("sync_auto", "true") == "true") }
    var syncInterval     by remember { mutableStateOf(dbHelper.getConfig("sync_interval", "15")) }
    var maxParallel      by remember { mutableStateOf(dbHelper.getConfig("sync_max_parallel", "3")) }
    var retryCount       by remember { mutableStateOf(dbHelper.getConfig("sync_retry_count", "3")) }
    var retryDelay       by remember { mutableStateOf(dbHelper.getConfig("sync_retry_delay", "5")) }
    var wifiOnly         by remember { mutableStateOf(dbHelper.getConfig("sync_wifi_only", "false") == "true") }
    var compression      by remember { mutableStateOf(dbHelper.getConfig("sync_compression", "true") == "true") }
    var debugLogging     by remember { mutableStateOf(dbHelper.getConfig("sync_debug", "false") == "true") }
    var backgroundSync   by remember { mutableStateOf(dbHelper.getConfig("sync_background", "true") == "true") }

    // ── Live state ────────────────────────────────────────────
    var activeTab        by remember { mutableStateOf("Dashboard") }
    var isGlobalSyncing  by remember { mutableStateOf(false) }
    var isPaused         by remember { mutableStateOf(false) }
    var backendPing      by remember { mutableStateOf("—") }
    var queueFilter      by remember { mutableStateOf("All") }
    var queueSearch      by remember { mutableStateOf("") }
    var logFilter        by remember { mutableStateOf("All") }
    var logSearch        by remember { mutableStateOf("") }

    val spinAngle by animateFloatAsState(
        targetValue   = if (isGlobalSyncing) 360f else 0f,
        animationSpec = if (isGlobalSyncing)
            infiniteRepeatable(tween(900, easing = LinearEasing))
        else tween(0),
        label = "spin"
    )

    val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
    val token     = dbHelper.getConfig("session_token", "")

    // ── Module statuses ───────────────────────────────────────
    val modules = remember {
        mutableStateListOf(
            ModuleSync("Gallery",      Icons.Default.Folder,       ModuleStatus.ONLINE,  "2 min ago"),
            ModuleSync("Last Message", Icons.Default.Message,      ModuleStatus.ONLINE,  "5 min ago"),
            ModuleSync("AI Config",    Icons.Default.SmartToy,     ModuleStatus.ONLINE,  "10 min ago"),
            ModuleSync("Knowledge Base", Icons.Default.LibraryBooks, ModuleStatus.WAITING, "1 hr ago"),
            ModuleSync("Social Media", Icons.Default.Share,        ModuleStatus.ONLINE,  "3 min ago"),
            ModuleSync("Location",     Icons.Default.LocationOn,   ModuleStatus.SYNCING, "Just now"),
            ModuleSync("Analytics",    Icons.Default.Analytics,    ModuleStatus.ONLINE,  "15 min ago"),
            ModuleSync("Settings",     Icons.Default.Settings,     ModuleStatus.ONLINE,  "1 hr ago"),
            ModuleSync("Notifications",Icons.Default.Notifications,ModuleStatus.ONLINE,  "30 min ago"),
            ModuleSync("Logs",         Icons.Default.List,         ModuleStatus.ONLINE,  "5 min ago"),
            ModuleSync("Backups",      Icons.Default.Backup,       ModuleStatus.WAITING, "Yesterday")
        )
    }

    // ── Task Queue ────────────────────────────────────────────
    val queue = remember {
        mutableStateListOf(
            SyncTask("t-001", "Location",     TaskPriority.CRITICAL, TaskStatus.RUNNING,   nowFull(), nowFull()),
            SyncTask("t-002", "AI Config",    TaskPriority.HIGH,     TaskStatus.PENDING,   nowFull()),
            SyncTask("t-003", "Gallery",      TaskPriority.NORMAL,   TaskStatus.COMPLETED, nowFull(), nowFull(), nowFull()),
            SyncTask("t-004", "Analytics",    TaskPriority.LOW,      TaskStatus.PENDING,   nowFull()),
            SyncTask("t-005", "Log Cleanup",  TaskPriority.BACKGROUND, TaskStatus.PENDING, nowFull()),
            SyncTask("t-006", "Last Message", TaskPriority.HIGH,     TaskStatus.FAILED,    nowFull(), nowFull(), nowFull(), 2, "Network timeout after 30s"),
        )
    }

    // ── Log entries ───────────────────────────────────────────
    val logs = remember {
        mutableStateListOf(
            LogEntry(now(), "Sync",     "INFO",  "Pull task: ${queue[0].id} dispatched to worker"),
            LogEntry(now(), "Location", "INFO",  "GPS fix obtained: ±8 m accuracy"),
            LogEntry(now(), "Gallery",  "INFO",  "3 images uploaded to Google Drive"),
            LogEntry(now(), "AI",       "INFO",  "Configuration synced to backend"),
            LogEntry(now(), "Backend",  "WARN",  "Response time elevated: 820 ms"),
            LogEntry(now(), "Auth",     "INFO",  "Session refreshed successfully"),
            LogEntry(now(), "Sync",     "ERROR", "Task t-006 failed after 2 retries: Network timeout"),
            LogEntry(now(), "Database", "INFO",  "D1 query executed in 14 ms"),
        )
    }

    // ── Connection statuses ───────────────────────────────────
    data class Connection(val name: String, var online: Boolean, var ping: String)
    val connections = remember {
        mutableStateListOf(
            Connection("Internet",     true,  "—"),
            Connection("Backend",      true,  "—"),
            Connection("Website",      true,  "—"),
            Connection("Cloudflare",   true,  "—"),
            Connection("Google Drive", true,  "—"),
            Connection("AI Provider",  true,  "—"),
            Connection("Database",     true,  "—")
        )
    }

    // ── Derived stats ─────────────────────────────────────────
    val totalTasks    = queue.size
    val pendingTasks  = queue.count { it.status == TaskStatus.PENDING }
    val runningTasks  = queue.count { it.status == TaskStatus.RUNNING }
    val failedTasks   = queue.count { it.status == TaskStatus.FAILED }
    val completedTasks = queue.count { it.status == TaskStatus.COMPLETED }

    // ── Actions ───────────────────────────────────────────────

    fun pingBackend() {
        scope.launch(Dispatchers.IO) {
            try {
                val start = System.currentTimeMillis()
                NetworkUtils.httpGet("$serverUrl/../health", "")
                val ms = System.currentTimeMillis() - start
                withContext(Dispatchers.Main) { backendPing = "${ms} ms" }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { backendPing = "Unreachable" }
            }
        }
    }

    fun runFullSync() {
        scope.launch {
            isGlobalSyncing = true
            logs.add(0, LogEntry(now(), "Sync", "INFO", "Global sync initiated by owner"))
            delay(1200)
            modules.forEachIndexed { i, m ->
                modules[i] = m.copy(status = ModuleStatus.SYNCING)
                delay(300)
                modules[i] = m.copy(status = ModuleStatus.COMPLETED, lastSync = "Just now")
            }
            dbHelper.saveConfig("last_sync_time", nowFull())
            logs.add(0, LogEntry(now(), "Sync", "INFO", "Global sync completed successfully"))
            isGlobalSyncing = false
            Toast.makeText(context, "✓ Full sync completed", Toast.LENGTH_SHORT).show()
        }
    }

    fun retryFailed() {
        queue.forEachIndexed { i, t ->
            if (t.status == TaskStatus.FAILED) {
                queue[i] = t.copy(status = TaskStatus.PENDING, retryCount = t.retryCount + 1, errorMsg = "")
                logs.add(0, LogEntry(now(), t.module, "INFO", "Task ${t.id} re-queued for retry"))
            }
        }
        Toast.makeText(context, "Failed tasks re-queued", Toast.LENGTH_SHORT).show()
    }

    fun clearCompleted() {
        val removed = queue.count { it.status == TaskStatus.COMPLETED }
        queue.removeAll { it.status == TaskStatus.COMPLETED }
        logs.add(0, LogEntry(now(), "Sync", "INFO", "$removed completed tasks cleared from queue"))
    }

    // Filtered queue
    val filteredQueue = queue.filter { t ->
        (queueSearch.isBlank() || t.id.contains(queueSearch, true) || t.module.contains(queueSearch, true)) &&
        (queueFilter == "All" || t.status.label == queueFilter)
    }

    // Filtered logs
    val filteredLogs = logs.filter { l ->
        (logSearch.isBlank() || l.message.contains(logSearch, true) || l.category.contains(logSearch, true)) &&
        (logFilter == "All" || l.category == logFilter || l.level == logFilter)
    }

    // Auto-start ping on open
    LaunchedEffect(Unit) { pingBackend() }

    // ─────────────────────────────────────────────────────────
    //  LAYOUT
    // ─────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(700.dp)
            .padding(horizontal = 2.dp)
    ) {
        // Header
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Sync Engine", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("v2.1.0 • ${if (isPaused) "Paused" else if (isGlobalSyncing) "Syncing…" else "Idle"}", color = TextSecondary, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Pause/Resume
                IconButton(onClick = {
                    isPaused = !isPaused
                    logs.add(0, LogEntry(now(), "Sync", "INFO", if (isPaused) "Queue paused" else "Queue resumed"))
                }) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = if (isPaused) ColorSuccess else ColorWarning, modifier = Modifier.size(20.dp))
                }
                // Global sync button
                IconButton(onClick = { runFullSync() }) {
                    Icon(Icons.Default.Sync, null, tint = Primary, modifier = Modifier.size(20.dp).rotate(spinAngle))
                }
            }
        }

        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf("Dashboard", "Modules", "Queue", "Connections", "History", "Logs", "Settings", "Performance", "Diagnostics").forEach { tab ->
                val active = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (active) Primary else BgCard)
                        .clickable { activeTab = tab }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                ) {
                    Text(tab, color = if (active) Color.Black else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {

                // ══════════════════════════════════════════
                // TAB 1 — DASHBOARD
                // ══════════════════════════════════════════
                "Dashboard" -> {
                    // Stat grid
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatCard("Total",     "$totalTasks",     Icons.Default.FormatListBulleted, Modifier.weight(1f))
                        MiniStatCard("Running",   "$runningTasks",   Icons.Default.PlayArrow,         Modifier.weight(1f))
                        MiniStatCard("Pending",   "$pendingTasks",   Icons.Default.HourglassEmpty,    Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatCard("Completed", "$completedTasks", Icons.Default.CheckCircle, Modifier.weight(1f))
                        MiniStatCard("Failed",    "$failedTasks",    Icons.Default.Cancel,      Modifier.weight(1f))
                        MiniStatCard("Ping",      backendPing,       Icons.Default.NetworkCheck, Modifier.weight(1f))
                    }

                    // Primary actions
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumButton(
                            text    = if (isGlobalSyncing) "Syncing…" else "Sync All",
                            onClick = { runFullSync() },
                            enabled = !isGlobalSyncing,
                            modifier = Modifier.weight(1f)
                        )
                        PremiumButton(
                            text    = "Retry Failed",
                            onClick = { retryFailed() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // System status card
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("System Status", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            SyncStatusRow("Internet",          true)
                            SyncStatusRow("Backend",           true)
                            SyncStatusRow("Cloudflare",        true)
                            SyncStatusRow("Database (D1)",     true)
                            SyncStatusRow("Google Drive",      true)
                            SyncStatusRow("AI Provider",       true)
                            SyncStatusRow("Queue",             !isPaused)
                            SyncStatusRow("Auto Sync",         autoSync)
                        }
                    }

                    // Last sync info
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Sync Timeline", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            InfoPair("Last Sync",     dbHelper.getConfig("last_sync_time", "Never"))
                            InfoPair("Upload Now",    "0 KB/s")
                            InfoPair("Download Now",  "0 KB/s")
                            InfoPair("Avg Time",      "2.3 s")
                            InfoPair("Last Failure",  "t-006 — Network timeout")
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 2 — MODULES
                // ══════════════════════════════════════════
                "Modules" -> {
                    Text("Module Sync Status", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    modules.forEachIndexed { i, mod ->
                        PremiumCard {
                            Row(
                                modifier              = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(mod.icon, null, tint = Secondary, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(mod.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Last: ${mod.lastSync}", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(mod.status.color.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(mod.status.label, color = mod.status.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Manual sync per module
                                    IconButton(onClick = {
                                        scope.launch {
                                            modules[i] = mod.copy(status = ModuleStatus.SYNCING)
                                            delay(800)
                                            modules[i] = mod.copy(status = ModuleStatus.COMPLETED, lastSync = "Just now")
                                            logs.add(0, LogEntry(now(), mod.name, "INFO", "${mod.name} synced manually"))
                                        }
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Refresh, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 3 — QUEUE
                // ══════════════════════════════════════════
                "Queue" -> {
                    // Controls row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumButton("Retry All",         { retryFailed() },   Modifier.weight(1f))
                        PremiumButton("Clear Done",        { clearCompleted() }, Modifier.weight(1f))
                    }

                    // Filter chips
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Pending", "Running", "Completed", "Failed").forEach { f ->
                            val sel = queueFilter == f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) Secondary.copy(alpha = 0.15f) else BgCard)
                                    .clickable { queueFilter = f }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(f, color = if (sel) Secondary else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Search
                    OutlinedTextField(
                        value         = queueSearch,
                        onValueChange = { queueSearch = it },
                        placeholder   = { Text("Search task ID or module…", color = TextSecondary, fontSize = 12.sp) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth().height(50.dp),
                        shape         = RoundedCornerShape(8.dp),
                        colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BgCard)
                    )

                    // Task cards
                    filteredQueue.forEach { task ->
                        PremiumCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(task.id, color = Secondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        PriorityBadge(task.priority)
                                    }
                                    StatusBadge(task.status)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Module: ${task.module}", color = TextPrimary, fontSize = 12.sp)
                                    Text("Retries: ${task.retryCount}", color = TextSecondary, fontSize = 11.sp)
                                }
                                Text("Created: ${task.created}", color = TextSecondary, fontSize = 10.sp)
                                if (task.errorMsg.isNotEmpty()) {
                                    Text("⚠ ${task.errorMsg}", color = ColorError, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                // Cancel action
                                if (task.status == TaskStatus.PENDING || task.status == TaskStatus.RUNNING) {
                                    TextButton(onClick = {
                                        val idx = queue.indexOfFirst { it.id == task.id }
                                        if (idx >= 0) queue[idx] = task.copy(status = TaskStatus.CANCELLED)
                                        logs.add(0, LogEntry(now(), task.module, "INFO", "Task ${task.id} cancelled"))
                                    }, modifier = Modifier.height(28.dp)) {
                                        Text("Cancel", color = ColorError, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (filteredQueue.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No tasks match the filter", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 4 — CONNECTIONS
                // ══════════════════════════════════════════
                "Connections" -> {
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Connection Monitor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { pingBackend() }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Refresh, null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                            }
                            connections.forEach { conn ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (conn.online) ColorSuccess else ColorError))
                                        Text(conn.name, color = TextPrimary, fontSize = 13.sp)
                                    }
                                    Text(
                                        if (conn.online) "Online" else "Offline",
                                        color      = if (conn.online) ColorSuccess else ColorError,
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Network Diagnostics", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            InfoPair("Backend Ping",    backendPing)
                            InfoPair("Packet Loss",     "0%")
                            InfoPair("Avg Latency",     "320 ms")
                            InfoPair("Bandwidth",       "~12 Mbps")
                            InfoPair("Quality",         "Good")
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 5 — HISTORY
                // ══════════════════════════════════════════
                "History" -> {
                    Text("Synchronization History", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    val history = listOf(
                        listOf("Today 18:42", "3.2 s", "Gallery",      "Completed", "0", "3 images uploaded"),
                        listOf("Today 18:30", "1.1 s", "AI Config",    "Completed", "0", "Settings synced"),
                        listOf("Today 18:15", "30 s",  "Location",     "Failed",    "2", "Network timeout"),
                        listOf("Today 18:00", "0.8 s", "Last Message", "Completed", "0", "Banner updated"),
                        listOf("Today 17:45", "2.4 s", "Knowledge",    "Completed", "0", "12 docs indexed"),
                        listOf("Yesterday",   "5.1 s", "Backup",       "Completed", "0", "DB exported"),
                    )
                    history.forEach { row ->
                        PremiumCard {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(row[0], color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background((if (row[3] == "Completed") ColorSuccess else ColorError).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(row[3], color = if (row[3] == "Completed") ColorSuccess else ColorError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Module: ${row[2]}  •  Duration: ${row[1]}  •  Retries: ${row[4]}", color = TextSecondary, fontSize = 11.sp)
                                Text(row[5], color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 6 — LOG VIEWER
                // ══════════════════════════════════════════
                "Logs" -> {
                    // Controls
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = logSearch,
                            onValueChange = { logSearch = it },
                            placeholder   = { Text("Search logs…", color = TextSecondary, fontSize = 12.sp) },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f).height(50.dp),
                            shape         = RoundedCornerShape(8.dp),
                            colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BgCard)
                        )
                        FilterChip(selected = logFilter != "All", onClick = {
                            logFilter = when (logFilter) {
                                "All" -> "ERROR"
                                "ERROR" -> "WARN"
                                "WARN"  -> "INFO"
                                else    -> "All"
                            }
                        }, label = { Text(logFilter, fontSize = 11.sp) })
                    }

                    // Log rows
                    filteredLogs.forEach { entry ->
                        val levelColor = when (entry.level) {
                            "ERROR" -> ColorError
                            "WARN"  -> ColorWarning
                            else    -> ColorSuccess
                        }
                        PremiumCard {
                            Row(
                                modifier          = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier        = Modifier.width(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(entry.level, color = levelColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(entry.category, color = Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(entry.time, color = TextSecondary, fontSize = 9.sp)
                                    }
                                    Text(entry.message, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }

                    // Clear logs button
                    PremiumButton(
                        text     = "Clear All Logs",
                        onClick  = { logs.clear(); logs.add(LogEntry(now(), "System", "INFO", "Logs cleared by owner")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ══════════════════════════════════════════
                // TAB 7 — SETTINGS
                // ══════════════════════════════════════════
                "Settings" -> {
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Synchronization Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            SettingToggle("Auto Synchronization",    autoSync)    { autoSync    = it; dbHelper.saveConfig("sync_auto", it.toString()) }
                            SettingToggle("Background Sync",         backgroundSync) { backgroundSync = it; dbHelper.saveConfig("sync_background", it.toString()) }
                            SettingToggle("Wi-Fi Only",              wifiOnly)    { wifiOnly    = it; dbHelper.saveConfig("sync_wifi_only", it.toString()) }
                            SettingToggle("Compression",             compression) { compression = it; dbHelper.saveConfig("sync_compression", it.toString()) }
                            SettingToggle("Debug Logging",           debugLogging){ debugLogging = it; dbHelper.saveConfig("sync_debug", it.toString()) }

                            HorizontalDivider(color = BgCard)

                            SyncTextField("Sync Interval (min)", syncInterval) { syncInterval = it; dbHelper.saveConfig("sync_interval", it) }
                            SyncTextField("Max Parallel Tasks",  maxParallel)  { maxParallel  = it; dbHelper.saveConfig("sync_max_parallel", it) }
                            SyncTextField("Retry Count",         retryCount)   { retryCount   = it; dbHelper.saveConfig("sync_retry_count", it) }
                            SyncTextField("Retry Delay (s)",     retryDelay)   { retryDelay   = it; dbHelper.saveConfig("sync_retry_delay", it) }
                        }
                    }

                    // Conflict resolution
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Conflict Resolution", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val strategies = listOf("Always Prefer Local", "Always Prefer Remote", "Manual Review")
                            var sel by remember { mutableStateOf("Always Prefer Local") }
                            strategies.forEach { s ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { sel = s },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(s, color = TextPrimary, fontSize = 12.sp)
                                    RadioButton(selected = sel == s, onClick = { sel = s })
                                }
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 8 — PERFORMANCE
                // ══════════════════════════════════════════
                "Performance" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatCard("Workers",    "3",      Icons.Default.DeveloperBoard, Modifier.weight(1f))
                        MiniStatCard("Threads",    "12",     Icons.Default.Memory,         Modifier.weight(1f))
                        MiniStatCard("Avg Task",  "2.3 s",  Icons.Default.Timer,          Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatCard("Max Task",  "30 s",   Icons.Default.TimerOff,   Modifier.weight(1f))
                        MiniStatCard("Queue Len", "$totalTasks", Icons.Default.FormatListNumbered, Modifier.weight(1f))
                        MiniStatCard("Storage",   "14 MB",  Icons.Default.Storage,    Modifier.weight(1f))
                    }
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Worker Status", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            listOf("SyncPullWorker" to "Active", "LocationWorker" to "Active", "GalleryWorker" to "Idle", "AnalyticsWorker" to "Idle").forEach { (name, st) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text(st, color = if (st == "Active") ColorSuccess else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ══════════════════════════════════════════
                // TAB 9 — DIAGNOSTICS
                // ══════════════════════════════════════════
                "Diagnostics" -> {
                    PremiumCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Developer Diagnostics", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            InfoPair("Sync Engine Version", "2.1.0")
                            InfoPair("API Version",         "v1")
                            InfoPair("DB Version",          "D1 R2 Schema v4")
                            InfoPair("Build",               "debug")
                            InfoPair("Branch",              "main")
                            InfoPair("Backend URL",         serverUrl.take(40) + "…")
                            InfoPair("Session Token",       if (token.length > 8) token.take(8) + "…" else "Not set")
                            InfoPair("Last Ping",           backendPing)
                            InfoPair("Workers",             "4 registered")
                            InfoPair("KV Cache",            "Active")
                        }
                    }
                    PremiumButton(
                        text    = "Re-ping Backend",
                        onClick = { pingBackend(); Toast.makeText(context, "Pinging…", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────

@Composable
private fun MiniStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard {
        Column(
            modifier            = modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Secondary, modifier = Modifier.size(18.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(title, color = TextSecondary, fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SyncStatusRow(label: String, ok: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (ok) ColorSuccess else ColorError))
            Text(label, color = TextPrimary, fontSize = 12.sp)
        }
        Text(if (ok) "OK" else "Offline", color = if (ok) ColorSuccess else ColorError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1.1f))
        Text(value, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(priority.color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(priority.label, color = priority.color, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StatusBadge(status: TaskStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(status.color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.label, color = status.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingToggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp)
        Switch(checked = value, onCheckedChange = onChange, modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SyncTextField(label: String, value: String, onSave: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value         = text,
        onValueChange = { text = it; onSave(it) },
        label         = { Text(label, fontSize = 11.sp) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(8.dp),
        colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BgCard)
    )
}
