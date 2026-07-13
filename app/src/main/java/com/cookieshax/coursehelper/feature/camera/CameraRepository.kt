package com.cookieshax.coursehelper.feature.camera

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import zxingcpp.BarcodeReader
import java.io.InputStream

private const val TAG = "CameraRepository"

class CameraRepository(
    private val onZoomSuggestion: ((Float) -> Boolean)?
) {
    // ML Kit 扫码客户端
    // 只进行缩放检测 不进行扫码
    // 低对比度复杂二维码比 XZing 识别率低
    private val mlKitScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .setZoomSuggestionOptions(
                ZoomSuggestionOptions.Builder { zoomRatio ->
                    Log.d(TAG, "ML Kit 建议缩放比例: $zoomRatio")
                    onZoomSuggestion?.invoke(zoomRatio) != false
                }.build()
            )
            .build()
    )

    // ZXing 扫码客户端
    private var barcodeReader = BarcodeReader(
        BarcodeReader.Options().apply {
            formats = setOf(BarcodeReader.Format.QR_CODE)
        }
    )

    @OptIn(ExperimentalGetImage::class)
    fun processCameraFrame(
        imageProxy: ImageProxy,
        onSuccess: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        try {
            val results = barcodeReader.read(imageProxy)
            if (results.isNotEmpty()) {
                val combinedResult = results.mapNotNull { it.text }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")

                if (combinedResult.isNotEmpty()) {
                    onSuccess(combinedResult)
                    imageProxy.close()
                    onComplete()
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ZXing 解码异常", e)
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // 调用 ML Kit 进行缩放检测
        mlKitScanner.process(inputImage).addOnCompleteListener {
            imageProxy.close()
            onComplete()
        }
    }

    fun processImageUri(
        context: Context,
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit = { e -> Log.e(TAG, "相册扫码失败", e) }
    ) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                onError(Exception("无法解析图片"))
                return
            }

            val results = barcodeReader.read(bitmap)
            if (results.isNotEmpty()) {
                val combinedText = results.mapNotNull { it.text }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")

                if (combinedText.isNotEmpty()) {
                    onSuccess(combinedText)
                } else {
                    onError(Exception("二维码内容为空"))
                }
            } else {
                onError(Exception("未发现二维码"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "图片读取失败", e)
            onError(e)
        }
    }

    fun close() {
        mlKitScanner.close()
    }
}
