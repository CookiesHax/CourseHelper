package com.cookieshax.coursehelper.core.location

import android.os.Handler
import android.os.Looper
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.geocode.GeoCodeResult
import com.baidu.mapapi.search.geocode.GeoCoder
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult
import com.cookieshax.coursehelper.app.CourseHelperApplication
import kotlinx.coroutines.suspendCancellableCoroutine

object GeoCodeService {
    private val geoCoderInitializer: Unit by lazy {
        if (!SDKInitializer.isInitialized()) {
            val context = CourseHelperApplication.context.applicationContext
            SDKInitializer.setAgreePrivacy(context, true)
            SDKInitializer.initialize(context)
        }
    }

    fun reverseGeoCode(
        latitude: Double,
        longitude: Double,
        callback: (ReverseGeoCodeResult?) -> Unit
    ): () -> Unit {
        geoCoderInitializer
        val handler = Handler(Looper.getMainLooper())

        var currentClient: GeoCoder? = null
        var retryRunnable: Runnable? = null
        var isCancelled = false

        fun runExecution(retryCount: Int) {
            if (isCancelled) return

            val client = GeoCoder.newInstance()
            currentClient = client

            client.setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
                override fun onGetReverseGeoCodeResult(result: ReverseGeoCodeResult?) {
                    if (isCancelled) {
                        client.destroy()
                        return
                    }
                    if ((result == null || result.address == null) && retryCount < 5) {
                        client.destroy()
                        val runnable = Runnable {
                            runExecution(retryCount + 1)
                        }
                        retryRunnable = runnable
                        handler.postDelayed(runnable, 200)
                    } else {
                        callback.invoke(result)
                        client.destroy()
                    }
                }

                override fun onGetGeoCodeResult(result: GeoCodeResult?) {}
            })

            val point = LatLng(latitude, longitude)
            val option = ReverseGeoCodeOption().location(point)
            client.reverseGeoCode(option)
        }

        runExecution(0)

        return {
            isCancelled = true
            retryRunnable?.let { handler.removeCallbacks(it) }
            currentClient?.destroy()
        }
    }

    suspend fun reverseGeoCodeSuspend(latitude: Double, longitude: Double): ReverseGeoCodeResult? {
        return suspendCancellableCoroutine { continuation ->
            val cancelAction = reverseGeoCode(latitude, longitude) { result ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.success(result))
                }
            }

            continuation.invokeOnCancellation {
                cancelAction.invoke()
            }
        }
    }
}
