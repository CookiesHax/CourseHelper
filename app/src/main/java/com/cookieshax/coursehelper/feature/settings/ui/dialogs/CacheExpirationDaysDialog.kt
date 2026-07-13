package com.cookieshax.coursehelper.feature.settings.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
fun CacheExpirationDaysDialog(
    currentDays: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("缓存过期时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(0, 1, 3, 7, 14, 30).forEach { days ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable {
                                onConfirm(days)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = (days == currentDays),
                            onClick = null
                        )
                        Text(
                            text = if (days > 0) "$days 天" else "立即",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
