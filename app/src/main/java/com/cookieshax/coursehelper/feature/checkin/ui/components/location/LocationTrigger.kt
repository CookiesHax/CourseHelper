package com.cookieshax.coursehelper.feature.checkin.ui.components.location

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.location.GeoCodeService
import com.cookieshax.coursehelper.core.location.LocationService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.rememberCoroutineScope
import com.cookieshax.coursehelper.feature.checkin.model.CheckInParams
import com.cookieshax.coursehelper.feature.checkin.model.LocationCheckInStrategy
import com.cookieshax.coursehelper.feature.checkin.ui.CheckInState
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationTrigger(
    url: String,
    taskId: String,
    courseId: String,
    checkInState: CheckInState,
    isNeedCaptcha: Boolean,
    viewModel: CheckInViewModel,
    semaphoreLimit: Int = 6
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isCheckingIn by viewModel.isCheckingIn.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isMockLocation by LocationService.isMockLocationFlow.collectAsState()

    var locationLatitude by remember {
        mutableStateOf(checkInState.locationLatitude.takeIf {
            abs(
                it
            ) > 1E-6
        })
    }
    var locationLongitude by remember {
        mutableStateOf(checkInState.locationLongitude.takeIf {
            abs(
                it
            ) > 1E-6
        })
    }
    var locationText by remember { mutableStateOf(checkInState.locationText) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            val locationHandle = LocationService.register()
            onDispose {
                locationHandle.dispose()
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(Unit) {
        if (!LocationService.isLocationEnabled(context)) {
            Toast.makeText(context, "请开启位置信息权限以使用定位功能", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCheckingIn && selectedIds.isNotEmpty(),
                onClick = {
                    scope.launch {
                        viewModel.setCheckingIn(true)
                        try {
                            // Location fetching logic
                            if (isMockLocation || locationLongitude == null || locationLatitude == null) {
                                try {
                                    val loc = LocationService.getCurrentLocation()
                                    locationLongitude = loc.longitude
                                    locationLatitude = loc.latitude
                                    locationText = ""
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            val currentLat = locationLatitude
                            val currentLng = locationLongitude

                            if (currentLat == null || currentLng == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "获取位置信息失败，请重试或手动模拟位置",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                return@launch
                            }

                            if (locationText.isBlank()) {
                                val result = withTimeoutOrNull(5000.milliseconds) {
                                    GeoCodeService.reverseGeoCodeSuspend(
                                        currentLat,
                                        currentLng
                                    )
                                }

                                val address = result?.address
                                if (!address.isNullOrBlank()) {
                                    locationText = address
                                }
                            }

                            viewModel.performCheckIn(
                                strategy = LocationCheckInStrategy(),
                                params = CheckInParams.Location(
                                    url = url,
                                    taskId = taskId,
                                    courseId = courseId,
                                    latitude = currentLat,
                                    longitude = currentLng,
                                    address = locationText,
                                    isNeedFaceCheck = checkInState.openCheckFaceFlag == 1
                                ),
                                isNeedCaptcha = isNeedCaptcha,
                                semaphoreLimit = semaphoreLimit
                            )
                        } finally {
                            viewModel.setCheckingIn(false)
                        }
                    }
                }
            ) {
                if (isCheckingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "对选中账号签到")
                }
            }
        }
    }
}
