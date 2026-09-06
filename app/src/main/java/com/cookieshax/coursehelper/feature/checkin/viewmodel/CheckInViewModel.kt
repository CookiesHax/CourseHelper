package com.cookieshax.coursehelper.feature.checkin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.cookieshax.coursehelper.core.database.entity.Account
import com.cookieshax.coursehelper.core.utils.showToast
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

    // --- State Properties ---

    private val _isCheckingIn = MutableStateFlow(false)
    val isCheckingIn: StateFlow<Boolean> = _isCheckingIn.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _resultsMap = MutableStateFlow<Map<String, ApiResult<String>>>(emptyMap())
    val resultsMap: StateFlow<Map<String, ApiResult<String>>> = _resultsMap.asStateFlow()

    private val _activeUploadAccountId = MutableStateFlow<String?>(null)
    val activeUploadAccountId: StateFlow<String?> = _activeUploadAccountId.asStateFlow()

    private val _uploadedObjectIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val uploadedObjectIds: StateFlow<Map<String, String>> = _uploadedObjectIds.asStateFlow()

    private val _manualCaptchaQueue = MutableStateFlow<List<ManualCaptchaRequest>>(emptyList())
    val manualCaptchaQueue: StateFlow<List<ManualCaptchaRequest>> =
        _manualCaptchaQueue.asStateFlow()

    // Gesture State
    private val _gesturePoints = MutableStateFlow<List<Int>>(emptyList())
    val gesturePoints: StateFlow<List<Int>> = _gesturePoints.asStateFlow()

    private val _isCorrectGesture = MutableStateFlow(false)
    val isCorrectGesture: StateFlow<Boolean> = _isCorrectGesture.asStateFlow()

    private val _gestureResult = MutableStateFlow(GestureResult.Idle)
    val gestureResult: StateFlow<GestureResult> = _gestureResult.asStateFlow()

    // Code State
    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isCorrectCode = MutableStateFlow(false)
    val isCorrectCode: StateFlow<Boolean> = _isCorrectCode.asStateFlow()

    private val _codeInputResult = MutableStateFlow(CodeInputResult.Idle)
    val codeInputResult: StateFlow<CodeInputResult> = _codeInputResult.asStateFlow()

    // --- Helper Objects ---

    private val settingsRepository = SettingsRepository(application)
    val faceCache = FaceCache()
    private var lastDefaultedTaskId: String? = null

    // --- UI Actions ---

    fun setCheckingIn(value: Boolean) {
        _isCheckingIn.value = value
    }

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

    // --- Captcha Queue Actions ---

    suspend fun submitManualCaptchaOffset(offset: Int) {
        val currentQueue = _manualCaptchaQueue.value
        if (currentQueue.isEmpty()) return

        val currentRequest = currentQueue.first()
        val (result, updatedCaptcha) = currentRequest.captcha.submit(offset)

        if (result != null) {
            currentRequest.deferred.complete(result)
        } else {
            val finalCaptcha = updatedCaptcha
            val currentParams = currentRequest.params
            if (currentParams is CheckInParams.QrCode) {
                val refreshResult =
                    currentRequest.strategy.execute(currentRequest.uid, currentParams, faceCache)
                if (refreshResult is ApiResult.Success) {
                    refreshResult.extractedEncToken?.let { newEnc2 ->
                        currentRequest.params = currentParams.copy(enc2 = newEnc2)
                    }
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

    // --- Core Business Logic ---

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
            if (currentParams is CheckInParams.QrCode && result is ApiResult.Success) {
                result.extractedEncToken?.let { enc2 ->
                    currentParams = currentParams.copy(enc2 = enc2)
                    return solveCaptchaAndRetry(
                        uid,
                        strategy,
                        currentParams,
                        isFirstAttempt = false
                    )
                }
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
        val autoResult = tryAutoSolveCaptcha(uid, strategy, params, isFirstAttempt)
        if (autoResult.isSuccess) {
            return strategy.execute(
                uid,
                autoResult.updatedParams.withValidate(autoResult.validate),
                faceCache
            )
        }

        return try {
            val (manualValidate, finalParams) = awaitManualCaptcha(
                uid,
                strategy,
                autoResult.updatedParams,
                autoResult.lastCaptcha
            )
            strategy.execute(uid, finalParams.withValidate(manualValidate), faceCache)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "验证已取消")
        }
    }

    private suspend fun tryAutoSolveCaptcha(
        uid: String,
        strategy: CheckInStrategy,
        params: CheckInParams,
        isFirstAttempt: Boolean
    ): AutoCaptchaResult {
        var currentParams = params
        var captcha = Captcha(uid = uid, referer = params.url)
        var counter = 0
        val maxCaptchaRetries = settingsRepository.maxCaptchaRetries.first()

        while (counter < maxCaptchaRetries) {
            if (currentParams is CheckInParams.QrCode && (isFirstAttempt || counter > 0)) {
                val refreshResult = strategy.execute(uid, currentParams, faceCache)
                if (refreshResult is ApiResult.Success) {
                    refreshResult.extractedEncToken?.let { newEnc2 ->
                        currentParams = currentParams.copy(enc2 = newEnc2)
                    }
                }
            }

            captcha = captcha.load()
            if (!captcha.isLoaded) break

            try {
                val x = CaptchaSolver.calculateCaptchaOffset(captcha)
                val (res, nextCaptcha) = captcha.submit(x)
                captcha = nextCaptcha
                if (!res.isNullOrBlank()) {
                    return AutoCaptchaResult(true, res, captcha, currentParams)
                }
                counter++
            } catch (e: Exception) {
                e.message?.showToast()
                break
            }
        }
        return AutoCaptchaResult(false, "", captcha, currentParams)
    }

    private suspend fun awaitManualCaptcha(
        uid: String,
        strategy: CheckInStrategy,
        params: CheckInParams,
        lastCaptcha: Captcha
    ): Pair<String, CheckInParams> {
        var currentParams = params
        if (currentParams is CheckInParams.QrCode && currentParams.enc2.isBlank()) {
            val refreshResult = strategy.execute(uid, currentParams, faceCache)
            if (refreshResult is ApiResult.Success) {
                refreshResult.extractedEncToken?.let { newEnc2 ->
                    currentParams = currentParams.copy(enc2 = newEnc2)
                }
            }
        }

        val loadedCaptcha = lastCaptcha.load()
        val deferred = CompletableDeferred<String>()
        val request = ManualCaptchaRequest(uid, loadedCaptcha, strategy, currentParams, deferred)
        _manualCaptchaQueue.update { it + request }

        return try {
            val validate = deferred.await()
            validate to request.params
        } finally {
            _manualCaptchaQueue.update { current -> current.filterNot { it.uid == uid } }
        }
    }

    // --- Private Helpers ---

    private val ApiResult.Success<String>.extractedEncToken: String?
        get() = if (data.startsWith("validate_")) data.substringAfter("validate_") else null

    private data class AutoCaptchaResult(
        val isSuccess: Boolean,
        val validate: String,
        val lastCaptcha: Captcha,
        val updatedParams: CheckInParams
    )
}
