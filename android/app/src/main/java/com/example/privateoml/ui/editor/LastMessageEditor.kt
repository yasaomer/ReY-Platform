package com.example.privateoml.ui.editor

import android.text.Html
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.data.DatabaseHelper.MessageDraft
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import com.example.privateoml.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastMessageEditor(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val dbHelper = remember { DatabaseHelper(context) }
  val scope = rememberCoroutineScope()

  // Active editor states
  var editorValue by remember { mutableStateOf(TextFieldValue("")) }
  var draftTitle by remember { mutableStateOf("Untitled Dispatch") }
  var currentDraftId by remember { mutableStateOf(UUID.randomUUID().toString()) }

  // Sync / Auto-save states
  var saveState by remember { mutableStateOf("Saved") }
  var syncState by remember { mutableStateOf("Synced") }
  var lastSavedTime by remember { mutableStateOf("Just now") }
  var versionNumber by remember { mutableIntStateOf(1) }
  var isUnsaved by remember { mutableStateOf(false) }

  // Panels and views toggle
  var viewMode by remember { mutableStateOf("Split") } // "Editor", "Preview", "Split"
  var showDraftsSheet by remember { mutableStateOf(false) }
  var savedDrafts by remember { mutableStateOf<List<MessageDraft>>(emptyList()) }
  var savedVersions by remember { mutableStateOf<List<MessageDraft>>(emptyList()) }

  // Calculate text metrics
  val textLength = editorValue.text.length
  val wordCount = editorValue.text.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
  val paragraphCount = editorValue.text.split(Regex("\n+")).filter { it.isNotEmpty() }.size
  val readingTime = (wordCount / 200).coerceAtLeast(1)
  
  // Media URL detection
  val mediaCount = Regex("https?://(www\\.)?(instagram\\.com|youtube\\.com|i\\.imgur\\.com|images\\.unsplash\\.com)[^\\s]*")
    .findAll(editorValue.text).count()

  // Load initial content
  LaunchedEffect(key1 = true) {
    val currentContent = dbHelper.getConfig("last_message_content", "")
    editorValue = TextFieldValue(currentContent)
    
    // Seed default drafts and versions lists
    savedDrafts = dbHelper.getMessageDrafts(isVersion = false)
    savedVersions = dbHelper.getMessageDrafts(isVersion = true)
    
    if (savedDrafts.isNotEmpty()) {
      val active = savedDrafts.first()
      currentDraftId = active.id
      draftTitle = active.title
      editorValue = TextFieldValue(active.content)
      versionNumber = active.versionNumber
    }
  }

  // Auto-Save Effect (saves every 3 seconds if changes exist)
  LaunchedEffect(key1 = editorValue) {
    if (editorValue.text.isEmpty()) return@LaunchedEffect
    isUnsaved = true
    saveState = "Saving..."
    delay(3000)
    
    val now = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
    val current = MessageDraft(
      id = currentDraftId,
      title = draftTitle,
      content = editorValue.text,
      createdAt = dbHelper.getConfig("draft_created_$currentDraftId", now),
      updatedAt = now,
      isVersion = false,
      versionNumber = versionNumber
    )
    dbHelper.saveMessageDraft(current)
    dbHelper.saveConfig("draft_created_$currentDraftId", current.createdAt)
    dbHelper.saveConfig("last_message_content", editorValue.text)
    
    savedDrafts = dbHelper.getMessageDrafts(isVersion = false)
    saveState = "Saved"
    lastSavedTime = now
    isUnsaved = false
  }

  // Format insertions helper
  fun insertTag(openTag: String, closeTag: String) {
    val text = editorValue.text
    val selection = editorValue.selection
    val start = selection.start
    val end = selection.end
    
    val newText = text.substring(0, start) + openTag + text.substring(start, end) + closeTag + text.substring(end)
    editorValue = TextFieldValue(
      text = newText,
      selection = androidx.compose.ui.text.TextRange(start + openTag.length, end + openTag.length)
    )
  }

  // Publish to Website
  fun publishMessage() {
    saveState = "Saving..."
    syncState = "Syncing..."
    
    scope.launch(Dispatchers.IO) {
      try {
        val now = java.time.LocalDateTime.now().toString()
        val serverUrl = dbHelper.getConfig("server_url", "https://rey-backend.yasaomer123.workers.dev/api/v1")
        val token = dbHelper.getConfig("session_token", "")

        // 1. Save local version history log
        val versionDraft = MessageDraft(
          id = UUID.randomUUID().toString(),
          title = "Version $versionNumber ($draftTitle)",
          content = editorValue.text,
          createdAt = now,
          updatedAt = now,
          isVersion = true,
          versionNumber = versionNumber
        )
        dbHelper.saveMessageDraft(versionDraft)
        
        // 2. Publish payload
        if (token.isNotEmpty()) {
          val payload = JSONObject().apply {
            put("message_content", editorValue.text)
          }
          val responseRaw = NetworkUtils.httpPost("$serverUrl/sync/last-message", payload.toString(), token)
          val res = JSONObject(responseRaw)
          
          scope.launch(Dispatchers.Main) {
            if (res.getBoolean("success")) {
              syncState = "Synced"
              versionNumber += 1
              savedVersions = dbHelper.getMessageDrafts(isVersion = true)
              Toast.makeText(context, "Published successfully!", Toast.LENGTH_SHORT).show()
            } else {
              syncState = "Failed"
              Toast.makeText(context, "Publish error: ${res.getString("message")}", Toast.LENGTH_LONG).show()
            }
          }
        } else {
          scope.launch(Dispatchers.Main) {
            syncState = "Local Only"
            Toast.makeText(context, "Saved locally. Log in to sync to website.", Toast.LENGTH_LONG).show()
          }
        }
      } catch (e: Exception) {
        scope.launch(Dispatchers.Main) {
          syncState = "Error"
          Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
        }
      } finally {
        scope.launch(Dispatchers.Main) {
          saveState = "Saved"
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(draftTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
              text = if (isUnsaved) "Unsaved changes" else "Saved at $lastSavedTime ($syncState)",
              fontSize = 11.sp,
              color = if (isUnsaved) ColorError else Secondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        actions = {
          // Version history button
          IconButton(onClick = { showDraftsSheet = true }) {
            Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = Secondary)
          }
          
          // Publish button
          IconButton(onClick = { publishMessage() }) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Publish", tint = Primary)
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .background(BgBase)
        .padding(innerPadding)
        .fillMaxSize()
    ) {
      
      // ==========================================
      // METRICS DASHBOARD BANNER
      // ==========================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(BgSurface)
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val stats = listOf(
          "Words" to "$wordCount",
          "Chars" to "$textLength",
          "Read Time" to "${readingTime}m",
          "Media embeds" to "$mediaCount"
        )
        
        stats.forEach { (label, value) ->
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(BgCard)
              .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(text = label, color = TextSecondary, fontSize = 10.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Formatting Toolbar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(BgCard)
          .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val formatButtons = listOf(
          Triple("B", "Bold", { insertTag("<b>", "</b>") }),
          Triple("I", "Italic", { insertTag("<i>", "</i>") }),
          Triple("U", "Underline", { insertTag("<u>", "</u>") }),
          Triple("S", "Strike", { insertTag("<s>", "</s>") }),
          Triple("H1", "Heading 1", { insertTag("<h1>", "</h1>") }),
          Triple("Quote", "Quote", { insertTag("<blockquote>", "</blockquote>") }),
          Triple("Line", "HR Line", { insertTag("", "<hr/>") })
        )
        
        formatButtons.forEach { (label, _, action) ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(BgBase)
              .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
              .clickable { action() }
              .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(text = label, color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // View Mode toggler
        IconButton(
          onClick = {
            viewMode = when (viewMode) {
              "Split" -> "Editor"
              "Editor" -> "Preview"
              else -> "Split"
            }
          }
        ) {
          Icon(
            imageVector = when (viewMode) {
              "Split" -> Icons.Default.VerticalSplit
              "Editor" -> Icons.Default.Edit
              else -> Icons.Default.RemoveRedEye
            },
            contentDescription = "View Mode",
            tint = Primary
          )
        }
      }

      // ==========================================
      // EDITOR AND PREVIEW LAYOUTS
      // ==========================================
      Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        
        // Editor panel
        if (viewMode == "Split" || viewMode == "Editor") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .padding(16.dp)
          ) {
            OutlinedTextField(
              value = editorValue,
              onValueChange = { editorValue = it },
              modifier = Modifier.fillMaxSize(),
              placeholder = { Text("Paste supported Instagram/YouTube/Image links on new lines to embed rich content...", color = TextSecondary) },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
          }
        }

        // Horizontal Divider in Split Screen
        if (viewMode == "Split") {
          Divider(color = Color(0x33FFFFFF), thickness = 2.dp)
        }

        // Live Preview Panel
        if (viewMode == "Split" || viewMode == "Preview") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .background(BgBase)
              .padding(16.dp)
              .verticalScroll(rememberScrollState())
          ) {
            Text(text = "LIVE WEBSITE PREVIEW", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Render parsed HTML text using AndroidView wrapping TextView
            AndroidView(
              factory = { ctx ->
                TextView(ctx).apply {
                  setTextColor(android.graphics.Color.WHITE)
                  textSize = 15f
                  setLineSpacing(0f, 1.4f)
                }
              },
              update = { textView ->
                textView.text = Html.fromHtml(editorValue.text, Html.FROM_HTML_MODE_LEGACY)
              },
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }

    // ==========================================
    // VERSION CONTROL BOTTOM SHEET
    // ==========================================
    if (showDraftsSheet) {
      PremiumBottomSheet(
        onDismiss = { showDraftsSheet = false },
        title = "Dispatch Versions & Drafts"
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          
          // Action row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            PremiumButton(
              text = "Create Draft",
              onClick = {
                val newId = UUID.randomUUID().toString()
                currentDraftId = newId
                draftTitle = "Draft ${savedDrafts.size + 1}"
                editorValue = TextFieldValue("")
                showDraftsSheet = false
              },
              modifier = Modifier.weight(1f)
            )
          }

          Text("Drafts List", color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(150.dp)
          ) {
            items(savedDrafts) { draft ->
              PremiumCard {
                Row(
                  modifier = Modifier
                    .clickable {
                      currentDraftId = draft.id
                      draftTitle = draft.title
                      editorValue = TextFieldValue(draft.content)
                      versionNumber = draft.versionNumber
                      showDraftsSheet = false
                    }
                    .padding(12.dp)
                    .fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(draft.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Saved at ${draft.updatedAt}", color = TextSecondary, fontSize = 11.sp)
                  }
                  Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Primary)
                }
              }
            }
          }

          Text("Published History Versions", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(150.dp)
          ) {
            items(savedVersions) { version ->
              PremiumCard {
                Row(
                  modifier = Modifier
                    .clickable {
                      editorValue = TextFieldValue(version.content)
                      draftTitle = "Restored Version ${version.versionNumber}"
                      showDraftsSheet = false
                    }
                    .padding(12.dp)
                    .fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("Version ${version.versionNumber}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Published: ${version.createdAt}", color = TextSecondary, fontSize = 11.sp)
                  }
                  Icon(imageVector = Icons.Default.Restore, contentDescription = "Restore", tint = Secondary)
                }
              }
            }
          }
        }
      }
    }
  }
}
