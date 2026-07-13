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
import com.cookieshax.coursehelper.feature.settings.ui.items.BooleanSettingItem
import com.cookieshax.coursehelper.feature.settings.ui.items.SettingSectionHeader
import com.cookieshax.coursehelper.feature.settings.ui.items.SliderSettingItem

@Composable
fun CourseSection(
    preferOkHttpOverWebView: Boolean,
    isCheckInDefaultSelectAll: Boolean,
    checkInSemaphoreLimit: Int,
    isOpencvEnabledForCaptcha: Boolean,
    maxCaptchaRetries: Int,
    showUnsupportedTasks: Boolean,
    showUnnecessaryCourses: Boolean,
    onTogglePreferOkHttp: (Boolean) -> Unit,
    onToggleCheckInDefaultSelectAll: (Boolean) -> Unit,
    onSetCheckInSemaphoreLimit: (Int) -> Unit,
    onToggleOpencvForCaptcha: (Boolean) -> Unit,
    onSetMaxCaptchaRetries: (Int) -> Unit,
    onToggleShowUnsupportedTasks: (Boolean) -> Unit,
    onToggleShowUnnecessaryCourses: (Boolean) -> Unit
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
            SettingSectionHeader(title = "课程")
            BooleanSettingItem(
                title = "优先使用OkHttp签到",
                subtitle = "启用后将优先使用OkHttp进行签到，而非WebView",
                checked = preferOkHttpOverWebView,
                onCheckedChange = onTogglePreferOkHttp
            )
            AnimatedVisibility(visible = preferOkHttpOverWebView) {
                Column {
                    BooleanSettingItem(
                        title = "签到界面默认全选账号",
                        subtitle = "启用后签到界面将默认全选账号，否则默认不选",
                        checked = isCheckInDefaultSelectAll,
                        onCheckedChange = onToggleCheckInDefaultSelectAll
                    )
                    SliderSettingItem(
                        title = "签到并发数",
                        value = checkInSemaphoreLimit,
                        range = 1 .. 16,
                        onValueChange = onSetCheckInSemaphoreLimit
                    )
                    BooleanSettingItem(
                        title = "使用OpenCV进行Captcha识别",
                        subtitle = "启用后将使用OpenCV进行Captcha识别，否则需要手动完成验证码",
                        checked = isOpencvEnabledForCaptcha,
                        onCheckedChange = onToggleOpencvForCaptcha
                    )
                    AnimatedVisibility(visible = isOpencvEnabledForCaptcha) {
                        SliderSettingItem(
                            title = "自动Captcha最大重试次数",
                            value = maxCaptchaRetries,
                            range = 1 .. 5,
                            onValueChange = onSetMaxCaptchaRetries
                        )
                    }
                }
            }
            BooleanSettingItem(
                title = "展示不受支持的任务活动",
                subtitle = "启用后将展示不受支持的任务活动，如通知",
                checked = showUnsupportedTasks,
                onCheckedChange = onToggleShowUnsupportedTasks
            )
            BooleanSettingItem(
                title = "显示不必要的课程",
                subtitle = "启用后将显示未开始和已结束的课程，否则将隐藏",
                checked = showUnnecessaryCourses,
                onCheckedChange = onToggleShowUnnecessaryCourses
            )
        }
    }
}
