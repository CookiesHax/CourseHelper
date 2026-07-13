package com.cookieshax.coursehelper.feature.login

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// 二维码登录界面
@Composable
fun QRCodeLoginScreen(
    onSwitchToPassword: () -> Unit,
    onSwitchToCode: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isExpired by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var qrImageData by remember { mutableStateOf<ByteArray?>(null) }
    var qrData by remember { mutableStateOf<ApiManager.QRCodeData?>(null) }

    fun loadQRCode() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                when (val dataResult = ApiManager.getQRCodeData()) {
                    is ApiResult.Success -> {
                        qrData = dataResult.data
                        qrData?.uuid?.let { uuid ->
                            when (val imageResult = ApiManager.getQRCodeImage(uuid)) {
                                is ApiResult.Success -> {
                                    qrImageData = imageResult.data
                                    isExpired = false
                                }

                                is ApiResult.Error -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("二维码图片加载失败: ${imageResult.message}")
                                    }
                                }
                            }
                        }
                    }

                    is ApiResult.Error -> {
                        scope.launch {
                            snackbarHostState.showSnackbar("获取二维码参数失败: ${dataResult.message}")
                        }
                    }
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadQRCode()
    }

    LaunchedEffect(qrData, isExpired) {
        if (qrData != null && !isExpired) {
            var checkCount = 0
            val maxCheckCount = 30 // 每分钟检查 30 次
            while (checkCount < maxCheckCount) {
                delay(2000.milliseconds) // 每 2 秒检查一次
                checkCount++
                try {
                    when (val result = ApiManager.checkQRAuthStatus(qrData!!.uuid, qrData!!.enc)) {
                        is ApiResult.Success -> {
                            val responseMap = StringUtils.parseJson(result.data)
                            val status =
                                StringUtils.getBoolean(responseMap ?: JsonObject(), "status")
                            val type =
                                StringUtils.getString(responseMap ?: JsonObject(), "type", "")
                            val mes = StringUtils.getString(responseMap ?: JsonObject(), "mes", "")

                            if (status) {
                                handlePostLoginSuccess(snackbarHostState, context, onBack)
                                break // 登录成功退出循环
                            } else if (type == "2") {
                                isExpired = true
                                break // 过期退出循环
                            } else {
                                // 其他状态
                                Log.d("QRCode", "二维码状态: $mes, 类型: $type")
                            }
                        }

                        is ApiResult.Error -> {
                            // 如果是网络错误 可以继续尝试
                            Log.e("QRCode", "检查二维码状态失败: ${result.message}")
                            // 如果连续多次网络错误
                            if (checkCount >= maxCheckCount) {
                                Toast.makeText(context, "网络错误，请重试", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("QRCode", "检查二维码状态异常: ${e.message}")
                    Log.e("QRCode", "检查二维码状态异常: ${e.stackTraceToString()}")
                    // 继续循环 稍后重试
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Text(text = "扫码登录", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "请使用学习通APP扫码登录",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isRefreshing -> {
                        CircularProgressIndicator()
                    }

                    isExpired -> {
                        QRStatusOverlay(
                            message = "二维码已失效\n点击刷新",
                            onClick = { loadQRCode() }
                        )
                    }

                    qrImageData != null -> {
                        // 使用 Image 组件显示二维码图片
                        Log.d("QRCode", "Displaying QR code image with size: ${qrImageData?.size}")
                        Image(
                            bitmap = BitmapFactory.decodeByteArray(
                                qrImageData!!,
                                0,
                                qrImageData!!.size
                            )
                                .asImageBitmap(),
                            contentDescription = "二维码登录",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        QRStatusOverlay(
                            message = "加载失败\n点击刷新",
                            onClick = { loadQRCode() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { loadQRCode() },
                enabled = !isRefreshing
            ) {
                Text(if (isRefreshing) "刷新中..." else "刷新二维码")
            }

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSwitchToCode) { Text("验证码登录") }
                TextButton(onClick = onSwitchToPassword) { Text("密码登录") }
            }

            val direction = LocalLayoutDirection.current
            val multiple = 1.2f
            val multipliedPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(direction) * multiple,
                top = paddingValues.calculateTopPadding() * multiple,
                end = paddingValues.calculateEndPadding(direction) * multiple,
                bottom = paddingValues.calculateBottomPadding() * multiple
            )
            Box(modifier = Modifier.padding(multipliedPadding))
        }
    }
}
