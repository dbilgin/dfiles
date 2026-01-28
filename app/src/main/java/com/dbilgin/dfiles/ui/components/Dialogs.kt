package com.dbilgin.dfiles.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbilgin.dfiles.data.model.FileItem
import com.dbilgin.dfiles.data.model.SortOrder
import com.dbilgin.dfiles.data.model.SortType

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { 
                    folderName = it
                    isError = it.contains("/") || it.contains("\\")
                },
                label = { Text("Folder name") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("Name cannot contain / or \\") }
                } else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (folderName.isNotBlank() && !isError) {
                        onCreate(folderName.trim())
                        onDismiss()
                    }
                },
                enabled = folderName.isNotBlank() && !isError
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
        title = { Text("New File") },
        text = {
            OutlinedTextField(
                value = fileName,
                onValueChange = { 
                    fileName = it
                    isError = it.contains("/") || it.contains("\\")
                },
                label = { Text("File name") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("Name cannot contain / or \\") }
                } else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (fileName.isNotBlank() && !isError) {
                        onCreate(fileName.trim())
                        onDismiss()
                    }
                },
                enabled = fileName.isNotBlank() && !isError
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { 
                    newName = it
                    isError = it.contains("/") || it.contains("\\")
                },
                label = { Text("New name") },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text("Name cannot contain / or \\") }
                } else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (newName.isNotBlank() && !isError && newName != currentName) {
                        onRename(newName.trim())
                        onDismiss()
                    }
                },
                enabled = newName.isNotBlank() && !isError && newName != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    itemCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Filled.Delete, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            ) 
        },
        title = { Text("Delete") },
        text = {
            Text(
                if (itemCount == 1) {
                    "Are you sure you want to delete this item? This action cannot be undone."
                } else {
                    "Are you sure you want to delete $itemCount items? This action cannot be undone."
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileDetailsDialog(
    fileItem: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        title = { Text("Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Name", fileItem.name)
                DetailRow("Path", fileItem.path)
                DetailRow("Type", if (fileItem.isDirectory) "Folder" else fileItem.extension.uppercase().ifEmpty { "Unknown" })
                if (!fileItem.isDirectory) {
                    DetailRow("Size", fileItem.formattedSize)
                }
                DetailRow("Modified", fileItem.formattedDate)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SortDialog(
    currentSortType: SortType,
    currentSortOrder: SortOrder,
    onDismiss: () -> Unit,
    onSortTypeSelected: (SortType) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Sort, contentDescription = null) },
        title = { Text("Sort by") },
        text = {
            Column {
                SortType.entries.forEach { sortType ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = sortType == currentSortType,
                            onClick = { onSortTypeSelected(sortType) }
                        )
                        Text(
                            text = when (sortType) {
                                SortType.NAME -> "Name"
                                SortType.DATE -> "Date"
                                SortType.SIZE -> "Size"
                                SortType.TYPE -> "Type"
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentSortOrder == SortOrder.ASCENDING,
                        onClick = { onSortOrderSelected(SortOrder.ASCENDING) },
                        label = { Text("Ascending") },
                        leadingIcon = if (currentSortOrder == SortOrder.ASCENDING) {
                            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = currentSortOrder == SortOrder.DESCENDING,
                        onClick = { onSortOrderSelected(SortOrder.DESCENDING) },
                        label = { Text("Descending") },
                        leadingIcon = if (currentSortOrder == SortOrder.DESCENDING) {
                            { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsSheet(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onSelect: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            HorizontalDivider()
            
            if (onSelect != null) {
                ListItem(
                    headlineContent = { Text("Select") },
                    leadingContent = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    modifier = Modifier.clickable { onSelect(); onDismiss() }
                )
                HorizontalDivider()
            }
            
            ListItem(
                headlineContent = { Text("Open") },
                leadingContent = { Icon(Icons.Filled.OpenInNew, contentDescription = null) },
                modifier = Modifier.clickable { onOpen(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Rename") },
                leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                modifier = Modifier.clickable { onRename(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Copy") },
                leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                modifier = Modifier.clickable { onCopy(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Cut") },
                leadingContent = { Icon(Icons.Filled.ContentCut, contentDescription = null) },
                modifier = Modifier.clickable { onCut(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Share") },
                leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                modifier = Modifier.clickable { onShare(); onDismiss() }
            )
            
            ListItem(
                headlineContent = { Text("Details") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                modifier = Modifier.clickable { onDetails(); onDismiss() }
            )
            
            HorizontalDivider()
            
            ListItem(
                headlineContent = { 
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    ) 
                },
                leadingContent = { 
                    Icon(
                        Icons.Filled.Delete, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    ) 
                },
                modifier = Modifier.clickable { onDelete(); onDismiss() }
            )
        }
    }
}

