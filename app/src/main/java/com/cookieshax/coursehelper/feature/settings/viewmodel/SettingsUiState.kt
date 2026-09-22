package com.cookieshax.coursehelper.feature.settings.viewmodel

import com.cookieshax.coursehelper.core.location.LocationMethod

data class SettingsUiState(
    val isReady: Boolean = false,
    val isDynamicColorEnabled: Boolean = true,
    val preferOkHttpOverWebView: Boolean = true,
    val clearCacheOnStartup: Boolean = false,
    val cacheExpirationDays: Int = 7,
    val loginEndpoint: String = "app",
    val checkInSemaphoreLimit: Int = 6,
    val checkInAccountSelectionMode: Int = 0,
    val checkInSelectAllOnScan: Boolean = false,
    val isOpencvEnabledForCaptcha: Boolean = true,
    val maxCaptchaRetries: Int = 3,
    val cacheSize: Long = 0,
    val appTheme: String = "system",
    val themeColor: String = "#769CDF",
    val showUnsupportedTasks: Boolean = false,
    val showUnnecessaryCourses: Boolean = false,
    val cacheAllAccountsOnStartup: Boolean = false,
    val excludeCheckedInAccounts: Boolean = false,
    val maxImageCacheSize: Int = 64,
    val userAgent: String = "",
    val packageName: String = "com.chaoxing.mobile",
    val deviceId: String = "",
    val locationMethod: String = LocationMethod.BAIDU.name
)
