package com.dbilgin.dfiles.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbilgin.dfiles.ui.viewmodel.FileViewModel

enum class MainTab {
    FILES, GALLERY, SETTINGS
}

@Composable
fun MainScreen(
    viewModel: FileViewModel,
    initialTab: MainTab = MainTab.FILES,
    initialPath: String? = null,
    onNavigateToFolder: (String) -> Unit,
    onOpenImage: (images: List<String>, index: Int, fromTab: String) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int, fromTab: String) -> Unit
) {
    // Use initialTab as key to ensure tab resets when route changes
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val storageVolumes by viewModel.storageVolumes.collectAsState()
    
    val fileListState by viewModel.fileListState.collectAsState()
    
    // Update tab when initialTab changes (e.g., when navigating back from image viewer)
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadStorageInfo()
        // If we have an initial path, load it
        if (initialPath != null) {
            viewModel.loadFiles(initialPath)
        } else if (fileListState.currentPath.isEmpty()) {
            // Only load default if there's no current path (first time)
            val internalPath = viewModel.getInternalStoragePath()
            viewModel.loadFiles(internalPath)
        }
        // Otherwise, keep the current path from ViewModel (preserves folder when navigating back)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Folder, contentDescription = "Files") },
                    label = { Text("Files") },
                    selected = selectedTab == MainTab.FILES,
                    onClick = { selectedTab = MainTab.FILES }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Photo, contentDescription = "Gallery") },
                    label = { Text("Gallery") },
                    selected = selectedTab == MainTab.GALLERY,
                    onClick = { 
                        selectedTab = MainTab.GALLERY
                        viewModel.loadGallery()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when (selectedTab) {
                MainTab.FILES -> {
                    // Files tab - TopAppBar handles status bar insets automatically
                    FilesTabContent(
                        viewModel = viewModel,
                        storageVolumes = storageVolumes,
                        onNavigateToFolder = onNavigateToFolder,
                        onOpenImage = { images, index -> onOpenImage(images, index, "FILES") },
                        onOpenVideo = { videos, index -> onOpenVideo(videos, index, "FILES") }
                    )
                }
                MainTab.GALLERY -> {
                    // Gallery needs status bar padding (no TopAppBar when embedded)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        GalleryTabContent(
                            viewModel = viewModel,
                            onOpenImage = { images, index -> onOpenImage(images, index, "GALLERY") },
                            onOpenVideo = { videos, index -> onOpenVideo(videos, index, "GALLERY") }
                        )
                    }
                }
                MainTab.SETTINGS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesTabContent(
    viewModel: FileViewModel,
    storageVolumes: List<com.dbilgin.dfiles.data.repository.FileRepository.StorageInfo>,
    onNavigateToFolder: (String) -> Unit,
    onOpenImage: (images: List<String>, index: Int) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int) -> Unit
) {
    val fileListState by viewModel.fileListState.collectAsState()
    val selectionState by viewModel.selectionState.collectAsState()
    val clipboardState by viewModel.clipboardState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    
    var showStorageSelector by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with storage selector
        TopAppBar(
            title = {
                val currentVolume = storageVolumes.find { 
                    fileListState.currentPath.startsWith(it.path) 
                }
                Text(currentVolume?.name ?: "Files")
            },
            actions = {
                // Storage selector (only show if multiple volumes)
                if (storageVolumes.size > 1) {
                    IconButton(onClick = { showStorageSelector = true }) {
                        Icon(Icons.Filled.Storage, contentDescription = "Select storage")
                    }
                }
                IconButton(onClick = { viewModel.startSearch() }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        )

        // Embedded file list
        EmbeddedFileList(
            viewModel = viewModel,
            onNavigateToFolder = onNavigateToFolder,
            onOpenImage = onOpenImage,
            onOpenVideo = onOpenVideo
        )
    }

    // Storage selector dropdown
    if (showStorageSelector) {
        AlertDialog(
            onDismissRequest = { showStorageSelector = false },
            title = { Text("Select Storage") },
            text = {
                Column {
                    storageVolumes.forEach { volume ->
                        ListItem(
                            headlineContent = { Text(volume.name) },
                            supportingContent = { Text("${volume.formattedFree} free") },
                            leadingContent = {
                                Icon(
                                    if (volume.name.contains("SD", ignoreCase = true)) 
                                        Icons.Filled.SdCard 
                                    else 
                                        Icons.Filled.PhoneAndroid,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.loadFiles(volume.path)
                                showStorageSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GalleryTabContent(
    viewModel: FileViewModel,
    onOpenImage: (images: List<String>, index: Int) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int) -> Unit
) {
    GalleryScreen(
        viewModel = viewModel,
        onOpenImage = onOpenImage,
        onOpenVideo = onOpenVideo,
        embedded = true
    )
}
