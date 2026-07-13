package com.cookieshax.coursehelper.feature.account.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.Tag
import com.cookieshax.coursehelper.ui.items.HctColorState
import com.cookieshax.coursehelper.ui.items.IcTag
import kotlin.random.Random

@Composable
fun TagEditDialog(
    tag: Tag?,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, List<String>) -> Unit,
    onPickColor: (HctColorState, (HctColorState) -> Unit) -> Unit,
    onSelectAccounts: () -> Unit,
    initialSelectedAccounts: List<String> = emptyList(),
    existingColors: List<Int> = emptyList(),
) {
    var tagName by remember(tag) { mutableStateOf(tag?.name ?: "") }
    var selectedColor by remember(tag) {
        mutableStateOf(
            if (tag != null) {
                HctColorState.fromHex(
                    String.format("#%06X", 0xFFFFFF and tag.color)
                )
            } else {
                val defaultHct = HctColorState.fromHex("#769CDF")
                if (existingColors.contains(defaultHct.color.toArgb())) {
                    defaultHct.copy(hue = Random.nextDouble(0.0, 360.0))
                } else {
                    defaultHct
                }
            }
        )
    }
    val selectedAccounts = remember(initialSelectedAccounts) {
        mutableStateListOf<String>().apply {
            addAll(initialSelectedAccounts)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tag != null) "编辑标签" else "新建标签") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = IcTag,
                            contentDescription = null,
                            tint = selectedColor.color,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = selectedColor.hex,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    TextButton(
                        onClick = {
                            onPickColor(selectedColor) { newColor ->
                                selectedColor = newColor
                            }
                        }
                    ) {
                        Text("选择颜色")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "此标签应用于${selectedAccounts.size}个账号",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    TextButton(
                        onClick = onSelectAccounts
                    ) {
                        Text("选择账号")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        tagName.trim(),
                        selectedColor.color.toArgb(),
                        selectedAccounts.toList()
                    )
                },
                enabled = tagName.isNotBlank()
            ) {
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
