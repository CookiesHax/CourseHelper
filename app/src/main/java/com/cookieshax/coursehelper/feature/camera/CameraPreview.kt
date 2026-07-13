package com.cookieshax.coursehelper.feature.camera

import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    viewModel: CameraViewModel,
    repository: CameraRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    // 从ViewModel获取状态
    val currentZoomRatio by viewModel.currentZoomRatio.collectAsStateWithLifecycle()
    val minZoomRatio by viewModel.minZoomRatio.collectAsStateWithLifecycle()
    val maxZoomRatio by viewModel.maxZoomRatio.collectAsStateWithLifecycle()
    val hasCameraPermission by viewModel.hasCameraPermission.collectAsStateWithLifecycle()
    val isBackCamera by viewModel.isBackCamera.collectAsStateWithLifecycle()

    // 管理资源生命周期
    val executor = remember { Executors.newSingleThreadExecutor() }
    var camera: Camera? by remember { mutableStateOf(null) }

    LaunchedEffect(hasCameraPermission, isBackCamera, previewView) {
        val currentPreviewView = previewView
        if (!hasCameraPermission || currentPreviewView == null) return@LaunchedEffect

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = currentPreviewView.surfaceProvider
        }

        val cameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            if (imageProxy.image != null) {
                repository.processCameraFrame(
                    imageProxy = imageProxy,
                    onSuccess = { text -> viewModel.handleScanResult(text) },
                    onComplete = { imageProxy.close() }
                )
            } else {
                imageProxy.close()
            }
        }

        val cam = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )

        cam.cameraControl.setZoomRatio(1f)
        camera = cam
        viewModel.camera = cam

        cam.cameraInfo.zoomState.let { zoomState ->
            viewModel.updateZoomRange(
                zoomState.value?.minZoomRatio ?: 1f,
                zoomState.value?.maxZoomRatio ?: 1f
            )
        }
    }

    LaunchedEffect(camera) {
        val currentCamera = camera ?: return@LaunchedEffect

        viewModel.onZoomRequested = { ratio ->
            currentCamera.cameraControl.setZoomRatio(ratio)
        }
    }

    // 监听缩放状态变化 同步到 ViewModel
    LaunchedEffect(camera) {
        val cam = camera ?: return@LaunchedEffect

        cam.cameraInfo.zoomState.asFlow().collect { zoomState ->
            viewModel.updateZoomRange(zoomState.minZoomRatio, zoomState.maxZoomRatio)
            viewModel.updateCurrentZoom(zoomState.zoomRatio)
        }
    }

    // 组件销毁时释放资源
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            viewModel.camera = null
            viewModel.onZoomRequested = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView = it }
            },
            update = { view ->
                if (view.scaleType != PreviewView.ScaleType.FILL_CENTER) {
                    view.scaleType = PreviewView.ScaleType.FILL_CENTER
                    view.requestLayout()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // 手势检测层
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(minZoomRatio, maxZoomRatio) {
                var accumulatedZoom = currentZoomRatio

                detectTransformGestures { _, _, zoom, _ ->
                    accumulatedZoom = (accumulatedZoom * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                    viewModel.requestZoom(accumulatedZoom)
                }
            }
    )
}
