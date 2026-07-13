package com.cookieshax.coursehelper.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context.applicationContext,
            "${context.packageName}.fileprovider",
            file
        )
    }

    // 从 Uri 中获取文件名
    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    // 查询 DISPLAY_NAME 字段
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }

        // 如果 content 方式失败或协议是 file 回退到路径截取
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        // 获取原始文件名
        val fileName = getFileName(context, uri) ?: "temp_file_${System.currentTimeMillis()}"

        // 创建临时文件
        val tempFile = File(context.cacheDir, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var sizeDouble = size.toDouble()
        var unitIndex = 0
        while (sizeDouble >= 1024 && unitIndex < units.size - 1) {
            sizeDouble /= 1024
            unitIndex++
        }
        return "%.2f %s".format(sizeDouble, units[unitIndex])
    }

    fun cleanupCache(context: Context, sec: Long = 0L) {
        val cacheFolder = context.cacheDir
        // 使用 walkBottomUp 自底向上遍历 确保先删除子文件再删除空文件夹
        cacheFolder.walkBottomUp().forEach { file ->
            // 不删除缓存根目录本身
            if (file == cacheFolder) return@forEach

            if (System.currentTimeMillis() - file.lastModified() > sec) {
                file.delete()
            }
        }
    }

    fun getCacheSize(context: Context): Long {
        return context.cacheDir.walkTopDown().sumOf { it.length() }
    }
}
