package com.cookieshax.coursehelper.feature.checkin.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cookieshax.coursehelper.feature.checkin.model.Captcha
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CaptchaDialog(
    captcha: Captcha,
    onVerify: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "请完成安全验证"
) {
    val bgBitmap = remember(captcha.bgData) {
        captcha.bgData?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val sliceBitmap = remember(captcha.sliceData) {
        captcha.sliceData?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    if (bgBitmap == null || sliceBitmap == null) {
        onDismiss()
        return
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var displayedBgWidth by remember { mutableFloatStateOf(0f) }
    val dragOffsetX = remember(captcha) { Animatable(0f) }

    val bgAspectRatio = bgBitmap.width.toFloat() / bgBitmap.height.toFloat()
    val scale = if (displayedBgWidth > 0) displayedBgWidth / bgBitmap.width.toFloat() else 1f

    val maxDisplayOffset = remember(displayedBgWidth, scale, sliceBitmap.width) {
        if (displayedBgWidth > 0f) {
            (displayedBgWidth - (sliceBitmap.width * scale)).coerceAtLeast(0f)
        } else {
            0f
        }
    }

    val sliceDisplayWidthDp = remember(scale, sliceBitmap.width) {
        with(density) { (sliceBitmap.width * scale).toDp() }
    }
    val sliceDisplayHeightDp = remember(scale, sliceBitmap.height) {
        with(density) { (sliceBitmap.height * scale).toDp() }
    }

    val sliderSizePx = with(density) { 40.dp.toPx() }
    val maxSliderDragOffset = (displayedBgWidth - sliderSizePx).coerceAtLeast(0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .fillMaxWidth() // 让弹窗本体直接填满屏幕宽度
            .padding(16.dp),
        confirmButton = {},
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bgAspectRatio)
                        .onGloballyPositioned { displayedBgWidth = it.size.width.toFloat() }
                ) {
                    Image(
                        bitmap = bgBitmap,
                        contentDescription = "Captcha Background",
                        contentScale = ContentScale.FillWidth, // 强制让图片的像素拉伸去对齐组件的宽度
                        modifier = Modifier.fillMaxWidth()
                    )

                    Image(
                        bitmap = sliceBitmap,
                        contentDescription = "Captcha Slice",
                        contentScale = ContentScale.FillWidth, // 同样采用横向拉伸模式
                        modifier = Modifier
                            .size(width = sliceDisplayWidthDp, height = sliceDisplayHeightDp)
                            .offset {
                                val clampedOffset = dragOffsetX.value.coerceIn(0f, maxDisplayOffset)
                                IntOffset(clampedOffset.roundToInt(), 0)
                            }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .offset {
                                val clampedSliderOffset =
                                    dragOffsetX.value.coerceIn(0f, maxSliderDragOffset)
                                IntOffset(clampedSliderOffset.roundToInt(), 0)
                            }
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp)
                            .pointerInput(captcha) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (dragOffsetX.value < 5f) {
                                            scope.launch {
                                                dragOffsetX.animateTo(0f, spring())
                                            }
                                            return@detectDragGestures
                                        }
                                        val originalImageOffset =
                                            (dragOffsetX.value / scale).roundToInt()
                                        onVerify(originalImageOffset)
                                        scope.launch {
                                            dragOffsetX.animateTo(0f, spring())
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            dragOffsetX.snapTo(
                                                (dragOffsetX.value + dragAmount.x).coerceIn(
                                                    0f,
                                                    maxDisplayOffset
                                                )
                                            )
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val lineModifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight(0.6f)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)

                            Box(modifier = lineModifier)
                            Box(modifier = lineModifier)
                            Box(modifier = lineModifier)
                        }
                    }
                }
            }
        }
    )
}
