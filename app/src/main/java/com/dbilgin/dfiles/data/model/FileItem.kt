package com.dbilgin.dfiles.data.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val mimeType: String
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")
    
    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
    
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/")
    
    val isApk: Boolean
        get() = extension.equals("apk", ignoreCase = true)
    
    val isText: Boolean
        get() = mimeType.startsWith("text/") || 
                extension in listOf("txt", "md", "json", "xml", "html", "css", "js", "kt", "java", "py", "c", "cpp", "h")
    
    val isPdf: Boolean
        get() = mimeType == "application/pdf" || extension.equals("pdf", ignoreCase = true)
    
    val isArchive: Boolean
        get() = extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz", "bz2")

    val formattedSize: String
        get() = when {
            isDirectory -> ""
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    companion object {
        fun fromFile(file: File): FileItem {
            val extension = file.extension.lowercase()
            return FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                extension = extension,
                mimeType = getMimeType(extension)
            )
        }

        private fun getMimeType(extension: String): String {
            return when (extension.lowercase()) {
                // Images
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "svg" -> "image/svg+xml"
                "ico" -> "image/x-icon"
                
                // Videos
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "avi" -> "video/x-msvideo"
                "mov" -> "video/quicktime"
                "wmv" -> "video/x-ms-wmv"
                "flv" -> "video/x-flv"
                "webm" -> "video/webm"
                "3gp" -> "video/3gpp"
                
                // Audio
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                "aac" -> "audio/aac"
                "m4a" -> "audio/mp4"
                "wma" -> "audio/x-ms-wma"
                
                // Documents
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "odt" -> "application/vnd.oasis.opendocument.text"
                "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
                "odp" -> "application/vnd.oasis.opendocument.presentation"
                
                // Text
                "txt" -> "text/plain"
                "html", "htm" -> "text/html"
                "css" -> "text/css"
                "js" -> "text/javascript"
                "json" -> "application/json"
                "xml" -> "text/xml"
                "md" -> "text/markdown"
                "csv" -> "text/csv"
                
                // Code
                "kt" -> "text/x-kotlin"
                "java" -> "text/x-java"
                "py" -> "text/x-python"
                "c", "cpp", "h" -> "text/x-c"
                "sh" -> "application/x-sh"
                
                // Archives
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                "tar" -> "application/x-tar"
                "gz" -> "application/gzip"
                
                // Android
                "apk" -> "application/vnd.android.package-archive"
                
                else -> "application/octet-stream"
            }
        }
    }
}

enum class SortType {
    NAME,
    DATE,
    SIZE,
    TYPE
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}
