package com.cookieshax.coursehelper.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlin.random.Random
import androidx.core.graphics.set
import java.io.ByteArrayOutputStream
import androidx.core.graphics.get
import java.io.File

object ImageUtils {
    fun modifyRandomPixelWithSimilarColor(
        jpegBytes: ByteArray,
        colorThreshold: Int = 20
    ): ByteArray {
        // 将二进制 JPEG 数据解码为可修改的 Bitmap
        val options = BitmapFactory.Options().apply { inMutable = true }
        val bitmap =
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, options) ?: return jpegBytes

        val width = bitmap.width
        val height = bitmap.height

        // 随机选择一个像素坐标
        val randomX = Random.nextInt(width)
        val randomY = Random.nextInt(height)

        // 获取该像素的原颜色
        val originalColor = bitmap[randomX, randomY]

        // 提取 R, G, B 通道
        val r = Color.red(originalColor)
        val g = Color.green(originalColor)
        val b = Color.blue(originalColor)

        // 在阈值范围内随机生成相似的颜色通道值 并限制在 0-255 范围内
        val newR = (r + Random.nextInt(-colorThreshold, colorThreshold + 1)).coerceIn(0, 255)
        val newG = (g + Random.nextInt(-colorThreshold, colorThreshold + 1)).coerceIn(0, 255)
        val newB = (b + Random.nextInt(-colorThreshold, colorThreshold + 1)).coerceIn(0, 255)

        val newColor = Color.rgb(newR, newG, newB)

        // 将修改后的相似颜色写入随机像素位置
        bitmap[randomX, randomY] = newColor

        // 将修改后的 Bitmap 重新压缩回 JPEG 二进制数据
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        // 释放 Bitmap 内存
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    fun saveByteArrayToFile(bytes: ByteArray, outputFile: File) {
        outputFile.outputStream().use { fos ->
            fos.write(bytes)
        }
    }
}
