package com.cookieshax.coursehelper.feature.checkin.ui.components.normal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.utils.FileUtils
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun NormalInputComponent(
    viewModel: CheckInViewModel,
    setUploadCallback: ((String) -> Unit) -> Unit,
    setCameraCallback: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    val albumPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val accountId = viewModel.activeUploadAccountId.value
        if (uri != null && accountId != null) {
            scope.launch {
                try {
                    val file = FileUtils.uriToFile(context, uri) ?: return@launch

                    val objectId = ApiManager.uploadFile(file = file, uid = accountId)
                    if (objectId != null) {
                        viewModel.setUploadedObjectId(accountId, objectId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    viewModel.setActiveUploadAccountId(null)
                }
            }
        } else {
            viewModel.setActiveUploadAccountId(null)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val accountId = viewModel.activeUploadAccountId.value
        val file = tempPhotoFile
        if (success && accountId != null && file != null) {
            scope.launch {
                try {
                    val objectId = ApiManager.uploadFile(file = file, uid = accountId)
                    if (objectId != null) {
                        viewModel.setUploadedObjectId(accountId, objectId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    viewModel.setActiveUploadAccountId(null)
                    tempPhotoFile = null
                }
            }
        } else {
            viewModel.setActiveUploadAccountId(null)
            tempPhotoFile = null
        }
    }

    DisposableEffect(Unit) {
        setUploadCallback { uid ->
            if (viewModel.activeUploadAccountId.value != null) return@setUploadCallback
            viewModel.setActiveUploadAccountId(uid)
            albumPickerLauncher.launch("image/*")
        }
        setCameraCallback { uid ->
            if (viewModel.activeUploadAccountId.value != null) return@setCameraCallback
            viewModel.setActiveUploadAccountId(uid)

            val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            tempPhotoFile = file
            val uri = FileUtils.getFileUri(context, file)
            cameraLauncher.launch(uri)
        }
        onDispose {
            setUploadCallback { _ -> }
            setCameraCallback { _ -> }
        }
    }
}
