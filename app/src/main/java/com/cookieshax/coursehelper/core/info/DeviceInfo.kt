package com.cookieshax.coursehelper.core.info

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.cookieshax.coursehelper.app.CourseHelperApplication
import java.util.Locale
import java.util.UUID

object DeviceInfo {
    private val context: Context get() = CourseHelperApplication.context

    val deviceId: String by lazy {
        UUID.randomUUID().toString()
    }

    val buildId: String get() = Build.ID

    val deviceName: String get() = Build.DEVICE

    val manufacturer: String get() = Build.MANUFACTURER

    val model: String get() = Build.MODEL

    val osVersionName: String get() = Build.VERSION.RELEASE

    val brand: String get() = Build.BRAND

    val hardware: String get() = Build.HARDWARE

    val language: String get() = Locale.getDefault().language

    val fingerprint: String get() = Build.FINGERPRINT

    val bootloader: String get() = Build.BOOTLOADER

    val locale: Locale get() = Locale.getDefault()

    val country: String get() = Locale.getDefault().country

    val cpuArch: String get() = Build.SUPPORTED_ABIS.joinToString(",")

    val dpi: String
        get() {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager?.currentWindowMetrics?.let {
                    val bounds = it.bounds
                    displayMetrics.apply {
                        widthPixels = bounds.width()
                        heightPixels = bounds.height()
                    }
                }
            }
            return displayMetrics.densityDpi.toString()
        }

    val resolution: String
        get() {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager?.currentWindowMetrics?.let {
                    val bounds = it.bounds
                    displayMetrics.apply {
                        widthPixels = bounds.width()
                        heightPixels = bounds.height()
                    }
                }
            }
            return "${displayMetrics.widthPixels}*${displayMetrics.heightPixels}"
        }

    val mediaDrmId: String
        get() {
            return try {
                val widevineDrm = MediaDrm(
                    UUID(-0x121074568629b532L, -0x5c37e5afc3bafe9L)
                )
                val id = widevineDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
                id.let { UUID.nameUUIDFromBytes(it).toString() }
            } catch (_: Exception) {
                ""
            }
        }

    val osName: String get() = Build.TYPE

    val oaid: String
        get() {
            return try {
                val id = Settings.Secure.getString(context.contentResolver, "OAID")
                id ?: ""
            } catch (_: Exception) {
                ""
            }
        }
}
