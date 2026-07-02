package com.example.privateoml.ui.gallery

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privateoml.data.DatabaseHelper
import com.example.privateoml.data.DatabaseHelper.LocalImage
import com.example.privateoml.theme.*
import com.example.privateoml.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTab(
  dbHelper: DatabaseHelper
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  
  var selectedFolder by remember { mutableStateOf(dbHelper.getConfig("gallery_folder_path", "")) }
  var images by remember { mutableStateOf<List<LocalImage>>(emptyList()) }
  var isScanning by remember { mutableStateOf(false) }

  // Search & Filter & Sorting state
  var searchQuery by remember { mutableStateOf("") }
  var filterStatus by remember { mutableStateOf("All") }
  var sortBy by remember { mutableStateOf("Newest") }

  // View state overlay
  var activeViewerImage by remember { mutableStateOf<LocalImage?>(null) }
  var activeDetailsImage by remember { mutableStateOf<LocalImage?>(null) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showQueueSheet by remember { mutableStateOf(false) }

  // Settings state variables
  var autoUpload by remember { mutableStateOf(dbHelper.getConfig("gallery_auto_upload", "true") == "true") }
  var wifiOnly by remember { mutableStateOf(dbHelper.getConfig("gallery_wifi_only", "false") == "true") }
  var qualityCompression by remember { mutableStateOf(dbHelper.getConfig("gallery_compression", "true") == "true") }
  
  // Launcher for SAF folder selection
  val folderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri: Uri? ->
    uri?.let {
      try {
        // Take persistable read & write permissions
        context.contentResolver.takePersistableUriPermission(
          it,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val pathString = it.toString()
        dbHelper.saveConfig("gallery_folder_path", pathString)
        selectedFolder = pathString
        Toast.makeText(context, "Gallery folder selected successfully", Toast.LENGTH_SHORT).show()
        
        // Seed mock images into cache upon folder setup
        seedMockImages(dbHelper)
        images = dbHelper.getGalleryImages()
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to persist folder permission: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  // Initial load
  LaunchedEffect(key1 = selectedFolder) {
    if (selectedFolder.isNotEmpty()) {
      images = dbHelper.getGalleryImages()
      if (images.isEmpty()) {
        seedMockImages(dbHelper)
        images = dbHelper.getGalleryImages()
      }
    }
  }

  // ==========================================
  // VIEW ROUTER (Setup vs Home Grid)
  // ==========================================
  Box(modifier = Modifier.fillMaxSize().background(BgBase)) {
    if (selectedFolder.isEmpty()) {
      // First-time Setup Wizard Welcome Panel
      GallerySetupWizard { folderLauncher.launch(null) }
    } else {
      // Active Gallery Console Layout
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // Title Bar & Action icons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Gallery Manager", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Synchronizing selected path with Cloud Drive", color = TextSecondary, fontSize = 12.sp)
          }
          
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Queue button
            IconButton(
              onClick = { showQueueSheet = true },
              modifier = Modifier.clip(CircleShape).background(BgCard)
            ) {
              Icon(imageVector = Icons.Default.CloudQueue, contentDescription = "Queue", tint = Secondary)
            }
            
            // Settings button
            IconButton(
              onClick = { showSettingsSheet = true },
              modifier = Modifier.clip(CircleShape).background(BgCard)
            ) {
              Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Primary)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multi-Filter controls bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Search box
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name...", color = TextSecondary, fontSize = 12.sp) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Primary,
              unfocusedBorderColor = BgCard,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          // Filter trigger menu dropdown placeholder
          Box(
            modifier = Modifier
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(BgCard)
              .clickable {
                // Cycle filter states
                filterStatus = when (filterStatus) {
                  "All" -> "Pending"
                  "Pending" -> "Synced"
                  "Synced" -> "Failed"
                  else -> "All"
                }
              }
              .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "Status: $filterStatus", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sync Trigger Refresh
        PremiumCard {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                coroutineScope.launch {
                  isScanning = true
                  delay(1500) // Mock folder scan
                  isScanning = false
                  Toast.makeText(context, "Local sync directory scan completed", Toast.LENGTH_SHORT).show()
                }
              }
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Monitored Tree Folder", color = TextSecondary, fontSize = 10.sp)
                Text(
                  text = selectedFolder.substringAfterLast("%2F").substringAfterLast("%3A"),
                  color = TextPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            if (isScanning) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Secondary)
            } else {
              Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync Now", tint = Secondary, modifier = Modifier.size(20.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Processed Images Grid Lists
        val filteredImages = images.filter {
          it.name.contains(searchQuery, ignoreCase = true) &&
            (filterStatus == "All" || it.syncStatus.equals(filterStatus, ignoreCase = true))
        }

        if (filteredImages.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text("No images match current filters", color = TextSecondary, fontSize = 14.sp)
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            items(filteredImages) { image ->
              ImageItemCard(
                image = image,
                onClick = { activeViewerImage = image },
                onFavorite = {
                  dbHelper.updateImageFavorite(image.id, !image.isFavorite)
                  images = dbHelper.getGalleryImages()
                },
                onDetails = { activeDetailsImage = image }
              )
            }
          }
        }
      }
    }

    // ==========================================
    // OVERLAYS (Sheets & Detail panels)
    // ==========================================
    
    // 1. Fullscreen Image Viewer
    if (activeViewerImage != null) {
      FullscreenViewer(
        image = activeViewerImage!!,
        onDismiss = { activeViewerImage = null },
        onDelete = {
          dbHelper.deleteGalleryImage(activeViewerImage!!.id)
          images = dbHelper.getGalleryImages()
          activeViewerImage = null
          Toast.makeText(context, "Deleted from local cache", Toast.LENGTH_SHORT).show()
        },
        onToggleHidden = {
          dbHelper.updateImageVisibility(activeViewerImage!!.id, !activeViewerImage!!.isHidden)
          images = dbHelper.getGalleryImages()
          activeViewerImage = null
          Toast.makeText(context, "Visibility toggled successfully", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // 2. Image Details Dialog
    if (activeDetailsImage != null) {
      ImageDetailsDialog(
        image = activeDetailsImage!!,
        onDismiss = { activeDetailsImage = null }
      )
    }

    // 3. Settings Bottom Sheet
    if (showSettingsSheet) {
      PremiumBottomSheet(
        onDismiss = { showSettingsSheet = false },
        title = "Gallery Sync Rules"
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Automated Backups", color = TextPrimary)
            Switch(
              checked = autoUpload,
              onCheckedChange = { autoUpload = it; dbHelper.saveConfig("gallery_auto_upload", it.toString()) }
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("WiFi Only uploads", color = TextPrimary)
            Switch(
              checked = wifiOnly,
              onCheckedChange = { wifiOnly = it; dbHelper.saveConfig("gallery_wifi_only", it.toString()) }
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Image Quality Compression", color = TextPrimary)
            Switch(
              checked = qualityCompression,
              onCheckedChange = { qualityCompression = it; dbHelper.saveConfig("gallery_compression", it.toString()) }
            )
          }
        }
      }
    }

    // 4. Queue Bottom Sheet
    if (showQueueSheet) {
      PremiumBottomSheet(
        onDismiss = { showQueueSheet = false },
        title = "Sync Active Queue"
      ) {
        val queueItems = images.filter { it.syncStatus == "pending" }
        if (queueItems.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("Upload queue is empty. All synced!", color = TextSecondary, fontSize = 14.sp)
          }
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(250.dp)
          ) {
            items(queueItems) { item ->
              PremiumCard {
                Row(
                  modifier = Modifier.padding(12.dp).fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(150.dp))
                  }
                  Text("Waiting", color = Secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

// ==========================================
// SUB COMPOSABLES
// ==========================================

@Composable
fun GallerySetupWizard(
  onSelectFolder: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(90.dp)
        .clip(CircleShape)
        .background(Primary.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(imageVector = Icons.Default.FolderShared, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Text("Monitored Tree Folder Setup", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
      text = "Select a local directory to host your public synchronization gallery. Only files inside the target path will be processed and backed up.",
      color = TextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(32.dp))

    PremiumButton(
      text = "Select Folder",
      onClick = onSelectFolder,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun ImageItemCard(
  image: LocalImage,
  onClick: () -> Unit,
  onFavorite: () -> Unit,
  onDetails: () -> Unit
) {
  PremiumCard(
    modifier = Modifier.clickable { onClick() }
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF0F111A))) {
      // Icon placeholder representing image thumbnail
      Icon(
        imageVector = Icons.Default.Image,
        contentDescription = null,
        tint = Primary.copy(alpha = 0.3f),
        modifier = Modifier.size(50.dp).align(Alignment.Center)
      )
      
      // Bottom title bar info overlay
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomStart)
          .background(Color(0xB3121420))
          .padding(8.dp)
      ) {
        Column {
          Text(
            text = image.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (image.syncStatus == "synced") "Synced" else "Pending",
            color = if (image.syncStatus == "synced") Secondary else ColorError,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Context menu buttons overlays
      Row(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        IconButton(
          onClick = onFavorite,
          modifier = Modifier.size(24.dp).background(Color(0x80121420), CircleShape)
        ) {
          Icon(
            imageVector = if (image.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = null,
            tint = if (image.isFavorite) ColorWarning else TextSecondary,
            modifier = Modifier.size(14.dp)
          )
        }
        
        IconButton(
          onClick = onDetails,
          modifier = Modifier.size(24.dp).background(Color(0x80121420), CircleShape)
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}

@Composable
fun FullscreenViewer(
  image: LocalImage,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onToggleHidden: () -> Unit
) {
  // Zoom gestures state variables
  var scale by remember { mutableStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  val state = rememberTransformableState { zoomChange, offsetChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 4f)
    offset += offsetChange
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .transformable(state = state)
  ) {
    // Zoomable Image Layout Panel
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(
          scaleX = scale,
          scaleY = scale,
          translationX = offset.x,
          translationY = offset.y
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Image,
        contentDescription = null,
        tint = Primary.copy(alpha = 0.2f),
        modifier = Modifier.size(150.dp)
      )
    }

    // Top Tool Bar controls overlay
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopStart)
        .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onDismiss) {
        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      
      Text(
        text = image.name,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(180.dp)
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onToggleHidden) {
          Icon(
            imageVector = if (image.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = "Visibility",
            tint = Color.White
          )
        }
        
        IconButton(onClick = onDelete) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ColorError)
        }
      }
    }

    // Bottom file size overlay
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomStart)
        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Size: ${image.sizeBytes / 1024} KB | Status: ${image.syncStatus.uppercase()}",
        color = TextSecondary,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
fun ImageDetailsDialog(
  image: LocalImage,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(image.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        val details = listOf(
          "Type" to "PNG File",
          "Size" to "${image.sizeBytes / 1024} KB",
          "Created" to image.createdTime,
          "Sync Status" to image.syncStatus,
          "Drive ID" to (image.thumbnailId ?: "Pending Upload")
        )
        
        details.forEach { (label, value) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Dismiss", color = Primary)
      }
    },
    containerColor = BgSurface,
    shape = RoundedCornerShape(16.dp)
  )
}

// Seed helper generating mock caching items
fun seedMockImages(dbHelper: DatabaseHelper) {
  val mockList = listOf(
    LocalImage("1", "IMG_20260702_1423.png", "hash1", null, 1242123L, "2026-07-02 14:23", "2026-07-02 14:23", false, false, "synced"),
    LocalImage("2", "IMG_20260701_1802.png", "hash2", null, 2452234L, "2026-07-01 18:02", "2026-07-01 18:02", true, false, "synced"),
    LocalImage("3", "IMG_20260701_0911.png", "hash3", null, 891234L, "2026-07-01 09:11", "2026-07-01 09:11", false, false, "pending"),
    LocalImage("4", "IMG_20260630_1654.png", "hash4", null, 3123412L, "2026-06-30 16:54", "2026-06-30 16:54", false, false, "failed")
  )
  mockList.forEach { dbHelper.saveGalleryImage(it) }
}
