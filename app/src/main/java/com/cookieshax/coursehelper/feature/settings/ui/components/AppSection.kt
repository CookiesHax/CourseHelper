package com.cookieshax.coursehelper.feature.settings.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.location.LocationMethod
import com.cookieshax.coursehelper.feature.settings.ui.items.ClickableSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SelectionSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SettingSectionHeader
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsDialogOpen
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsUiState

@Composable
fun AppSection(
    uiState: SettingsUiState,
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
            SettingSectionHeader(title = "应用")
            ClickableSettingItem(
                title = "伪装包名",
                subtitle = uiState.packageName,
                onClick = { onOpenDialog(SettingsDialogOpen.PACKAGE_NAME) }
            )
            ClickableSettingItem(
                title = "设备标识 ID",
                subtitle = uiState.deviceId.ifEmpty { "正在获取..." },
                onClick = { onOpenDialog(SettingsDialogOpen.DEVICE_ID) }
            )
            ClickableSettingItem(
                title = "User-Agent",
                subtitle = uiState.userAgent.ifEmpty { "无" },
                onClick = { onOpenDialog(SettingsDialogOpen.USER_AGENT) }
            )
            SelectionSettingItem(
                title = "密码登录端点",
                currentValue = if (uiState.loginEndpoint == "web") "Web 端" else "App 端",
                onClick = { onOpenDialog(SettingsDialogOpen.LOGIN_ENDPOINT) }
            )
            SelectionSettingItem(
                title = "定位方式",
                currentValue = try {
                    LocationMethod.valueOf(uiState.locationMethod).description
                } catch (_: Exception) {
                    uiState.locationMethod
                },
                onClick = { onOpenDialog(SettingsDialogOpen.LOCATION_METHOD) }
            )
        }
    }
}
