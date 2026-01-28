package com.dbilgin.dfiles.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.dbilgin.dfiles.data.model.FileItem

object MimeTypeUtils {

    fun getFileIcon(fileItem: FileItem): ImageVector {
        if (fileItem.isDirectory) {
            return Icons.Filled.Folder
        }

        return when {
            fileItem.isImage -> Icons.Filled.Image
            fileItem.isVideo -> Icons.Filled.VideoFile
            fileItem.isAudio -> Icons.Filled.AudioFile
            fileItem.isPdf -> Icons.Filled.PictureAsPdf
            fileItem.isApk -> Icons.Filled.Android
            fileItem.isArchive -> Icons.Filled.FolderZip
            fileItem.isText -> Icons.Filled.Description
            fileItem.extension in listOf("doc", "docx", "odt") -> Icons.Filled.Description
            fileItem.extension in listOf("xls", "xlsx", "ods") -> Icons.Filled.TableChart
            fileItem.extension in listOf("ppt", "pptx", "odp") -> Icons.Filled.Slideshow
            else -> Icons.Filled.InsertDriveFile
        }
    }

    fun getCategoryIcon(category: String): ImageVector {
        return when (category.lowercase()) {
            "downloads" -> Icons.Filled.Download
            "dcim", "pictures", "images" -> Icons.Filled.Image
            "music", "audio" -> Icons.Filled.MusicNote
            "movies", "videos" -> Icons.Filled.VideoLibrary
            "documents" -> Icons.Filled.Description
            else -> Icons.Filled.Folder
        }
    }
}
