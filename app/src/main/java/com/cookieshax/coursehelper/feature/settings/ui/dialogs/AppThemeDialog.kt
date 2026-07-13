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
fun AppThemeDialog(
    currentTheme: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("应用主题") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("system", "light", "dark").forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clickable {
                                onConfirm(theme)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null
                        )
                        Text(
                            text = when (theme) {
                                "system" -> "跟随系统"
                                "light" -> "浅色"
                                "dark" -> "深色"
                                else -> theme
                            },
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
