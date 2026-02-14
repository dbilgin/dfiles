package com.dbilgin.dfiles.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dbilgin.dfiles.data.model.FileItem
import com.dbilgin.dfiles.ui.components.*
import com.dbilgin.dfiles.ui.viewmodel.FileViewModel
import com.dbilgin.dfiles.ui.viewmodel.OperationResult
import com.dbilgin.dfiles.ui.viewmodel.ViewMode
import com.dbilgin.dfiles.util.FileUtils
import java.io.File

private fun fileMatchesPickerType(fileItem: FileItem, pickerType: String?): Boolean {
    if (pickerType == null) return true
    return when (pickerType) {
        "image/*" -> fileItem.isImage
        "video/*" -> fileItem.isVideo
        else -> true // */* or any other type
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddedFileList(
    viewModel: FileViewModel,
    onNavigateToFolder: (String) -> Unit,
    onOpenImage: (images: List<String>, index: Int) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int) -> Unit,
    pickerMode: Boolean = false,
    pickerType: String? = null,
    onPickFile: ((path: String, mimeType: String) -> Unit)? = null,
    onPickerCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fileListState by viewModel.fileListState.collectAsState()
    val selectionState by viewModel.selectionState.collectAsState()
    val clipboardState by viewModel.clipboardState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    // Dialog states
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf<FileItem?>(null) }
    var showActionsSheet by remember { mutableStateOf<FileItem?>(null) }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }

    // Handle system back button
    val backHandlerEnabled = selectionState.isSelectionMode || searchState.isSearching ||
        !viewModel.isRootPath() || (pickerMode && viewModel.isRootPath())
    BackHandler(enabled = backHandlerEnabled) {
        when {
            selectionState.isSelectionMode -> viewModel.clearSelection()
            searchState.isSearching -> viewModel.stopSearch()
            !viewModel.isRootPath() -> {
                val parentPath = viewModel.getParentPath()
                if (parentPath != null) {
                    viewModel.loadFiles(parentPath)
                }
            }
            pickerMode && viewModel.isRootPath() -> onPickerCancel?.invoke()
        }
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

    val displayedFiles = if (searchState.isSearching && searchState.query.isNotBlank()) {
        searchState.results
    } else {
        fileListState.files
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar or breadcrumb/actions
        if (searchState.isSearching) {
            SearchBarCompact(
                query = searchState.query,
                onQueryChange = { viewModel.search(it) },
                onClose = { viewModel.stopSearch() },
                isRecursive = searchState.isRecursive,
                onToggleRecursive = { viewModel.setRecursiveSearch(!searchState.isRecursive) }
            )
        } else {
            // Breadcrumb and actions row
            BreadcrumbRow(
                viewModel = viewModel,
                isSelectionMode = selectionState.isSelectionMode,
                selectedCount = selectionState.selectedFiles.size,
                viewMode = fileListState.viewMode,
                showHidden = fileListState.showHidden,
                hasClipboard = clipboardState.files.isNotEmpty(),
                onToggleViewMode = {
                    viewModel.setViewMode(
                        if (fileListState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    )
                },
                onToggleHidden = { viewModel.setShowHidden(!fileListState.showHidden) },
                onShowSort = { showSortDialog = true },
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onCreateFolder = { showNewFolderDialog = true },
                onCreateFile = { showNewFileDialog = true },
                onPaste = { viewModel.paste() }
            )
        }

        // File list content
        // Fill width so center alignment is truly centered (weight alone can wrap width).
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                fileListState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                fileListState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(fileListState.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshCurrentDirectory() }) { Text("Retry") }
                    }
                }
                displayedFiles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.FolderOff, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchState.isSearching) "No results found" else "This folder is empty",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    if (fileListState.viewMode == ViewMode.LIST) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(displayedFiles, key = { it.path }) { fileItem ->
                                FileItemRow(
                                    fileItem = fileItem,
                                    isSelected = selectionState.selectedFiles.contains(fileItem.path),
                                    isSelectionMode = selectionState.isSelectionMode,
                                    onClick = {
                                        handleFileClick(fileItem, selectionState.isSelectionMode, viewModel, context, onOpenImage, onOpenVideo, pickerType, onPickFile)
                                    },
                                    onLongClick = {
                                        // Long press shows action sheet (which includes info/details)
                                        showActionsSheet = fileItem
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(100.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedFiles, key = { it.path }) { fileItem ->
                                FileItemGrid(
                                    fileItem = fileItem,
                                    isSelected = selectionState.selectedFiles.contains(fileItem.path),
                                    isSelectionMode = selectionState.isSelectionMode,
                                    onClick = {
                                        handleFileClick(fileItem, selectionState.isSelectionMode, viewModel, context, onOpenImage, onOpenVideo, pickerType, onPickFile)
                                    },
                                    onLongClick = {
                                        // Long press shows action sheet (which includes info/details)
                                        showActionsSheet = fileItem
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Clipboard indicator
            if (clipboardState.files.isNotEmpty() && !selectionState.isSelectionMode) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (clipboardState.operation == com.dbilgin.dfiles.ui.viewmodel.ClipboardOperation.COPY)
                                Icons.Filled.ContentCopy else Icons.Filled.ContentCut,
                            null, Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${clipboardState.files.size} item(s)", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.clearClipboard() }) { Text("Clear") }
                    }
                }
            }
        }

        // Selection action bar
        if (selectionState.isSelectionMode && selectionState.selectedFiles.isNotEmpty()) {
            SelectionActionBar(
                selectedCount = selectionState.selectedFiles.size,
                onCopy = { viewModel.copyToClipboard(viewModel.getSelectedFileItems()) },
                onCut = { viewModel.cutToClipboard(viewModel.getSelectedFileItems()) },
                onDelete = { showDeleteDialog = true },
                onShare = {
                    FileUtils.shareFiles(context, viewModel.getSelectedFileItems())
                    viewModel.clearSelection()
                }
            )
        }
    }

    // Dialogs
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { viewModel.createFolder(it) }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onCreate = { viewModel.createFile(it) }
        )
    }

    showRenameDialog?.let { fileItem ->
        RenameDialog(
            currentName = fileItem.name,
            onDismiss = { showRenameDialog = null },
            onRename = { viewModel.rename(fileItem, it) }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            itemCount = selectionState.selectedFiles.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { viewModel.deleteSelected() }
        )
    }

    fileToDelete?.let { fileItem ->
        DeleteConfirmationDialog(
            itemCount = 1,
            onDismiss = { fileToDelete = null },
            onConfirm = { viewModel.deleteFile(fileItem); fileToDelete = null }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSortType = fileListState.sortType,
            currentSortOrder = fileListState.sortOrder,
            onDismiss = { showSortDialog = false },
            onSortTypeSelected = { viewModel.setSortType(it) },
            onSortOrderSelected = { viewModel.setSortOrder(it) }
        )
    }

    showDetailsDialog?.let { fileItem ->
        FileDetailsDialog(fileItem = fileItem, onDismiss = { showDetailsDialog = null })
    }

    showActionsSheet?.let { fileItem ->
        FileActionsSheet(
            fileItem = fileItem,
            onDismiss = { showActionsSheet = null },
            onOpen = { handleFileOpen(fileItem, viewModel, context, onOpenImage, onOpenVideo, pickerType, onPickFile) },
            onRename = { showRenameDialog = fileItem },
            onCopy = { viewModel.copyToClipboard(listOf(fileItem)) },
            onCut = { viewModel.cutToClipboard(listOf(fileItem)) },
            onDelete = { fileToDelete = fileItem },
            onShare = { FileUtils.shareFile(context, fileItem) },
            onDetails = { showDetailsDialog = fileItem },
            onSelect = {
                // Enter selection mode and select this item
                viewModel.toggleFileSelection(fileItem.path)
            }
        )
    }
}

