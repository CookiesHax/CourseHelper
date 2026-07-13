package com.cookieshax.coursehelper.feature.webview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.core.info.ChaoXingAppInfo
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.core.permission.PermissionManager
import com.cookieshax.coursehelper.core.utils.FileUtils
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.camera.EXTRA_IMAGE_URI
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WebViewScreen(
    url: String,
    onBackPressed: () -> Unit,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel = viewModel<WebViewViewModel>()
    val isLoading = remember { mutableStateOf(true) }
    val isDarkTheme = isSystemInDarkTheme()

    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var tempPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        filePathCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        filePathCallback = null
    }

    val bridgeImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bridge = viewModel.jsBridge
            if (bridge != null) {
                scope.launch {
                    CameraResultHandler.handleImageResult(context, uri.toString(), bridge)
                }
            }
        }
    }

    val bridgeCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d("WebViewDebug", "bridgeCameraLauncher success: $success, path: $tempPhotoPath")
        if (success) {
            tempPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Log.d("WebViewDebug", "File exists, size: ${file.length()}")
                    val bridge = viewModel.jsBridge
                    if (bridge != null) {
                        scope.launch {
                            CameraResultHandler.handleImageFile(file, bridge)
                        }
                    }
                } else {
                    Log.e("WebViewDebug", "File does not exist at path: $path")
                }
            }
        }
        tempPhotoPath = null
    }

    // 获取权限状态
    val locationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // 获取当前页面标题
    val pageTitle = viewModel.pageTitle.value

    val fixLayoutJs = WebViewConfigurator.loadFixLayoutJs(context)

    val currentBackStackEntry = navController?.currentBackStackEntry
    val currentEntry = navController?.currentBackStackEntry
    val cameraScanUrl = currentEntry?.savedStateHandle
        ?.getStateFlow<String?>("SHOULD_LOAD_URL", null)
        ?.collectAsState()?.value ?: ""

    // 同步初始化 WebView 确保在 AndroidView factory 前完成
    if (viewModel.webView == null) {
        viewModel.initializeWebView(context, isDarkTheme)
    }

    // 生命周期管理 - 只清理 LocationService
    // WebView 由 ViewModel 管理 在 ViewModel 销毁时清理
    DisposableEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            val locationHandle = LocationService.register()
            onDispose {
                Log.d("WebViewDebug", "WebViewScreen OnDispose - LocationService cleanup")
                locationHandle.dispose()
                // WebView 由 ViewModel 管理
            }
        } else {
            onDispose {}
        }
    }

    // 获取权限状态
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (!PermissionManager.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (!locationPermissionState.status.isGranted) {
                locationPermissionState.launchPermissionRequest()
            }
        }

        if (!LocationService.isLocationEnabled(context)) {
            Toast.makeText(context, "请开启位置信息权限以使用定位功能", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!LocationService.isLocationEnabled(context)) {
            Toast.makeText(context, "请开启位置信息权限以使用定位功能", Toast.LENGTH_LONG)
                .show()
        }
    }

    // 监听传入的 url 参数变化
    LaunchedEffect(url) {
        viewModel.updateCurrentUrl(url)
    }

    // 加载 URL 和 Cookie - 只有在 URL 真正变化时才重新加载
    LaunchedEffect(viewModel.currentUrl.value) {
        // 如果 WebView 已经加载了这个 URL 就不需要重新加载了
        if (viewModel.hasLoadedInitially.value && viewModel.webViewCurrentUrl.value == viewModel.currentUrl.value) {
            return@LaunchedEffect
        }

        val currentUserId = AccountRepository.activeAccountIdFlow.value
        if (currentUserId != null) {
            WebViewCookieManager.loadAndSetCookies(viewModel.currentUrl.value, currentUserId)
        }

        // 只在首次加载时清除缓存
        if (!viewModel.hasLoadedInitially.value) {
            viewModel.clearCacheAndStorage()
        }

        // 在主线程加载 URL
        val headers = mapOf(
            "X-Requested-With" to ChaoXingAppInfo.PACKAGE_NAME,
            "X-App-Version" to ChaoXingAppInfo.VERSION
        )
        viewModel.loadUrl(viewModel.currentUrl.value, headers)
    }

    LaunchedEffect(currentBackStackEntry) {
        Log.d("WebViewDebug", "LaunchedEffect for camera result triggered")
        val imageUriString = currentBackStackEntry?.savedStateHandle?.get<String>(EXTRA_IMAGE_URI)
        Log.d("WebViewDebug", "imageUriString: $imageUriString")
        if (imageUriString != null) {
            currentBackStackEntry.savedStateHandle.remove<String>(EXTRA_IMAGE_URI)

            val bridge = viewModel.jsBridge
            if (bridge != null) {
                scope.launch {
                    CameraResultHandler.handleImageResult(
                        context,
                        imageUriString,
                        bridge
                    )
                }
            }
        }
        // 从相机返回停止加载动画
        Log.d("WebViewDebug", "Setting isLoading to false after camera return")
        isLoading.value = false
    }

    LaunchedEffect(cameraScanUrl) {
        if (cameraScanUrl.isNotEmpty()) {
            currentEntry?.savedStateHandle?.remove<String>("SHOULD_LOAD_URL")
            viewModel.loadUrl(cameraScanUrl)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pageTitle ?: "加载中...") },
                navigationIcon = {
                    IconButton(onClick = {
                        // 用户点击返回 整个 WebView 屏幕关闭
                        onBackPressed()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    viewModel.webView!!.apply {
                        viewModel.createJsBridge { notificationName, paramsJson ->
                            val bridge = viewModel.jsBridge ?: return@createJsBridge
                            val handlers = JsBridgeHandlers(
                                jsBridgeInterface = bridge,
                                navController = navController,
                                scope = scope,
                                onOpenUrl = { newUrl -> viewModel.updateCurrentUrl(newUrl) },
                                onCloseWebView = onBackPressed,
                                onChooseImage = {
                                    showImageSourceDialog = true
                                }
                            )
                            handlers.handle(notificationName, paramsJson)
                        }
                        webViewClient = WebViewConfigurator.createWebViewClient(
                            isLoading,
                            fixLayoutJs
                        ) { title ->
                            viewModel.pageTitle.value = title
                        }
                        webChromeClient = WebViewConfigurator.createWebChromeClient { callback, _ ->
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            fileChooserLauncher.launch("image/*")
                            true
                        }
                    }
                },
                update = {},
                onRelease = { view ->
                    view.stopLoading()
                }
            )

            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("选择图片来源") },
            text = { Text("拍照上传或从相册选取图片") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    try {
                        val storageDir = context.externalCacheDir ?: context.cacheDir
                        val file =
                            File(storageDir, "bridge_capture_${System.currentTimeMillis()}.jpg")
                        tempPhotoPath = file.absolutePath
                        val uri = FileUtils.getFileUri(context, file)

                        // 授权
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        val resInfoList = context.packageManager.queryIntentActivities(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                        for (resolveInfo in resInfoList) {
                            context.grantUriPermission(
                                resolveInfo.activityInfo.packageName,
                                uri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        bridgeCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        Log.e("WebViewDebug", "Error launching camera", e)
                    }
                }) {
                    Text("拍照")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    bridgeImagePickerLauncher.launch("image/*")
                }) {
                    Text("相册")
                }
            }
        )
    }
}
