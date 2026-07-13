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
import com.cookieshax.coursehelper.core.utils.FileUtils.formatFileSize
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsDialogOpen
import com.cookieshax.coursehelper.feature.settings.ui.items.BooleanSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.ClickableSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SelectionSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.StepSliderSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SettingSectionHeader

@Composable
fun AppSection(
    clearCacheOnStartup: Boolean,
    cacheExpirationDays: Int,
    cacheSize: Long,
    maxImageCacheSize: Int,
    onOpenDialog: (SettingsDialogOpen) -> Unit,
    onToggleClearCacheOnStartup: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onSetMaxImageCacheSize: (Int) -> Unit
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
            BooleanSettingItem(
                title = "启动时清理缓存",
                subtitle = "每次启动应用时自动清理过期缓存",
                checked = clearCacheOnStartup,
                onCheckedChange = onToggleClearCacheOnStartup
            )
            AnimatedVisibility(visible = clearCacheOnStartup) {
                SelectionSettingItem(
                    title = "缓存过期时间",
                    currentValue = if (cacheExpirationDays > 0) "$cacheExpirationDays 天" else "立即",
                    onClick = { onOpenDialog(SettingsDialogOpen.CACHE_EXPIRATION_DAYS) }
                )
            }
            val cacheSteps = listOf(0, 8, 16, 32, 64, 128, 256, -1)
            StepSliderSettingItem(
                title = "图片缓存上限",
                value = maxImageCacheSize,
                steps = cacheSteps,
                valueLabel = { valMb ->
                    when (valMb) {
                        -1 -> "无限制"
                        0 -> "不缓存"
                        else -> "$valMb MB"
                    }
                },
                onValueChange = onSetMaxImageCacheSize
            )
            ClickableSettingItem(
                title = "立即清理缓存",
                subtitle = "当前缓存: ${formatFileSize(cacheSize)}",
                onClick = onClearCache
            )
        }
    }
}
