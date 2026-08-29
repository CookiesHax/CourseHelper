package com.cookieshax.coursehelper.feature.checkin.model

import android.util.Log

object CaptchaSolver {
    init {
        try {
            System.loadLibrary("captcha_solver")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("CaptchaSolver", "Failed to load native library", e)
        }
    }

    // 使用 JNI 调用 C++ 实现的 OpenCV 逻辑
    private external fun nativeCalculateOffset(bgData: ByteArray, sliceData: ByteArray): Int

    fun calculateCaptchaOffset(captcha: Captcha): Int {
        val bgBytes = captcha.bgData ?: return 0
        val sliderBytes = captcha.sliceData ?: return 0

        val xOffset = nativeCalculateOffset(bgBytes, sliderBytes)
        Log.d("CaptchaSolver", "Native Offset: $xOffset")

        return xOffset
    }
}
