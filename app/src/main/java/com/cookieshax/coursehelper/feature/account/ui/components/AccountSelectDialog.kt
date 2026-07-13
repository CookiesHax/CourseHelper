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
import com.cookieshax.coursehelper.core.utils.maskedPhone
import com.cookieshax.coursehelper.feature.account.model.Account

@Composable
fun AccountSelectDialog(
    accounts: List<Account>,
    initialSelected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialSelected.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择账号") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "选择此标签需要关联的账号",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.size(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp), // 限制最大高度 防止账号过多撑满屏幕
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(accounts) { account ->
                        val isSelected = selectedIds.contains(account.uid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .clickable {
                                    selectedIds = if (isSelected) {
                                        selectedIds - account.uid
                                    } else {
                                        selectedIds + account.uid
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = account.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "(${account.phone.maskedPhone})",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
