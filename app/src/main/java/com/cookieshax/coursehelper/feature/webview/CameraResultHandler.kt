package com.cookieshax.coursehelper.feature.webview

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object CameraResultHandler {
    suspend fun handleImageFile(
        imageFile: File,
        jsBridgeInterface: JsBridgeInterface
    ): Boolean {
        // 上传图片
        return try {
            val objectId = withContext(Dispatchers.IO) {
                ApiManager.uploadFile(imageFile)
            }

            objectId?.let { id ->
                val filesArray = JSONArray().apply {
                    val fileObj = JSONObject().apply {
                        put("name", imageFile.name)
                        put("objectid", id)
                        put("type", imageFile.extension)
                    }
                    put(fileObj)
                }

                val rootJson = JSONObject().apply {
                    put("files", filesArray.toString())
                }

                Log.d("CameraResultHandler", rootJson.toString())

                jsBridgeInterface.sendMessageToWebView(
                    "CLIENT_CHOOSE_IMAGE_RESULT",
                    rootJson.toString()
                )
            }
            true
        } catch (e: Exception) {
            Log.e("CameraResultHandler", "图片上传失败", e)
            false
        } finally {
            if (imageFile.exists()) imageFile.delete()
        }
    }

    suspend fun handleImageResult(
        context: Context,
        imageUriString: String,
        jsBridgeInterface: JsBridgeInterface
    ): Boolean {
        val imageUri = imageUriString.toUri()

        // 将 URI 转换为 File
        val imageFile = FileUtils.uriToFile(context, imageUri) ?: return false
        return handleImageFile(imageFile, jsBridgeInterface)
    }
}
