package com.cookieshax.coursehelper.feature.account.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.Tag
import com.cookieshax.coursehelper.ui.items.IcTag

@Composable
fun TagSelectDialog(
    tags: List<Tag>,
    initialSelected: List<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialSelected.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "为该账号选择标签",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.size(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(tags) { tag ->
                        val isSelected = selectedIds.contains(tag.tagId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .clickable {
                                    selectedIds = if (isSelected) {
                                        selectedIds - tag.tagId
                                    } else {
                                        selectedIds + tag.tagId
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        disabledCheckedColor = MaterialTheme.colorScheme.primary,
                                        disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.38f
                                        )
                                    )
                                )
                            }

                            Icon(
                                imageVector = IcTag,
                                contentDescription = null,
                                tint = Color(tag.color),
                                modifier = Modifier.size(24.dp)
                            )

                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds.toList()) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
