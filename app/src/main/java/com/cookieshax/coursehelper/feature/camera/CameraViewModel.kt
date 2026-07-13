package com.cookieshax.coursehelper.feature.camera

import androidx.camera.core.Camera
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {
    // 相机权限状态
    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    // 相机镜头
    private val _isBackCamera = MutableStateFlow(true)
    val isBackCamera: StateFlow<Boolean> = _isBackCamera.asStateFlow()

    // 相机引用
    var camera: Camera? = null

    // 缩放状态
    private val _minZoomRatio = MutableStateFlow(1f)
    val minZoomRatio: StateFlow<Float> = _minZoomRatio.asStateFlow()

    private val _maxZoomRatio = MutableStateFlow(1f)
    val maxZoomRatio: StateFlow<Float> = _maxZoomRatio.asStateFlow()

    private val _currentZoomRatio = MutableStateFlow(1f)
    val currentZoomRatio: StateFlow<Float> = _currentZoomRatio.asStateFlow()

    // 扫码结果
    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()

    // 缩放回调
    var onZoomRequested: ((Float) -> Unit)? = null

    // 防抖相关
    private var lastScanResult = ""
    private var lastScanTime = 0L
    private val debounceDelay = 1000L

    fun setCameraPermission(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun updateZoomRange(min: Float, max: Float) {
        _minZoomRatio.value = min
        _maxZoomRatio.value = max
    }

    fun updateCurrentZoom(ratio: Float) {
        _currentZoomRatio.value = ratio
    }

    fun requestZoom(ratio: Float) {
        val clampedRatio = ratio.coerceIn(_minZoomRatio.value, _maxZoomRatio.value)
        onZoomRequested?.invoke(clampedRatio)
    }

    fun handleScanResult(result: String): Boolean {
        val currentTime = System.currentTimeMillis()

        // 防抖检查 结果不同或时间间隔足够
        if (result != lastScanResult || currentTime - lastScanTime >= debounceDelay) {
            lastScanResult = result
            lastScanTime = currentTime
            _scanResult.value = result
            return true
        }
        return false
    }

    fun clearScanResult() {
        _scanResult.value = null
    }
}
