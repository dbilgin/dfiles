package com.dbilgin.dfiles.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

object PathUtils {
    
    /**
     * Extracts file path from an intent, handling both file:// and content:// URIs
     */
    fun extractPathFromIntent(context: Context, intent: Intent?): String? {
        if (intent == null) return null
        
        val uri = intent.data ?: return null
        
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> {
                // Use the improved content URI handler
                getPathFromContentUri(context, uri) ?: run {
                    // Fallback: Try to extract path from URI path string
                    uri.path?.let { path ->
                        // Handle /external/... paths
                        if (path.contains("/external/")) {
                            val externalPath = path.substringAfter("/external/")
                            val fullPath = "${Environment.getExternalStorageDirectory()}/$externalPath"
                            // Normalize download folder case
                            normalizeDownloadPath(fullPath) ?: fullPath
                        } else if (path.startsWith("/document/")) {
                            // Handle document provider paths like /document/primary:Download/file.apk
                            val docPath = path.substringAfter("/document/")
                            if (docPath.startsWith("primary:")) {
                                val fullPath = "${Environment.getExternalStorageDirectory()}/${docPath.substringAfter("primary:")}"
                                // Normalize download folder case
                                normalizeDownloadPath(fullPath) ?: fullPath
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }
            else -> null
        }
    }
    
    /**
     * Extracts file path from a content URI using multiple methods
     */
    fun getPathFromContentUri(context: Context, uri: Uri): String? {
        // Try to get the actual file path from content URI
        try {
            // First, try to get the DATA column (file path)
            var dataPath: String? = null
            try {
                context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        dataPath = cursor.getString(columnIndex)
                    }
                }
            } catch (e: Exception) {
                // DATA column might not be available for all URIs
            }
            
            // If we got a path from DATA, check if it exists and normalize case
            if (!dataPath.isNullOrEmpty()) {
                val file = File(dataPath)
                if (file.exists()) {
                    return dataPath
                }
                
                // Path doesn't exist - might be case sensitivity issue
                // Try to normalize common download folder names
                val normalizedPath = normalizeDownloadPath(dataPath)
                if (normalizedPath != null && File(normalizedPath).exists()) {
                    return normalizedPath
                }
            }
            
            // Try RELATIVE_PATH column (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    var relativePath: String? = null
                    var displayName: String? = null
                    context.contentResolver.query(uri, arrayOf(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        MediaStore.MediaColumns.DISPLAY_NAME
                    ), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val relPathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                            if (relPathIndex >= 0) relativePath = cursor.getString(relPathIndex)
                            if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                        }
                    }
                    
                    if (relativePath != null && displayName != null) {
                        val fullPath = "${Environment.getExternalStorageDirectory()}/$relativePath$displayName"
                        if (File(fullPath).exists()) {
                            return fullPath
                        }
                    }
                } catch (e: Exception) {
                    // RELATIVE_PATH might not be available
                }
            }
            
            // Fallback: Try DISPLAY_NAME and check common download locations
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileName = cursor.getString(0)
                    // Try multiple download folder variations
                    val downloadFolders = listOf("Download", "downloads", "Downloads")
                    val basePath = Environment.getExternalStorageDirectory().absolutePath
                    
                    for (folder in downloadFolders) {
                        val downloadPath = "$basePath/$folder/$fileName"
                        if (File(downloadPath).exists()) {
                            return downloadPath
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * Normalizes download folder paths to handle case sensitivity issues
     * Converts common variations like "downloads" to "Download"
     */
    fun normalizeDownloadPath(path: String?): String? {
        if (path == null) return null
        
        // Common case sensitivity fixes for download folders
        val patterns = listOf(
            "/downloads/" to "/Download/",
            "/Downloads/" to "/Download/",
            "/0/downloads/" to "/0/Download/",
            "/0/Downloads/" to "/0/Download/"
        )
        
        var normalized = path
        for ((wrong, correct) in patterns) {
            if (normalized?.contains(wrong, ignoreCase = true) == true) {
                normalized = normalized.replace(wrong, correct, ignoreCase = true)
                break
            }
        }
        
        return normalized
    }
}
