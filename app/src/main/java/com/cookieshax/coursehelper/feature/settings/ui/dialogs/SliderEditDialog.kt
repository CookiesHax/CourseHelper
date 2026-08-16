package com.cookieshax.coursehelper.feature.settings.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SliderEditDialog(
    title: String,
    value: Int,
    range: IntRange,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(value.toString()) }
    val intValue = textValue.toIntOrNull()
    val isValid = intValue != null && intValue in range

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "编辑 $title") },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        textValue = newValue
                    }
                },
                label = { Text("数值 (${range.first} - ${range.last})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(intValue)
                    }
                },
                enabled = isValid
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
