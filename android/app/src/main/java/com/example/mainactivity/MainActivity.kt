package com.example.mainactivity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mainactivity.data.*
import com.example.mainactivity.ui.HomePage
import com.example.mainactivity.ui.RoomPage
import com.example.mainactivity.ui.SettingsPage
import com.example.mainactivity.util.FileUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var preferencesManager: PreferencesManager
    private var pendingLocationAction: (() -> Unit)? = null
    private var pendingDownloadAction: (() -> Unit)? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pendingLocationAction?.invoke()
        } else {
            Log.w("FaceSend", "位置权限被拒绝")
        }
        pendingLocationAction = null
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingDownloadAction?.invoke()
        } else {
            Log.w("FaceSend", "存储权限被拒绝")
        }
        pendingDownloadAction = null
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> uploadFile(uri) }
        }
    }

    sealed class Screen {
        object Home : Screen()
        object Settings : Screen()
        data class Room(val code: String, val roomId: Long, val isOwner: Boolean) : Screen()
    }

    private val api by lazy { RetrofitClient.api }
    private val deviceId by lazy {
        "android-${android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)}"
    }

    private var currentRoomId: Long = 0L

    private var uploadProgressState: MutableState<Float>? = null
    private var uploadFileNameState: MutableState<String>? = null
    private var downloadProgressState: MutableState<Float>? = null

    private var isDebugMode = mutableStateOf(false)
    private var debugLat = mutableStateOf("")
    private var debugLng = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        preferencesManager = PreferencesManager(this)
        requestStoragePermissions()

        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var isLoading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }
                var files by remember { mutableStateOf(emptyList<FileInfo>()) }
                var roomMembers by remember { mutableStateOf(emptyList<RoomMemberInfo>()) }
                var allowMemberUpload by remember { mutableStateOf(false) }
                var downloadingFileId by remember { mutableStateOf<Long?>(null) }
                var uploadProgress by remember { mutableStateOf(0f) }
                var uploadingFileName by remember { mutableStateOf("") }
                var downloadProgress by remember { mutableStateOf(0f) }
                var nickname by remember { mutableStateOf(preferencesManager.nickname) }

                uploadProgressState = remember { mutableStateOf(0f) }.also { uploadProgress = it.value }
                uploadFileNameState = remember { mutableStateOf("") }.also { uploadingFileName = it.value }
                downloadProgressState = remember { mutableStateOf(0f) }.also { downloadProgress = it.value }

                LaunchedEffect(currentScreen) {
                    if (currentScreen is Screen.Room) {
                        val roomId = (currentScreen as Screen.Room).roomId
                        while (true) {
                            try {
                                val resp = api.listFiles(roomId)
                                if (resp.code == 200 && resp.data != null) {
                                    files = resp.data
                                }
                            } catch (_: Exception) {}
                            try {
                                val membersResp = api.getRoomMembers(roomId, deviceId)
                                if (membersResp.code == 200 && membersResp.data != null) {
                                    roomMembers = membersResp.data.members
                                    allowMemberUpload = membersResp.data.allowMemberUpload
                                }
                            } catch (e: Exception) {
                                Log.e("FaceSend", "轮询成员列表失败: ${e.message}")
                            }
                            delay(2000)
                        }
                    }
                }

                BackHandler(enabled = currentScreen !is Screen.Home) {
                    if (currentScreen is Screen.Room && (currentScreen as Screen.Room).isOwner) {
                        val roomId = (currentScreen as Screen.Room).roomId
                        lifecycleScope.launch {
                            try { api.closeRoom(roomId) } catch (_: Exception) {}
                        }
                    }
                    currentScreen = Screen.Home
                }

                when (currentScreen) {
                    Screen.Home -> {
                        HomePage(
                            onCreateRoomClick = {
                                lifecycleScope.launch {
                                    errorMessage = ""
                                    isLoading = true
                                    try {
                                        val loc = getLastKnownLocation()
                                        val nn = nickname.ifBlank { "用户${deviceId.takeLast(4)}" }
                                        val body = CreateRoomRequest(deviceId, loc.first, loc.second, nn)
                                        val resp = api.createRoom(body)
                                        if (resp.code == 200 && resp.data != null) {
                                            currentRoomId = resp.data.roomId
                                            currentScreen = Screen.Room(resp.data.code, resp.data.roomId, isOwner = true)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        errorMessage = "创建失败：${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            onJoinRoomClick = { code ->
                                lifecycleScope.launch {
                                    errorMessage = ""
                                    isLoading = true
                                    try {
                                        val loc = getLastKnownLocation()
                                        val nn = nickname.ifBlank { "用户${deviceId.takeLast(4)}" }
                                        val body = JoinRoomRequest(code, deviceId, loc.first, loc.second, nn)
                                        val resp = api.joinRoom(body)
                                        if (resp.code == 200 && resp.data != null) {
                                            currentRoomId = resp.data.roomId
                                            currentScreen = Screen.Room(code, resp.data.roomId, isOwner = false)
                                        } else {
                                            errorMessage = "房间不存在或已过期，请检查房间码"
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        errorMessage = "加入失败：${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            onSettingsClick = { currentScreen = Screen.Settings },
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            debugMode = isDebugMode.value,
                            debugLat = debugLat.value,
                            debugLng = debugLng.value,
                            onDebugToggle = { isDebugMode.value = !isDebugMode.value },
                            onDebugLatChange = { debugLat.value = it },
                            onDebugLngChange = { debugLng.value = it }
                        )
                    }
                    Screen.Settings -> {
                        SettingsPage(
                            nickname = nickname,
                            defaultNickname = "用户${deviceId.takeLast(4)}",
                            onNicknameChange = { newNickname ->
                                nickname = newNickname
                                preferencesManager.nickname = newNickname
                            },
                            onBackClick = { currentScreen = Screen.Home },
                            onSelectDirClick = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                intent.addFlags(
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                                pickFileLauncher.launch(intent)
                            }
                        )
                    }
                    is Screen.Room -> {
                        val s = currentScreen as Screen.Room
                        var downloadedFileIds by remember { mutableStateOf(emptySet<Long>()) }
                        RoomPage(
                            roomCode = s.code,
                            isOwner = s.isOwner,
                            files = files,
                            roomMembers = roomMembers,
                            currentDeviceId = deviceId,
                            allowMemberUpload = allowMemberUpload,
                            uploadProgress = uploadProgressState?.value ?: 0f,
                            uploadingFileName = uploadFileNameState?.value ?: "",
                            downloadingFileId = downloadingFileId,
                            downloadProgress = downloadProgressState?.value ?: 0f,
                            downloadedFileIds = downloadedFileIds,
                            onBackClick = {
                                if (s.isOwner) {
                                    lifecycleScope.launch {
                                        try { api.closeRoom(s.roomId) } catch (_: Exception) {}
                                    }
                                }
                                currentScreen = Screen.Home
                            },
                            onUploadClick = {
                                val intent = Intent(Intent.ACTION_GET_CONTENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                intent.type = "*/*"
                                pickFileLauncher.launch(Intent.createChooser(intent, "选择要上传的文件"))
                            },
                            onDownloadClick = { fileInfo ->
                                lifecycleScope.launch {
                                    downloadingFileId = fileInfo.id
                                    downloadProgressState?.value = 0f
                                    try {
                                        downloadFile(fileInfo)
                                        downloadedFileIds = downloadedFileIds + fileInfo.id
                                    } catch (_: Exception) {}
                                    downloadingFileId = null
                                }
                            },
                            onToggleAllowUpload = { allow ->
                                lifecycleScope.launch {
                                    try {
                                        api.setAllowMemberUpload(s.roomId, deviceId, allow)
                                    } catch (_: Exception) {}
                                }
                            },
                            onUpdateNickname = { newNickname ->
                                lifecycleScope.launch {
                                    try {
                                        api.updateNickname(UpdateNicknameRequest(s.roomId, deviceId, newNickname))
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun ensureStoragePermissionThen(action: () -> Unit) {
        if (hasStoragePermission()) {
            action()
        } else {
            pendingDownloadAction = action
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(): Pair<Double, Double> {
        if (isDebugMode.value) {
            val lat = debugLat.value.toDoubleOrNull()
            val lng = debugLng.value.toDoubleOrNull()
            if (lat != null && lng != null) {
                Log.d("FaceSend", "DEBUG 坐标: $lat, $lng")
                return Pair(lat, lng)
            }
        }

        if (!hasLocationPermission()) {
            suspendCancellableCoroutine { cont ->
                pendingLocationAction = { cont.resume(Unit) }
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }

        val lastLoc = suspendCancellableCoroutine<Location?> { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        if (lastLoc != null) {
            Log.d("FaceSend", "使用缓存位置: ${lastLoc.latitude}, ${lastLoc.longitude}")
            return Pair(lastLoc.latitude, lastLoc.longitude)
        }

        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d("FaceSend", "实时定位成功: ${location.latitude}, ${location.longitude}")
                        cont.resume(Pair(location.latitude, location.longitude))
                    } else {
                        Log.w("FaceSend", "无法获取位置")
                        cont.resume(Pair(0.0, 0.0))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceSend", "定位失败: ${e.message}")
                    cont.resume(Pair(0.0, 0.0))
                }
        }
    }

    private fun uploadFile(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var fileName = contentResolver.query(uri, null, null, null, null)?.use {
                    val ix = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    it.moveToFirst()
                    if (ix >= 0) it.getString(ix) else null
                } ?: "unknown"

                val lastDotIndex = fileName.lastIndexOf('.')
                val hasValidExtension = if (lastDotIndex > 0) {
                    val ext = fileName.substring(lastDotIndex + 1)
                    ext.length in 1..10 && ext.all { it.isLetterOrDigit() }
                } else {
                    false
                }
                if (!hasValidExtension) {
                    val mimeType = contentResolver.getType(uri)
                    val ext = android.webkit.MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(mimeType)
                    if (!ext.isNullOrEmpty()) {
                        fileName = "$fileName.$ext"
                    }
                }

                withContext(Dispatchers.Main) {
                    uploadFileNameState?.value = fileName
                    uploadProgressState?.value = 0f
                }

                val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: -1L
                val streamBody = object : okhttp3.RequestBody() {
                    override fun contentType() = "application/octet-stream".toMediaType()
                    override fun contentLength() = fileSize
                    override fun writeTo(sink: okio.BufferedSink) {
                        contentResolver.openInputStream(uri)?.use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }
                val progressBody = ProgressRequestBody(streamBody) { progress ->
                    uploadProgressState?.value = progress
                }
                val part = MultipartBody.Part.createFormData("file", fileName, progressBody)
                val roomIdBody = currentRoomId.toString().toRequestBody("text/plain".toMediaType())
                val uploaderIdBody = deviceId.toRequestBody("text/plain".toMediaType())

                api.uploadFile(roomIdBody, uploaderIdBody, part)

                withContext(Dispatchers.Main) {
                    uploadProgressState?.value = 1f
                }
                delay(1500)
                withContext(Dispatchers.Main) {
                    uploadProgressState?.value = 0f
                    uploadFileNameState?.value = ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    uploadProgressState?.value = 0f
                    uploadFileNameState?.value = ""
                }
            }
        }
    }

    private suspend fun downloadFile(fileInfo: FileInfo) {
        withContext(Dispatchers.IO) {
            val resp = api.downloadFile(fileInfo.id)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                val totalBytes = body.contentLength()

                val outputStream = FileUtils.getOutputStream(this@MainActivity, fileInfo.fileName)
                    ?: throw Exception("无法创建输出文件")

                body.byteStream().use { input ->
                    outputStream.use { os ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        var written = 0L
                        while (input.read(buffer).also { len = it } != -1) {
                            os.write(buffer, 0, len)
                            written += len
                            if (totalBytes > 0) {
                                downloadProgressState?.value = written.toFloat() / totalBytes.toFloat()
                            }
                        }
                        os.flush()
                    }
                }

                downloadProgressState?.value = 1f
            } else {
                throw Exception("下载失败，响应码: ${resp.code()}")
            }
        }
    }
}
