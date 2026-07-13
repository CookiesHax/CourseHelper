package com.cookieshax.coursehelper.feature.settings.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsDialogOpen
import com.cookieshax.coursehelper.feature.settings.ui.items.BooleanSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SelectionSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SettingSectionHeader

@Composable
fun PersonalizationSection(
    isDynamicColorEnabled: Boolean,
    appTheme: String,
    themeColor: String,
    onToggleDynamicColor: (Boolean) -> Unit,
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
            SettingSectionHeader(title = "个性化")
            BooleanSettingItem(
                title = "莫奈取色",
                subtitle = "根据壁纸颜色自动调整应用主题色 (Android 12+)",
                checked = isDynamicColorEnabled,
                onCheckedChange = onToggleDynamicColor
            )
            SelectionSettingItem(
                title = "应用主题",
                currentValue = when (appTheme) {
                    "system" -> "跟随系统"
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> appTheme
                },
                onClick = { onOpenDialog(SettingsDialogOpen.APP_THEME) }
            )
            AnimatedVisibility(visible = !isDynamicColorEnabled) {
                SelectionSettingItem(
                    title = "主题色",
                    currentValue = themeColor,
                    onClick = { onOpenDialog(SettingsDialogOpen.THEME_COLOR) }
                )
            }
        }
    }
}
