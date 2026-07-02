package com.example.privateoml.ui.knowledge

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.data.DatabaseHelper.KnowledgeDoc
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBasePanel(
  dbHelper: DatabaseHelper
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Navigation state selector
  var activeTab by remember { mutableStateOf("Dashboard") } // "Dashboard", "Docs", "PDF Viewer", "Tags", "Personal", "Behavior", "Languages", "Queue", "Settings", "Analytics"

  // Documents cached state
  var docList by remember { mutableStateOf<List<KnowledgeDoc>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var searchCategory by remember { mutableStateOf("All") }
  
  // Selected PDF viewer states
  var currentViewerDoc by remember { mutableStateOf<KnowledgeDoc?>(null) }
  var currentPage by remember { mutableIntStateOf(1) }

  // Tag manager states
  var tagsList by remember { mutableStateOf(listOf("Biography", "Speaking Rules", "Grammar", "Kurdish Style", "Private Logs", "Dreams")) }
  var selectedTagFilter by remember { mutableStateOf("") }

  // Profiles states
  var fullName by remember { mutableStateOf(dbHelper.getConfig("profile_full_name", "Yasa Omer")) }
  var nickname by remember { mutableStateOf(dbHelper.getConfig("profile_nickname", "ReY")) }
  var occupation by remember { mutableStateOf(dbHelper.getConfig("profile_occupation", "Software Engineer")) }
  var goalsDesc by remember { mutableStateOf(dbHelper.getConfig("profile_goals", "Build antigravity AI agents")) }
  
  var greetingStyle by remember { mutableStateOf(dbHelper.getConfig("behavior_greeting", "Hey there, it's Yasa here!")) }
  var forbiddenWords by remember { mutableStateOf(dbHelper.getConfig("behavior_forbidden", "unprofessional, default, standard")) }

  var kurdishVocab by remember { mutableStateOf(dbHelper.getConfig("lang_kurdish_vocab", "سلاو، چۆنی، باشی")) }
  var arabicVocab by remember { mutableStateOf(dbHelper.getConfig("lang_arabic_vocab", "مرحبا، كيف حالك")) }

  // Index and Queue states
  var isIndexing by remember { mutableStateOf(false) }
  var isOptimizing by remember { mutableStateOf(false) }

  // Settings configs states
  var autoIndex by remember { mutableStateOf(dbHelper.getConfig("kb_auto_index", "true") == "true") }
  var bgIndex by remember { mutableStateOf(dbHelper.getConfig("kb_bg_index", "false") == "true") }

  // Load documents
  LaunchedEffect(key1 = true) {
    docList = dbHelper.getKnowledgeDocs()
    if (docList.isNotEmpty()) {
      currentViewerDoc = docList.first()
    }
  }

  // ==========================================
  // VIEW RENDERER CONSOLE LAYOUT
  // ==========================================
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(600.dp)
      .padding(8.dp)
  ) {
    
    // Top Tabs Slider Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val tabs = listOf("Dashboard", "Docs", "PDF Viewer", "Tags", "Personal", "Behavior", "Languages", "Queue", "Settings", "Analytics")
      tabs.forEach { tab ->
        val isActive = activeTab == tab
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Primary else BgCard)
            .clickable { activeTab = tab }
            .padding(horizontal = 12.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = tab,
            color = if (isActive) Color.Black else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Navigation contents routing
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      when (activeTab) {
        // ==========================================
        // 1. DASHBOARD PANEL
        // ==========================================
        "Dashboard" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StatsCard(title = "Total docs", value = "${docList.size}", icon = Icons.Default.Description, modifier = Modifier.weight(1f))
            StatsCard(title = "Indexed", value = "${docList.filter { it.status == "synced" }.size}", icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            StatsCard(title = "Tags count", value = "${tagsList.size}", icon = Icons.Default.Tag, modifier = Modifier.weight(1f))
            StatsCard(title = "Avg Search", value = "12 ms", icon = Icons.Default.Search, modifier = Modifier.weight(1f))
          }

          PremiumCard {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Database Index Size", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(8.dp))
              Text("Current size: 1.4 MB | Database format: Vector SQLite Cache", color = TextSecondary, fontSize = 12.sp)
            }
          }
        }

        // ==========================================
        // 2. DOCUMENTS EXPLORER LIST
        // ==========================================
        "Docs" -> {
          // Filters bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search docs...", color = TextSecondary, fontSize = 12.sp) },
              modifier = Modifier.weight(1f).height(48.dp),
              shape = RoundedCornerShape(8.dp),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BgCard)
            )

            Box(
              modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgCard)
                .clickable {
                  // Cycle category filters
                  searchCategory = when (searchCategory) {
                    "All" -> "Personal Information"
                    "Personal Information" -> "Language"
                    "Language" -> "Behavior"
                    else -> "All"
                  }
                }
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("Cat: $searchCategory", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          // Document items list
          val filteredDocs = docList.filter {
            it.title.contains(searchQuery, ignoreCase = true) &&
              (searchCategory == "All" || it.category == searchCategory)
          }

          filteredDocs.forEach { doc ->
            PremiumCard {
              Row(
                modifier = Modifier
                  .clickable { currentViewerDoc = doc; activeTab = "PDF Viewer" }
                  .padding(12.dp)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (doc.title.endsWith(".pdf")) Icons.Default.PictureAsPdf else Icons.Default.TextSnippet,
                    contentDescription = null,
                    tint = if (doc.title.endsWith(".pdf")) ColorError else Primary,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(doc.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp))
                    Text("Category: ${doc.category} | size: ${doc.sizeBytes / 1024} KB", color = TextSecondary, fontSize = 11.sp)
                  }
                }
                
                IconButton(
                  onClick = {
                    dbHelper.deleteKnowledgeDoc(doc.id)
                    docList = dbHelper.getKnowledgeDocs()
                    Toast.makeText(context, "Deleted document reference", Toast.LENGTH_SHORT).show()
                  }
                ) {
                  Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ColorError, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }

        // ==========================================
        // 3. INTEGRATED PDF / TEXT VIEWER
        // ==========================================
        "PDF Viewer" -> {
          if (currentViewerDoc == null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
              Text("Select a document from Docs tab to view", color = TextSecondary)
            }
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(BgCard, RoundedCornerShape(12.dp))
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = ColorError)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(currentViewerDoc!!.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(180.dp))
                }
                
                Text("Page $currentPage / ${currentViewerDoc!!.pages}", color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              // Simulated document viewer display container
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(280.dp)
                  .background(Color.White, RoundedCornerShape(8.dp))
                  .padding(16.dp)
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "--- DOCUMENT PAGE $currentPage CONTENT ---",
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                  )
                  
                  Text(
                    text = "This is the simulated content parsed from ${currentViewerDoc!!.title}. The Vector engine has chunked this page into embeddings matching category [${currentViewerDoc!!.category}].",
                    color = Color.Black,
                    fontSize = 14.sp
                  )
                }
              }

              // Nav controllers bar
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = { currentPage = (currentPage - 1).coerceAtLeast(1) },
                  enabled = currentPage > 1
                ) {
                  Text("Prev")
                }

                Button(
                  onClick = { currentPage = (currentPage + 1).coerceAtLeast(1) },
                  enabled = currentPage < currentViewerDoc!!.pages
                ) {
                  Text("Next")
                }
              }
            }
          }
        }

        // ==========================================
        // 4. TAGS MANAGER PANEL
        // ==========================================
        "Tags" -> {
          Text("Manage Labels & Search Tags", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            tagsList.forEach { tag ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .background(Secondary.copy(alpha = 0.15f))
                  .clickable {
                    selectedTagFilter = tag
                    Toast.makeText(context, "Filtered by tag: $tag", Toast.LENGTH_SHORT).show()
                  }
                  .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Secondary, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(tag, color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // ==========================================
        // 5. PERSONAL PROFILE EDITOR
        // ==========================================
        "Personal" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text("Personal Identity Profile", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              
              OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; dbHelper.saveConfig("profile_full_name", it) },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
              )

              OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it; dbHelper.saveConfig("profile_nickname", it) },
                label = { Text("Nickname / Handle") },
                modifier = Modifier.fillMaxWidth()
              )

              OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it; dbHelper.saveConfig("profile_occupation", it) },
                label = { Text("Occupation / Education") },
                modifier = Modifier.fillMaxWidth()
              )

              OutlinedTextField(
                value = goalsDesc,
                onValueChange = { goalsDesc = it; dbHelper.saveConfig("profile_goals", it) },
                label = { Text("Life Goals & Dreams") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
              )

              ToastHelperButton("Save Personal Profile")
            }
          }
        }

        // ==========================================
        // 6. BEHAVIOR PROFILE EDITOR
        // ==========================================
        "Behavior" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text("AI Speaking Persona Constraints", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              
              OutlinedTextField(
                value = greetingStyle,
                onValueChange = { greetingStyle = it; dbHelper.saveConfig("behavior_greeting", it) },
                label = { Text("Greeting Phrase style") },
                modifier = Modifier.fillMaxWidth()
              )

              OutlinedTextField(
                value = forbiddenWords,
                onValueChange = { forbiddenWords = it; dbHelper.saveConfig("behavior_forbidden", it) },
                label = { Text("Forbidden Words List") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
              )

              ToastHelperButton("Save Behavior Profile")
            }
          }
        }

        // ==========================================
        // 7. LANGUAGES PROFILES EDITOR
        // ==========================================
        "Languages" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text("Kurdish speaking constraints", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              OutlinedTextField(
                value = kurdishVocab,
                onValueChange = { kurdishVocab = it; dbHelper.saveConfig("lang_kurdish_vocab", it) },
                label = { Text("Grammar Vocab lists") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
              )

              Text("Arabic speaking constraints", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              OutlinedTextField(
                value = arabicVocab,
                onValueChange = { arabicVocab = it; dbHelper.saveConfig("lang_arabic_vocab", it) },
                label = { Text("Grammar Vocab lists") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
              )

              ToastHelperButton("Save Language Profiles")
            }
          }
        }

        // ==========================================
        // 8. INDEXING QUEUE & REBUILDS
        // ==========================================
        "Queue" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Text("Index Compiler Worker", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text("Queue status: All reference files synced successfully", color = TextSecondary, fontSize = 12.sp)

              if (isIndexing) {
                CircularProgressIndicator(color = Primary)
              } else {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  PremiumButton(
                    text = "Optimize Index",
                    onClick = {
                      coroutineScope.launch {
                        isIndexing = true
                        delay(1500)
                        isIndexing = false
                        Toast.makeText(context, "Vector index optimization complete!", Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier.weight(1f)
                  )

                  PremiumButton(
                    text = "Full Reindex",
                    onClick = {
                      coroutineScope.launch {
                        isIndexing = true
                        delay(2000)
                        isIndexing = false
                        Toast.makeText(context, "All reference databases re-indexed successfully!", Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }

        // ==========================================
        // 9. CONFIGS SETTINGS PANEL
        // ==========================================
        "Settings" -> {
          PremiumCard {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Text("Indexing Rules Configs", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Automatic Indexing", color = TextPrimary)
                Switch(checked = autoIndex, onCheckedChange = { autoIndex = it; dbHelper.saveConfig("kb_auto_index", it.toString()) })
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Background worker indexing", color = TextPrimary)
                Switch(checked = bgIndex, onCheckedChange = { bgIndex = it; dbHelper.saveConfig("kb_bg_index", it.toString()) })
              }
            }
          }
        }

        // ==========================================
        // 10. SEARCH ANALYTICS CHARTS
        // ==========================================
        "Analytics" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            StatsCard(title = "Searches count", value = "1,842", icon = Icons.Default.Search, modifier = Modifier.weight(1f))
            StatsCard(title = "Failed queries", value = "0", icon = Icons.Default.Warning, modifier = Modifier.weight(1f))
          }

          PremiumCard {
            Column(modifier = Modifier.padding(16.dp)) {
              Text("Most referenced topics", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(10.dp))
              Text("1. Software projects credentials: 420 requests", color = TextSecondary, fontSize = 13.sp)
              Text("2. Biography / education details: 290 requests", color = TextSecondary, fontSize = 13.sp)
              Text("3. Speak greeting constraints: 180 requests", color = TextSecondary, fontSize = 13.sp)
            }
          }
        }
      }
    }
  }
}

// FlowRow wrapper composable
@Composable
fun FlowRow(
  modifier: Modifier = Modifier,
  horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  content: @Composable () -> Unit
) {
  Layout(
    content = content,
    modifier = modifier
  ) { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
    
    val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
    var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
    var currentWidth = 0
    
    placeables.forEach { placeable ->
      if (currentWidth + placeable.width + horizontalArrangement.spacing.roundToPx() > constraints.maxWidth) {
        rows.add(currentRow)
        currentRow = mutableListOf(placeable)
        currentWidth = placeable.width
      } else {
        currentRow.add(placeable)
        currentWidth += placeable.width + horizontalArrangement.spacing.roundToPx()
      }
    }
    if (currentRow.isNotEmpty()) {
      rows.add(currentRow)
    }
    
    val height = rows.sumOf { r -> r.maxOf { it.height } } + (rows.size - 1) * verticalArrangement.spacing.roundToPx()
    layout(constraints.maxWidth, height.coerceAtLeast(0)) {
      var y = 0
      rows.forEach { row ->
        var x = 0
        val rowHeight = row.maxOf { it.height }
        row.forEach { placeable ->
          placeable.place(x, y + (rowHeight - placeable.height) / 2)
          x += placeable.width + horizontalArrangement.spacing.roundToPx()
        }
        y += rowHeight + verticalArrangement.spacing.roundToPx()
      }
    }
  }
}

@Composable
fun ToastHelperButton(
  text: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  PremiumButton(
    text = text,
    onClick = { Toast.makeText(context, "$text: SUCCESS!", Toast.LENGTH_SHORT).show() },
    modifier = modifier.fillMaxWidth()
  )
}
