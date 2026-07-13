package com.cookieshax.coursehelper.feature.account.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.ui.items.IcTag

@Composable
fun TagItem(
    tagName: String,
    color: Color,
    accounts: List<Account>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isDragging: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isDragging,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                CardDefaults.cardColors().containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 6.dp, bottom = 12.dp, top = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = IcTag,
                contentDescription = "标签",
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 标签名称
            Text(
                text = tagName,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${accounts.size} 个账号",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(1.dp))

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑标签"
                )
            }
        }
    }
}
