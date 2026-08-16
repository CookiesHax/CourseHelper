package com.cookieshax.coursehelper.core.location

import android.content.Context
import android.location.LocationManager
import android.util.Log
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.cookieshax.coursehelper.app.CourseHelperApplication
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

    private val _locationUpdates = MutableStateFlow<LocationData?>(null)
    val locationUpdates: StateFlow<LocationData?> = _locationUpdates.asStateFlow()

    private val _isMockLocation = MutableStateFlow(false)
    val isMockLocationFlow: StateFlow<Boolean> = _isMockLocation.asStateFlow()

    private val locationClient: LocationClient by lazy {
        val context = CourseHelperApplication.context
        LocationClient(context).apply {
            registerLocationListener(locationListener)

            val option = LocationClientOption().apply {
                locationMode = LocationClientOption.LocationMode.Hight_Accuracy
                setCoorType("bd09ll")
                setScanSpan(3000)
                setIsNeedAddress(true)
                setIsNeedLocationPoiList(true)
            }

            locOption = option
        }
    }

    private val locationListener = object : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation?) {
            if (!isLocationStarted || location == null) return

            Log.d(TAG, "onReceiveLocation: $location")

            val lat = location.latitude
            val lng = location.longitude

            if (lat != 0.0 || lng != 0.0) {
                val newDirection = location.direction

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
        locationClient.start()
        isLocationStarted = true
        Log.d(TAG, "Location started")
    }

    private fun stopLocation() {
        if (isLocationStarted) {
            locationClient.stop()
            isLocationStarted = false
            Log.d(TAG, "Location stopped")
        }
    }

    private fun unregister() = synchronized(lock) {
        val count = --referenceCount
        Log.d(TAG, "Unregister location service, reference count: $count")

        if (count <= 0) {
            stopLocation()
            if (count < 0) {
                referenceCount = 0
            }
        }
    }
}
