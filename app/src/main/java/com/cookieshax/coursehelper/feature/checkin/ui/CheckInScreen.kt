package com.cookieshax.coursehelper.feature.checkin.ui

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.MapRoute
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.checkin.ui.components.CheckInLayout
import com.cookieshax.coursehelper.feature.checkin.ui.components.code.CodeInputComponent
import com.cookieshax.coursehelper.feature.checkin.ui.components.code.CodeTrigger
import com.cookieshax.coursehelper.feature.checkin.ui.components.gesture.GestureInputComponent
import com.cookieshax.coursehelper.feature.checkin.ui.components.gesture.GestureTrigger
import com.cookieshax.coursehelper.feature.checkin.ui.components.location.LocationTrigger
import com.cookieshax.coursehelper.feature.checkin.ui.components.normal.NormalInputComponent
import com.cookieshax.coursehelper.feature.checkin.ui.components.normal.NormalTrigger
import com.cookieshax.coursehelper.feature.checkin.ui.components.qrcode.QrCodeTrigger
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsViewModel
import com.cookieshax.coursehelper.ui.items.Placeholder
import org.json.JSONObject

sealed class CheckInType {
    object Normal : CheckInType()
    object QRCode : CheckInType()
    object Gesture : CheckInType()
    object Location : CheckInType()
    object Code : CheckInType()
    object Unknown : CheckInType()
}

@Keep
data class CheckInState(
    // 通用状态
    var otherId: String = "",
    var ifNeedVCode: Int = 0,
    var openCheckFaceFlag: Int = 0,
    var starttime: Long = 0L,
    var endTime: Long = 0L,
    var signInId: Long = 0L, // 存疑
    var signOutId: Long = 0L, // 存疑
    var signOutPublishTimeStamp: Long = 0L,

    // 位置签到
    var locationLatitude: Double = .0,
    var locationLongitude: Double = .0,
    var locationRange: Double = .0,
    var locationText: String = "",

    // 拍照签到
    var ifphoto: Int = 0,

    // 二维码签到
    var ifopenAddress: Int = 0,
    var ifrefreshewm: Int = 0,

    // 签到码签到
    var numberCount: Int = 0
)

