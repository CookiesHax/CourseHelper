package com.cookieshax.coursehelper.feature.login

import android.content.Context
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.network.CookieManager
import com.cookieshax.coursehelper.core.utils.showToast

@Keep
@Serializable
enum class LoginType {
    PASSWORD, VERIFICATION_CODE, QRCODE
}

suspend fun handlePostLoginSuccess(
    snackbarHostState: SnackbarHostState,
    context: Context,
    onBack: () -> Unit
): Boolean {
    // 在登录成功后 Cookie 已经存储在上下文 CookieJar 中
    // 调用 refreshAccount(null) 以使用上下文 Cookie 获取并保存用户信息
    return when (val result = AccountRepository.refreshAccount(null)) {
        is ApiResult.Success -> {
            val newAccount = result.data
            // 将登录 Cookie 从临时 Jar 转移到对应 UID 的持久化存储
            CookieManager.transferLoginCookiesToUser(newAccount.uid, context)
            AccountRepository.switchActiveAccount(newAccount.uid)

            "「${newAccount.name}」登录成功".showToast()
            onBack()
            true
        }

        is ApiResult.Error -> {
            snackbarHostState.showSnackbar(result.message)
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    initialLoginType: LoginType = LoginType.PASSWORD,
    viewModel: LoginViewModel = viewModel()
) {
    // 初始化登录模式
    LaunchedEffect(initialLoginType) {
        viewModel.setLoginType(initialLoginType)
    }

    // 当前登录模式
    val currentType by viewModel.currentType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // 根据类型显示不同内容
            when (currentType) {
                LoginType.PASSWORD -> PasswordLoginScreen(
                    viewModel = viewModel,
                    onSwitchToCode = { viewModel.setLoginType(LoginType.VERIFICATION_CODE) },
                    onSwitchToQR = { viewModel.setLoginType(LoginType.QRCODE) },
                    onBack = onBack
                )

                LoginType.VERIFICATION_CODE -> VerificationCodeLoginScreen(
                    viewModel = viewModel,
                    onSwitchToPassword = { viewModel.setLoginType(LoginType.PASSWORD) },
                    onSwitchToQR = { viewModel.setLoginType(LoginType.QRCODE) },
                    onBack = onBack
                )

                LoginType.QRCODE -> QRCodeLoginScreen(
                    onSwitchToPassword = { viewModel.setLoginType(LoginType.PASSWORD) },
                    onSwitchToCode = { viewModel.setLoginType(LoginType.VERIFICATION_CODE) },
                    onBack = onBack
                )
            }
        }
    }
}
