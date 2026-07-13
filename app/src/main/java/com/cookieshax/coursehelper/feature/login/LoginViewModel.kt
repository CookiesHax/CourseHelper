package com.cookieshax.coursehelper.feature.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    private val _currentType = MutableStateFlow(LoginType.PASSWORD)
    val currentType: StateFlow<LoginType> = _currentType.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val _verificationCode = MutableStateFlow("")
    val verificationCode: StateFlow<String> = _verificationCode.asStateFlow()

    private val _codeCountdown = MutableStateFlow(0)
    val codeCountdown: StateFlow<Int> = _codeCountdown.asStateFlow()

    fun setLoginType(type: LoginType) {
        _currentType.value = type
    }

    fun setUsername(username: String) {
        _username.value = username
    }

    fun setPassword(password: String) {
        _password.value = password
    }

    fun setPasswordVisible(visible: Boolean) {
        _passwordVisible.value = visible
    }

    fun setVerificationCode(code: String) {
        _verificationCode.value = code
    }

    fun sendVerificationCode(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = _username.value
            when (val result = ApiManager.sendVerificationCode(user)) {
                is ApiResult.Success -> {
                    val responseMap = StringUtils.parseJson(result.data)
                    val status = StringUtils.getBoolean(responseMap ?: JsonObject(), "status")
                    if (status) {
                        onResult(true, null)
                        _codeCountdown.value = 60
                        while (_codeCountdown.value > 0) {
                            delay(1000.milliseconds)
                            _codeCountdown.value -= 1
                        }
                    } else {
                        val message =
                            StringUtils.getString(responseMap ?: JsonObject(), "mes", "发送失败")
                        onResult(false, message)
                    }
                }

                is ApiResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun loginByPassword(onResult: (ApiResult<String>) -> Unit, onNewDeviceRequired: () -> Unit) {
        viewModelScope.launch {
            val endpoint = repository.loginEndpoint.first()

            val result = when (endpoint) {
                "app" -> ApiManager.loginByPassword(_username.value, _password.value)
                "web" -> ApiManager.loginByPasswordWeb(_username.value, _password.value)
                else -> return@launch
            }

            if (result is ApiResult.Success) {
                val loginMap = StringUtils.parseJson(result.data) ?: JsonObject()
                val status = StringUtils.getBoolean(loginMap, "status")

                if (!status) {
                    val errorKey = if (endpoint == "app") "mes" else "msg2"
                    val errorMsg = StringUtils.getString(loginMap, errorKey, "未知错误")
                    onResult(ApiResult.Error("登录失败: $errorMsg"))
                    return@launch
                }

                // 只有 app 接口需要校验新设备验证
                if (endpoint == "app" && !loginMap.has("url")) {
                    onNewDeviceRequired()
                    return@launch
                }
            }
            onResult(result)
        }
    }

    fun loginByVerificationCode(onResult: (ApiResult<String>) -> Unit) {
        viewModelScope.launch {
            val result =
                ApiManager.loginByVerificationCode(_username.value, _verificationCode.value)
            if (result is ApiResult.Success) {
                val loginMap = StringUtils.parseJson(result.data)
                val status = StringUtils.getBoolean(loginMap ?: JsonObject(), "status")
                if (!status) {
                    onResult(
                        ApiResult.Error(
                            "登录失败: ${
                                StringUtils.getString(
                                    loginMap ?: JsonObject(),
                                    "mes",
                                    "未知错误"
                                )
                            }"
                        )
                    )
                    return@launch
                }
            }
            onResult(result)
        }
    }
}
