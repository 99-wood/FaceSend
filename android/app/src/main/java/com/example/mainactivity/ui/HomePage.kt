package com.example.mainactivity.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onCreateRoomClick: () -> Unit,
    onJoinRoomClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    isLoading: Boolean,
    errorMessage: String,
    debugMode: Boolean = false,
    debugLat: String = "",
    debugLng: String = "",
    onDebugToggle: () -> Unit = {},
    onDebugLatChange: (String) -> Unit = {},
    onDebugLngChange: (String) -> Unit = {}
) {
    var roomCodeText by remember { mutableStateOf("") }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(roomCodeText) {
        if (roomCodeText.length == 6 && !isLoading) {
            onJoinRoomClick(roomCodeText)
            roomCodeText = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FaceSend",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 2000) {
                                tapCount = 1
                            } else {
                                tapCount++
                            }
                            lastTapTime = now
                            if (tapCount >= 5) {
                                tapCount = 0
                                onDebugToggle()
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "面对面文件传输",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCreateRoomClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Create, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("创建房间", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                "输入房间码加入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = roomCodeText,
                    onValueChange = { v ->
                        if (v.length <= 6 && v.all { it.isDigit() }) roomCodeText = v
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.surface, fontSize = 1.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.surface),
                    enabled = !isLoading,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .size(1.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { focusRequester.requestFocus() }
                ) {
                    for (i in 0 until 6) {
                        val char = roomCodeText.getOrNull(i)
                        val isCurrent = i == roomCodeText.length
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char?.toString() ?: "",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            }

            if (debugMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("DEBUG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = debugLat,
                                onValueChange = onDebugLatChange,
                                label = { Text("纬度") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = debugLng,
                                onValueChange = onDebugLngChange,
                                label = { Text("经度") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                }
            }
        }
    }
}
