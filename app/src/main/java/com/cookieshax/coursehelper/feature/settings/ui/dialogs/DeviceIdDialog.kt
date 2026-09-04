package com.cookieshax.coursehelper.feature.settings.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.utils.EncryptionUtils

@Composable
fun DeviceIdDialog(
    currentDeviceId: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentDeviceId) }
    // UUID v4: 8-4-4-4-12 -> 32位 plain 格式中 第 13 位（索引 12）固定为 '4'
    val isValid = text.matches(Regex("^[0-9a-fA-F]{12}4[0-9a-fA-F]{19}$"))

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("设备标识 ID") },
        text = {
            Column {
                Text("修改设备 ID 将会导致 User-Agent 发生变化。")
                Text(
                    text = "注意：UUID v4 第 13 位固定为 '4'。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("设备 ID") },
                        placeholder = { Text("32 位十六进制 UUID") },
                        isError = !isValid,
                        supportingText = {
                            if (!isValid) {
                                Text("请输入符合规范的 32 位 ID（第 13 位应为 4）")
                            }
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { text = EncryptionUtils.getPlainUuid() },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "重新生成"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(text) },
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
