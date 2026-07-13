package com.cookieshax.coursehelper.feature.settings.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp

@Composable
fun UserAgentDialog(
    currentUA: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentUA) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("修改 User-Agent") },
        text = {
            Column {
                Text("留空则使用系统默认生成的 UA")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("User-Agent") },
                    placeholder = { Text("输入自定义 UA...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
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
