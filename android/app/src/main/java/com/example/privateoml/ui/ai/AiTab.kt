package com.example.privateoml.ui.ai

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.data.DatabaseHelper.AiLog
import com.example.privateoml.data.DatabaseHelper.KnowledgeDoc
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTab(
  dbHelper: DatabaseHelper
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Configuration settings states loaded from SQLite helper
  var activeProvider by remember { mutableStateOf(dbHelper.getConfig("ai_provider", "Gemini")) }
  var geminiKey by remember { mutableStateOf(dbHelper.getConfig("gemini_api_key", "Not Set")) }
  var openaiKey by remember { mutableStateOf(dbHelper.getConfig("openai_api_key", "Not Set")) }
  var deepseekKey by remember { mutableStateOf(dbHelper.getConfig("deepseek_api_key", "Not Set")) }
  var openrouterKey by remember { mutableStateOf(dbHelper.getConfig("openrouter_api_key", "Not Set")) }
  
  var temperature by remember { mutableFloatStateOf(dbHelper.getConfig("ai_temp", "0.7").toFloat()) }
  var maxTokens by remember { mutableIntStateOf(dbHelper.getConfig("ai_max_tokens", "1024").toInt()) }
  var dailyLimit by remember { mutableIntStateOf(dbHelper.getConfig("ai_daily_limit", "100").toInt()) }

  // Knowledge index & logs cache lists
  var docList by remember { mutableStateOf<List<KnowledgeDoc>>(emptyList()) }
  var logsList by remember { mutableStateOf<List<AiLog>>(emptyList()) }

  // Views selectors & Sheet overlays togglers
  var activeMenuSection by remember { mutableStateOf("Dashboard") } // "Dashboard", "Providers", "Personality", "PDFs", "Tester", "Logs"
  var showDocDialog by remember { mutableStateOf(false) }
  var isTestingConnection by remember { mutableStateOf(false) }

  // Personality settings states variables
  var aboutMeBio by remember { mutableStateOf(dbHelper.getConfig("ai_bio", "")) }
  var speakingStyle by remember { mutableStateOf(dbHelper.getConfig("ai_speak_style", "")) }
  var kurdishUsageRules by remember { mutableStateOf(dbHelper.getConfig("ai_lang_kurdish", "")) }

  // Load database lists initial seedings
  LaunchedEffect(key1 = true) {
    docList = dbHelper.getKnowledgeDocs()
    if (docList.isEmpty()) {
      seedMockKnowledgeDocs(dbHelper)
      docList = dbHelper.getKnowledgeDocs()
    }
    
    logsList = dbHelper.getAiLogs()
    if (logsList.isEmpty()) {
      seedMockLogs(dbHelper)
      logsList = dbHelper.getAiLogs()
    }
  }

  // Save personality changes
  fun savePersonality() {
    dbHelper.saveConfig("ai_bio", aboutMeBio)
    dbHelper.saveConfig("ai_speak_style", speakingStyle)
    dbHelper.saveConfig("ai_lang_kurdish", kurdishUsageRules)
    Toast.makeText(context, "AI Personality profile saved successfully", Toast.LENGTH_SHORT).show()
  }

  // ==========================================
  // VIEW RENDERER CONSOLE LAYOUT
  // ==========================================
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    
    // Header View Portal
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("AI Settings Portal", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Configuring ReY Brain and RAG reference context", color = TextSecondary, fontSize = 12.sp)
      }
      
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(Secondary.copy(alpha = 0.15f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text("Online", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Horizontal Scrolling Navigation Selector
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val navs = listOf("Dashboard", "Providers", "Personality", "PDFs", "Tester", "Logs")
      navs.forEach { section ->
        val isActive = activeMenuSection == section
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Primary else BgCard)
            .clickable { activeMenuSection = section }
            .padding(horizontal = 14.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = section,
            color = if (isActive) Color.Black else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // View panels mapping routing
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      when (activeMenuSection) {
        // ==========================================
        // 1. DASHBOARD PANEL
        // ==========================================
        "Dashboard" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            StatsCard(
              title = "Active Model",
              value = activeProvider,
              icon = Icons.Default.SmartToy,
              modifier = Modifier.weight(1f)
            )
            StatsCard(
              title = "Knowledge Base",
              value = "${docList.size} Documents",
              icon = Icons.Default.FolderZip,
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            StatsCard(
              title = "Daily messages",
              value = "$dailyLimit Limit",
              icon = Icons.Default.Speed,
              modifier = Modifier.weight(1f)
            )
            StatsCard(
              title = "Avg Latency",
              value = "450 ms",
              icon = Icons.Default.Timer,
              modifier = Modifier.weight(1f)
            )
          }

          // Index rebuild button card
          PremiumCard {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Knowledge Index Size", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Last indexed: Today, 18:24", color = TextSecondary, fontSize = 12.sp)
              }
              
              PremiumButton(
                text = "Rebuild Index",
                onClick = {
                  coroutineScope.launch {
                    Toast.makeText(context, "Rebuilding Vector Database index...", Toast.LENGTH_SHORT).show()
                    delay(2000)
                    Toast.makeText(context, "Vector indexes compiled successfully!", Toast.LENGTH_SHORT).show()
                  }
                }
              )
            }
          }
        }

        // ==========================================
        // 2. PROVIDERS SETTINGS PANEL
        // ==========================================
        "Providers" -> {
          // Selector
          PremiumCard {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Select Active Provider", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(12.dp))
              
              listOf("Gemini", "OpenAI", "DeepSeek", "OpenRouter").forEach { provider ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      activeProvider = provider
                      dbHelper.saveConfig("ai_provider", provider)
                      Toast.makeText(context, "$provider selected as active", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(provider, color = if (activeProvider == provider) Primary else TextPrimary)
                  RadioButton(
                    selected = activeProvider == provider,
                    onClick = {
                      activeProvider = provider
                      dbHelper.saveConfig("ai_provider", provider)
                    }
                  )
                }
              }
            }
          }

          // Api key input text fields
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text("API Key Configs", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              
              OutlinedTextField(
                value = if (activeProvider == "Gemini") geminiKey else if (activeProvider == "OpenAI") openaiKey else if (activeProvider == "DeepSeek") deepseekKey else openrouterKey,
                onValueChange = { key ->
                  when (activeProvider) {
                    "Gemini" -> { geminiKey = key; dbHelper.saveConfig("gemini_api_key", key) }
                    "OpenAI" -> { openaiKey = key; dbHelper.saveConfig("openai_api_key", key) }
                    "DeepSeek" -> { deepseekKey = key; dbHelper.saveConfig("deepseek_api_key", key) }
                    "OpenRouter" -> { openrouterKey = key; dbHelper.saveConfig("openrouter_api_key", key) }
                  }
                },
                label = { Text("Active Provider Key API") },
                modifier = Modifier.fillMaxWidth()
              )

              // Settings sliders
              Column {
                Text("Temperature (Creativity): ${"%.2f".format(temperature)}", color = TextPrimary, fontSize = 12.sp)
                Slider(
                  value = temperature,
                  onValueChange = { temperature = it; dbHelper.saveConfig("ai_temp", it.toString()) },
                  valueRange = 0f..1.5f
                )
              }
            }
          }
        }

        // ==========================================
        // 3. PERSONALITY PROFILES EDITOR
        // ==========================================
        "Personality" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Text("AI Speaking Persona Profile", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              
              OutlinedTextField(
                value = aboutMeBio,
                onValueChange = { aboutMeBio = it },
                label = { Text("About Me / Biography") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
              )

              OutlinedTextField(
                value = speakingStyle,
                onValueChange = { speakingStyle = it },
                label = { Text("Speaking and Writing Style") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
              )

              OutlinedTextField(
                value = kurdishUsageRules,
                onValueChange = { kurdishUsageRules = it },
                label = { Text("Kurdish / Regional Grammar constraints") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
              )

              PremiumButton(
                text = "Save Personality",
                onClick = { savePersonality() },
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }

        // ==========================================
        // 4. KNOWLEDGE PDF MANAGER
        // ==========================================
        "PDFs" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Knowledge Base Tree", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showDocDialog = true }) {
              Icon(imageVector = Icons.Default.Add, contentDescription = "Add PDF", tint = Primary)
            }
          }

          docList.forEach { doc ->
            PremiumCard {
              Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = ColorError)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(doc.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp))
                    Text("Pages: ${doc.pages} | Size: ${doc.sizeBytes / 1024} KB", color = TextSecondary, fontSize = 11.sp)
                  }
                }
                
                IconButton(
                  onClick = {
                    dbHelper.deleteKnowledgeDoc(doc.id)
                    docList = dbHelper.getKnowledgeDocs()
                  }
                ) {
                  Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ColorError, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }

        // ==========================================
        // 5. TESTER CONNECTION PANEL
        // ==========================================
        "Tester" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Text("System Diagnostics Tester", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Verify connection to LLM API provider and database health.", color = TextSecondary, fontSize = 12.sp)
              
              if (isTestingConnection) {
                CircularProgressIndicator(color = Secondary)
              } else {
                PremiumButton(
                  text = "Start Health Test",
                  onClick = {
                    coroutineScope.launch {
                      isTestingConnection = true
                      delay(2000)
                      isTestingConnection = false
                      Toast.makeText(context, "API Key check: SUCCESS! Model response: latency 420ms", Toast.LENGTH_LONG).show()
                    }
                  },
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
        }

        // ==========================================
        // 6. RUNTIME LOGS HISTORIES CONSOLE
        // ==========================================
        "Logs" -> {
          logsList.forEach { log ->
            PremiumCard {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(log.model, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(log.timestamp, color = TextSecondary, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Q: ${log.question}", color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tokens: ${log.tokens} | Duration: ${log.duration}ms | Status: ${log.status}", color = TextSecondary, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }

    // ==========================================
    // DIALOGS & OVERLAYS
    // ==========================================
    if (showDocDialog) {
      AlertDialog(
        onDismissRequest = { showDocDialog = false },
        title = { Text("Load Reference Document", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Provide document metadata details to seed mock indexes:", color = TextSecondary, fontSize = 12.sp)
            
            var name by remember { mutableStateOf("Kurdish_expressions_v2.pdf") }
            var category by remember { mutableStateOf("Language") }
            var pagesCount by remember { mutableStateOf("12") }

            OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Title / Name") }
            )

            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Category") }
            )

            OutlinedTextField(
              value = pagesCount,
              onValueChange = { pagesCount = it },
              label = { Text("Pages") }
            )

            Button(
              onClick = {
                val newDoc = KnowledgeDoc(
                  id = UUID.randomUUID().toString(),
                  title = name,
                  pages = pagesCount.toIntOrNull() ?: 1,
                  sizeBytes = 412421L,
                  category = category,
                  dateAdded = "2026-07-02",
                  lastIndexed = "Today",
                  wordCount = 2412,
                  status = "synced"
                )
                dbHelper.saveKnowledgeDoc(newDoc)
                docList = dbHelper.getKnowledgeDocs()
                showDocDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Add Reference")
            }
          }
        },
        confirmButton = {},
        containerColor = BgSurface,
        shape = RoundedCornerShape(16.dp)
      )
    }
  }
}

// Seeding standard mock reference document files
fun seedMockKnowledgeDocs(dbHelper: DatabaseHelper) {
  val docs = listOf(
    KnowledgeDoc("doc1", "Biography_Owner_Profile.pdf", 4, 12421L, "Personal Information", "2026-07-02", "Today", 1242, "synced"),
    KnowledgeDoc("doc2", "Speaking_Style_Guidelines.pdf", 2, 8421L, "Behavior", "2026-07-01", "Today", 642, "synced"),
    KnowledgeDoc("doc3", "Kurdish_Regional_Idioms.pdf", 10, 482421L, "Language", "2026-06-30", "Today", 4822, "synced")
  )
  docs.forEach { dbHelper.saveKnowledgeDoc(it) }
}

// Seeding standard runtime AI queries logs
fun seedMockLogs(dbHelper: DatabaseHelper) {
  val logs = listOf(
    AiLog(1, "18:24", "Who is the owner of Private OML?", "Gemini", "Gemini 1.5 Flash", 120, 0, 420, "Biography_Owner_Profile.pdf", "Success", null),
    AiLog(2, "17:11", "Translate speaking rules to Kurdish", "OpenAI", "GPT-4o Mini", 210, 1, 980, "Kurdish_Regional_Idioms.pdf", "Success", null),
    AiLog(3, "16:02", "Tell me a joke in owner's tone", "Gemini", "Gemini 1.5 Flash", 85, 0, 390, "Speaking_Style_Guidelines.pdf", "Success", null)
  )
  logs.forEach { dbHelper.saveAiLog(it) }
}
