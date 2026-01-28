package com.dbilgin.dfiles.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dbilgin.dfiles.data.model.FileItem
import com.dbilgin.dfiles.ui.components.DeleteConfirmationDialog
import com.dbilgin.dfiles.ui.components.SelectionActionBar
import com.dbilgin.dfiles.ui.viewmodel.FileViewModel
import com.dbilgin.dfiles.ui.viewmodel.GalleryFolder
import com.dbilgin.dfiles.ui.viewmodel.OperationResult
import com.dbilgin.dfiles.util.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: FileViewModel,
    onOpenImage: (images: List<String>, index: Int) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int) -> Unit,
    embedded: Boolean = false
) {
    val context = LocalContext.current
    val galleryState by viewModel.galleryState.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadGallery()
    }

    // Handle back button for selection mode
    BackHandler(enabled = galleryState.isSelectionMode) {
        viewModel.clearGallerySelection()
    }

    // Handle operation results
    LaunchedEffect(operationResult) {
        operationResult?.let { result ->
            when (result) {
                is OperationResult.Success -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                is OperationResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
            viewModel.clearOperationResult()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header (only if not embedded - embedded mode has its own header)
        if (!embedded) {
            TopAppBar(
                title = { Text("Gallery") }
            )
        }

        // Selection mode header
        if (galleryState.isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.clearGallerySelection() }) {
                        Icon(Icons.Filled.Close, "Clear selection")
                    }
                    Text(
                        "${galleryState.selectedItems.size} selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Tabs
        TabRow(selectedTabIndex = galleryState.selectedTab) {
            Tab(
                selected = galleryState.selectedTab == 0,
                onClick = { viewModel.setGalleryTab(0) },
                text = { 
                    val count = galleryState.imageFolders.sumOf { it.items.size }
                    Text("Images ($count)") 
                },
                icon = { Icon(Icons.Filled.Image, null) }
            )
            Tab(
                selected = galleryState.selectedTab == 1,
                onClick = { viewModel.setGalleryTab(1) },
                text = { 
                    val count = galleryState.videoFolders.sumOf { it.items.size }
                    Text("Videos ($count)") 
                },
                icon = { Icon(Icons.Filled.VideoLibrary, null) }
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when {
                galleryState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Scanning for media...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                galleryState.selectedTab == 0 -> {
                    if (galleryState.imageFolders.isEmpty()) {
                        EmptyState("No images found", Icons.Filled.Image)
                    } else {
                        FolderList(
                            folders = galleryState.imageFolders,
                            selectedItems = galleryState.selectedItems,
                            isSelectionMode = galleryState.isSelectionMode,
                            onFolderToggle = { viewModel.toggleGalleryFolderExpanded(it) },
                            onItemClick = { folder, item ->
                                if (galleryState.isSelectionMode) {
                                    viewModel.toggleGalleryItemSelection(item.path)
                                } else {
                                    val images = folder.items.map { it.path }
                                    val index = images.indexOf(item.path).coerceAtLeast(0)
                                    onOpenImage(images, index)
                                }
                            },
                            onItemLongClick = { viewModel.toggleGalleryItemSelection(it.path) },
                            showPlayIcon = false
                        )
                    }
                }
                galleryState.selectedTab == 1 -> {
                    if (galleryState.videoFolders.isEmpty()) {
                        EmptyState("No videos found", Icons.Filled.VideoLibrary)
                    } else {
                        FolderList(
                            folders = galleryState.videoFolders,
                            selectedItems = galleryState.selectedItems,
                            isSelectionMode = galleryState.isSelectionMode,
                            onFolderToggle = { viewModel.toggleGalleryFolderExpanded(it) },
                            onItemClick = { folder, item ->
                                if (galleryState.isSelectionMode) {
                                    viewModel.toggleGalleryItemSelection(item.path)
                                } else {
                                    val videos = folder.items.map { it.path }
                                    val index = videos.indexOf(item.path).coerceAtLeast(0)
                                    onOpenVideo(videos, index)
                                }
                            },
                            onItemLongClick = { viewModel.toggleGalleryItemSelection(it.path) },
                            showPlayIcon = true
                        )
                    }
                }
            }
        }

        // Selection action bar
        if (galleryState.isSelectionMode && galleryState.selectedItems.isNotEmpty()) {
            SelectionActionBar(
                selectedCount = galleryState.selectedItems.size,
                onCopy = { 
                    viewModel.copyToClipboard(viewModel.getSelectedGalleryItems())
                    viewModel.clearGallerySelection()
                },
                onCut = { 
                    viewModel.cutToClipboard(viewModel.getSelectedGalleryItems())
                    viewModel.clearGallerySelection()
                },
                onDelete = { showDeleteDialog = true },
                onShare = {
                    FileUtils.shareFiles(context, viewModel.getSelectedGalleryItems())
                    viewModel.clearGallerySelection()
                }
            )
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            itemCount = galleryState.selectedItems.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { viewModel.deleteSelectedGalleryItems() }
        )
    }
}

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FolderList(
    folders: List<GalleryFolder>,
    selectedItems: Set<String>,
    isSelectionMode: Boolean,
    onFolderToggle: (String) -> Unit,
    onItemClick: (GalleryFolder, FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    showPlayIcon: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        folders.forEach { folder ->
            item(key = "header_${folder.path}") {
                FolderHeader(
                    folder = folder,
                    onToggle = { onFolderToggle(folder.path) }
                )
            }

            item(key = "content_${folder.path}") {
                AnimatedVisibility(visible = folder.isExpanded) {
                    FolderContent(
                        folder = folder,
                        selectedItems = selectedItems,
                        isSelectionMode = isSelectionMode,
                        onItemClick = { onItemClick(folder, it) },
                        onItemLongClick = onItemLongClick,
                        showPlayIcon = showPlayIcon
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderHeader(
    folder: GalleryFolder,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onToggle,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (folder.isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
                "Toggle folder"
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${folder.items.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderContent(
    folder: GalleryFolder,
    selectedItems: Set<String>,
    isSelectionMode: Boolean,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    showPlayIcon: Boolean
) {
    // Use a fixed-height grid within the lazy column
    val rows = (folder.items.size + 3) / 4 // 4 items per row
    val height = (rows * 110).dp // ~100dp per item + spacing

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        items(folder.items, key = { it.path }) { item ->
            MediaGridItem(
                fileItem = item,
                isSelected = selectedItems.contains(item.path),
                isSelectionMode = isSelectionMode,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                showPlayIcon = showPlayIcon
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showPlayIcon: Boolean
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = File(fileItem.path),
            contentDescription = fileItem.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            )
            Icon(
                Icons.Filled.CheckCircle,
                "Selected",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp),
                tint = Color.White
            )
        }

        // Play icon for videos
        if (showPlayIcon && !isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayCircle,
                    "Play",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }

        // File size label - using explicit white color
        if (!isSelected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = fileItem.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
