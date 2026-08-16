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

    val buildId: String inline get() = Build.ID

    val deviceName: String inline get() = Build.DEVICE

    val manufacturer: String inline get() = Build.MANUFACTURER

    val model: String inline get() = Build.MODEL

    val osVersionName: String inline get() = Build.VERSION.RELEASE

    val brand: String inline get() = Build.BRAND

    val hardware: String inline get() = Build.HARDWARE

    val language: String inline get() = Locale.getDefault().language

    val fingerprint: String inline get() = Build.FINGERPRINT

    val bootloader: String inline get() = Build.BOOTLOADER

    val locale: Locale get() = Locale.getDefault()

    val country: String get() = Locale.getDefault().country

    val cpuArch: String get() = Build.SUPPORTED_ABIS.joinToString(",")

    val dpi: String get() = context.resources.displayMetrics.toString()

    val resolution: String
        get() {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager?.currentWindowMetrics?.bounds
                "${bounds?.width() ?: 0}*${bounds?.height() ?: 0}"
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.getMetrics(metrics)
                "${metrics.widthPixels}*${metrics.heightPixels}"
            }
        }

    val mediaDrmId: String
        get() {
            return try {
                val widevineDrm = MediaDrm(UUID(-0x121074568629b532L, -0x5c37e5afc3bafe9L))
                val id = widevineDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
                widevineDrm.close()
                id.let { UUID.nameUUIDFromBytes(it).toString() }
            } catch (_: Exception) {
                ""
            }
        }

    val osName: String inline get() = Build.TYPE

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
