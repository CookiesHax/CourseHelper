package com.cookieshax.coursehelper.feature.checkin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.checkin.ui.items.ResultItem

@Composable
fun CheckInResultDialog(
    results: Map<String, ApiResult<String>>,
    accounts: List<Account>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("签到结果") },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { account ->
                        val result = results[account.uid]
                        if (result != null) {
                            ResultItem(account.name, result)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
