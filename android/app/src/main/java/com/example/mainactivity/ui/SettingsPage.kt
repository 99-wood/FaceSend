package com.example.mainactivity.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    nickname: String,
    defaultNickname: String,
    onNicknameChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSelectDirClick: () -> Unit
) {
    var nicknameInput by remember(nickname) {
        mutableStateOf(nickname.ifBlank { defaultNickname })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text("昵称", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = nicknameInput,
                onValueChange = { if (it.length <= 20) nicknameInput = it },
                label = { Text("昵称") },
                placeholder = { Text(defaultNickname) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onNicknameChange(nicknameInput) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = nicknameInput.isNotBlank() && nicknameInput != nickname
            ) {
                Text("保存")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "昵称将在房间中显示给其他用户",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("存储", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "下载的文件保存在 Download/FaceSend 目录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSelectDirClick,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("选择其他目录")
            }
        }
    }
}
