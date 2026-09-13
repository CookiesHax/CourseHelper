package com.cookieshax.coursehelper.feature.checkin.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.utils.maskedPhone
import com.cookieshax.coursehelper.core.database.entity.Account

@Composable
fun CheckInAccountItem(
    account: Account,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    isNeedPhoto: Boolean,
    onUploadImage: () -> Unit,
    onCameraClick: () -> Unit,
    isUploadSuccess: Boolean,
    isCheckedIn: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCheckedIn) 0.6f else 1.0f)
            .clickable(enabled = isNeedPhoto) { onCameraClick() },
        colors = CardDefaults.cardColors(
            containerColor = CardDefaults.cardColors().containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange
                )
                Spacer(modifier = Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "(${account.phone.maskedPhone})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isCheckedIn) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "已签到",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (isNeedPhoto) {
                IconButton(onClick = onUploadImage) {
                    Icon(
                        imageVector = if (isUploadSuccess) Icons.Default.CheckCircle else Icons.Default.Photo,
                        contentDescription = "上传图片",
                        tint = if (isUploadSuccess) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
