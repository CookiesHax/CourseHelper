package com.cookieshax.coursehelper.feature.checkin.model

import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

object CaptchaSolver {
    init {
        if (!OpenCVLoader.initLocal()) {
            throw RuntimeException("Failed to initialize OpenCV")
        }
    }

    fun calculateCaptchaOffset(captcha: Captcha): Int {
        val bgBytes = captcha.bgData!!
        val sliderBytes = captcha.sliceData!!

        // 将 ByteArray 转换为 MatOfByte
        val bgMat = Imgcodecs.imdecode(MatOfByte(*bgBytes), Imgcodecs.IMREAD_COLOR)
        val sliderMat = Imgcodecs.imdecode(MatOfByte(*sliderBytes), Imgcodecs.IMREAD_COLOR)

        // 转灰度
        val bgGray = Mat()
        val tpGray = Mat()
        Imgproc.cvtColor(bgMat, bgGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(sliderMat, tpGray, Imgproc.COLOR_BGR2GRAY)

        // 获取滑块的有效范围
        val (ymin, ymax) = getCropBounds(tpGray)

        // 裁剪滑块和背景以保持高度一致
        val cropHeight = ymax - ymin + 1
        val tpCut = tpGray.submat(Rect(0, ymin, tpGray.cols(), cropHeight))
        val bgCut = bgGray.submat(Rect(0, ymin, bgGray.cols(), cropHeight))

        // 边缘检测
        val bgEdge = Mat()
        val tpEdge = Mat()
        Imgproc.Canny(bgCut, bgEdge, 100.0, 200.0)
        Imgproc.Canny(tpCut, tpEdge, 100.0, 200.0)

        // 模板匹配
        val res = Mat()
        Imgproc.matchTemplate(bgEdge, tpEdge, res, Imgproc.TM_CCOEFF_NORMED)

        // 获取最佳匹配位置
        val mmr = Core.minMaxLoc(res)
        val xOffset = mmr.maxLoc.x.roundToInt()

        // 清理中间 Mat
        listOf(bgGray, tpGray, tpCut, bgCut, bgEdge, tpEdge, res).forEach { it.release() }
        Log.d("CaptchaSolver", xOffset.toString())
        return xOffset
    }

    fun getCropBounds(grayImg: Mat): Pair<Int, Int> {
        val h = grayImg.rows()
        // 将图像横向压缩 取每行的最大值
        val rowMax = Mat()
        Core.reduce(grayImg, rowMax, 1, Core.REDUCE_MAX)

        var ymin = 0
        var ymax = h - 1

        // 找第一个非 0 行
        for (i in 0 until h) {
            if (rowMax.get(i, 0)[0] > 0) {
                ymin = i
                break
            }
        }
        // 找最后一个非 0 行
        for (i in h - 1 downTo 0) {
            if (rowMax.get(i, 0)[0] > 0) {
                ymax = i
                break
            }
        }

        rowMax.release() // 释放内存
        return Pair(ymin, ymax)
    }
}
