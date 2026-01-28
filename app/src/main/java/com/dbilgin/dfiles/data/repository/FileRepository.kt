package com.dbilgin.dfiles.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.dbilgin.dfiles.data.model.FileItem
import com.dbilgin.dfiles.data.model.SortOrder
import com.dbilgin.dfiles.data.model.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileRepository(private val context: Context) {

    data class StorageInfo(
        val name: String,
        val path: String,
        val totalSpace: Long,
        val freeSpace: Long,
        val usedSpace: Long
    ) {
        val usedPercentage: Float
            get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace.toFloat()) * 100f else 0f
        
        val formattedTotal: String
            get() = formatSize(totalSpace)
        
        val formattedFree: String
            get() = formatSize(freeSpace)
        
        val formattedUsed: String
            get() = formatSize(usedSpace)
        
        private fun formatSize(size: Long): String = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun getInternalStoragePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    fun getStorageVolumes(): List<StorageInfo> {
        val volumes = mutableListOf<StorageInfo>()
        
        // Internal storage
        val internalPath = Environment.getExternalStorageDirectory()
        if (internalPath.exists()) {
            val stat = StatFs(internalPath.absolutePath)
            val total = stat.blockSizeLong * stat.blockCountLong
            val free = stat.blockSizeLong * stat.availableBlocksLong
            volumes.add(
                StorageInfo(
                    name = "Internal Storage",
                    path = internalPath.absolutePath,
                    totalSpace = total,
                    freeSpace = free,
                    usedSpace = total - free
                )
            )
        }
        
        // External storage (SD cards)
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes
            
            for (volume in storageVolumes) {
                if (!volume.isPrimary && volume.isRemovable) {
                    val path = volume.directory
                    if (path != null && path.exists()) {
                        val stat = StatFs(path.absolutePath)
                        val total = stat.blockSizeLong * stat.blockCountLong
                        val free = stat.blockSizeLong * stat.availableBlocksLong
                        volumes.add(
                            StorageInfo(
                                name = volume.getDescription(context) ?: "SD Card",
                                path = path.absolutePath,
                                totalSpace = total,
                                freeSpace = free,
                                usedSpace = total - free
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return volumes
    }

    fun getQuickAccessPaths(): Map<String, String> {
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        return mapOf(
            "Downloads" to "$basePath/${Environment.DIRECTORY_DOWNLOADS}",
            "DCIM" to "$basePath/${Environment.DIRECTORY_DCIM}",
            "Pictures" to "$basePath/${Environment.DIRECTORY_PICTURES}",
            "Music" to "$basePath/${Environment.DIRECTORY_MUSIC}",
            "Movies" to "$basePath/${Environment.DIRECTORY_MOVIES}",
            "Documents" to "$basePath/${Environment.DIRECTORY_DOCUMENTS}"
        )
    }

    suspend fun listFiles(
        path: String,
        sortType: SortType = SortType.NAME,
        sortOrder: SortOrder = SortOrder.ASCENDING,
        showHidden: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext emptyList()
        }

        val files = directory.listFiles()?.toList() ?: emptyList()
        
        val filteredFiles = if (showHidden) files else files.filter { !it.name.startsWith(".") }
        
        val fileItems = filteredFiles.map { FileItem.fromFile(it) }
        
        // Sort: directories first, then files
        val (directories, regularFiles) = fileItems.partition { it.isDirectory }
        
        val sortedDirectories = sortFiles(directories, sortType, sortOrder)
        val sortedFiles = sortFiles(regularFiles, sortType, sortOrder)
        
        sortedDirectories + sortedFiles
    }

    private fun sortFiles(
        files: List<FileItem>,
        sortType: SortType,
        sortOrder: SortOrder
    ): List<FileItem> {
        val comparator: Comparator<FileItem> = when (sortType) {
            SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortType.DATE -> compareBy { it.lastModified }
            SortType.SIZE -> compareBy { it.size }
            SortType.TYPE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.extension }
        }
        
        return if (sortOrder == SortOrder.ASCENDING) {
            files.sortedWith(comparator)
        } else {
            files.sortedWith(comparator.reversed())
        }
    }

    suspend fun createFolder(parentPath: String, folderName: String): Result<File> = 
        withContext(Dispatchers.IO) {
            try {
                val newFolder = File(parentPath, folderName)
                if (newFolder.exists()) {
                    Result.failure(Exception("Folder already exists"))
                } else if (newFolder.mkdir()) {
                    Result.success(newFolder)
                } else {
                    Result.failure(Exception("Failed to create folder"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createFile(parentPath: String, fileName: String): Result<File> = 
        withContext(Dispatchers.IO) {
            try {
                val newFile = File(parentPath, fileName)
                if (newFile.exists()) {
                    Result.failure(Exception("File already exists"))
                } else if (newFile.createNewFile()) {
                    Result.success(newFile)
                } else {
                    Result.failure(Exception("Failed to create file"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun rename(file: File, newName: String): Result<File> = 
        withContext(Dispatchers.IO) {
            try {
                val newFile = File(file.parent, newName)
                if (newFile.exists()) {
                    Result.failure(Exception("A file with this name already exists"))
                } else if (file.renameTo(newFile)) {
                    Result.success(newFile)
                } else {
                    Result.failure(Exception("Failed to rename"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun delete(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (file in files) {
            if (deleteRecursively(file)) {
                deletedCount++
            }
        }
        if (deletedCount == files.size) {
            Result.success(deletedCount)
        } else {
            Result.failure(Exception("Failed to delete some files. Deleted $deletedCount of ${files.size}"))
        }
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                if (!deleteRecursively(child)) {
                    return false
                }
            }
        }
        return file.delete()
    }

    suspend fun copy(sources: List<File>, destinationPath: String): Result<Int> = 
        withContext(Dispatchers.IO) {
            var copiedCount = 0
            for (source in sources) {
                val destination = File(destinationPath, source.name)
                if (copyRecursively(source, destination)) {
                    copiedCount++
                }
            }
            if (copiedCount == sources.size) {
                Result.success(copiedCount)
            } else {
                Result.failure(Exception("Failed to copy some files. Copied $copiedCount of ${sources.size}"))
            }
        }

    private fun copyRecursively(source: File, destination: File): Boolean {
        return try {
            if (source.isDirectory) {
                if (!destination.exists()) {
                    destination.mkdirs()
                }
                source.listFiles()?.forEach { child ->
                    val newDestination = File(destination, child.name)
                    if (!copyRecursively(child, newDestination)) {
                        return false
                    }
                }
                true
            } else {
                copyFile(source, destination)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun move(sources: List<File>, destinationPath: String): Result<Int> = 
        withContext(Dispatchers.IO) {
            var movedCount = 0
            for (source in sources) {
                val destination = File(destinationPath, source.name)
                // Try simple rename first (works if on same volume)
                if (source.renameTo(destination)) {
                    movedCount++
                } else {
                    // Fall back to copy + delete
                    if (copyRecursively(source, destination) && deleteRecursively(source)) {
                        movedCount++
                    }
                }
            }
            if (movedCount == sources.size) {
                Result.success(movedCount)
            } else {
                Result.failure(Exception("Failed to move some files. Moved $movedCount of ${sources.size}"))
            }
        }

    suspend fun searchFiles(
        path: String,
        query: String,
        recursive: Boolean = false
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val directory = File(path)
        
        if (!directory.exists() || !directory.isDirectory) {
            return@withContext results
        }
        
        searchInDirectory(directory, query.lowercase(), recursive, results)
        results
    }

    private fun searchInDirectory(
        directory: File,
        query: String,
        recursive: Boolean,
        results: MutableList<FileItem>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.name.lowercase().contains(query)) {
                results.add(FileItem.fromFile(file))
            }
            if (recursive && file.isDirectory && !file.name.startsWith(".")) {
                searchInDirectory(file, query, recursive, results)
            }
        }
    }

    data class MediaFolder(
        val name: String,
        val path: String,
        val items: List<FileItem>
    )

    suspend fun getImagesGroupedByFolder(): List<MediaFolder> = withContext(Dispatchers.IO) {
        val images = mutableListOf<FileItem>()
        val rootPath = Environment.getExternalStorageDirectory()
        findMediaFiles(rootPath, images, isImage = true, isVideo = false)
        groupByFolder(images)
    }

    suspend fun getVideosGroupedByFolder(): List<MediaFolder> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<FileItem>()
        val rootPath = Environment.getExternalStorageDirectory()
        findMediaFiles(rootPath, videos, isImage = false, isVideo = true)
        groupByFolder(videos)
    }

    private fun groupByFolder(items: List<FileItem>): List<MediaFolder> {
        return items
            .groupBy { File(it.path).parent ?: "" }
            .map { (folderPath, folderItems) ->
                MediaFolder(
                    name = File(folderPath).name.ifEmpty { "Root" },
                    path = folderPath,
                    items = folderItems.sortedByDescending { it.lastModified }
                )
            }
            .sortedByDescending { folder -> 
                folder.items.maxOfOrNull { it.lastModified } ?: 0L 
            }
    }

    suspend fun getAllImages(): List<FileItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<FileItem>()
        val rootPath = Environment.getExternalStorageDirectory()
        findMediaFiles(rootPath, images, isImage = true, isVideo = false)
        images.sortedByDescending { it.lastModified }
    }

    suspend fun getAllVideos(): List<FileItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<FileItem>()
        val rootPath = Environment.getExternalStorageDirectory()
        findMediaFiles(rootPath, videos, isImage = false, isVideo = true)
        videos.sortedByDescending { it.lastModified }
    }

    suspend fun getAllMedia(): List<FileItem> = withContext(Dispatchers.IO) {
        val media = mutableListOf<FileItem>()
        val rootPath = Environment.getExternalStorageDirectory()
        findMediaFiles(rootPath, media, isImage = true, isVideo = true)
        media.sortedByDescending { it.lastModified }
    }

    private fun findMediaFiles(
        directory: File,
        results: MutableList<FileItem>,
        isImage: Boolean,
        isVideo: Boolean
    ) {
        // Skip hidden directories and Android data directories
        if (directory.name.startsWith(".") || 
            directory.name == "Android" ||
            directory.absolutePath.contains("/Android/data")) {
            return
        }

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findMediaFiles(file, results, isImage, isVideo)
            } else {
                val item = FileItem.fromFile(file)
                if ((isImage && item.isImage) || (isVideo && item.isVideo)) {
                    results.add(item)
                }
            }
        }
    }

    fun getFileDetails(path: String): FileItem? {
        val file = File(path)
        return if (file.exists()) FileItem.fromFile(file) else null
    }

    fun getParentPath(path: String): String? {
        val file = File(path)
        return file.parent
    }

    fun isRootPath(path: String): Boolean {
        val internalRoot = Environment.getExternalStorageDirectory().absolutePath
        return path == internalRoot || path == "/" || 
               getStorageVolumes().any { it.path == path }
    }
}
