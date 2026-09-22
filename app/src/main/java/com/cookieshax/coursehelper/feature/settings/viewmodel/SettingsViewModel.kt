package com.cookieshax.coursehelper.feature.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.core.imageloader.CoilConfig
import com.cookieshax.coursehelper.core.info.ChaoXingAppInfo
import com.cookieshax.coursehelper.core.network.NetworkClient
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.cookieshax.coursehelper.core.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsDialogOpen {
    CACHE_EXPIRATION_DAYS,
    LOGIN_ENDPOINT,
    APP_THEME,
    THEME_COLOR,
    USER_AGENT,
    PACKAGE_NAME,
    CHECK_IN_SEMAPHORE,
    MAX_CAPTCHA_RETRIES,
    MAX_IMAGE_CACHE_SIZE,
    DEVICE_ID,
    LOCATION_METHOD,
    CHECK_IN_ACCOUNT_SELECTION_MODE
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val app = application

    private val _activeDialog = MutableStateFlow<SettingsDialogOpen?>(null)
    val activeDialog: StateFlow<SettingsDialogOpen?> = _activeDialog.asStateFlow()

    private val _cacheSize = MutableStateFlow(FileUtils.getCacheSize(app))
    private val _deviceId = MutableStateFlow("")

    val uiState: StateFlow<SettingsUiState> = combine<Any?, SettingsUiState>(
        listOf(
            repository.isDynamicColorEnabled,
            repository.preferOkHttpOverWebView,
            repository.clearCacheOnStartup,
            repository.cacheExpirationDays,
            repository.loginEndpoint,
            repository.checkInSemaphoreLimit,
            repository.checkInAccountSelectionMode,
            repository.checkInSelectAllOnScan,
            repository.isOpencvEnabledForCaptcha,
            repository.maxCaptchaRetries,
            repository.appTheme,
            repository.themeColor,
            repository.showUnsupportedTasks,
            repository.showUnnecessaryCourses,
            repository.cacheAllAccountsOnStartup,
            repository.excludeCheckedInAccounts,
            repository.maxImageCacheSize,
            repository.userAgent.onEach { NetworkClient.clearUserAgentCache() },
            repository.packageName.onEach { ChaoXingAppInfo.packageName = it },
            repository.locationMethod,
            _cacheSize,
            _deviceId
        )
    ) { args ->
        SettingsUiState(
            isReady = true,
            isDynamicColorEnabled = args[0] as Boolean,
            preferOkHttpOverWebView = args[1] as Boolean,
            clearCacheOnStartup = args[2] as Boolean,
            cacheExpirationDays = args[3] as Int,
            loginEndpoint = args[4] as String,
            checkInSemaphoreLimit = args[5] as Int,
            checkInAccountSelectionMode = args[6] as Int,
            checkInSelectAllOnScan = args[7] as Boolean,
            isOpencvEnabledForCaptcha = args[8] as Boolean,
            maxCaptchaRetries = args[9] as Int,
            appTheme = args[10] as String,
            themeColor = args[11] as String,
            showUnsupportedTasks = args[12] as Boolean,
            showUnnecessaryCourses = args[13] as Boolean,
            cacheAllAccountsOnStartup = args[14] as Boolean,
            excludeCheckedInAccounts = args[15] as Boolean,
            maxImageCacheSize = args[16] as Int,
            userAgent = args[17] as String,
            packageName = args[18] as String,
            locationMethod = args[19] as String,
            cacheSize = args[20] as Long,
            deviceId = args[21] as String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isReady = false)
    )

    init {
        viewModelScope.launch {
            _deviceId.value = NetworkClient.getDeviceId()
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColorEnabled(enabled) }
    }

    fun togglePreferOkHttp(prefer: Boolean) {
        viewModelScope.launch { repository.setPreferOkHttpOverWebView(prefer) }
    }

    fun toggleClearCacheOnStartup(enabled: Boolean) {
        viewModelScope.launch { repository.setClearCacheOnStartup(enabled) }
    }

    fun setCacheExpirationDays(days: Int) {
        viewModelScope.launch { repository.setCacheExpirationDays(days) }
    }

    fun setLoginEndpoint(endpoint: String) {
        viewModelScope.launch { repository.setLoginEndpoint(endpoint) }
    }

    fun setCheckInSemaphoreLimit(limit: Int) {
        val verifiedLimit = limit.coerceAtLeast(1)
        viewModelScope.launch { repository.setCheckInSemaphoreLimit(verifiedLimit) }
    }

    fun toggleOpencvForCaptcha(enabled: Boolean) {
        viewModelScope.launch { repository.setOpencvEnabledForCaptcha(enabled) }
    }

    fun setMaxCaptchaRetries(retries: Int) {
        val verifiedRetries = retries.coerceAtLeast(0)
        viewModelScope.launch { repository.setMaxCaptchaRetries(verifiedRetries) }
    }

    fun setCheckInAccountSelectionMode(mode: Int) {
        viewModelScope.launch { repository.setCheckInAccountSelectionMode(mode) }
    }

    fun toggleCheckInSelectAllOnScan(enabled: Boolean) {
        viewModelScope.launch { repository.setCheckInSelectAllOnScan(enabled) }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch { repository.setAppTheme(theme) }
    }

    fun setThemeColor(color: String) {
        viewModelScope.launch { repository.setThemeColor(color) }
    }

    fun toggleShowUnsupportedTasks(enabled: Boolean) {
        viewModelScope.launch { repository.setShowUnsupportedTasks(enabled) }
    }

    fun setActiveDialog(dialog: SettingsDialogOpen?) {
        if (dialog == null || _activeDialog.value == null) {
            _activeDialog.value = dialog
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            FileUtils.cleanupCache(app, 0L)
            _cacheSize.value = FileUtils.getCacheSize(app)
        }
    }

    fun refreshCacheSize() {
        _cacheSize.value = FileUtils.getCacheSize(app)
    }

    fun toggleShowUnnecessaryCourses(enabled: Boolean) {
        viewModelScope.launch { repository.setShowUnnecessaryCourses(enabled) }
    }

    fun setMaxImageCacheSize(size: Int) {
        viewModelScope.launch {
            repository.setMaxImageCacheSize(size)
            CoilConfig.resetImageLoader()
        }
    }

    fun setUserAgent(ua: String) {
        viewModelScope.launch { repository.setUserAgent(ua) }
    }

    fun setPackageName(packageName: String) {
        viewModelScope.launch {
            val name = packageName.ifBlank { "com.chaoxing.mobile" }
            repository.setPackageName(name)
            repository.setUserAgent("") // Trigger UA regeneration
        }
    }

    fun setLocationMethod(method: String) {
        viewModelScope.launch { repository.setLocationMethod(method) }
    }

    fun toggleCacheAllAccountsOnStartup(enabled: Boolean) {
        viewModelScope.launch { repository.setCacheAllAccountsOnStartup(enabled) }
    }

    fun toggleExcludeCheckedInAccounts(enabled: Boolean) {
        viewModelScope.launch { repository.setExcludeCheckedInAccounts(enabled) }
    }

    fun updateDeviceId(id: String) {
        viewModelScope.launch {
            NetworkClient.setDeviceId(id)
            _deviceId.value = id
            repository.setUserAgent("") // Trigger UA regeneration
        }
    }
}
