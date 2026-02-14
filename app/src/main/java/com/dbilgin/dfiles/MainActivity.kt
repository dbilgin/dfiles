package com.dbilgin.dfiles

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dbilgin.dfiles.ui.screens.ImageViewerScreen
import com.dbilgin.dfiles.ui.screens.MainScreen
import com.dbilgin.dfiles.ui.screens.MainTab
import com.dbilgin.dfiles.ui.screens.PermissionScreen
import com.dbilgin.dfiles.ui.screens.VideoPlayerScreen
import com.dbilgin.dfiles.ui.theme.DfilesTheme
import com.dbilgin.dfiles.ui.viewmodel.FileViewModel
import com.dbilgin.dfiles.util.PathUtils
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            DfilesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isPickerMode = intent?.action == Intent.ACTION_GET_CONTENT ||
                        intent?.action == Intent.ACTION_PICK
                    val pickerType = when {
                        !isPickerMode -> null
                        intent?.type?.startsWith("image/") == true -> "image/*"
                        intent?.type?.startsWith("video/") == true -> "video/*"
                        else -> intent?.type ?: "*/*"
                    }
                    val onPickFile: ((path: String, mimeType: String) -> Unit)? =
                        if (isPickerMode) { path, _ ->
                            val file = File(path)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    this@MainActivity,
                                    "com.dbilgin.dfiles.provider",
                                    file
                                )
                                setResult(Activity.RESULT_OK, Intent().apply {
                                    data = uri
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                                finish()
                            }
                        } else null
                    val onPickerCancel: (() -> Unit)? =
                        if (isPickerMode) {
                            {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        } else null
                    DfilesNavigation(
                        onRequestPermission = { requestStoragePermission() },
                        intent = intent,
                        pickerMode = isPickerMode,
                        pickerType = pickerType,
                        onPickFile = onPickFile,
                        onPickerCancel = onPickerCancel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Recreate to handle new intent
        recreate()
    }

    private fun requestStoragePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

@Composable
fun DfilesNavigation(
    onRequestPermission: () -> Unit,
    intent: Intent?,
    pickerMode: Boolean = false,
    pickerType: String? = null,
    onPickFile: ((path: String, mimeType: String) -> Unit)? = null,
    onPickerCancel: (() -> Unit)? = null
) {
    val navController = rememberNavController()
    val viewModel: FileViewModel = viewModel()
    
    var hasPermission by remember { mutableStateOf(Environment.isExternalStorageManager()) }
    
    // Extract path from deeplink intent
    val context = LocalContext.current
    val deeplinkPath = remember(intent) {
        PathUtils.extractPathFromIntent(context, intent)
    }
    
    LaunchedEffect(Unit) {
        hasPermission = Environment.isExternalStorageManager()
    }

    NavHost(
        navController = navController,
        startDestination = if (hasPermission) "main/FILES" else "permission"
    ) {
        composable("permission") {
            PermissionScreen(
                onRequestPermission = onRequestPermission,
                onPermissionGranted = {
                    hasPermission = true
                    navController.navigate("main/FILES") {
                        popUpTo("permission") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "main/{tab}",
            arguments = listOf(navArgument("tab") { type = NavType.StringType })
        ) { backStackEntry ->
            // Determine initial path - use deeplink path's parent folder if available
            val initialPath = remember(deeplinkPath) {
                deeplinkPath?.let { path ->
                    val file = File(path)
                    if (file.isDirectory) path
                    else file.parent ?: Environment.getExternalStorageDirectory().absolutePath
                }
            }
            
            // Get saved tab from route, default to FILES
            val tabString = backStackEntry.arguments?.getString("tab") ?: "FILES"
            val savedTab = remember(tabString) {
                try { MainTab.valueOf(tabString) } catch (e: Exception) { MainTab.FILES }
            }
            
            MainScreen(
                viewModel = viewModel,
                initialTab = savedTab,
                initialPath = initialPath,
                onNavigateToFolder = { path ->
                    viewModel.loadFiles(path)
                },
                onOpenImage = { images, index, fromTab ->
                    val encodedImages = images.joinToString("|") { 
                        URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) 
                    }
                    navController.navigate("imageViewer/$fromTab/$index/$encodedImages") {
                        // Save state so we can restore tab and folder when navigating back
                        launchSingleTop = false
                    }
                },
                onOpenVideo = { videos, index, fromTab ->
                    val encodedVideos = videos.joinToString("|") { 
                        URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) 
                    }
                    navController.navigate("videoPlayer/$fromTab/$index/$encodedVideos") {
                        // Save state so we can restore tab and folder when navigating back
                        launchSingleTop = false
                    }
                },
                pickerMode = pickerMode,
                pickerType = pickerType,
                onPickFile = onPickFile,
                onPickerCancel = onPickerCancel
            )
        }

        composable(
            route = "imageViewer/{fromTab}/{index}/{images}",
            arguments = listOf(
                navArgument("fromTab") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType },
                navArgument("images") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val fromTab = backStackEntry.arguments?.getString("fromTab") ?: "FILES"
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            val encodedImages = backStackEntry.arguments?.getString("images") ?: ""
            val images = encodedImages.split("|").map { 
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) 
            }
            
            ImageViewerScreen(
                images = images,
                startIndex = index,
                onNavigateBack = { 
                    // First pop the image viewer from the stack
                    navController.popBackStack()
                    // Then navigate to the correct tab route based on fromTab
                    // This ensures we go back to the correct tab even if the route wasn't updated when tab changed
                    navController.navigate("main/$fromTab") {
                        // Don't create duplicate entries if already at that route
                        launchSingleTop = true
                        // Restore state
                        restoreState = true
                    }
                },
                onDelete = { deletedPath ->
                    viewModel.getFileDetails(deletedPath)?.let { viewModel.deleteFile(it) }
                    navController.popBackStack()
                    navController.navigate("main/$fromTab") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(
            route = "videoPlayer/{fromTab}/{index}/{videos}",
            arguments = listOf(
                navArgument("fromTab") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType },
                navArgument("videos") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val fromTab = backStackEntry.arguments?.getString("fromTab") ?: "FILES"
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            val encodedVideos = backStackEntry.arguments?.getString("videos") ?: ""
            val videos = encodedVideos.split("|").map { 
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) 
            }
            
            VideoPlayerScreen(
                videos = videos,
                startIndex = index,
                onNavigateBack = { 
                    // First pop the video player from the stack
                    navController.popBackStack()
                    // Then navigate to the correct tab route based on fromTab
                    navController.navigate("main/$fromTab") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onDelete = { deletedPath ->
                    viewModel.getFileDetails(deletedPath)?.let { viewModel.deleteFile(it) }
                    navController.popBackStack()
                    navController.navigate("main/$fromTab") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
