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
import java.util.concurrent.atomic.AtomicInteger

interface DisposableHandle {
    fun dispose()
}

object LocationService {
    private var isLocationStarted = false

    private val locationClient: LocationClient by lazy {
        val context = CourseHelperApplication.context.applicationContext
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

    private var latitude = 0.0
    private var longitude = 0.0
    private var direction = 0.0f
    private var isMock = false
    private var mockLatitude = 0.0
    private var mockLongitude = 0.0

    private val referenceCount = AtomicInteger(0)
    private val lock = Any()

    private val _locationUpdates = MutableStateFlow<LocationData?>(null)
    val locationUpdates: StateFlow<LocationData?> = _locationUpdates

    private val _isMockLocation = MutableStateFlow(false)
    val isMockLocationFlow: StateFlow<Boolean> = _isMockLocation

    private val locationListener = object : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation?) {
            if (!isLocationStarted) return
            if (location == null) return
            Log.d("LocationService", "onReceiveLocation: $location")
            location.let {
                if (it.latitude != 0.0 || it.longitude != 0.0) {
                    val newLatitude = it.latitude
                    val newLongitude = it.longitude
                    val newDirection = it.direction

                    latitude = newLatitude
                    longitude = newLongitude
                    direction = newDirection

                    if (!isMock) {
                        _locationUpdates.value = LocationData(
                            latitude = newLatitude,
                            longitude = newLongitude,
                            direction = newDirection
                        )
                    }
                }
            }
        }
    }

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val direction: Float
    )

    private class LocationDisposableHandle(private val disposeAction: () -> Unit) :
        DisposableHandle {
        override fun dispose() {
            disposeAction()
        }
    }

    private fun startLocation() {
        synchronized(lock) {
            locationClient.start()
            isLocationStarted = true
            Log.d("LocationService", "Location started")
        }
    }

    private fun stopLocation() {
        synchronized(lock) {
            if (isLocationStarted) {
                locationClient.stop()
                isLocationStarted = false
                Log.d("LocationService", "Location stopped")
            }
        }
    }

    fun register(): DisposableHandle {
        // 只在首次注册且定位未启动时启动定位
        synchronized(lock) {
            val count = referenceCount.incrementAndGet()
            Log.d("LocationService", "Register location service, reference count: $count")

            if (count == 1 && !isLocationStarted) {
                startLocation()
            }
        }

        return LocationDisposableHandle { unregister() }
    }

    private fun unregister() {
        synchronized(lock) {
            val count = referenceCount.decrementAndGet()
            Log.d("LocationService", "Unregister location service, reference count: $count")

            if (count <= 0) {
                stopLocation()
                if (count < 0) {
                    referenceCount.set(0)
                }
            }
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun setMockLocation(latitude: Double, longitude: Double) {
        _locationUpdates.value = LocationData(
            latitude = latitude,
            longitude = longitude,
            direction = 0f
        )

        isMock = true
        mockLatitude = latitude
        mockLongitude = longitude

        _isMockLocation.value = true
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
}
