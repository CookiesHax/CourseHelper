package com.cookieshax.coursehelper.feature.checkin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.checkin.model.Captcha
import com.cookieshax.coursehelper.feature.checkin.model.CaptchaSolver
import com.cookieshax.coursehelper.feature.checkin.model.CheckInParams
import com.cookieshax.coursehelper.feature.checkin.model.CheckInStrategy
import com.cookieshax.coursehelper.feature.checkin.model.FaceCache
import com.cookieshax.coursehelper.feature.checkin.ui.components.code.CodeInputResult
import com.cookieshax.coursehelper.feature.checkin.ui.components.gesture.GestureResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    data class ManualCaptchaRequest(
        val uid: String,
        val captcha: Captcha,
        val strategy: CheckInStrategy,
        var params: CheckInParams,
        val deferred: CompletableDeferred<String>
    )

    private val settingsRepository = SettingsRepository(application)
    private val _isCheckingIn = MutableStateFlow(false)
    val isCheckingIn: StateFlow<Boolean> = _isCheckingIn.asStateFlow()

    val faceCache = FaceCache()

    fun setCheckingIn(value: Boolean) {
        _isCheckingIn.value = value
    }

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private var lastDefaultedTaskId: String? = null

    private val _resultsMap = MutableStateFlow<Map<String, ApiResult<String>>>(emptyMap())
    val resultsMap: StateFlow<Map<String, ApiResult<String>>> = _resultsMap.asStateFlow()

    private val _activeUploadAccountId = MutableStateFlow<String?>(null)
    val activeUploadAccountId: StateFlow<String?> = _activeUploadAccountId.asStateFlow()

    private val _uploadedObjectIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val uploadedObjectIds: StateFlow<Map<String, String>> = _uploadedObjectIds.asStateFlow()

    // Manual Captcha State
    private val _manualCaptchaQueue = MutableStateFlow<List<ManualCaptchaRequest>>(emptyList())
    val manualCaptchaQueue: StateFlow<List<ManualCaptchaRequest>> =
        _manualCaptchaQueue.asStateFlow()

    // Gesture Sign-In State
    private val _gesturePoints = MutableStateFlow<List<Int>>(emptyList())

    val gesturePoints: StateFlow<List<Int>> = _gesturePoints.asStateFlow()

    private val _isCorrectGesture = MutableStateFlow(false)
    val isCorrectGesture: StateFlow<Boolean> = _isCorrectGesture.asStateFlow()

    private val _gestureResult = MutableStateFlow(GestureResult.Idle)
    val gestureResult: StateFlow<GestureResult> = _gestureResult.asStateFlow()

    // Code Sign-In State
    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isCorrectCode = MutableStateFlow(false)
    val isCorrectCode: StateFlow<Boolean> = _isCorrectCode.asStateFlow()

    private val _codeInputResult = MutableStateFlow(CodeInputResult.Idle)
    val codeInputResult: StateFlow<CodeInputResult> = _codeInputResult.asStateFlow()

    fun setSelectedAccountsById(ids: Set<String>) {
        _selectedIds.value = ids
    }

    fun setSelectedAccounts(accounts: List<Account>) {
        setSelectedAccountsById(accounts.map { it.uid }.toSet())
    }

    fun setAccountSelected(uid: String, isSelected: Boolean) {
        _selectedIds.update { current ->
            if (isSelected) current + uid else current - uid
        }
    }

    fun toggleAccountsSelection(uids: List<String>, shouldSelect: Boolean) {
        _selectedIds.update { current ->
            if (shouldSelect) current + uids else current - uids.toSet()
        }
    }

    fun applyDefaultSelection(taskId: String, accounts: List<Account>, shouldDefault: Boolean) {
        if (shouldDefault && lastDefaultedTaskId != taskId) {
            setSelectedAccounts(accounts)
            lastDefaultedTaskId = taskId
        }
    }

    fun clearResults() {
        _resultsMap.value = emptyMap()
    }

    fun setActiveUploadAccountId(uid: String?) {
        _activeUploadAccountId.value = uid
    }

    fun setUploadedObjectId(uid: String, objectId: String) {
        _uploadedObjectIds.update { it + (uid to objectId) }
    }

    fun updateGesture(points: List<Int>, correct: Boolean, result: GestureResult) {
        _gesturePoints.value = points
        _isCorrectGesture.value = correct
        _gestureResult.value = result
    }

    fun updateCode(newCode: String, correct: Boolean, result: CodeInputResult) {
        _code.value = newCode
        _isCorrectCode.value = correct
        _codeInputResult.value = result
    }

    suspend fun submitManualCaptchaOffset(offset: Int) {
        val currentQueue = _manualCaptchaQueue.value
        if (currentQueue.isEmpty()) return

        val currentRequest = currentQueue.first()
        val (result, updatedCaptcha) = currentRequest.captcha.submit(offset)

        if (result != null) {
            currentRequest.deferred.complete(result)
        } else {
            // 如果验证失败且是扫码签到 在加载新验证码前必须先刷新 enc2
            val finalCaptcha = updatedCaptcha
            val currentParams = currentRequest.params
            if (currentParams is CheckInParams.QrCode) {
                val refreshResult =
                    currentRequest.strategy.execute(currentRequest.uid, currentParams, faceCache)
                if (refreshResult is ApiResult.Success && refreshResult.data.startsWith("validate_")) {
                    val newEnc2 = refreshResult.data.split("_")[1]
                    currentRequest.params = currentParams.copy(enc2 = newEnc2)
                }
            }
            val refreshedCaptcha = finalCaptcha.load()

            _manualCaptchaQueue.update { queue ->
                val updatedQueue = queue.toMutableList()
                if (updatedQueue.isNotEmpty()) {
                    updatedQueue[0] = updatedQueue[0].copy(
                        captcha = refreshedCaptcha
                    )
                }
                updatedQueue
            }
        }
    }

    fun cancelManualCaptcha() {
        val currentQueue = _manualCaptchaQueue.value
        if (currentQueue.isEmpty()) return

        val currentRequest = currentQueue.first()
        currentRequest.deferred.completeExceptionally(Exception("Cancelled by user"))
        _manualCaptchaQueue.update { it.drop(1) }
    }

    suspend fun performCheckIn(
        strategy: CheckInStrategy,
        params: CheckInParams,
        isNeedCaptcha: Boolean,
        semaphoreLimit: Int = 6
    ) {
        _isCheckingIn.value = true
        val semaphore = Semaphore(semaphoreLimit)

        try {
            val resultsMap = withContext(Dispatchers.IO) {
                supervisorScope {
                    _selectedIds.value.map { uid ->
                        async {
                            semaphore.withPermit {
                                uid to executeWithCaptchaRetry(uid, strategy, params, isNeedCaptcha)
                            }
                        }
                    }.awaitAll().toMap()
                }
            }
            _resultsMap.value = resultsMap
        } finally {
            _isCheckingIn.value = false
        }
    }

    private suspend fun executeWithCaptchaRetry(
        uid: String,
        strategy: CheckInStrategy,
        params: CheckInParams,
        isNeedCaptcha: Boolean
    ): ApiResult<String> {
        var currentParams = params

        if (!isNeedCaptcha) {
            val result = strategy.execute(uid, currentParams, faceCache)
            // 如果接口响应了 validate_ 依然要走验证码流程
            if (currentParams is CheckInParams.QrCode
                && result is ApiResult.Success
                && result.data.startsWith("validate_")
            ) {
                val enc2 = result.data.split("_")[1]
                currentParams = currentParams.copy(enc2 = enc2)
                return solveCaptchaAndRetry(
                    uid,
                    strategy,
                    currentParams,
                    isFirstAttempt = false
                )
            }
            return result
        }

        return solveCaptchaAndRetry(
            uid,
            strategy,
            currentParams,
            isFirstAttempt = true
        )
    }

    private suspend fun solveCaptchaAndRetry(
        uid: String,
        strategy: CheckInStrategy,
        params: CheckInParams,
        isFirstAttempt: Boolean = false
    ): ApiResult<String> {
        val url = when (params) {
            is CheckInParams.Normal -> params.url
            is CheckInParams.QrCode -> params.url
            is CheckInParams.Location -> params.url
            is CheckInParams.Gesture -> params.url
            is CheckInParams.Code -> params.url
        }

        var currentParams = params
        var captcha = Captcha(uid = uid, referer = url)

        var counter = 0
        var validate = ""
        val maxCaptchaRetries = settingsRepository.maxCaptchaRetries.first()

        while (validate.isBlank() && counter < maxCaptchaRetries) {
            if (
                currentParams is CheckInParams.QrCode
                && (isFirstAttempt || counter > 0)
            ) {
                val refreshResult = strategy.execute(uid, currentParams, faceCache)
                if (refreshResult is ApiResult.Success && refreshResult.data.startsWith("validate_")) {
                    val newEnc2 = refreshResult.data.split("_")[1]
                    currentParams = currentParams.copy(enc2 = newEnc2)
                }
            }

            captcha = captcha.load()
            if (!captcha.isLoaded) {
                return ApiResult.Error("验证码加载失败: ${captcha.errorMessage}")
            }

            val x = CaptchaSolver.calculateCaptchaOffset(captcha)
            val (res, nextCaptcha) = captcha.submit(x)
            captcha = nextCaptcha
            validate = res ?: ""
            counter++
        }

        // 手动打码
        if (validate.isBlank()) {
            // 确保获取 enc2
            if (currentParams is CheckInParams.QrCode && currentParams.enc2.isBlank()) {
                val refreshResult = strategy.execute(uid, currentParams, faceCache)
                if (refreshResult is ApiResult.Success && refreshResult.data.startsWith("validate_")) {
                    val newEnc2 = refreshResult.data.split("_")[1]
                    currentParams = currentParams.copy(enc2 = newEnc2)
                }
            }

            // 在放入队列前必须先 load 以确保 UI 显示的图片和 Token 一致
            val loadedCaptcha = captcha.load()

            val deferred = CompletableDeferred<String>()
            val request =
                ManualCaptchaRequest(uid, loadedCaptcha, strategy, currentParams, deferred)
            _manualCaptchaQueue.update { it + request }

            try {
                validate = deferred.await()
            } catch (_: Exception) {
                return ApiResult.Error("验证已取消")
            }

            val updatedParamsFromQueue = request.params
            _manualCaptchaQueue.update { current -> current.filterNot { it.uid == uid } }
            val finalParams = when (updatedParamsFromQueue) {
                is CheckInParams.Normal -> updatedParamsFromQueue.copy(validate = validate)
                is CheckInParams.QrCode -> updatedParamsFromQueue.copy(validate = validate)
                is CheckInParams.Location -> updatedParamsFromQueue.copy(validate = validate)
                is CheckInParams.Gesture -> updatedParamsFromQueue.copy(validate = validate)
                is CheckInParams.Code -> updatedParamsFromQueue.copy(validate = validate)
            }
            return strategy.execute(uid, finalParams, faceCache)
        }

        val updatedParams = when (currentParams) {
            is CheckInParams.Normal -> currentParams.copy(validate = validate)
            is CheckInParams.QrCode -> currentParams.copy(validate = validate)
            is CheckInParams.Location -> currentParams.copy(validate = validate)
            is CheckInParams.Gesture -> currentParams.copy(validate = validate)
            is CheckInParams.Code -> currentParams.copy(validate = validate)
        }
        return strategy.execute(uid, updatedParams, faceCache)
    }
}
