package com.cookieshax.coursehelper.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.CameraRoute
import com.cookieshax.coursehelper.app.navigation.CheckInRoute
import com.cookieshax.coursehelper.app.navigation.WebViewRoute
import com.cookieshax.coursehelper.core.permission.PermissionManager
import com.cookieshax.coursehelper.core.utils.showToast
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

const val EXTRA_IMAGE_URI = "image_uri"

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: CameraViewModel = viewModel()
    val repository = remember {
        CameraRepository(
            onZoomSuggestion = { ratio ->
                viewModel.requestZoom(ratio)
                true
            }
        )
    }

    // 获取权限状态
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // 监听扫码结果
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = viewModel()
    val preferOkHttpOverWebView by settingsViewModel.preferOkHttpOverWebView.collectAsStateWithLifecycle()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // 通常认为屏幕最短边大于等于 600dp 的设备为平板
    val isTablet = configuration.smallestScreenWidthDp >= 600

    // 请求权限
    LaunchedEffect(Unit) {
        if (!PermissionManager.hasPermission(context, Manifest.permission.CAMERA)) {
            PermissionManager.requestPermissions(context, arrayOf(Manifest.permission.CAMERA)) { }
        }
    }

    // 更新权限状态到 ViewModel
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.setCameraPermission(cameraPermissionState.status.isGranted)
    }

    // 处理扫码结果
    LaunchedEffect(scanResult) {
        val result = scanResult ?: return@LaunchedEffect

        if (!result.startsWith("http://") && !result.startsWith("https://")) {
            "扫描结果: $result".showToast(Toast.LENGTH_LONG)
            viewModel.clearScanResult()
            return@LaunchedEffect
        }

        val previousRoute = navController.previousBackStackEntry?.destination?.route

        if (result.contains("chaoxing") && result.contains("sign/") && preferOkHttpOverWebView) {
            if (previousRoute?.contains("CheckInRoute") == true) {
                // 回传给上一个签到界面
                navController.previousBackStackEntry?.savedStateHandle?.set("scan_result", result)
                navController.popBackStack()
            } else {
                // 跳转到新签到界面
                val taskId = result.toUri().getQueryParameter("id")
                if (taskId == null) {
                    "缺少签到任务ID".showToast(Toast.LENGTH_LONG)
                    viewModel.clearScanResult()
                    return@LaunchedEffect
                } else {
                    navController.navigate(CheckInRoute(url = result, taskId = taskId)) {
                        popUpTo<CameraRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            viewModel.clearScanResult()
            return@LaunchedEffect
        }

        if (previousRoute?.contains("WebViewRoute") == true) {
            navController.previousBackStackEntry?.savedStateHandle?.set("SHOULD_LOAD_URL", result)
            navController.popBackStack()
        } else {
            navController.navigate(WebViewRoute(result)) {
                popUpTo<CameraRoute> { inclusive = true }
                launchSingleTop = true
            }
        }
        viewModel.clearScanResult()
    }

    DisposableEffect(Unit) {
        onDispose {
            repository.close()
        }
    }

    // 相册选择器的 Launcher
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                repository.processImageUri(
                    context = context,
                    uri = it,
                    onSuccess = { text ->
                        viewModel.handleScanResult(text)
                    },
                    onError = { e ->
                        Log.e("CameraScreen", "相册扫码失败", e)
                        "扫码失败".showToast()
                    }
                )
            }
        }

    // 渲染主体 UI
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = {
            if (!(isLandscape && !isTablet)) {
                TopAppBar(
                    title = {
                        Text("扫码")
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { galleryLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "从相册选择"
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(
                    viewModel = viewModel,
                    repository = repository
                )

                QrScannerOverlay()
            } else {
                Surface(
                    color = Color.Black,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
        }
    }
}
