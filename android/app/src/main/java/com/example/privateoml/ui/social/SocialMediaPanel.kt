package com.example.privateoml.ui.social

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ============================================================
//  DATA MODEL
// ============================================================

data class SocialAccount(
    val id: String,
    val platform: String,
    var enabled: Boolean,
    var isHidden: Boolean,
    var username: String,
    var email: String,
    var phone: String,
    var password: String,
    var link: String,
    var notes: String
)

private val ALL_PLATFORMS = listOf(
    "Instagram", "Facebook", "Messenger", "Snapchat",
    "Telegram", "TikTok", "Discord", "LinkedIn", "Threads", "YouTube"
)

private val PLATFORM_COLORS = mapOf(
    "Instagram"  to Color(0xFFE1306C),
    "Facebook"   to Color(0xFF1877F2),
    "Messenger"  to Color(0xFF006AFF),
    "Snapchat"   to Color(0xFFFFFC00),
    "Telegram"   to Color(0xFF2CA5E0),
    "TikTok"     to Color(0xFF010101),
    "Discord"    to Color(0xFF5865F2),
    "LinkedIn"   to Color(0xFF0A66C2),
    "Threads"    to Color(0xFF1A1A1A),
    "YouTube"    to Color(0xFFFF0000)
)

// ============================================================
//  HELPERS
// ============================================================

private fun loadAccounts(dbHelper: DatabaseHelper): List<SocialAccount> {
    val raw = dbHelper.getConfig("social_media_platforms", "[]")
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            SocialAccount(
                id       = obj.optString("id", obj.optString("platform", "p$i")),
                platform = obj.optString("platform", ""),
                enabled  = obj.optBoolean("enabled", false),
                isHidden = obj.optBoolean("isHidden", false),
                username = obj.optString("username", ""),
                email    = obj.optString("email", ""),
                phone    = obj.optString("phone", ""),
                password = obj.optString("password", ""),
                link     = obj.optString("link", ""),
                notes    = obj.optString("notes", "")
            )
        }
    } catch (_: Exception) { emptyList() }
}

private fun buildDefaultAccounts(): List<SocialAccount> = ALL_PLATFORMS.map { p ->
    SocialAccount(id = p, platform = p, enabled = false, isHidden = false,
        username = "", email = "", phone = "", password = "", link = "", notes = "")
}

private fun accountsToJson(list: List<SocialAccount>): String {
    val arr = JSONArray()
    list.forEach { a ->
        arr.put(JSONObject().apply {
            put("id",       a.id)
            put("platform", a.platform)
            put("enabled",  a.enabled)
            put("isHidden", a.isHidden)
            put("username", a.username)
            put("email",    a.email)
            put("phone",    a.phone)
            put("password", a.password)
            put("link",     a.link)
            put("notes",    a.notes)
        })
    }
    return arr.toString()
}

