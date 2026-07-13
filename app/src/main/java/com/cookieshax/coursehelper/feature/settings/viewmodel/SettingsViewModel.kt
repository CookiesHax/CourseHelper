package com.cookieshax.coursehelper.feature.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cookieshax.coursehelper.core.utils.FileUtils
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.cookieshax.coursehelper.core.imageloader.CoilConfig
import com.cookieshax.coursehelper.core.network.NetworkClient
import kotlinx.coroutines.flow.first

enum class SettingsDialogOpen {
    CACHE_EXPIRATION_DAYS,
    LOGIN_ENDPOINT,
    APP_THEME,
    THEME_COLOR,
    USER_AGENT
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val app = application

    private val _activeDialog = MutableStateFlow<SettingsDialogOpen?>(null)
    val activeDialog: StateFlow<SettingsDialogOpen?> = _activeDialog.asStateFlow()

    private val _cacheSize = MutableStateFlow(FileUtils.getCacheSize(app))
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isDynamicColorEnabled = MutableStateFlow(true)
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

    private val _preferOkHttpOverWebView = MutableStateFlow(true)
    val preferOkHttpOverWebView: StateFlow<Boolean> = _preferOkHttpOverWebView.asStateFlow()

    private val _clearCacheOnStartup = MutableStateFlow(false)
    val clearCacheOnStartup: StateFlow<Boolean> = _clearCacheOnStartup.asStateFlow()

    private val _cacheExpirationDays = MutableStateFlow(7)
    val cacheExpirationDays: StateFlow<Int> = _cacheExpirationDays.asStateFlow()

    private val _loginEndpoint = MutableStateFlow("app")
    val loginEndpoint: StateFlow<String> = _loginEndpoint.asStateFlow()

    private val _checkInSemaphoreLimit = MutableStateFlow(6)
    val checkInSemaphoreLimit: StateFlow<Int> = _checkInSemaphoreLimit.asStateFlow()

    private val _isOpencvEnabledForCaptcha = MutableStateFlow(true)
    val isOpencvEnabledForCaptcha: StateFlow<Boolean> = _isOpencvEnabledForCaptcha.asStateFlow()

    private val _maxCaptchaRetries = MutableStateFlow(3)
    val maxCaptchaRetries: StateFlow<Int> = _maxCaptchaRetries.asStateFlow()

    private val _isCheckInDefaultSelectAll = MutableStateFlow(true)
    val isCheckInDefaultSelectAll: StateFlow<Boolean> = _isCheckInDefaultSelectAll.asStateFlow()

    private val _appTheme = MutableStateFlow("system")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _themeColor = MutableStateFlow("#769CDF")
    val themeColor: StateFlow<String> = _themeColor.asStateFlow()

    private val _showUnsupportedTasks = MutableStateFlow(false)
    val showUnsupportedTasks: StateFlow<Boolean> = _showUnsupportedTasks.asStateFlow()

    private val _showUnnecessaryCourses = MutableStateFlow(false)
    val showUnnecessaryCourses: StateFlow<Boolean> = _showUnnecessaryCourses.asStateFlow()

    private val _maxImageCacheSize = MutableStateFlow(64)
    val maxImageCacheSize: StateFlow<Int> = _maxImageCacheSize.asStateFlow()

    private val _userAgent = MutableStateFlow("")
    val userAgent: StateFlow<String> = _userAgent.asStateFlow()

