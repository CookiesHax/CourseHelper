package com.cookieshax.coursehelper.feature.checkin.ui.components.gesture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

enum class GestureResult {
    Idle, Success, Failure
}

@Composable
fun GestureCanvas(
    modifier: Modifier = Modifier,
    selectedPoints: List<Int>,
    gestureResult: GestureResult = GestureResult.Idle,
    onPointsChange: (List<Int>) -> Unit,
    onGestureComplete: (String) -> Unit
) {
    var currentFingerPosition by remember { mutableStateOf<Offset?>(null) }
    val dotOffsets = remember { mutableStateOf(Array(9) { Offset.Zero }) } // 存储 9 个点的中心坐标

    val currentSelectedPoints by rememberUpdatedState(selectedPoints)

    val colorScheme = MaterialTheme.colorScheme

    val gestureColor = when (gestureResult) {
        GestureResult.Success -> Color(0xFF4CAF50) // Material Green
        GestureResult.Failure -> Color(0xFFF44336) // Material Red
        GestureResult.Idle -> colorScheme.primary
    }

    val density = LocalDensity.current
    val radiusPx = remember(density) { with(density) { 30.dp.toPx() } }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDefaults.cardColors().containerColor)
            .onSizeChanged { size ->
                // 在尺寸确定时立即计算坐标
                val canvasWidth = size.width.toFloat()
                val canvasHeight = size.height.toFloat()
                val horizontalPadding = canvasWidth * 0.22f
                val usableWidth = canvasWidth - (horizontalPadding * 2)
                val verticalPadding = (canvasHeight - usableWidth) / 2
                val step = usableWidth / 2

                val newOffsets = Array(9) { i ->
                    Offset(
                        x = horizontalPadding + (i % 3) * step,
                        y = verticalPadding + (i / 3) * step
                    )
                }
                dotOffsets.value = newOffsets
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onPointsChange(emptyList())
                        // 检查起始点是否在某个圆圈内
                        val firstPoint = findClosestPoint(offset, dotOffsets.value, radiusPx)
                        onPointsChange(if (firstPoint != null) listOf(firstPoint) else emptyList())
                    },
                    onDrag = { change, _ ->
                        currentFingerPosition = change.position
                        findClosestPoint(
                            change.position,
                            dotOffsets.value,
                            radiusPx
                        )?.let { index ->
                            if (index !in currentSelectedPoints) {
                                val newList = currentSelectedPoints.toMutableList()
                                if (newList.isNotEmpty()) {
                                    val lastIndex = newList.last()
                                    val lastRow = lastIndex / 3
                                    val lastCol = lastIndex % 3
                                    val currentRow = index / 3
                                    val currentCol = index % 3

                                    if ((lastRow + currentRow) % 2 == 0 && (lastCol + currentCol) % 2 == 0) {
                                        val middleIndex =
                                            ((lastRow + currentRow) / 2) * 3 + (lastCol + currentCol) / 2
                                        if (middleIndex !in newList) {
                                            newList.add(middleIndex)
                                        }
                                    }
                                }
                                newList.add(index)
                                onPointsChange(newList) // 通知外部状态更新
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentSelectedPoints.isNotEmpty()) {
                            val gestureResultStr =
                                currentSelectedPoints.joinToString("") { (it + 1).toString() }
                            onGestureComplete(gestureResultStr)
                        }

                        currentFingerPosition = null
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 计算左右留空后的起始点和有效宽度
        val horizontalPadding = canvasWidth * 0.22f
        val usableWidth = canvasWidth - (horizontalPadding * 2)

        val verticalPadding = (canvasHeight - usableWidth) / 2

        // 计算点与点之间的间距 (3个点之间有2个间隔)
        val stepX = usableWidth / 2
        val stepY = usableWidth / 2 // 使用 usableWidth 保持正方形比例

        // 更新坐标计算
        for (i in 0..8) {
            val x = horizontalPadding + (i % 3) * stepX
            val y = verticalPadding + (i / 3) * stepY
            dotOffsets.value[i] = Offset(x, y)
        }

        // 绘制背景圆点
        dotOffsets.value.forEachIndexed { index, offset ->
            if (offset == Offset.Zero) return@forEachIndexed

            val isSelected = index in selectedPoints
            drawCircle(
                color = if (isSelected) gestureColor else Color.LightGray.copy(alpha = 0.5f),
                radius = radiusPx,
                center = offset,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 绘制已连接的线条
        if (selectedPoints.size > 1) {
            for (i in 0 until selectedPoints.size - 1) {
                drawLine(
                    color = gestureColor,
                    start = dotOffsets.value[selectedPoints[i]],
                    end = dotOffsets.value[selectedPoints[i + 1]],
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 绘制从最后一个选中的点到手指当前位置的线条
        currentFingerPosition?.let { finger ->
            if (selectedPoints.isNotEmpty()) {
                drawLine(
                    color = gestureColor,
                    start = dotOffsets.value[selectedPoints.last()],
                    end = finger,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// 判断触摸位置是否接近某个点
private fun findClosestPoint(offset: Offset, points: Array<Offset>, threshold: Float): Int? {
    points.forEachIndexed { index, point ->
        // 计算手指与点中心的距离
        val distance = sqrt((offset.x - point.x).pow(2) + (offset.y - point.y).pow(2))
        // 只有距离小于传入的 threshold (即 radius) 时才触发
        if (distance < threshold) return index
    }
    return null
}
