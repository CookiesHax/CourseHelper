package com.cookieshax.coursehelper.feature.login

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookieshax.coursehelper.core.network.ApiResult
import kotlinx.coroutines.launch

@Composable
fun PasswordLoginScreen(
    viewModel: LoginViewModel,
    onSwitchToCode: () -> Unit,
    onSwitchToQR: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    var showNewDeviceDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Text(text = "密码登录", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { newText ->
                    // 数字
                    if (newText.isEmpty() || newText.all { it.isDigit() }) {
                        viewModel.setUsername(newText)
                    }
                },
                label = { Text("手机号 / 超星号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number, // 设置键盘为数字键盘
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    viewModel.setPassword(it)
                },
                label = { Text("密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { viewModel.setPasswordVisible(!passwordVisible) }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "显示密码"
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.loginByPassword(
                        onResult = { result ->
                            when (result) {
                                is ApiResult.Success -> {
                                    scope.launch {
                                        handlePostLoginSuccess(snackbarHostState, context, onBack)
                                    }
                                }

                                is ApiResult.Error -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(result.message)
                                    }
                                }
                            }
                        },
                        onNewDeviceRequired = {
                            showNewDeviceDialog = true
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank()
                        && password.length in 8 .. 16
                        && listOf(
                    password.any { it.isLetter() },
                    password.any { it.isDigit() },
                    password.any { !it.isLetterOrDigit() }
                ).count { it } >= 2
            ) {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSwitchToQR) {
                    Text("二维码登录")
                }
                TextButton(onClick = onSwitchToCode) {
                    Text("验证码登录")
                }
            }

            val direction = LocalLayoutDirection.current
            val multiple = 1.5f
            val multipliedPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(direction) * multiple,
                top = paddingValues.calculateTopPadding() * multiple,
                end = paddingValues.calculateEndPadding(direction) * multiple,
                bottom = paddingValues.calculateBottomPadding() * multiple
            )
            Box(modifier = Modifier.padding(multipliedPadding))
        }
    }

    // 新设备登录提示对话框
    if (showNewDeviceDialog) {
        AlertDialog(
            onDismissRequest = {
                // 点击遮罩层或返回键关闭弹窗
                showNewDeviceDialog = false
            },
            title = {
                Text("安全提示")
            },
            text = {
                Text("检测到您在新设备上登录，为了您的账号安全，需切换至验证码登录。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewDeviceDialog = false
                        onSwitchToCode()
                    }
                ) {
                    Text("去验证")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // 仅关闭弹窗
                        showNewDeviceDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}