    init {
        viewModelScope.launch {
            // Initial sync fetch of all values
            _isDynamicColorEnabled.value = repository.isDynamicColorEnabled.first()
            _preferOkHttpOverWebView.value = repository.preferOkHttpOverWebView.first()
            _clearCacheOnStartup.value = repository.clearCacheOnStartup.first()
            _cacheExpirationDays.value = repository.cacheExpirationDays.first()
            _loginEndpoint.value = repository.loginEndpoint.first()
            _checkInSemaphoreLimit.value = repository.checkInSemaphoreLimit.first()
            _isCheckInDefaultSelectAll.value = repository.isCheckInDefaultSelectAll.first()
            _appTheme.value = repository.appTheme.first()
            _themeColor.value = repository.themeColor.first()
            _showUnsupportedTasks.value = repository.showUnsupportedTasks.first()
            _showUnnecessaryCourses.value = repository.showUnnecessaryCourses.first()
            _maxImageCacheSize.value = repository.maxImageCacheSize.first()
            _userAgent.value = repository.userAgent.first()

            _isReady.value = true

            // Start collecting for real-time updates
            launch {
                repository.isDynamicColorEnabled.collect {
                    _isDynamicColorEnabled.value = it
                }
            }
            launch {
                repository.preferOkHttpOverWebView.collect {
                    _preferOkHttpOverWebView.value = it
                }
            }
            launch { repository.clearCacheOnStartup.collect { _clearCacheOnStartup.value = it } }
            launch { repository.cacheExpirationDays.collect { _cacheExpirationDays.value = it } }
            launch { repository.loginEndpoint.collect { _loginEndpoint.value = it } }
            launch {
                repository.checkInSemaphoreLimit.collect {
                    _checkInSemaphoreLimit.value = it
                }
            }
            launch {
                repository.isOpencvEnabledForCaptcha.collect {
                    _isOpencvEnabledForCaptcha.value = it
                }
            }
            launch { repository.maxCaptchaRetries.collect { _maxCaptchaRetries.value = it } }
            launch {
                repository.isCheckInDefaultSelectAll.collect {
                    _isCheckInDefaultSelectAll.value = it
                }
            }
            launch { repository.appTheme.collect { _appTheme.value = it } }
            launch { repository.themeColor.collect { _themeColor.value = it } }
            launch { repository.showUnsupportedTasks.collect { _showUnsupportedTasks.value = it } }
            launch {
                repository.showUnnecessaryCourses.collect {
                    _showUnnecessaryCourses.value = it
                }
            }
            launch { repository.maxImageCacheSize.collect { _maxImageCacheSize.value = it } }
            launch {
                repository.userAgent.collect {
                    _userAgent.value = it
                    NetworkClient.clearUserAgentCache()
                }
            }
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColorEnabled(enabled)
        }
    }

    fun togglePreferOkHttp(prefer: Boolean) {
        viewModelScope.launch {
            repository.setPreferOkHttpOverWebView(prefer)
        }
    }

    fun toggleClearCacheOnStartup(enabled: Boolean) {
        viewModelScope.launch {
            repository.setClearCacheOnStartup(enabled)
        }
    }

    fun setCacheExpirationDays(days: Int) {
        viewModelScope.launch {
            repository.setCacheExpirationDays(days)
        }
    }

    fun setLoginEndpoint(endpoint: String) {
        viewModelScope.launch {
            repository.setLoginEndpoint(endpoint)
        }
    }

    fun setCheckInSemaphoreLimit(limit: Int) {
        viewModelScope.launch {
            repository.setCheckInSemaphoreLimit(limit)
        }
    }

    fun toggleOpencvForCaptcha(enabled: Boolean) {
        viewModelScope.launch {
            repository.setOpencvEnabledForCaptcha(enabled)
        }
    }

    fun setMaxCaptchaRetries(retries: Int) {
        viewModelScope.launch {
            repository.setMaxCaptchaRetries(retries)
        }
    }

    fun toggleCheckInDefaultSelectAll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCheckInDefaultSelectAll(enabled)
        }
    }

    suspend fun shouldDefaultSelectAll(): Boolean {
        return repository.isCheckInDefaultSelectAll.first()
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            repository.setAppTheme(theme)
        }
    }

    fun setThemeColor(color: String) {
        viewModelScope.launch {
            repository.setThemeColor(color)
        }
    }

    fun toggleShowUnsupportedTasks(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowUnsupportedTasks(enabled)
        }
    }

    fun setActiveDialog(dialog: SettingsDialogOpen?) {
        _activeDialog.value = dialog
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
        viewModelScope.launch {
            repository.setShowUnnecessaryCourses(enabled)
        }
    }

    fun setMaxImageCacheSize(size: Int) {
        viewModelScope.launch {
            repository.setMaxImageCacheSize(size)
            CoilConfig.resetImageLoader()
        }
    }

    fun setUserAgent(ua: String) {
        viewModelScope.launch {
            repository.setUserAgent(ua)
        }
    }
}
