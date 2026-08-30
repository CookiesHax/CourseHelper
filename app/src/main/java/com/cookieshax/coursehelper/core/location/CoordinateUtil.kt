package com.cookieshax.coursehelper.core.location

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

object CoordinateUtil {
    private const val X_PI = 3.141592653589793 * 3000.0 / 180.0
    private const val PI = 3.141592653589793
    private const val A = 6378245.0
    private const val EE = 0.006693421622965943

    fun wgs84ToBd09(lng: Double, lat: Double): Pair<Double, Double> {
        val gcj = wgs84ToGcj02(lng, lat)
        return gcj02ToBd09(gcj.first, gcj.second)
    }

    private fun wgs84ToGcj02(lng: Double, lat: Double): Pair<Double, Double> {
        if (outOfChina(lng, lat)) return Pair(lng, lat)
        var dlat = transformLat(lng - 105.0, lat - 35.0)
        var dlng = transformLng(lng - 105.0, lat - 35.0)
        val radlat = lat / 180.0 * PI
        var magic = sin(radlat)
        magic = 1 - EE * magic * magic
        val sqrtmagic = sqrt(magic)
        dlat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
        dlng = (dlng * 180.0) / (A / sqrtmagic * cos(radlat) * PI)
        return Pair(lng + dlng, lat + dlat)
    }

    private fun gcj02ToBd09(lng: Double, lat: Double): Pair<Double, Double> {
        val z = sqrt(lng * lng + lat * lat) + 0.00002 * sin(lat * X_PI)
        val theta = atan2(lat, lng) + 0.000003 * cos(lng * X_PI)
        return Pair(z * cos(theta) + 0.0065, z * sin(theta) + 0.006)
    }

    private fun outOfChina(lng: Double, lat: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