private fun handleFileClick(
    fileItem: FileItem,
    isSelectionMode: Boolean,
    viewModel: FileViewModel,
    context: android.content.Context,
    onOpenImage: (images: List<String>, index: Int) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int) -> Unit,
    pickerType: String? = null,
    onPickFile: ((path: String, mimeType: String) -> Unit)? = null
) {
    if (onPickFile != null && !fileItem.isDirectory && fileMatchesPickerType(fileItem, pickerType)) {
        onPickFile(fileItem.path, fileItem.mimeType)
        return
    }
    if (isSelectionMode) {
        viewModel.toggleFileSelection(fileItem.path)
    } else {
        when {
            fileItem.isDirectory -> {
                // Navigate to folder
                viewModel.loadFiles(fileItem.path)
            }
            fileItem.isImage -> {
                // Get all images in the same folder for swiping
                val parentFile = File(fileItem.path).parentFile
                val imagesInFolder = parentFile?.listFiles()
                    ?.filter { it.isFile && FileItem.fromFile(it).isImage }
                    ?.sortedBy { it.name }
                    ?.map { it.absolutePath }
                    ?: listOf(fileItem.path)
                val index = imagesInFolder.indexOf(fileItem.path).coerceAtLeast(0)
                onOpenImage(imagesInFolder, index)
            }
            fileItem.isVideo -> {
                // Get all videos in the same folder for swiping
                val parentFile = File(fileItem.path).parentFile
                val videosInFolder = parentFile?.listFiles()
                    ?.filter { it.isFile && FileItem.fromFile(it).isVideo }
                    ?.sortedBy { it.name }
                    ?.map { it.absolutePath }
                    ?: listOf(fileItem.path)
                val index = videosInFolder.indexOf(fileItem.path).coerceAtLeast(0)
                onOpenVideo(videosInFolder, index)
            }
            fileItem.isApk -> {
                FileUtils.installApk(context, File(fileItem.path))
            }
            else -> {
                FileUtils.openFile(context, fileItem)
            }
        }
    }
}