// ============================================================
//  COMPOSABLE – MAIN PANEL
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaPanel(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── State ────────────────────────────────────────────────
    var activeTab      by remember { mutableStateOf("Dashboard") }
    var accounts       by remember {
        mutableStateOf(
            loadAccounts(dbHelper).takeIf { it.isNotEmpty() } ?: buildDefaultAccounts()
        )
    }
    var introText      by remember { mutableStateOf(dbHelper.getConfig("social_media_intro", "")) }
    var isSyncing      by remember { mutableStateOf(false) }
    var showPasswords  by remember { mutableStateOf(false) }
    var searchQuery    by remember { mutableStateOf("") }
    var sortOrder      by remember { mutableStateOf("Default") }
    var filterMode     by remember { mutableStateOf("All") }
    var expandedCard   by remember { mutableStateOf<String?>(null) }

    val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")

    // ── Helpers ──────────────────────────────────────────────
    fun saveLocally() {
        dbHelper.saveConfig("social_media_platforms", accountsToJson(accounts))
        dbHelper.saveConfig("social_media_intro", introText)
        dbHelper.saveConfig("is_social_synced", "false")
    }

    fun syncToCloud() {
        scope.launch {
            isSyncing = true
            saveLocally()
            try {
                val token   = dbHelper.getConfig("session_token", "")
                val payload = JSONObject().apply {
                    put("platforms", org.json.JSONArray(accountsToJson(accounts)))
                    put("intro", introText)
                }
                withContext(Dispatchers.IO) {
                    val url = java.net.URL("$serverUrl/sync/social-config")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod  = "POST"
                    conn.doOutput       = true
                    conn.connectTimeout = 10_000
                    conn.readTimeout    = 10_000
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                    val code = conn.responseCode
                    if (code != 200) {
                        throw Exception("HTTP Error $code")
                    }
                    conn.disconnect()
                }
                dbHelper.saveConfig("is_social_synced", "true")
                Toast.makeText(context, "✓ Synced to cloud!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "⚠ Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSyncing = false
            }
        }
    }

    // ── Derived stats ─────────────────────────────────────────
    val totalAccounts    = accounts.size
    val enabledAccounts  = accounts.count { it.enabled }
    val hiddenAccounts   = accounts.count { it.isHidden }
    val disabledAccounts = accounts.count { !it.enabled }

    // ── Filtered / sorted list for Accounts tab ───────────────
    val displayAccounts = accounts
        .filter { a ->
            (searchQuery.isBlank() ||
             a.platform.contains(searchQuery, ignoreCase = true) ||
             a.username.contains(searchQuery, ignoreCase = true) ||
             a.email.contains(searchQuery, ignoreCase = true))
            &&
            when (filterMode) {
                "Enabled"  -> a.enabled
                "Disabled" -> !a.enabled
                "Hidden"   -> a.isHidden
                "Visible"  -> !a.isHidden
                else       -> true
            }
        }
        .sortedWith(
            when (sortOrder) {
                "A-Z"          -> compareBy { it.platform }
                "Z-A"          -> compareByDescending { it.platform }
                "Enabled First" -> compareByDescending { it.enabled }
                else           -> compareBy { ALL_PLATFORMS.indexOf(it.platform) }
            }
        )

    // ── Layout ────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(640.dp)
            .padding(horizontal = 4.dp)
    ) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Dashboard", "Accounts", "Intro", "Security", "Analytics").forEach { tab ->
                val active = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Primary else BgCard)
                        .clickable { activeTab = tab }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(tab,
                        color      = if (active) Color.Black else TextSecondary,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {

                // ════════════════════════════════════════
                // TAB 1 — DASHBOARD
                // ════════════════════════════════════════
                "Dashboard" -> {
                    // Stat grid
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashStatCard("Total",    "$totalAccounts",    Icons.Default.AccountCircle, Modifier.weight(1f))
                        DashStatCard("Enabled",  "$enabledAccounts",  Icons.Default.CheckCircle,   Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashStatCard("Hidden",   "$hiddenAccounts",   Icons.Default.VisibilityOff, Modifier.weight(1f))
                        DashStatCard("Disabled", "$disabledAccounts", Icons.Default.Cancel,        Modifier.weight(1f))
                    }

                    // Quick sync
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cloud Synchronization", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Sync all account credentials and the intro message to the website in one tap.",
                                color = TextSecondary, fontSize = 12.sp
                            )
                            if (isSyncing) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Syncing to cloud…", color = TextSecondary, fontSize = 12.sp)
                                }
                            } else {
                                PremiumButton(
                                    text     = "Sync to Website",
                                    onClick  = { syncToCloud() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // TAB 2 — ACCOUNTS
                // ════════════════════════════════════════
                "Accounts" -> {
                    // Search + filter row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder   = { Text("Search accounts…", color = TextSecondary, fontSize = 12.sp) },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f).height(50.dp),
                            shape         = RoundedCornerShape(8.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Primary,
                                unfocusedBorderColor = BgCard
                            )
                        )
                        // Filter chip
                        FilterChip(
                            selected    = filterMode != "All",
                            onClick     = {
                                filterMode = when (filterMode) {
                                    "All"      -> "Enabled"
                                    "Enabled"  -> "Disabled"
                                    "Disabled" -> "Hidden"
                                    "Hidden"   -> "Visible"
                                    else       -> "All"
                                }
                            },
                            label       = { Text(filterMode, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(14.dp)) }
                        )
                    }

                    // Sort row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Default", "A-Z", "Enabled First").forEach { s ->
                            val sel = sortOrder == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) Secondary.copy(alpha = 0.2f) else BgCard)
                                    .clickable { sortOrder = s }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(s, color = if (sel) Secondary else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Platform cards
                    displayAccounts.forEach { account ->
                        val isExpanded = expandedCard == account.id
                        val accentColor = PLATFORM_COLORS[account.platform] ?: Primary

                        PremiumCard {
                            Column(Modifier.padding(14.dp)) {
                                // Header row
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCard = if (isExpanded) null else account.id
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier          = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accentColor),
                                            contentAlignment  = Alignment.Center
                                        ) {
                                            Text(
                                                account.platform.take(1),
                                                color      = if (account.platform == "Snapchat") Color.Black else Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize   = 16.sp
                                            )
                                        }
                                        Column {
                                            Text(account.platform, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            val statusText = when {
                                                !account.enabled -> "Disabled"
                                                account.isHidden -> "Hidden"
                                                else             -> "Visible on website"
                                            }
                                            Text(statusText, color = when {
                                                !account.enabled -> ColorError
                                                account.isHidden -> Secondary
                                                else             -> ColorSuccess
                                            }, fontSize = 11.sp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked         = account.enabled,
                                            onCheckedChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(enabled = v) else a
                                                }
                                                saveLocally()
                                            },
                                            modifier = Modifier.height(24.dp)
                                        )
                                        Icon(
                                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint     = TextSecondary,
                                            modifier = Modifier.size(20.dp).padding(start = 4.dp)
                                        )
                                    }
                                }

                                // Expanded credential fields
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        Modifier.padding(top = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HorizontalDivider(color = BgCard)

                                        // Username
                                        CredentialField(
                                            label = "Username",
                                            value = account.username,
                                            onValueChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(username = v) else a
                                                }
                                            }
                                        )
                                        // Email
                                        CredentialField(
                                            label = "Email",
                                            value = account.email,
                                            onValueChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(email = v) else a
                                                }
                                            }
                                        )
                                        // Phone
                                        CredentialField(
                                            label = "Phone",
                                            value = account.phone,
                                            onValueChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(phone = v) else a
                                                }
                                            }
                                        )
                                        // Password
                                        CredentialField(
                                            label    = "Password",
                                            value    = account.password,
                                            isSecret = !showPasswords,
                                            onValueChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(password = v) else a
                                                }
                                            }
                                        )
                                        // Link
                                        CredentialField(
                                            label = "Profile Link",
                                            value = account.link,
                                            onValueChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(link = v) else a
                                                }
                                            }
                                        )
                                        // Notes
                                        OutlinedTextField(
                                            value         = account.notes,
                                            onValueChange = { v ->
                                                accounts = accounts.map { a ->
                                                    if (a.id == account.id) a.copy(notes = v) else a
                                                }
                                            },
                                            label   = { Text("Notes") },
                                            modifier = Modifier.fillMaxWidth().height(70.dp),
                                            maxLines = 3,
                                            shape   = RoundedCornerShape(8.dp),
                                            colors  = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor   = Primary,
                                                unfocusedBorderColor = BgCard
                                            )
                                        )

                                        // Visibility toggle
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment     = Alignment.CenterVertically
                                        ) {
                                            Text("Hide from website", color = TextPrimary, fontSize = 13.sp)
                                            Switch(
                                                checked         = account.isHidden,
                                                onCheckedChange = { v ->
                                                    accounts = accounts.map { a ->
                                                        if (a.id == account.id) a.copy(isHidden = v) else a
                                                    }
                                                    saveLocally()
                                                },
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }

                                        // Save button
                                        PremiumButton(
                                            text     = "Save ${account.platform}",
                                            onClick  = {
                                                saveLocally()
                                                Toast.makeText(context, "${account.platform} saved locally!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // TAB 3 — INTRO MESSAGE EDITOR
                // ════════════════════════════════════════
                "Intro" -> {
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Website Introduction", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "This message appears at the top of the Social page on the website. Supports basic HTML (bold, paragraphs, line breaks).",
                                color = TextSecondary, fontSize = 12.sp
                            )

                            // Quick formatting buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("<b>Bold</b>", "<br/>", "<em>Italic</em>").forEach { snippet ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BgCard)
                                            .clickable { introText += snippet }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(snippet.take(6) + "…", color = Secondary, fontSize = 10.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value         = introText,
                                onValueChange = { introText = it; dbHelper.saveConfig("social_media_intro", it) },
                                placeholder   = { Text("Write your introduction…", color = TextSecondary) },
                                modifier      = Modifier.fillMaxWidth().height(180.dp),
                                shape         = RoundedCornerShape(8.dp),
                                colors        = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = Primary,
                                    unfocusedBorderColor = BgCard
                                )
                            )

                            Text("Characters: ${introText.length}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // ════════════════════════════════════════
                // TAB 4 — SECURITY
                // ════════════════════════════════════════
                "Security" -> {
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Security, null, tint = Primary, modifier = Modifier.size(20.dp))
                                Text("Credential Security", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (!showPasswords) {
                                PremiumCard {
                                    Row(
                                        Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = ColorWarning, modifier = Modifier.size(18.dp))
                                        Text(
                                            "Passwords are currently hidden. Tap below to reveal them inside the account forms.",
                                            color    = TextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text("Show passwords in forms", color = TextPrimary)
                                Switch(
                                    checked         = showPasswords,
                                    onCheckedChange = { showPasswords = it }
                                )
                            }

                            HorizontalDivider(color = BgCard)
                            Text("Accounts overview", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            accounts.filter { it.password.isNotBlank() }.forEach { a ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(a.platform, color = TextPrimary, fontSize = 13.sp)
                                    Text(
                                        if (showPasswords) a.password else "••••••••",
                                        color      = TextSecondary,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier   = Modifier.width(120.dp),
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // ════════════════════════════════════════
                // TAB 5 — ANALYTICS
                // ════════════════════════════════════════
                "Analytics" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashStatCard("Views",    "1,284", Icons.Default.Visibility, Modifier.weight(1f))
                        DashStatCard("Copies",   "392",   Icons.Default.CopyAll,    Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashStatCard("Platforms", "$totalAccounts", Icons.Default.Apps, Modifier.weight(1f))
                        DashStatCard("Active",    "$enabledAccounts", Icons.Default.CheckCircle, Modifier.weight(1f))
                    }
                    PremiumCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Most Visited Platforms", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            listOf("Instagram: 640 views", "Facebook: 318 views", "Telegram: 210 views", "TikTok: 116 views").forEach {
                                Text("• $it", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  SMALL REUSABLE COMPOSABLES
// ════════════════════════════════════════════════════════════

@Composable
private fun DashStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    PremiumCard {
        Column(
            modifier         = modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = Secondary, modifier = Modifier.size(22.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(title, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CredentialField(
    label: String,
    value: String,
    isSecret: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label, fontSize = 12.sp) },
        singleLine      = true,
        visualTransformation = if (isSecret)
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        else
            androidx.compose.ui.text.input.VisualTransformation.None,
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(8.dp),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Primary,
            unfocusedBorderColor = BgCard
        )
    )
}
