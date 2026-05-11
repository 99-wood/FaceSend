package com.example.mainactivity.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.OutputStream

object FileUtils {
    private const val TAG = "FaceSend"
    private const val SUB_DIR = "FaceSend"

    fun getOutputStream(context: Context, filename: String): OutputStream? {
        // Android 10+ 强制要求 Scoped Storage，需通过 MediaStore 写入 Downloads 目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getOutputStreamMediaStore(context, filename)
        }
        return getOutputStreamLegacy(context, filename)
    }

    private fun getOutputStreamMediaStore(context: Context, filename: String): OutputStream? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$SUB_DIR")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            Log.d(TAG, "MediaStore 写入: $uri")
            return context.contentResolver.openOutputStream(uri)
        }
        Log.e(TAG, "MediaStore insert 失败，降级到应用私有目录")
        return getOutputStreamAppPrivate(context, filename)
    }

    private fun getOutputStreamLegacy(context: Context, filename: String): OutputStream? {
        // 方案1: 公共 Downloads/FaceSend
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            Log.d(TAG, "公共 Downloads 路径: ${publicDownloads?.absolutePath}, 存在: ${publicDownloads?.exists()}")
            if (publicDownloads != null && publicDownloads.exists()) {
                val faceSendDir = File(publicDownloads, SUB_DIR)
                if (faceSendDir.exists() || faceSendDir.mkdirs()) {
                    val file = File(faceSendDir, filename)
                    Log.d(TAG, "使用公共目录: ${file.absolutePath}")
                    return file.outputStream()
                }
                Log.w(TAG, "公共目录 mkdirs 失败: ${faceSendDir.absolutePath}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "公共目录异常: ${e.message}")
        }

        // 方案2: 外部存储根目录下的 Download
        try {
            val extStorage = Environment.getExternalStorageDirectory()
            Log.d(TAG, "外部存储根: ${extStorage?.absolutePath}, 存在: ${extStorage?.exists()}")
            if (extStorage != null && extStorage.exists()) {
                val downloadDir = File(extStorage, "Download/$SUB_DIR")
                if (downloadDir.exists() || downloadDir.mkdirs()) {
                    val file = File(downloadDir, filename)
                    Log.d(TAG, "使用外部存储 Download: ${file.absolutePath}")
                    return file.outputStream()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "外部存储根异常: ${e.message}")
        }

        // 方案3: 应用私有外部目录（不需要权限）
        return getOutputStreamAppPrivate(context, filename)
    }

    private fun getOutputStreamAppPrivate(context: Context, filename: String): OutputStream? {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (externalDir != null) {
            val faceSendDir = File(externalDir, SUB_DIR)
            if (faceSendDir.exists() || faceSendDir.mkdirs()) {
                val file = File(faceSendDir, filename)
                Log.d(TAG, "使用应用私有外部目录: ${file.absolutePath}")
                return file.outputStream()
            }
        }

        // 方案4: 应用内部存储（最后兜底，一定能写）
        val internalDir = File(context.filesDir, SUB_DIR)
        if (!internalDir.exists()) internalDir.mkdirs()
        val file = File(internalDir, filename)
        Log.d(TAG, "使用内部存储兜底: ${file.absolutePath}")
        return file.outputStream()
    }
}
