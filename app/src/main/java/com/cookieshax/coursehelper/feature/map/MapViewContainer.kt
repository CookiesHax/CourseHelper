package com.cookieshax.coursehelper.feature.map

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.PoiInfo
import com.cookieshax.coursehelper.core.location.LocationService

@Composable
fun MapViewContainer(
    modifier: Modifier = Modifier,
    locationUpdates: LocationService.LocationData?,
    selectedPoi: PoiInfo? = null,
    longPressLocation: LatLng? = null,
    onLongPress: (LatLng) -> Unit = {},
    onMapViewCreated: (BaiduMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isSdkInitialized by remember {
        mutableStateOf(SDKInitializer.isInitialized())
    }
    if (!isSdkInitialized) {
        SDKInitializer.setAgreePrivacy(context.applicationContext, true)
        SDKInitializer.initialize(context.applicationContext)
        isSdkInitialized = true
    }

    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            showZoomControls(false)
        }
    }

    val baiduMap = remember { mapView.map }
    var isMapInitialized by remember { mutableStateOf(false) }

    // 生命周期管理
    val lifecycle = lifecycleOwner.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // 地图初始化
    LaunchedEffect(baiduMap) {
        onMapViewCreated(baiduMap)
        isMapInitialized = true
        baiduMap.setOnMapLongClickListener { point ->
            onLongPress(point)
        }
    }

    // 位置更新
    LaunchedEffect(locationUpdates, isMapInitialized) {
        if (isMapInitialized) {
            locationUpdates?.let { location ->
                val locationData = MyLocationData.Builder()
                    .latitude(location.latitude)
                    .longitude(location.longitude)
                    .direction(location.direction)
                    .build()
                baiduMap.setMyLocationData(locationData)
            }
        }
    }

    // 选中的POI
    LaunchedEffect(selectedPoi) {
        selectedPoi?.let { poi ->
            val location = poi.location
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                baiduMap.clear()
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation))
                baiduMap.addOverlay(markerOptions)
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(latLng, 17f))
            }
        } ?: run {
            baiduMap.clear()
        }
    }

    // 长按位置更新
    LaunchedEffect(longPressLocation) {
        longPressLocation?.let { latLng ->
            baiduMap.clear()
            val markerOptions = MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation))
            baiduMap.addOverlay(markerOptions)
        } ?: run {
            baiduMap.clear()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}
