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
import com.cookieshax.coursehelper.feature.course.model.Course

@Composable
fun CourseItem(course: Course, onItemClick: (Course) -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(course) },
        colors = CardDefaults.cardColors(
            containerColor = CardDefaults.cardColors().containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp), // 给整体内容留出边距
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val painter = rememberAsyncImagePainter(
                model = course.imageUrl,
                imageLoader = CoilConfig.getImageLoader(context),
                onError = { result ->
                    Log.e(
                        "CoilError",
                        "课程图片加载失败: ${course.imageUrl}, 错误信息: ${result.result.throwable.message}",
                        result.result.throwable
                    )
                }
            )
            // 使用自定义painter
            Image(
                painter = painter,
                contentDescription = "课程图片",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)), // 圆角矩形
                contentScale = ContentScale.Crop // 裁剪填充
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 课程详细信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "教师: ${course.teacher}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (!course.note.isNullOrEmpty()) {
                    Text(
                        text = "班级: ${course.note}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (!course.beginDate.isNullOrEmpty() && !course.endDate.isNullOrEmpty()) {
                    Text(
                        text = "时间: ${course.beginDate} 至 ${course.endDate}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (!course.schools.isNullOrEmpty()) {
                    Text(
                        text = course.schools,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
