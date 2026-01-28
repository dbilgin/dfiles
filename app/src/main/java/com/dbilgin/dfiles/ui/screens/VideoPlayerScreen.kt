package com.dbilgin.dfiles.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    videos: List<String>,
    startIndex: Int,
    onNavigateBack: () -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0)),
        pageCount = { videos.size }
    )
    
    var showControls by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    
    val currentVideo = videos.getOrNull(pagerState.currentPage)
    val currentFile = currentVideo?.let { File(it) }
    
    // Handle hardware back button
    BackHandler {
        onNavigateBack()
    }
    
    // Reset playing state when video changes
    LaunchedEffect(pagerState.currentPage) {
        isPlaying = false
        currentPosition = 0
        duration = 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Pager for swiping between videos
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val videoPath = videos[page]
            VideoPlayer(
                videoPath = videoPath,
                isCurrentPage = page == pagerState.currentPage,
                onTap = { showControls = !showControls },
                onPlayingStateChanged = { playing -> isPlaying = playing },
                onPositionChanged = { pos -> currentPosition = pos },
                onDurationChanged = { dur -> duration = dur }
            )
        }

        // Top bar
        if (showControls && currentFile != null) {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = currentFile.name,
                            maxLines = 1,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${videos.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareVideo(context, currentFile) }) {
                        Icon(Icons.Filled.Share, "Share", tint = Color.White)
                    }
                    IconButton(onClick = { openVideoWithExternalApp(context, currentFile) }) {
                        Icon(Icons.Filled.OpenInNew, "Open with", tint = Color.White)
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Bottom info bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (duration > 0) {
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = formatFileSize(currentFile.length()),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && currentVideo != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete this video?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke(currentVideo)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VideoPlayer(
    videoPath: String,
    isCurrentPage: Boolean,
    onTap: () -> Unit,
    onPlayingStateChanged: (Boolean) -> Unit,
    onPositionChanged: (Int) -> Unit,
    onDurationChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    var videoView: VideoView? by remember { mutableStateOf(null) }
    
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                videoView = this
                
                val file = File(videoPath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        ctx,
                        "com.dbilgin.dfiles.provider",
                        file
                    )
                    setVideoURI(uri)
                    
                    setOnPreparedListener { mediaPlayer ->
                        onDurationChanged(mediaPlayer.duration)
                    }
                    
                    setOnCompletionListener {
                        onPlayingStateChanged(false)
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
    )
    
    // Handle play/pause based on current page
    LaunchedEffect(isCurrentPage, videoPath) {
        videoView?.let { view ->
            if (isCurrentPage) {
                // Auto-play when this video becomes the current page
                if (!view.isPlaying) {
                    view.start()
                    onPlayingStateChanged(true)
                }
            } else {
                // Pause when not the current page
                if (view.isPlaying) {
                    view.pause()
                    onPlayingStateChanged(false)
                }
            }
        }
    }
    
    // Update position periodically
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            while (true) {
                kotlinx.coroutines.delay(500)
                videoView?.let {
                    if (it.isPlaying) {
                        onPositionChanged(it.currentPosition)
                    }
                }
            }
        }
    }
}

private fun shareVideo(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.dbilgin.dfiles.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openVideoWithExternalApp(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.dbilgin.dfiles.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatFileSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
    else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
