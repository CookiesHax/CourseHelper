package com.cookieshax.coursehelper.feature.settings.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckInAccountSelectionModeDialog(
    currentMode: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val modes = listOf(
        0 to "全不选",
        1 to "选择加入此课程的账号 (智能选择)",
        2 to "全选"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("签到界面账号选择模式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                modes.forEach { (mode, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                onConfirm(mode)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = null
                        )
                        Text(
                            text = description,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Text(
                    text = "注：智能选择对直接扫码进入的签到无效",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "提示：使用智能选择时，建议开启“启动时缓存所有账号的课程”以获得最佳体验",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
