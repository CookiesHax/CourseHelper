package com.cookieshax.coursehelper.feature.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun QrScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxSize = kotlin.math.min(width, height) * 0.7f // 扫描框占宽高的较小值的 70%
        val left = (width - boxSize) / 2
        val top = (height - boxSize) / 2
        val rect = Rect(Offset(left, top), Size(boxSize, boxSize))

        // 绘制半透明黑色背景
        drawRect(color = Color.Black.copy(alpha = 0.5f))

        // 镂空中间的扫描框
        drawRoundRect(
            color = Color.Transparent,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(8.dp.toPx()),
            blendMode = BlendMode.Clear
        )
    }
}