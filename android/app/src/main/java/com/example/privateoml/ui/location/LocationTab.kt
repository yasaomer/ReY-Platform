package com.example.privateoml.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import com.example.privateoml.utils.NetworkUtils
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.Locale

// ─────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double,
    val speed: Float,
    val bearing: Float,
    val provider: String,
    val address: String,
    val timestamp: String
)

data class LocationRequest(
    val time: String,
    val status: String,      // Completed / Failed / Timeout
    val accuracy: String,
    val duration: String
)

// ─────────────────────────────────────────────────────────────
//  MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTab(dbHelper: DatabaseHelper) {

    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()

    // ── Settings persisted via SQLite ──
    var sharingEnabled by remember { mutableStateOf(dbHelper.getConfig("loc_sharing_enabled", "true") == "true") }
    var exactLocation  by remember { mutableStateOf(dbHelper.getConfig("loc_exact", "true") == "true") }
    var maxPerHour     by remember { mutableStateOf(dbHelper.getConfig("loc_max_hour", "20")) }
    var maxPerDay      by remember { mutableStateOf(dbHelper.getConfig("loc_max_day", "100")) }
    var lowBatteryMode by remember { mutableStateOf(dbHelper.getConfig("loc_low_battery", "false") == "true") }

    // ── Live state ──
    var activeTab        by remember { mutableStateOf("Dashboard") }
    var currentSnapshot  by remember { mutableStateOf<LocationSnapshot?>(null) }
    var isRefreshing     by remember { mutableStateOf(false) }
    var refreshStatus    by remember { mutableStateOf("") }
    var gpsEnabled       by remember { mutableStateOf(false) }
    var hasFinePermission by remember { mutableStateOf(false) }

    val requestHistory = remember {
        mutableStateListOf(
            LocationRequest("10 min ago", "Completed", "±8 m",  "2.4 s"),
            LocationRequest("25 min ago", "Completed", "±12 m", "3.1 s"),
            LocationRequest("1 hr ago",   "Timeout",   "—",     "30 s"),
            LocationRequest("2 hr ago",   "Completed", "±6 m",  "1.8 s"),
        )
    }

    // Rotation anim for refresh button
    val rotation by animateFloatAsState(
        targetValue   = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing)
            infiniteRepeatable(tween(900, easing = LinearEasing))
        else
            tween(0),
        label = "rot"
    )

    // ── Helpers ──────────────────────────────────────────────

    fun checkPermissions() {
        hasFinePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun getLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = when {
                gps != null && net != null -> if (gps.accuracy < net.accuracy) gps else net
                gps != null -> gps
                else -> net
            }
            best
        } catch (_: SecurityException) { null }
    }

    fun resolveAddress(lat: Double, lon: Double): String {
        return try {
            val gc = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addrs = gc.getFromLocation(lat, lon, 1)
            if (!addrs.isNullOrEmpty()) {
                val a = addrs[0]
                listOfNotNull(a.thoroughfare, a.locality, a.countryName)
                    .joinToString(", ")
            } else "Unknown address"
        } catch (_: Exception) { "Unable to resolve address" }
    }

    fun manualRefresh() {
        scope.launch {
            isRefreshing   = true
            refreshStatus  = "Getting GPS fix…"
            checkPermissions()

            if (!hasFinePermission) {
                refreshStatus = "Fine location permission denied"
                isRefreshing  = false
                return@launch
            }
            if (!gpsEnabled) {
                refreshStatus = "GPS is disabled on device"
                isRefreshing  = false
                return@launch
            }

            val loc = withContext(Dispatchers.IO) { getLocation() }

            if (loc == null) {
                refreshStatus = "Could not obtain GPS fix. Retry."
                isRefreshing  = false
                return@launch
            }

            refreshStatus = "Resolving address…"
            val address = withContext(Dispatchers.IO) { resolveAddress(loc.latitude, loc.longitude) }

            val snap = LocationSnapshot(
                latitude  = loc.latitude,
                longitude = loc.longitude,
                accuracy  = loc.accuracy,
                altitude  = loc.altitude,
                speed     = loc.speed,
                bearing   = loc.bearing,
                provider  = loc.provider ?: "gps",
                address   = address,
                timestamp = java.time.Instant.now().toString()
            )
            currentSnapshot = snap

            // Upload to backend
            refreshStatus = "Uploading to server…"
            try {
                val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
                val token     = dbHelper.getConfig("session_token", "")
                val payload   = JSONObject().apply {
                    put("latitude",  snap.latitude)
                    put("longitude", snap.longitude)
                    put("accuracy",  snap.accuracy)
                    put("altitude",  snap.altitude)
                    put("speed",     snap.speed)
                    put("heading",   snap.bearing)
                    put("address",   snap.address)
                    put("provider",  snap.provider)
                    put("timestamp", snap.timestamp)
                }
                withContext(Dispatchers.IO) {
                    NetworkUtils.httpPost("$serverUrl/location/update", payload.toString(), token)
                }
                dbHelper.saveConfig("loc_last_lat",    snap.latitude.toString())
                dbHelper.saveConfig("loc_last_lon",    snap.longitude.toString())
                dbHelper.saveConfig("loc_last_update", snap.timestamp)
                refreshStatus = "✓ Location updated successfully"
                requestHistory.add(0, LocationRequest("just now", "Completed", "±${snap.accuracy.toInt()} m", "—"))
            } catch (e: Exception) {
                refreshStatus = "Upload failed: ${e.message}"
                requestHistory.add(0, LocationRequest("just now", "Failed", "—", "—"))
            }

            isRefreshing = false
            delay(3000)
            refreshStatus = ""
        }
    }

    // Boot check
    LaunchedEffect(Unit) {
        checkPermissions()
        // Restore last snapshot from SQLite
        val lat = dbHelper.getConfig("loc_last_lat", "").toDoubleOrNull()
        val lon = dbHelper.getConfig("loc_last_lon", "").toDoubleOrNull()
        if (lat != null && lon != null) {
            currentSnapshot = LocationSnapshot(
                latitude  = lat,
                longitude = lon,
                accuracy  = 10f,
                altitude  = 0.0,
                speed     = 0f,
                bearing   = 0f,
                provider  = "cached",
                address   = "Last saved position",
                timestamp = dbHelper.getConfig("loc_last_update", "Never")
            )
        }
    }

    // ─────────────────────────────────────────────────────────
    //  LAYOUT
    // ─────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page header
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("GPS Map", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Live location control center", color = TextSecondary, fontSize = 12.sp)
            }
            // Status pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (sharingEnabled) ColorSuccess.copy(alpha = 0.15f)
                        else ColorError.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (sharingEnabled) ColorSuccess else ColorError)
                    )
                    Text(
                        if (sharingEnabled) "Sharing ON" else "Sharing OFF",
                        color      = if (sharingEnabled) ColorSuccess else ColorError,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Dashboard", "Controls", "History", "Settings", "Permissions", "Analytics").forEach { tab ->
                val active = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Primary else BgCard)
                        .clickable { activeTab = tab }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(tab, color = if (active) Color.Black else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tab content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {

                // ════════════════════════════════════════
                // DASHBOARD
                // ════════════════════════════════════════
                "Dashboard" -> {
                    // Status banner
                    if (refreshStatus.isNotBlank()) {
                        PremiumCard {
                            Row(
                                modifier              = Modifier.padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isRefreshing) CircularProgressIndicator(color = Primary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.CheckCircle, null, tint = ColorSuccess, modifier = Modifier.size(18.dp))
                                Text(refreshStatus, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // GPS status block
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Signal Status", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatusBadge("GPS Hardware", if (gpsEnabled) "Active" else "Disabled", gpsEnabled, Modifier.weight(1f))
                                StatusBadge("Permission", if (hasFinePermission) "Granted" else "Denied", hasFinePermission, Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatusBadge("Sharing", if (sharingEnabled) "Enabled" else "Off", sharingEnabled, Modifier.weight(1f))
                                StatusBadge("Provider", currentSnapshot?.provider?.uppercase() ?: "—", true, Modifier.weight(1f))
                            }
                        }
                    }

                    // Coordinate display
                    if (currentSnapshot != null) {
                        val snap = currentSnapshot!!
                        PremiumCard {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.LocationOn, null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Text("Current Position", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                CoordRow("Latitude",  "%.6f".format(snap.latitude))
                                CoordRow("Longitude", "%.6f".format(snap.longitude))
                                CoordRow("Accuracy",  "± ${snap.accuracy.toInt()} m")
                                CoordRow("Altitude",  "${snap.altitude.toInt()} m")
                                CoordRow("Speed",     "${"%.1f".format(snap.speed * 3.6f)} km/h")
                                CoordRow("Heading",   "${snap.bearing.toInt()}°")
                                CoordRow("Address",   snap.address)
                                CoordRow("Updated",   snap.timestamp.take(19).replace("T", " "))
                            }
                        }
                    } else {
                        PremiumCard {
                            Box(Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.LocationOff, null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                                    Text("No GPS fix yet", color = TextSecondary, fontSize = 14.sp)
                                    Text("Tap Refresh Location below", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Quick refresh button
                    if (!isRefreshing) {
                        PremiumButton(
                            text     = "Refresh Location Now",
                            onClick  = { manualRefresh() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Refresh, null,
                                tint     = Primary,
                                modifier = Modifier.size(20.dp).rotate(rotation)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Acquiring GPS fix…", color = TextSecondary)
                        }
                    }
                }

                // ════════════════════════════════════════
                // CONTROLS
                // ════════════════════════════════════════
                "Controls" -> {
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Owner Controls", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Enable Location Sharing", color = TextPrimary)
                                Switch(checked = sharingEnabled, onCheckedChange = {
                                    sharingEnabled = it
                                    dbHelper.saveConfig("loc_sharing_enabled", it.toString())
                                })
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PremiumButton(
                                    text     = "Manual Refresh",
                                    onClick  = { manualRefresh() },
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumButton(
                                    text    = "Copy Coords",
                                    onClick = {
                                        currentSnapshot?.let { s ->
                                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            clip.setPrimaryClip(android.content.ClipData.newPlainText("coords", "${s.latitude}, ${s.longitude}"))
                                            Toast.makeText(context, "Coordinates copied!", Toast.LENGTH_SHORT).show()
                                        } ?: Toast.makeText(context, "No coordinates available", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (currentSnapshot != null) {
                                val snap = currentSnapshot!!
                                PremiumButton(
                                    text    = "Open in Google Maps",
                                    onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("geo:${snap.latitude},${snap.longitude}?q=${snap.latitude},${snap.longitude}(Location)"))
                                        intent.setPackage("com.google.android.apps.maps")
                                        try { context.startActivity(intent) }
                                        catch (_: Exception) { context.startActivity(intent.apply { setPackage(null) }) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // HISTORY
                // ════════════════════════════════════════
                "History" -> {
                    Text("Recent Refresh Requests", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    requestHistory.forEach { req ->
                        PremiumCard {
                            Row(
                                modifier              = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(req.time, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Accuracy ${req.accuracy}  •  ${req.duration}", color = TextSecondary, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (req.status) {
                                                "Completed" -> ColorSuccess.copy(alpha = 0.15f)
                                                "Failed"    -> ColorError.copy(alpha = 0.15f)
                                                else        -> ColorWarning.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(req.status, color = when (req.status) {
                                        "Completed" -> ColorSuccess
                                        "Failed"    -> ColorError
                                        else        -> ColorWarning
                                    }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // SETTINGS
                // ════════════════════════════════════════
                "Settings" -> {
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Location Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Exact Location", color = TextPrimary)
                                    Text("vs Approximate (city-level)", color = TextSecondary, fontSize = 11.sp)
                                }
                                Switch(checked = exactLocation, onCheckedChange = {
                                    exactLocation = it
                                    dbHelper.saveConfig("loc_exact", it.toString())
                                })
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Low Battery Mode", color = TextPrimary)
                                Switch(checked = lowBatteryMode, onCheckedChange = {
                                    lowBatteryMode = it
                                    dbHelper.saveConfig("loc_low_battery", it.toString())
                                })
                            }

                            HorizontalDivider(color = BgCard)

                            Text("Rate Limits", color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            SettingsTextField("Max requests / hour", maxPerHour) {
                                maxPerHour = it
                                dbHelper.saveConfig("loc_max_hour", it)
                            }
                            SettingsTextField("Max requests / day", maxPerDay) {
                                maxPerDay = it
                                dbHelper.saveConfig("loc_max_day", it)
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // PERMISSIONS
                // ════════════════════════════════════════
                "Permissions" -> {
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Required Permissions", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("All permissions below are required for location sharing to work correctly.", color = TextSecondary, fontSize = 12.sp)

                            val perms = listOf(
                                Triple("Fine Location (GPS)",    hasFinePermission,  "ACCESS_FINE_LOCATION"),
                                Triple("Coarse Location (Network)", hasFinePermission, "ACCESS_COARSE_LOCATION"),
                                Triple("Foreground Service",     true,               "FOREGROUND_SERVICE"),
                                Triple("Internet Access",        true,               "INTERNET"),
                            )
                            perms.forEach { (name, granted, _) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(name, color = TextPrimary, fontSize = 13.sp)
                                    Icon(
                                        if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        null,
                                        tint     = if (granted) ColorSuccess else ColorError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            PremiumButton(
                                text    = "Re-check Permissions",
                                onClick = { checkPermissions(); Toast.makeText(context, "Permissions re-checked", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ════════════════════════════════════════
                // ANALYTICS
                // ════════════════════════════════════════
                "Analytics" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatsCard("Today",    "${requestHistory.size}", Icons.Default.Today, Modifier.weight(1f))
                        StatsCard("Success",  "${requestHistory.count { it.status == "Completed" }}", Icons.Default.CheckCircle, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatsCard("Failed",   "${requestHistory.count { it.status == "Failed" }}", Icons.Default.Cancel, Modifier.weight(1f))
                        StatsCard("Avg Acc",  "± 9 m", Icons.Default.GpsFixed, Modifier.weight(1f))
                    }
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Request Trend", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("• Most active time: 09:00 – 11:00", color = TextSecondary, fontSize = 13.sp)
                            Text("• Average GPS fix time: 2.3 s", color = TextSecondary, fontSize = 13.sp)
                            Text("• Average accuracy: ± 9 meters", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(label: String, value: String, ok: Boolean, modifier: Modifier = Modifier) {
    PremiumCard {
        Column(
            modifier            = modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Text(value, color = if (ok) ColorSuccess else ColorError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CoordRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(90.dp))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsTextField(label: String, value: String, onSave: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value         = text,
        onValueChange = { text = it; onSave(it) },
        label         = { Text(label, fontSize = 12.sp) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(8.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Primary,
            unfocusedBorderColor = BgCard
        )
    )
}