private fun handleFileOpen(
    fileItem: FileItem,
    viewModel: FileViewModel,
    context: android.content.Context,
    onOpenImage: (images: List<String>, index: Int) -> Unit,
    onOpenVideo: (videos: List<String>, index: Int) -> Unit,
    pickerType: String? = null,
    onPickFile: ((path: String, mimeType: String) -> Unit)? = null
) {
    if (onPickFile != null && !fileItem.isDirectory && fileMatchesPickerType(fileItem, pickerType)) {
        onPickFile(fileItem.path, fileItem.mimeType)
        return
    }
    when {
        fileItem.isDirectory -> {
            viewModel.loadFiles(fileItem.path)
        }
        fileItem.isImage -> {
            val parentFile = File(fileItem.path).parentFile
            val imagesInFolder = parentFile?.listFiles()
                ?.filter { it.isFile && FileItem.fromFile(it).isImage }
                ?.sortedBy { it.name }
                ?.map { it.absolutePath }
                ?: listOf(fileItem.path)
            val index = imagesInFolder.indexOf(fileItem.path).coerceAtLeast(0)
            onOpenImage(imagesInFolder, index)
        }
        fileItem.isVideo -> {
            // Get all videos in the same folder for swiping
            val parentFile = File(fileItem.path).parentFile
            val videosInFolder = parentFile?.listFiles()
                ?.filter { it.isFile && FileItem.fromFile(it).isVideo }
                ?.sortedBy { it.name }
                ?.map { it.absolutePath }
                ?: listOf(fileItem.path)
            val index = videosInFolder.indexOf(fileItem.path).coerceAtLeast(0)
            onOpenVideo(videosInFolder, index)
        }
        fileItem.isApk -> {
            FileUtils.installApk(context, File(fileItem.path))
        }
        else -> {
            FileUtils.openFile(context, fileItem)
        }
    }
}

@Composable
private fun BreadcrumbRow(
    viewModel: FileViewModel,
    isSelectionMode: Boolean,
    selectedCount: Int,
    viewMode: ViewMode,
    showHidden: Boolean,
    hasClipboard: Boolean,
    onToggleViewMode: () -> Unit,
    onToggleHidden: () -> Unit,
    onShowSort: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateFile: () -> Unit,
    onPaste: () -> Unit
) {
    val fileListState by viewModel.fileListState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Filled.Close, "Clear selection")
            }
            Text("$selectedCount selected", modifier = Modifier.weight(1f))
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Filled.SelectAll, "Select all")
            }
        } else {
            // Breadcrumb
            BreadcrumbNavigation(
                currentPath = fileListState.currentPath,
                rootPath = viewModel.getInternalStoragePath(),
                onNavigateToPath = { viewModel.loadFiles(it) },
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onToggleViewMode) {
                Icon(
                    if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                    "Toggle view"
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("New folder") },
                        leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                        onClick = { showMenu = false; onCreateFolder() }
                    )
                    DropdownMenuItem(
                        text = { Text("New file") },
                        leadingIcon = { Icon(Icons.Filled.NoteAdd, null) },
                        onClick = { showMenu = false; onCreateFile() }
                    )
                    if (hasClipboard) {
                        DropdownMenuItem(
                            text = { Text("Paste") },
                            leadingIcon = { Icon(Icons.Filled.ContentPaste, null) },
                            onClick = { showMenu = false; onPaste() }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Sort") },
                        leadingIcon = { Icon(Icons.Filled.Sort, null) },
                        onClick = { showMenu = false; onShowSort() }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Show hidden")
                                Spacer(Modifier.weight(1f))
                                Checkbox(checked = showHidden, onCheckedChange = null)
                            }
                        },
                        onClick = { onToggleHidden() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBarCompact(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    isRecursive: Boolean,
    onToggleRecursive: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.ArrowBack, "Close search")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, "Clear")
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isRecursive, onCheckedChange = { onToggleRecursive() })
            Text("Search in subfolders", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BreadcrumbNavigation(
    currentPath: String,
    rootPath: String,
    onNavigateToPath: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(currentPath, rootPath) {
        com.dbilgin.dfiles.util.FileUtils.getPathSegments(currentPath, rootPath)
    }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, (name, path) ->
            TextButton(
                onClick = { onNavigateToPath(path) },
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index == segments.lastIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index < segments.lastIndex) {
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
