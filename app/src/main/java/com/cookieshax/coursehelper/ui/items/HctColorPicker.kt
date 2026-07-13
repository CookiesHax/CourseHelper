package com.cookieshax.coursehelper.ui.items

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.hct.Hct

data class HctColorState(
    val hue: Double,
    val chroma: Double,
    val tone: Double
) {
    val hex: String
        get() {
            val argb = Hct.from(hue, chroma, tone).toInt()
            return String.format("#%06X", 0xFFFFFF and argb).uppercase()
        }

    val color: Color
        get() = Color(Hct.from(hue, chroma, tone).toInt())

    companion object {
        fun fromHex(
            hex: String,
            fallback: HctColorState = HctColorState(263.3, 42.7, 64.1)
        ): HctColorState {
            return runCatching {
                val cleanHex = hex.removePrefix("#")
                val argb = ("FF$cleanHex").toLong(16).toInt()
                val hct = Hct.fromInt(argb)
                HctColorState(hct.hue, hct.chroma, hct.tone)
            }.getOrDefault(fallback)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HctColorPickerDialog(
    state: HctColorState,
    onStateChange: (HctColorState) -> Unit,
    onDismissRequest: () -> Unit,
    onApply: () -> Unit
) {
    // 处理 Hex 输入框在输入过程中的中间文本
    var hexInput by remember(state) { mutableStateOf(state.hex) }

    // 根据当前 Tone 值动态计算滑块圆点的颜色
    val thumbColor = if (state.tone >= 78.0) Color.Black else Color.White
    val currentColor = state.color

    // 更新 HCT 状态
    val updateState = { h: Double, c: Double, t: Double ->
        onStateChange(HctColorState(h, c, t))
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = "Palette Icon",
                tint = currentColor
            )
        },
        title = {
            Text(
                text = "HCT Color Picker",
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ColorPropertyInput(
                    label = "Hex Color",
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val cleanHex = input.removePrefix("#")
                        if (cleanHex.length == 6) {
                            runCatching {
                                val argb = ("FF$cleanHex").toLong(16).toInt()
                                val hct = Hct.fromInt(argb)
                                updateState(hct.hue, hct.chroma, hct.tone)
                            }
                        }
                    }
                )

                ColorPropertyInput(
                    label = "Hue",
                    value = state.hue.toString(),
                    onValueChange = {
                        val v = it.toDoubleOrNull() ?: 0.0
                        updateState(v, state.chroma, state.tone)
                    }
                )
                GradientSlider(
                    value = state.hue,
                    onValueChange = { updateState(it, state.chroma, state.tone) },
                    valueRange = 0.0..360.0,
                    thumbColor = Color.White,
                    colors = (0..360 step 90).map {
                        Color(Hct.from(it.toDouble(), state.chroma, 50.0).toInt())
                    }
                )

                ColorPropertyInput(
                    label = "Chroma",
                    value = state.chroma.toString(),
                    onValueChange = {
                        val v = it.toDoubleOrNull() ?: 0.0
                        updateState(state.hue, v, state.tone)
                    }
                )
                GradientSlider(
                    value = state.chroma,
                    onValueChange = { updateState(state.hue, it, state.tone) },
                    valueRange = 0.0..150.0,
                    thumbColor = Color.White,
                    colors = (0..150 step 75).map {
                        Color(Hct.from(state.hue, it.toDouble(), 50.0).toInt())
                    }
                )

                ColorPropertyInput(
                    label = "Tone",
                    value = state.tone.toString(),
                    onValueChange = {
                        val v = it.toDoubleOrNull() ?: 0.0
                        updateState(state.hue, state.chroma, v)
                    }
                )
                GradientSlider(
                    value = state.tone,
                    onValueChange = { updateState(state.hue, state.chroma, it) },
                    valueRange = 0.0..100.0,
                    thumbColor = thumbColor,
                    colors = (0..100 step 50).map {
                        Color(Hct.from(state.hue, state.chroma, it.toDouble()).toInt())
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

// 输入框
@Composable
fun ColorPropertyInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(160.dp)
                .height(56.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start)
        )
    }
}

// 滑动条
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientSlider(
    value: Double,
    onValueChange: (Double) -> Unit,
    valueRange: ClosedFloatingPointRange<Double>,
    thumbColor: Color,
    colors: List<Color>
) {
    var isPressed by remember { mutableStateOf(false) }

    // 为波纹效果添加平滑的缩放动画
    val haloScale by animateFloatAsState(
        targetValue = if (isPressed) 2.2f else 0f,
        label = "haloScale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isPressed = event.changes.any { it.pressed }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val maxWidth = constraints.maxWidth
        val density = LocalDensity.current
        val thumbSize = 18.dp
        val thumbSizePx = with(density) { thumbSize.toPx() }

        val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)

        // 背景渐变层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Brush.horizontalGradient(colors))
        )

        // 交互层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toDouble()) },
                valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
                thumb = {},
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 视觉圆点层
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (progress * maxWidth - thumbSizePx / 2).toInt(),
                        y = 0
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 波纹效果
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .graphicsLayer {
                        scaleX = haloScale
                        scaleY = haloScale
                    }
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            // 实际圆点
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(
                        color = thumbColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
