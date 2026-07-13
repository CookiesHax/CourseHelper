package com.cookieshax.coursehelper.feature.map

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MyLocationConfiguration
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.PoiInfo
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.ui.items.SearchInput
import com.cookieshax.coursehelper.ui.items.SearchTrigger
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    SharedTransitionLayout {
        val context = LocalContext.current
        val density = LocalDensity.current
        val locationUpdates by LocationService.locationUpdates.collectAsState()
        val isMockLocationFlow by LocationService.isMockLocationFlow.collectAsState()

        // 搜索状态
        var isSearching by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedPoi by remember { mutableStateOf<PoiInfo?>(null) }
        var longPressLocation by remember { mutableStateOf<LatLng?>(null) }
        val baiduMapRef = remember { mutableStateOf<BaiduMap?>(null) }

        // POI 搜索处理器
        val poiSearchHandler = rememberPoiSearchHandler()

        // 搜索防抖
        LaunchedSearchEffect(query = searchQuery, handler = poiSearchHandler)

        BackHandler(enabled = isSearching) {
            isSearching = false
            searchQuery = ""
        }

        // 获取权限状态
        val locationPermissionState =
            rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

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
                Toast.makeText(context, "请开启位置信息权限以使用定位功能", Toast.LENGTH_LONG)
                    .show()
            }
        }

        // 显示选中的 POI
        fun showPoiOnMap(poi: PoiInfo) {
            selectedPoi = poi
            isSearching = false
            searchQuery = ""
        }

        val statusBarHeightDp = with(density) {
            WindowInsets.systemBars.getTop(density).toDp()
        }

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val currentMarkerLocation = longPressLocation ?: selectedPoi?.location
                        if (currentMarkerLocation != null) {
                            LocationService.setMockLocation(
                                currentMarkerLocation.latitude,
                                currentMarkerLocation.longitude
                            )
                            longPressLocation = null
                            selectedPoi = null
                        } else if (isMockLocationFlow) {
                            LocationService.clearMockLocation()
                        } else {
                            locationUpdates?.let { location ->
                                val latLng = LatLng(location.latitude, location.longitude)
                                baiduMapRef.value?.animateMapStatus(
                                    MapStatusUpdateFactory.newLatLngZoom(latLng, 17f)
                                )
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = if (isMockLocationFlow) {
                        MaterialTheme.colorScheme.error // 红色表示正在模拟位置
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "定位"
                    )
                }
            },
            topBar = {
                AnimatedContent(
                    targetState = isSearching,
                    transitionSpec = {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    },
                    label = "map_search_transition"
                ) { searching ->
                    if (searching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = statusBarHeightDp + 8.dp,
                                    start = 8.dp,
                                    end = 8.dp,
                                    bottom = 8.dp
                                )
                        ) {
                            SearchInput(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onClose = {
                                    isSearching = false
                                    searchQuery = ""
                                },
                                hint = "搜索地点...",
                                animatedVisibilityScope = this@AnimatedContent
                            )
                        }
                    } else {
                        TopAppBar(
                            title = { Text("位置模拟") },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            },
                            actions = {
                                SearchTrigger(
                                    onClick = { isSearching = true },
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = modifier) {
                MapViewContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    locationUpdates = locationUpdates,
                    selectedPoi = selectedPoi,
                    longPressLocation = longPressLocation,
                    onLongPress = { point ->
                        longPressLocation = point
                        selectedPoi = null
                    },
                    onMapViewCreated = { baiduMap ->
                        baiduMapRef.value = baiduMap
                        baiduMap.isMyLocationEnabled = true
                        baiduMap.setMyLocationConfiguration(
                            MyLocationConfiguration(
                                MyLocationConfiguration.LocationMode.FOLLOWING,
                                true,
                                null
                            )
                        )
                    }
                )

                // 搜索结果列表
                if (isSearching && poiSearchHandler.results.isNotEmpty()) {
                    val searchResultsTopOffset = statusBarHeightDp + 64.dp + 16.dp
                    MapSearchResults(
                        results = poiSearchHandler.results,
                        onPoiSelected = { poi -> showPoiOnMap(poi) },
                        topOffset = searchResultsTopOffset
                    )
                }
            }
        }
    }
}
