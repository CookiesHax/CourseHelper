package com.cookieshax.coursehelper.feature.checkin.ui.components.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class CodeInputResult {
    Idle, Success, Failure
}

@Composable
fun CheckInCodeInput(
    codeLength: Int,
    modifier: Modifier = Modifier,
    value: String = "",
    result: CodeInputResult = CodeInputResult.Idle,
    onValueChange: (String) -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.length <= codeLength && it.all { char -> char.isDigit() }) {
                onValueChange(it)
            }
        },
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword
        ),
        modifier = modifier,
        decorationBox = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDefaults.cardColors().containerColor),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(codeLength) { index ->
                    val char = value.getOrNull(index)?.toString() ?: ""
                    val isFocused = value.length == index

                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 56.dp)
                            .padding(horizontal = 4.dp)
                            // 使用 drawBehind 绘制底部下划线
                            .drawBehind {
                                val strokeWidth = if (isFocused) 2.dp.toPx() else 1.dp.toPx()
                                val color = when (result) {
                                    CodeInputResult.Success -> Color(0xFF4CAF50) // Material Green
                                    CodeInputResult.Failure -> Color(0xFFF44336) // Material Red
                                    CodeInputResult.Idle -> if (isFocused) Color(0xFF2196F3) else Color.Gray
                                }
                                val yOffset = size.height - 4.dp.toPx()

                                drawLine(
                                    color = color,
                                    start = Offset(0f, yOffset),
                                    end = Offset(size.width, yOffset),
                                    strokeWidth = strokeWidth
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }
    )
}
