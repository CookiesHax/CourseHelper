package com.cookieshax.coursehelper.feature.account.ui.items

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.cookieshax.coursehelper.core.imageloader.CoilConfig
import com.cookieshax.coursehelper.core.utils.maskedPhone
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.AccountStatus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainAccountItem(
    account: Account,
    isSelected: Boolean,
    isActiveAccount: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isDragging: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { if (!isDragging) onClick() }),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectionMode) {
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else CardDefaults.cardColors().containerColor
            } else {
                if (isActiveAccount) MaterialTheme.colorScheme.primaryContainer
                else CardDefaults.cardColors().containerColor
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
                model = account.avatarUrl,
                imageLoader = CoilConfig.getImageLoader(context),
                onError = { result ->
                    Log.e(
                        "CoilError",
                        "头像加载失败: ${account.avatarUrl}, 错误信息: ${result.result.throwable.message}",
                        result.result.throwable
                    )
                }
            )
            // 使用自定义painter
            Image(
                painter = painter,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${account.uid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "手机号: ${account.phone.maskedPhone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isActiveAccount && account.status == AccountStatus.VALID) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "活动账户",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (account.status == AccountStatus.EXPIRED) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "失效账户",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
