package com.cookieshax.coursehelper.feature.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookieshax.coursehelper.core.network.ApiResult
import kotlinx.coroutines.launch

@Composable
fun VerificationCodeLoginScreen(
    viewModel: LoginViewModel,
    onSwitchToPassword: () -> Unit,
    onSwitchToQR: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val username by viewModel.username.collectAsState()
    val code by viewModel.verificationCode.collectAsState()
    val countdown by viewModel.codeCountdown.collectAsState()

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
            Text(text = "验证码登录", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { newText ->
                    // 数字
                    if (newText.isEmpty() || newText.all { it.isDigit() }) {
                        viewModel.setUsername(newText)
                    }
                },
                label = { Text("手机号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top // 垂直居中对齐
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { viewModel.setVerificationCode(it) },
                    label = { Text("验证码") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.sendVerificationCode { success, message ->
                            if (success) {
                                Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, message ?: "发送失败", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    enabled = countdown == 0 && username.length == 11,
                    modifier = Modifier
                        .fillMaxHeight() // 让按钮填满父容器高度
                        .width(110.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (countdown > 0) "${countdown}s" else "发送")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.loginByVerificationCode { result ->
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
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.length == 11 && code.length == 6
            ) {
                Text("登录")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onSwitchToQR() }) {
                    Text("二维码登录")
                }
                TextButton(onClick = { onSwitchToPassword() }) {
                    Text("密码登录")
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
}

