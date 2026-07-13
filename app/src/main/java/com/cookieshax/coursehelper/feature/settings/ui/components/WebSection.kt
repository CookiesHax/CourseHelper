package com.cookieshax.coursehelper.feature.settings.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.settings.ui.items.ClickableSettingItem
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsDialogOpen
import com.cookieshax.coursehelper.feature.settings.ui.items.SelectionSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SettingSectionHeader

@Composable
fun WebSection(
    userAgent: String,
    loginEndpoint: String,
    onOpenDialog: (SettingsDialogOpen) -> Unit
) {
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
            SettingSectionHeader(title = "网络")
            ClickableSettingItem(
                title = "User-Agent",
                subtitle = userAgent.ifEmpty { "无" },
                onClick = { onOpenDialog(SettingsDialogOpen.USER_AGENT) }
            )
            SelectionSettingItem(
                title = "密码登录端点",
                currentValue = if (loginEndpoint == "web") "Web 端" else "App 端",
                onClick = { onOpenDialog(SettingsDialogOpen.LOGIN_ENDPOINT) }
            )
        }
    }
}
