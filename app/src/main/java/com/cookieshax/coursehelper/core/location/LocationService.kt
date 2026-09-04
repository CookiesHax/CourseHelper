package com.cookieshax.coursehelper.core.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.cookieshax.coursehelper.core.utils.showToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface DisposableHandle {
    fun dispose()
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val direction: Float
)

object LocationService {
    private const val TAG = "LocationService"

    private val lock = Any()
    private var referenceCount = 0

    @Volatile
    private var isLocationStarted = false

    @Volatile
    private var isNativeStarted = false

    @Volatile
    private var latitude = 0.0

    @Volatile
    private var longitude = 0.0

    @Volatile
    private var direction = 0.0f

    @Volatile
    private var isMock = false

    @Volatile
    private var mockLatitude = 0.0

    @Volatile
    private var mockLongitude = 0.0

    @Volatile
    private var currentLocationMethod = LocationMethod.BAIDU

    private val _locationUpdates = MutableStateFlow<LocationData?>(null)
    val locationUpdates: StateFlow<LocationData?> = _locationUpdates.asStateFlow()

    private val _isMockLocation = MutableStateFlow(false)
    val isMockLocationFlow: StateFlow<Boolean> = _isMockLocation.asStateFlow()

    private val nativeLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!isLocationStarted) return
            Log.d(TAG, "Native location update: ${location.latitude}, ${location.longitude}")

            // 转换坐标 WGS84 -> BD09LL
            val (bdLng, bdLat) = CoordinateUtil.wgs84ToBd09(location.longitude, location.latitude)
            Log.d(TAG, "Converted to BD09LL: $bdLat, $bdLng")
            updateLocation(bdLat, bdLng, location.bearing)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val bdLocationClient: LocationClient by lazy {
        val context = CourseHelperApplication.context
        LocationClient(context).apply {
            registerLocationListener(bdLocationListener)

            val option = LocationClientOption().apply {
                locationMode = LocationClientOption.LocationMode.Hight_Accuracy
                setCoorType("bd09ll")
                setScanSpan(5000)
                setIsNeedAddress(true)
                setIsNeedLocationPoiList(true)
            }

            locOption = option
        }
    }

    private val bdLocationListener = object : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation?) {
            if (!isLocationStarted || location == null) return

            val locType = location.locType
            val locDescription = location.locTypeDescription
            Log.d(TAG, "onReceiveLocation type: $locType ($locDescription)")

            // 61 - GPS成功, 161 - 网络定位成功, 66 - 离线定位成功
            val isSuccess = locType == 61 || locType == 161 || locType == 66

            if (isSuccess) {
                val lat = location.latitude
                val lng = location.longitude
                Log.d(TAG, "Baidu location update: $lat, $lng")

                // 百度 SDK 在未授权或失败时可能返回 4.9E-324 (Double.MIN_VALUE) 或 0.0
                if (lat != 0.0 && lng != 0.0 && lat != Double.MIN_VALUE && lng != Double.MIN_VALUE) {
                    updateLocation(lat, lng, location.direction)
                }
            } else {
                Log.w(TAG, "Baidu Location abnormal (type $locType), starting native fallback")
                startNativeLocation()
            }
        }

        override fun onLocDiagnosticMessage(
            locType: Int,
            diagnosticType: Int,
            diagnosticMessage: String?
        ) {
            Log.d(
                TAG,
                "onLocDiagnosticMessage: locType=$locType, diagnosticType=$diagnosticType, message=$diagnosticMessage"
            )
        }
    }

    private fun updateLocation(lat: Double, lng: Double, newDirection: Float) {
        latitude = lat
        longitude = lng
        direction = newDirection

        if (!isMock) {
            _locationUpdates.value = LocationData(
                latitude = lat,
                longitude = lng,
                direction = newDirection
            )
        }
    }

    fun register(): DisposableHandle {
        // 只在首次注册且定位未启动时启动定位
        synchronized(lock) {
            val count = ++referenceCount
            Log.d(TAG, "Register location service, reference count: $count")

            if (count == 1 && !isLocationStarted) {
                startLocation()
            }
        }

        return DisposableHandle { unregister() }
    }

    fun setLocationMethod(method: LocationMethod) {
        if (currentLocationMethod == method) return
        Log.d(TAG, "Setting location method to $method")
        synchronized(lock) {
            currentLocationMethod = method

            // 如果已经启动定位 先停止
            if (isLocationStarted) {
                stopLocation()
            }

            if (!isLocationEnabled(CourseHelperApplication.context)) {
                "请开启位置信息权限以使用定位功能".showToast(Toast.LENGTH_LONG)
            }

            // 当拥有引用或始终运行时 重新启动定位
            if (referenceCount > 0 || method == LocationMethod.GPS_ALWAYS) {
                startLocation()
            }
        }
    }

    fun setMockLocation(latitude: Double, longitude: Double) {
        isMock = true
        mockLatitude = latitude
        mockLongitude = longitude

        _isMockLocation.value = true
        _locationUpdates.value = LocationData(
            latitude = latitude,
            longitude = longitude,
            direction = 0f
        )
    }

    fun clearMockLocation() {
        isMock = false
        mockLatitude = 0.0
        mockLongitude = 0.0

        _isMockLocation.value = false

        if (latitude != 0.0 || longitude != 0.0) {
            _locationUpdates.value = LocationData(
                latitude = latitude,
                longitude = longitude,
                direction = direction
            )
        }
    }

    fun getCurrentLocation(): LocationData {
        return if (isMock) {
            LocationData(
                latitude = mockLatitude,
                longitude = mockLongitude,
                direction = 0f
            )
        } else {
            LocationData(
                latitude = latitude,
                longitude = longitude,
                direction = direction
            )
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun startLocation() {
        Log.d(TAG, "Starting location service with method: $currentLocationMethod")
        when (currentLocationMethod) {
            LocationMethod.BAIDU -> {
                try {
                    bdLocationClient.start()
                    Log.d(TAG, "Baidu Location started")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start Baidu Location: ${e.message}")
                    startNativeLocation()
                }
            }

            LocationMethod.GPS_ONLY, LocationMethod.GPS_ALWAYS -> {
                startNativeLocation()
            }
        }
        isLocationStarted = true
    }

    private fun startNativeLocation() {
        synchronized(lock) {
            if (isNativeStarted) return
            val context = CourseHelperApplication.context
            Log.d(TAG, "Starting Native Location (Method: $currentLocationMethod)...")
            try {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val mainLooper = Looper.getMainLooper()

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L,
                        0f,
                        nativeLocationListener,
                        mainLooper
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        0f,
                        nativeLocationListener,
                        mainLooper
                    )
                }
                isNativeStarted = true
                Log.d(TAG, "Native Location started successfully")
            } catch (e: SecurityException) {
                Log.e(TAG, "Native location permission missing: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start native location: ${e.message}")
            }
        }
    }

    private fun stopLocation() {
        if (isLocationStarted) {
            // 停止百度
            try {
                bdLocationClient.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop Baidu Location: ${e.message}")
            }

            // 停止原生
            stopNativeLocation()

            isLocationStarted = false
            Log.d(TAG, "Location stopped")
        }
    }

    private fun stopNativeLocation() {
        synchronized(lock) {
            if (isNativeStarted) {
                val context = CourseHelperApplication.context
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager.removeUpdates(nativeLocationListener)
                isNativeStarted = false
                Log.d(TAG, "Native Location stopped")
            }
        }
    }

    private fun unregister() = synchronized(lock) {
        val count = --referenceCount
        Log.d(TAG, "Unregister location service, reference count: $count")

        if (count <= 0) {
            if (currentLocationMethod != LocationMethod.GPS_ALWAYS) {
                stopLocation()
            }
            if (count < 0) {
                referenceCount = 0
            }
        }
    }
}
