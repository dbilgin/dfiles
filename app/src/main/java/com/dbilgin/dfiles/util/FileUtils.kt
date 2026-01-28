package com.dbilgin.dfiles.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dbilgin.dfiles.data.model.FileItem
import java.io.File

object FileUtils {
    
    fun openFile(context: Context, fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.dbilgin.dfiles.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileItem.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Create chooser to let user pick app (excluding ourselves for non-folder items)
            val chooser = Intent.createChooser(intent, "Open with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun installApk(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.dbilgin.dfiles.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            // Find package installer and set it explicitly to avoid our app intercepting
            val packageManager = context.packageManager
            val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Find the system package installer (not our app)
            val installerInfo = resolveInfos.firstOrNull { 
                it.activityInfo.packageName != context.packageName &&
                (it.activityInfo.packageName.contains("packageinstaller") ||
                 it.activityInfo.packageName.contains("google") ||
                 it.activityInfo.name.contains("Installer", ignoreCase = true))
            } ?: resolveInfos.firstOrNull { it.activityInfo.packageName != context.packageName }
            
            if (installerInfo != null) {
                intent.component = ComponentName(
                    installerInfo.activityInfo.packageName,
                    installerInfo.activityInfo.name
                )
                context.startActivity(intent)
            } else {
                // Fallback: use chooser but exclude our package
                val chooser = Intent.createChooser(intent, "Install with")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot install APK: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun openVideo(context: Context, fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) {
            Toast.makeText(context, "Video file not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.dbilgin.dfiles.provider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileItem.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            
            // Find video player app and set it explicitly to avoid our app intercepting
            val packageManager = context.packageManager
            val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            
            // Find a video player app (not our app)
            val videoPlayerInfo = resolveInfos.firstOrNull { 
                it.activityInfo.packageName != context.packageName &&
                (it.activityInfo.packageName.contains("video") ||
                 it.activityInfo.packageName.contains("player") ||
                 it.activityInfo.packageName.contains("mx") ||
                 it.activityInfo.packageName.contains("vlc") ||
                 it.activityInfo.packageName.contains("vplayer") ||
                 it.activityInfo.name.contains("Video", ignoreCase = true) ||
                 it.activityInfo.name.contains("Player", ignoreCase = true))
            } ?: resolveInfos.firstOrNull { it.activityInfo.packageName != context.packageName }
            
            if (videoPlayerInfo != null) {
                intent.component = ComponentName(
                    videoPlayerInfo.activityInfo.packageName,
                    videoPlayerInfo.activityInfo.name
                )
                context.startActivity(intent)
            } else {
                // Fallback: use chooser (user can pick, but our app won't be in the list if we filter)
                val chooser = Intent.createChooser(intent, "Open video with")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open video: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun shareFile(context: Context, fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.dbilgin.dfiles.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = fileItem.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFiles(context: Context, fileItems: List<FileItem>) {
        if (fileItems.isEmpty()) return
        
        if (fileItems.size == 1) {
            shareFile(context, fileItems.first())
            return
        }

        try {
            val uris = ArrayList<Uri>()
            for (item in fileItems) {
                val file = File(item.path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.dbilgin.dfiles.provider",
                        file
                    )
                    uris.add(uri)
                }
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${uris.size} files"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share files: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getPathSegments(path: String, rootPath: String): List<Pair<String, String>> {
        val segments = mutableListOf<Pair<String, String>>()
        var currentPath = path
        
        while (currentPath != rootPath && currentPath.isNotEmpty() && currentPath != "/") {
            val file = File(currentPath)
            segments.add(0, file.name to currentPath)
            currentPath = file.parent ?: break
        }
        
        // Add root
        val rootName = if (rootPath == "/") "Root" else File(rootPath).name.ifEmpty { "Storage" }
        segments.add(0, rootName to rootPath)
        
        return segments
    }
}
