package com.cookieshax.coursehelper.feature.course.ui.items

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.cookieshax.coursehelper.core.imageloader.CoilConfig
import com.cookieshax.coursehelper.feature.course.model.CourseTask

@Composable
fun TaskItem(courseTask: CourseTask, onItemClick: (CourseTask) -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(courseTask) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
                model = courseTask.picUrl,
                imageLoader = CoilConfig.getImageLoader(context),
                onError = { result ->
                    Log.e(
                        "CoilError",
                        "任务图片加载失败: ${courseTask.picUrl}, 错误信息: ${result.result.throwable.message}",
                        result.result.throwable
                    )
                }
            )
            // 显示任务图片
            Image(
                painter = painter,
                contentDescription = "任务图片",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 任务详细信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 任务标题
                Text(
                    text = courseTask.nameOne,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 任务类型和状态
                val statusText = when (courseTask.status) {
                    0 -> "未开始"
                    1 -> "进行中"
                    2 -> "已结束"
                    else -> "未知状态"
                }

                Text(
                    text = "状态: $statusText",
                    fontSize = 14.sp,
                    color = when (courseTask.status) {
                        0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )

                // 活动类型
                val activeTypeText = CourseTask.activeTypeToText(courseTask.activeType)

                Text(
                    text = "活动类型: $activeTypeText",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 结束时间 / 剩余时间
                Text(
                    text = courseTask.nameTwo.takeIf { it.isNotBlank() } ?: courseTask.nameFour,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
