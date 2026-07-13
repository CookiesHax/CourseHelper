package com.cookieshax.coursehelper.feature.course.model

import android.util.Log
import androidx.annotation.Keep
import com.cookieshax.coursehelper.core.utils.getBooleanOrDefault
import com.cookieshax.coursehelper.core.utils.getIntOrDefault
import com.cookieshax.coursehelper.core.utils.getStringOrDefault
import com.cookieshax.coursehelper.core.utils.getStringOrEmpty
import com.cookieshax.coursehelper.core.utils.getStringOrNull
import com.google.gson.JsonObject

@Keep
data class Course(
    val courseId: String,
    val classId: String,
    val cpi: String,
    val imageUrl: String,
    val name: String,
    val teacher: String,
    val state: Boolean,
    val isStart: Boolean,
    val note: String?,
    val schools: String?,
    val beginDate: String?,
    val endDate: String?
) {
    companion object {
        private const val TAG = "Course"

        fun fromApiResponse(
            apiResponse: JsonObject?,
            showUnnecessary: Boolean = false
        ): List<Course> {
            if (apiResponse == null) {
                Log.e(TAG, "API响应为null")
                return emptyList()
            }

            val channelList = apiResponse.getAsJsonArray("channelList") ?: run {
                Log.e(TAG, "API响应中没有channelList字段")
                return emptyList()
            }

            val courseList = mutableListOf<Course>()

            channelList.forEachIndexed { index, element ->
                try {
                    val content =
                        element.asJsonObject.getAsJsonObject("content") ?: return@forEachIndexed

                    // 提取公共信息
                    val cpi = content.getStringOrEmpty("cpi")
                    val note = content.getStringOrEmpty("name")
                    val stateValue = content.getIntOrDefault("state", 0)
                    val beginDate = content.getStringOrEmpty("beginDate")
                    val endDate = content.getStringOrEmpty("endDate")
                    val isStart = content.getBooleanOrDefault("isstart", true)
                    val classId = content.getStringOrEmpty("id")

                    val courseDataArray = content.getAsJsonObject("course")?.getAsJsonArray("data")
                        ?: return@forEachIndexed

                    courseDataArray.forEach { jsonElement ->
                        val courseObj = jsonElement.asJsonObject

                        val course = Course(
                            courseId = courseObj.getStringOrEmpty("id"),
                            classId = classId,
                            cpi = cpi,
                            imageUrl = courseObj.getStringOrEmpty("imageurl")
                                .replaceFirst("http://", "https://", ignoreCase = true),
                            name = courseObj.getStringOrDefault("name", "未知课程"),
                            teacher = courseObj.getStringOrDefault("teacherfactor", "未知教师"),
                            schools = courseObj.getStringOrNull("schools"),
                            state = stateValue == 0,
                            isStart = isStart,
                            note = note,
                            beginDate = beginDate,
                            endDate = endDate
                        )

                        if (showUnnecessary || (course.state && course.isStart)) {
                            courseList.add(course)
                        } else {
                            val reason = if (!course.state) "尚未开始" else "已结束"
                            Log.d(TAG, "课程 ${course.name} $reason，跳过")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析第 $index 个channelItem时出错: ${e.message}")
                }
            }

            Log.d(TAG, "成功解析 ${courseList.size} 门课程")
            return courseList
        }

    }
}
