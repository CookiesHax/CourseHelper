package com.cookieshax.coursehelper.feature.checkin.ui

import android.content.ClipData
import androidx.annotation.Keep
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.MapRoute
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.core.utils.getDoubleOrDefault
import com.cookieshax.coursehelper.core.utils.getIntOrDefault
import com.cookieshax.coursehelper.core.utils.getLongOrDefault
import com.cookieshax.coursehelper.core.utils.getAsJsonObjectOrNull
import com.cookieshax.coursehelper.core.utils.getStringOrEmpty
import com.cookieshax.coursehelper.core.utils.getStringOrNull
import com.cookieshax.coursehelper.core.utils.showToast
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
import kotlinx.coroutines.launch

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
    val rawTaskInfo = remember { mutableStateOf("") }
    val showInfoDialog = remember { mutableStateOf(false) }

    val checkInViewModel: CheckInViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val semaphoreLimit = settingsViewModel.checkInSemaphoreLimit

    LaunchedEffect(taskId) {
        when (val response = ApiManager.getCheckInTaskInfo(taskId)) {
            is ApiResult.Success -> {
                rawTaskInfo.value = response.data
                val rootObject = StringUtils.parseJson(response.data)
                val dataObject = rootObject?.getAsJsonObjectOrNull("data")
                if (dataObject != null) {
                    // 兼容字符串形式的经纬度
                    var lat = dataObject.getDoubleOrDefault("locationLatitude", 0.0)
                    var lng = dataObject.getDoubleOrDefault("locationLongitude", 0.0)

                    // 如果外层没有拿到有效经纬度 尝试从内层 timer 对象获取
                    if (lat == 0.0 || lng == 0.0) {
                        val timerObject = dataObject.getAsJsonObjectOrNull("timer")
                        if (timerObject != null) {
                            lat = timerObject.getDoubleOrDefault("locationLatitude", 0.0)
                            lng = timerObject.getDoubleOrDefault("locationLongitude", 0.0)
                        }
                    }

                    // 如果依然没有 最后尝试从 content 字符串中解析
                    val contentStr = dataObject.getStringOrNull("content")
                    if ((lat == 0.0 || lng == 0.0) && contentStr != null) {
                        try {
                            val contentObject = StringUtils.parseJson(contentStr)
                            if (contentObject != null) {
                                val contentTimer = contentObject.getAsJsonObjectOrNull("timer")
                                if (contentTimer != null) {
                                    lat = contentTimer.getDoubleOrDefault("locationLatitude", 0.0)
                                    lng = contentTimer.getDoubleOrDefault("locationLongitude", 0.0)
                                } else {
                                    lat = contentObject.getDoubleOrDefault("locationLatitude", 0.0)
                                    lng = contentObject.getDoubleOrDefault("locationLongitude", 0.0)
                                }
                            }
                        } catch (e: Exception) { // 防止 content 字符串格式化失败导致崩溃
                            e.printStackTrace()
                        }
                    }

                    checkInState.value = checkInState.value.copy(
                        otherId = dataObject.getStringOrEmpty("otherId"),
                        ifNeedVCode = dataObject.getIntOrDefault("ifNeedVCode", 0),
                        openCheckFaceFlag = dataObject.getIntOrDefault("openCheckFaceFlag", 0),
                        starttime = dataObject.getLongOrDefault("starttime", 0L),
                        endTime = dataObject.getLongOrDefault("endTime", 0L),
                        signInId = dataObject.getLongOrDefault("signInId", 0L),
                        signOutId = dataObject.getLongOrDefault("signOutId", 0L),
                        signOutPublishTimeStamp = dataObject.getLongOrDefault(
                            "signOutPublishTimeStamp",
                            0L
                        ),
                        locationLatitude = lat,
                        locationLongitude = lng,
                        locationRange = dataObject.getDoubleOrDefault("locationRange", 0.0),
                        locationText = dataObject.getStringOrEmpty("locationText"),
                        ifphoto = dataObject.getIntOrDefault("ifphoto", 0),
                        ifopenAddress = dataObject.getIntOrDefault("ifopenAddress", 0),
                        ifrefreshewm = dataObject.getIntOrDefault("ifrefreshewm", 0),
                        numberCount = dataObject.getIntOrDefault("numberCount", 0)
                    )
                }
            }

            is ApiResult.Error -> {
                rawTaskInfo.value = "Error: ${response.message}"
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
                    },
                    actions = {
                        IconButton(onClick = { showInfoDialog.value = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "任务信息"
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

    if (showInfoDialog.value) {
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val formattedJson = remember(rawTaskInfo.value) {
            try {
                StringUtils.prettyGson.toJson(StringUtils.parseJson(rawTaskInfo.value))
            } catch (_: Exception) {
                rawTaskInfo.value
            }
        }

        AlertDialog(
            onDismissRequest = { showInfoDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog.value = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        val clipData = ClipData.newPlainText("Task Info", formattedJson)
                        clipboard.setClipEntry(ClipEntry(clipData))
                        "已复制到剪贴板".showToast()
                    }
                }) {
                    Text("复制")
                }
            },
            title = { Text("任务详情") },
            text = {
                val vScrollState = rememberScrollState()
                val hScrollState = rememberScrollState()

                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(vScrollState)
                            .horizontalScroll(hScrollState)
                    ) {
                        SelectionContainer {
                            Text(
                                text = formattedJson,
                                color = Color(0xFFD4D4D4),
                                style = MaterialTheme.typography.bodySmall,
                                softWrap = false // 禁用自动换行以支持横向滚动
                            )
                        }
                    }
                }
            }
        )
    }
}
