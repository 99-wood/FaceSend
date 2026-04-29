package com.example.mainactivity.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mainactivity.data.FileInfo
import com.example.mainactivity.data.RoomMemberInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomPage(
    roomCode: String,
    isOwner: Boolean,
    files: List<FileInfo>,
    roomMembers: List<RoomMemberInfo>,
    currentDeviceId: String,
    allowMemberUpload: Boolean,
    uploadProgress: Float,
    uploadingFileName: String,
    downloadingFileId: Long?,
    downloadProgress: Float,
    downloadedFileIds: Set<Long>,
    onBackClick: () -> Unit,
    onUploadClick: () -> Unit,
    onDownloadClick: (FileInfo) -> Unit,
    onToggleAllowUpload: (Boolean) -> Unit,
    onUpdateNickname: (String) -> Unit
) {
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }
    val canUpload = isOwner || allowMemberUpload

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { if (it.length <= 20) nicknameInput = it },
                    label = { Text("新昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nicknameInput.isNotBlank()) {
                            onUpdateNickname(nicknameInput)
                            showNicknameDialog = false
                        }
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("房间 $roomCode") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canUpload) {
                FloatingActionButton(
                    onClick = onUploadClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "上传文件")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 房间码卡片
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        roomCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isOwner) "将此码分享给对方" else "已连接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // 成员区域 - 限制最大高度，超出可滚动
            if (roomMembers.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${roomMembers.size} 人在线", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        roomMembers.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (member.deviceId == currentDeviceId) "${member.nickname} (我)" else member.nickname,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                if (member.deviceId == currentDeviceId) {
                                    IconButton(onClick = {
                                        nicknameInput = member.nickname
                                        showNicknameDialog = true
                                    }, modifier = Modifier.size(22.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "修改昵称", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 房主控制
            if (isOwner) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("允许成员上传", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(
                        checked = allowMemberUpload,
                        onCheckedChange = { onToggleAllowUpload(it) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // 上传进度
            if (uploadProgress in 0.01f..0.99f) {
                UploadProgressCard(uploadingFileName, uploadProgress)
                Spacer(Modifier.height(4.dp))
            }

            // 文件列表标题
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("文件", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("${files.size} 个", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 文件列表
            if (files.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    if (!canUpload) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("等待文件...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            "点击 + 上传文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(files) { file ->
                        RoomFileCard(
                            file = file,
                            isDownloading = downloadingFileId == file.id,
                            downloadProgress = if (downloadingFileId == file.id) downloadProgress else 0f,
                            isDownloaded = downloadedFileIds.contains(file.id),
                            onDownloadClick = { onDownloadClick(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomFileCard(
    file: FileInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    isDownloaded: Boolean,
    onDownloadClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(targetValue = downloadProgress, label = "dl")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatFileSize(file.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                when {
                    isDownloading -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                    }
                    isDownloaded -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已下载",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onDownloadClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("下载", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            if (isDownloading && downloadProgress > 0f) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UploadProgressCard(fileName: String, progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "upload")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("上传中", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(fileName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0 / 1024.0)} MB"
        else -> "${"%.2f".format(bytes / 1024.0 / 1024.0 / 1024.0)} GB"
    }
}
