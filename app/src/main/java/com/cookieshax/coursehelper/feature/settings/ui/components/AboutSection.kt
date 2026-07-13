package com.cookieshax.coursehelper.feature.settings.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.cookieshax.coursehelper.feature.settings.ui.items.ClickableSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SettingSectionHeader

@Composable
fun AboutSection() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDefaults.cardColors().containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            SettingSectionHeader(title = "关于")

            ClickableSettingItem(
                title = "GitHub 仓库",
                subtitle = "查看项目源码",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/CookiesHax/CourseHelper".toUri()
                    )
                    context.startActivity(intent)
                }
            )

            ClickableSettingItem(
                title = "开发者",
                subtitle = "CookiesHax",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/CookiesHax".toUri())
                    context.startActivity(intent)
                }
            )
        }
    }
}
