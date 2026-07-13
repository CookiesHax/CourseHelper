package com.cookieshax.coursehelper.feature.checkin.ui.items

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.network.ApiResult

@Composable
fun ResultItem(name: String, result: ApiResult<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = resultStringMapper(result),
            color = when {
                result is ApiResult.Success
                        && (result.data.startsWith("success")
                        || result.data.startsWith("您已签到过了"))
                    -> MaterialTheme.colorScheme.primary

                else -> MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

fun resultStringMapper(result: ApiResult<String>): String {
    return when (result) {
        is ApiResult.Success -> {
            if (result.data.startsWith("success")) {
                "签到成功"
            } else if (result.data.startsWith("errorLocation")) {
                "位置错误: ${result.data}"
            } else if (result.data.startsWith("[face]")) {
                "人脸错误: ${result.data.replaceFirst("[face]", "")}"
            } else {
                result.data
            }
        }

        is ApiResult.Error -> result.message
    }
}