private fun mapToCheckInType(id: String?): CheckInType {
    return when (id) {
        "0" -> CheckInType.Normal
        "2" -> CheckInType.QRCode
        "3" -> CheckInType.Gesture
        "4" -> CheckInType.Location
        "5" -> CheckInType.Code
        else -> CheckInType.Unknown
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    url: String,
    taskId: String,
    navController: NavController,
    courseId: String? = null
) {
    val isNavigating = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(true) }
    val checkInType = remember { mutableStateOf<CheckInType?>(null) }
    val checkInState = remember { mutableStateOf(CheckInState()) }
    val showPhotoDialog = remember { mutableStateOf(false) }

    val checkInViewModel: CheckInViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val semaphoreLimit = settingsViewModel.checkInSemaphoreLimit

    LaunchedEffect(taskId) {
        when (val response = ApiManager.getCheckInTaskInfo(taskId)) {
            is ApiResult.Success -> {
                val dataObject = JSONObject(response.data).optJSONObject("data")
                if (dataObject != null) {
                    // 兼容字符串形式的经纬度
                    var lat =
                        dataObject.optString("locationLatitude", "0.0").toDoubleOrNull() ?: 0.0
                    var lng =
                        dataObject.optString("locationLongitude", "0.0").toDoubleOrNull() ?: 0.0

                    // 如果外层没有拿到有效经纬度 尝试从内层 timer 对象获取
                    if (lat == 0.0 || lng == 0.0) {
                        val timerObject = dataObject.optJSONObject("timer")
                        if (timerObject != null) {
                            lat = timerObject.optString("locationLatitude", "0.0").toDoubleOrNull()
                                ?: 0.0
                            lng = timerObject.optString("locationLongitude", "0.0").toDoubleOrNull()
                                ?: 0.0
                        }
                    }

                    // 如果依然没有 最后尝试从 content 字符串中解析
                    if ((lat == 0.0 || lng == 0.0) && !dataObject.isNull("content")) {
                        try {
                            val contentObject = JSONObject(dataObject.optString("content", "{}"))
                            val contentTimer = contentObject.optJSONObject("timer")
                            if (contentTimer != null) {
                                lat = contentTimer.optString("locationLatitude", "0.0")
                                    .toDoubleOrNull() ?: 0.0
                                lng = contentTimer.optString("locationLongitude", "0.0")
                                    .toDoubleOrNull() ?: 0.0
                            } else {
                                lat = contentObject.optString("locationLatitude", "0.0")
                                    .toDoubleOrNull() ?: 0.0
                                lng = contentObject.optString("locationLongitude", "0.0")
                                    .toDoubleOrNull() ?: 0.0
                            }
                        } catch (e: Exception) { // 防止 content 字符串格式化失败导致崩溃
                            e.printStackTrace()
                        }
                    }

                    checkInState.value = checkInState.value.copy(
                        otherId = dataObject.optString("otherId", ""),
                        ifNeedVCode = dataObject.optInt("ifNeedVCode", 0),
                        openCheckFaceFlag = dataObject.optInt("openCheckFaceFlag", 0),
                        starttime = dataObject.optLong("starttime", 0L),
                        endTime = dataObject.optLong("endTime", 0L),
                        signInId = dataObject.optLong("signInId", 0L),
                        signOutId = dataObject.optLong("signOutId", 0L),
                        signOutPublishTimeStamp = dataObject.optLong("signOutPublishTimeStamp", 0L),
                        locationLatitude = lat,
                        locationLongitude = lng,
                        locationRange = dataObject.optDouble("locationRange", .0),
                        locationText =
                            if (dataObject.isNull("locationText")) "" // filter "null"
                            else dataObject.optString("locationText", ""),
                        ifphoto = dataObject.optInt("ifphoto", 0),
                        ifopenAddress = dataObject.optInt("ifopenAddress", 0),
                        ifrefreshewm = dataObject.optInt("ifrefreshewm", 0),
                        numberCount = dataObject.optInt("numberCount", 0)
                    )
                }
            }

            is ApiResult.Error -> {
                checkInState.value = checkInState.value.copy(otherId = "-1")
            }
        }

        if (settingsViewModel.shouldDefaultSelectAll()) {
            AccountRepository.getCurrentListSnapshot().let { accountList ->
                checkInViewModel.applyDefaultSelection(taskId, accountList, true)
            }
        }

        checkInType.value = mapToCheckInType(checkInState.value.otherId)
        isLoading.value = false
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("签到") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (!isNavigating.value) {
                                    isNavigating.value = true
                                    navController.popBackStack()
                                }
                            },
                            enabled = !isNavigating.value
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                val type = checkInType.value
                val state = checkInState.value
                val isNeedLocation = state.ifopenAddress == 1
                val isMockLocationFlow by LocationService.isMockLocationFlow.collectAsState()

                if ((type == CheckInType.Location || (type == CheckInType.QRCode && isNeedLocation))) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(MapRoute) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp, bottom = 64.dp),
                        containerColor = if (isMockLocationFlow) {
                            MaterialTheme.colorScheme.error // 红色表示正在模拟位置
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "虚拟定位")
                    }
                } else if (type == CheckInType.Normal && state.ifphoto == 1) {
                    FloatingActionButton(
                        onClick = {
                            showPhotoDialog.value = true
                        },
                        modifier = Modifier.padding(end = 16.dp, bottom = 64.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "拍照说明")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val accounts by AccountRepository.accountList.collectAsState()
                    val tagsWithAccounts by AccountRepository.allTagsWithAccountsFlow.collectAsState()
                    val type = checkInType.value
                    val state = checkInState.value
                    val isNeedCaptcha = state.ifNeedVCode == 1
                    val isNeedLocation = state.ifopenAddress == 1

                    if (type == null || type == CheckInType.Unknown) {
                        Placeholder("发生错误", "未知的签到类型 请重新尝试")
                    } else {
                        val limit by semaphoreLimit.collectAsState()
                        CheckInLayout(
                            viewModel = checkInViewModel,
                            accounts = accounts,
                            tagsWithAccounts = tagsWithAccounts,
                            isNeedPhoto = state.ifphoto == 1,
                            inputComponent = { vm, setUploadCallback, setCameraCallback ->
                                when (type) {
                                    CheckInType.Normal -> {
                                        NormalInputComponent(
                                            viewModel = vm,
                                            setUploadCallback = setUploadCallback,
                                            setCameraCallback = setCameraCallback
                                        )
                                    }

                                    CheckInType.Gesture -> {
                                        GestureInputComponent(
                                            taskId = taskId,
                                            viewModel = vm
                                        )
                                    }

                                    CheckInType.Code -> {
                                        CodeInputComponent(
                                            taskId = taskId,
                                            codeLength = state.numberCount,
                                            viewModel = vm
                                        )
                                    }

                                    else -> {
                                        Box(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            },
                            triggerComponent = { vm ->
                                when (type) {
                                    CheckInType.Normal -> {
                                        NormalTrigger(
                                            url = url,
                                            taskId = taskId,
                                            courseId = courseId ?: "",
                                            isNeedCaptcha = isNeedCaptcha,
                                            viewModel = vm,
                                            semaphoreLimit = limit
                                        )
                                    }

                                    CheckInType.QRCode -> {
                                        QrCodeTrigger(
                                            url = url,
                                            taskId = taskId,
                                            courseId = courseId ?: "",
                                            checkInState = state,
                                            isNeedCaptcha = isNeedCaptcha,
                                            isNeedLocation = isNeedLocation,
                                            viewModel = vm,
                                            navController = navController
                                        )
                                    }

                                    CheckInType.Gesture -> {
                                        GestureTrigger(
                                            url = url,
                                            taskId = taskId,
                                            courseId = courseId ?: "",
                                            isNeedCaptcha = isNeedCaptcha,
                                            viewModel = vm,
                                            semaphoreLimit = limit
                                        )
                                    }

                                    CheckInType.Location -> {
                                        LocationTrigger(
                                            url = url,
                                            taskId = taskId,
                                            courseId = courseId ?: "",
                                            checkInState = state,
                                            isNeedCaptcha = isNeedCaptcha,
                                            viewModel = vm,
                                            semaphoreLimit = limit
                                        )
                                    }

                                    CheckInType.Code -> {
                                        CodeTrigger(
                                            url = url,
                                            taskId = taskId,
                                            courseId = courseId ?: "",
                                            isNeedCaptcha = isNeedCaptcha,
                                            viewModel = vm,
                                            semaphoreLimit = limit
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPhotoDialog.value) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showPhotoDialog.value = false }) {
                    Text("确定")
                }
            },
            title = { Text("拍照签到说明") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("点击账号以打开相机进行拍照")
                    Text("或点击图标来通过相册选取器上传图片")
                    Text("上传成功之后 对应账户的图标会变为绿色")
                }
            }
        )
    }
}
