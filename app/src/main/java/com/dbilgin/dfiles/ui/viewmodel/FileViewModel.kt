package com.dbilgin.dfiles.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dbilgin.dfiles.data.model.FileItem
import com.dbilgin.dfiles.data.model.SortOrder
import com.dbilgin.dfiles.data.model.SortType
import com.dbilgin.dfiles.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class ClipboardOperation {
    COPY,
    MOVE
}

data class ClipboardState(
    val files: List<FileItem> = emptyList(),
    val operation: ClipboardOperation = ClipboardOperation.COPY,
    val sourcePath: String = ""
)

data class FileListState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortType: SortType = SortType.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val showHidden: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST
)

enum class ViewMode {
    LIST,
    GRID
}

data class SelectionState(
    val isSelectionMode: Boolean = false,
    val selectedFiles: Set<String> = emptySet()
)

data class SearchState(
    val isSearching: Boolean = false,
    val query: String = "",
    val results: List<FileItem> = emptyList(),
    val isRecursive: Boolean = false
)

// New gallery state with folder grouping
data class GalleryFolder(
    val name: String,
    val path: String,
    val items: List<FileItem>,
    val isExpanded: Boolean = true
)

data class GalleryState(
    val imageFolders: List<GalleryFolder> = emptyList(),
    val videoFolders: List<GalleryFolder> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0 = images, 1 = videos
    val selectedItems: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

class FileViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = FileRepository(application.applicationContext)
    
    // File list state
    private val _fileListState = MutableStateFlow(FileListState())
    val fileListState: StateFlow<FileListState> = _fileListState.asStateFlow()
    
    // Selection state
    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()
    
    // Clipboard state
    private val _clipboardState = MutableStateFlow(ClipboardState())
    val clipboardState: StateFlow<ClipboardState> = _clipboardState.asStateFlow()
    
    // Search state
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    // Gallery state
    private val _galleryState = MutableStateFlow(GalleryState())
    val galleryState: StateFlow<GalleryState> = _galleryState.asStateFlow()
    
    // Storage info
    private val _storageVolumes = MutableStateFlow<List<FileRepository.StorageInfo>>(emptyList())
    val storageVolumes: StateFlow<List<FileRepository.StorageInfo>> = _storageVolumes.asStateFlow()
    
    // Quick access paths
    private val _quickAccessPaths = MutableStateFlow<Map<String, String>>(emptyMap())
    val quickAccessPaths: StateFlow<Map<String, String>> = _quickAccessPaths.asStateFlow()
    
    // Operation result
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    init {
        loadStorageInfo()
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            _storageVolumes.value = repository.getStorageVolumes()
            _quickAccessPaths.value = repository.getQuickAccessPaths()
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _fileListState.value = _fileListState.value.copy(
                currentPath = path,
                isLoading = true,
                error = null
            )
            
            try {
                val files = repository.listFiles(
                    path = path,
                    sortType = _fileListState.value.sortType,
                    sortOrder = _fileListState.value.sortOrder,
                    showHidden = _fileListState.value.showHidden
                )
                _fileListState.value = _fileListState.value.copy(
                    files = files,
                    isLoading = false
                )
            } catch (e: Exception) {
                _fileListState.value = _fileListState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load files"
                )
            }
            
            // Clear selection when navigating
            clearSelection()
        }
    }

    fun refreshCurrentDirectory() {
        val currentPath = _fileListState.value.currentPath
        if (currentPath.isNotEmpty()) {
            loadFiles(currentPath)
        }
    }

    fun setSortType(sortType: SortType) {
        _fileListState.value = _fileListState.value.copy(sortType = sortType)
        refreshCurrentDirectory()
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _fileListState.value = _fileListState.value.copy(sortOrder = sortOrder)
        refreshCurrentDirectory()
    }

    fun toggleSortOrder() {
        val newOrder = if (_fileListState.value.sortOrder == SortOrder.ASCENDING) {
            SortOrder.DESCENDING
        } else {
            SortOrder.ASCENDING
        }
        setSortOrder(newOrder)
    }

    fun setShowHidden(show: Boolean) {
        _fileListState.value = _fileListState.value.copy(showHidden = show)
        refreshCurrentDirectory()
    }

    fun setViewMode(mode: ViewMode) {
        _fileListState.value = _fileListState.value.copy(viewMode = mode)
    }

    // Selection operations
    fun toggleSelectionMode() {
        _selectionState.value = _selectionState.value.copy(
            isSelectionMode = !_selectionState.value.isSelectionMode,
            selectedFiles = emptySet()
        )
    }

    fun toggleFileSelection(path: String) {
        val currentSelection = _selectionState.value.selectedFiles.toMutableSet()
        if (currentSelection.contains(path)) {
            currentSelection.remove(path)
        } else {
            currentSelection.add(path)
        }
        _selectionState.value = _selectionState.value.copy(
            selectedFiles = currentSelection,
            isSelectionMode = currentSelection.isNotEmpty()
        )
    }

    fun selectAll() {
        val allPaths = _fileListState.value.files.map { it.path }.toSet()
        _selectionState.value = _selectionState.value.copy(
            selectedFiles = allPaths,
            isSelectionMode = true
        )
    }

    fun clearSelection() {
        _selectionState.value = SelectionState()
    }

    fun getSelectedFileItems(): List<FileItem> {
        return _fileListState.value.files.filter { 
            _selectionState.value.selectedFiles.contains(it.path) 
        }
    }

    // Clipboard operations
    fun copyToClipboard(files: List<FileItem>) {
        _clipboardState.value = ClipboardState(
            files = files,
            operation = ClipboardOperation.COPY,
            sourcePath = _fileListState.value.currentPath
        )
        clearSelection()
    }

    fun cutToClipboard(files: List<FileItem>) {
        _clipboardState.value = ClipboardState(
            files = files,
            operation = ClipboardOperation.MOVE,
            sourcePath = _fileListState.value.currentPath
        )
        clearSelection()
    }

    fun paste() {
        val clipboard = _clipboardState.value
        if (clipboard.files.isEmpty()) return
        
        val destinationPath = _fileListState.value.currentPath
        
        viewModelScope.launch {
            _fileListState.value = _fileListState.value.copy(isLoading = true)
            
            val result = when (clipboard.operation) {
                ClipboardOperation.COPY -> {
                    repository.copy(
                        sources = clipboard.files.map { File(it.path) },
                        destinationPath = destinationPath
                    )
                }
                ClipboardOperation.MOVE -> {
                    repository.move(
                        sources = clipboard.files.map { File(it.path) },
                        destinationPath = destinationPath
                    )
                }
            }
            
            result.fold(
                onSuccess = { count ->
                    val operationName = if (clipboard.operation == ClipboardOperation.COPY) "Copied" else "Moved"
                    _operationResult.value = OperationResult.Success("$operationName $count item(s)")
                    if (clipboard.operation == ClipboardOperation.MOVE) {
                        clearClipboard()
                    }
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Operation failed")
                }
            )
            
            refreshCurrentDirectory()
        }
    }

    fun clearClipboard() {
        _clipboardState.value = ClipboardState()
    }

    fun hasClipboardContent(): Boolean = _clipboardState.value.files.isNotEmpty()

    // File operations
    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = repository.createFolder(_fileListState.value.currentPath, name)
            result.fold(
                onSuccess = {
                    _operationResult.value = OperationResult.Success("Folder created")
                    refreshCurrentDirectory()
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Failed to create folder")
                }
            )
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val result = repository.createFile(_fileListState.value.currentPath, name)
            result.fold(
                onSuccess = {
                    _operationResult.value = OperationResult.Success("File created")
                    refreshCurrentDirectory()
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Failed to create file")
                }
            )
        }
    }

    fun rename(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            val result = repository.rename(File(fileItem.path), newName)
            result.fold(
                onSuccess = {
                    _operationResult.value = OperationResult.Success("Renamed successfully")
                    refreshCurrentDirectory()
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Failed to rename")
                }
            )
        }
    }

    fun deleteSelected() {
        val selectedItems = getSelectedFileItems()
        if (selectedItems.isEmpty()) return
        
        viewModelScope.launch {
            _fileListState.value = _fileListState.value.copy(isLoading = true)
            
            val result = repository.delete(selectedItems.map { File(it.path) })
            result.fold(
                onSuccess = { count ->
                    _operationResult.value = OperationResult.Success("Deleted $count item(s)")
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Failed to delete")
                }
            )
            
            clearSelection()
            refreshCurrentDirectory()
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            val result = repository.delete(listOf(File(fileItem.path)))
            result.fold(
                onSuccess = {
                    _operationResult.value = OperationResult.Success("Deleted successfully")
                    refreshCurrentDirectory()
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Failed to delete")
                }
            )
        }
    }

    // Search operations
    fun startSearch() {
        _searchState.value = _searchState.value.copy(isSearching = true)
    }

    fun stopSearch() {
        _searchState.value = SearchState()
    }

    fun search(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        
        if (query.isBlank()) {
            _searchState.value = _searchState.value.copy(results = emptyList())
            return
        }
        
        viewModelScope.launch {
            val results = repository.searchFiles(
                path = _fileListState.value.currentPath,
                query = query,
                recursive = _searchState.value.isRecursive
            )
            _searchState.value = _searchState.value.copy(results = results)
        }
    }

    fun setRecursiveSearch(recursive: Boolean) {
        _searchState.value = _searchState.value.copy(isRecursive = recursive)
        if (_searchState.value.query.isNotBlank()) {
            search(_searchState.value.query)
        }
    }

    // Gallery operations
    fun loadGallery() {
        viewModelScope.launch {
            _galleryState.value = _galleryState.value.copy(isLoading = true)
            
            val imageFolders = repository.getImagesGroupedByFolder().map { folder ->
                GalleryFolder(
                    name = folder.name,
                    path = folder.path,
                    items = folder.items,
                    isExpanded = true
                )
            }
            
            val videoFolders = repository.getVideosGroupedByFolder().map { folder ->
                GalleryFolder(
                    name = folder.name,
                    path = folder.path,
                    items = folder.items,
                    isExpanded = true
                )
            }
            
            _galleryState.value = _galleryState.value.copy(
                imageFolders = imageFolders,
                videoFolders = videoFolders,
                isLoading = false
            )
        }
    }

    fun setGalleryTab(tab: Int) {
        _galleryState.value = _galleryState.value.copy(selectedTab = tab)
    }

    fun toggleGalleryFolderExpanded(folderPath: String) {
        val state = _galleryState.value
        val updatedImageFolders = state.imageFolders.map { folder ->
            if (folder.path == folderPath) folder.copy(isExpanded = !folder.isExpanded)
            else folder
        }
        val updatedVideoFolders = state.videoFolders.map { folder ->
            if (folder.path == folderPath) folder.copy(isExpanded = !folder.isExpanded)
            else folder
        }
        _galleryState.value = state.copy(
            imageFolders = updatedImageFolders,
            videoFolders = updatedVideoFolders
        )
    }

    // Gallery selection
    fun toggleGalleryItemSelection(path: String) {
        val currentSelection = _galleryState.value.selectedItems.toMutableSet()
        if (currentSelection.contains(path)) {
            currentSelection.remove(path)
        } else {
            currentSelection.add(path)
        }
        _galleryState.value = _galleryState.value.copy(
            selectedItems = currentSelection,
            isSelectionMode = currentSelection.isNotEmpty()
        )
    }

    fun clearGallerySelection() {
        _galleryState.value = _galleryState.value.copy(
            selectedItems = emptySet(),
            isSelectionMode = false
        )
    }

    fun getSelectedGalleryItems(): List<FileItem> {
        val selectedPaths = _galleryState.value.selectedItems
        val allItems = mutableListOf<FileItem>()
        _galleryState.value.imageFolders.forEach { allItems.addAll(it.items) }
        _galleryState.value.videoFolders.forEach { allItems.addAll(it.items) }
        return allItems.filter { selectedPaths.contains(it.path) }
    }

    fun deleteSelectedGalleryItems() {
        val selectedItems = getSelectedGalleryItems()
        if (selectedItems.isEmpty()) return
        
        viewModelScope.launch {
            val result = repository.delete(selectedItems.map { File(it.path) })
            result.fold(
                onSuccess = { count ->
                    _operationResult.value = OperationResult.Success("Deleted $count item(s)")
                    clearGallerySelection()
                    loadGallery() // Refresh gallery
                },
                onFailure = { error ->
                    _operationResult.value = OperationResult.Error(error.message ?: "Failed to delete")
                }
            )
        }
    }

    // Utility
    fun getParentPath(): String? {
        return repository.getParentPath(_fileListState.value.currentPath)
    }

    fun isRootPath(): Boolean {
        return repository.isRootPath(_fileListState.value.currentPath)
    }

    fun getInternalStoragePath(): String = repository.getInternalStoragePath()

    fun clearOperationResult() {
        _operationResult.value = null
    }

    fun getFileDetails(path: String): FileItem? = repository.getFileDetails(path)
}

sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Error(val message: String) : OperationResult()
}
