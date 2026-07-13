package com.cookieshax.coursehelper.feature.course.model

import androidx.annotation.Keep
import com.cookieshax.coursehelper.core.utils.StringUtils

@Keep
data class CourseTask(
    val nameTwo: String, // 结束时间
    val groupId: Int,
    val isLook: Int,
    val releaseNum: Int,
    val url: String,
    val picUrl: String,
    val attendNum: Int,
    val activeType: Int,
    val nameOne: String, // 活动标题
    val startTime: Long,
    val id: Long,
    val status: Int,
    val nameFour: String, // 剩余时间
    val otherId: String = "" // 签到类型
) {
    companion object {
        // 解析API响应中的任务列表
        fun fromApiResponse(apiResponse: String): List<CourseTask> {
            val courseTasks = mutableListOf<CourseTask>()
            try {
                val jsonObject =
                    StringUtils.parseJson(apiResponse)?.asJsonObject ?: return courseTasks
                val dataArray = jsonObject.getAsJsonArray("activeList") ?: return courseTasks

                for (i in 0 until dataArray.size()) {
                    val taskObj = dataArray[i].asJsonObject

                    val courseTask = CourseTask(
                        nameTwo = taskObj.get("nameTwo")?.asString ?: "",
                        groupId = taskObj.get("groupId")?.asInt ?: 0,
                        isLook = taskObj.get("isLook")?.asInt ?: 0,
                        releaseNum = taskObj.get("releaseNum")?.asInt ?: 0,
                        url = taskObj.get("url")?.asString ?: "",
                        picUrl = taskObj.get("picUrl")?.asString ?: "",
                        attendNum = taskObj.get("attendNum")?.asInt ?: 0,
                        activeType = taskObj.get("activeType")?.asInt ?: 0,
                        nameOne = taskObj.get("nameOne")?.asString ?: "",
                        startTime = taskObj.get("startTime")?.asLong ?: 0L,
                        id = taskObj.get("id")?.asLong ?: 0L,
                        status = taskObj.get("status")?.asInt ?: 0,
                        nameFour = taskObj.get("nameFour")?.asString ?: ""
                    )
                    courseTasks.add(courseTask)
                }
            } catch (e: Exception) {
                // 解析错误时返回空列表
                e.printStackTrace()
            }
            return courseTasks
        }

        fun updateTasksWithWebApiResponse(
            tasks: List<CourseTask>,
            webApiResponse: String
        ): List<CourseTask> {
            try {
                val jsonObject = StringUtils.parseJson(webApiResponse)?.asJsonObject ?: return tasks
                val dataObj = jsonObject.get("data")?.asJsonObject ?: return tasks
                val activeList = dataObj.getAsJsonArray("activeList") ?: return tasks

                val webDataMap = mutableMapOf<Long, Pair<String, String>>()
                for (i in 0 until activeList.size()) {
                    val taskObj = activeList[i].asJsonObject
                    val id = taskObj.get("id")?.asLong ?: 0L
                    val otherId = taskObj.get("otherId")?.asString ?: ""
                    val nameFour = taskObj.get("nameFour")?.asString ?: ""
                    if (id != 0L) webDataMap[id] = Pair(otherId, nameFour)
                }

                // 如果该任务在 Map 中有值 则补全
                return tasks.map { task ->
                    val webData = webDataMap[task.id]
                    if (webData != null) {
                        val updatedNameFour = task.nameFour.ifEmpty { webData.second }
                        task.copy(otherId = webData.first, nameFour = updatedNameFour)
                    } else {
                        task
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return tasks
            }
        }

        fun isUnsupportedTask(task: CourseTask): Boolean {
            return task.url.isBlank() || task.activeType == 45 // 通知
        }

        // 将活动类型转换为文本描述
        fun activeTypeToText(activeType: Int): String {
            return when (activeType) {
                2 -> "签到"
                4 -> "抢答"
                5 -> "主题讨论"
                11 -> "选人"
                14 -> "问卷"
                17 -> "直播"
                19 -> "作业"
                23 -> "评分"
                35 -> "分组任务"
                40 -> "PPT课堂"
                42 -> "随堂练习"
                43 -> "投票"
                44 -> "文件"
                45 -> "通知"
                46 -> "学生反馈"
                47 -> "计时器"
                49 -> "白板"
                51 -> "同步课堂"
                54 -> "定时签到"
                56 -> "超星课堂"
                59 -> "抽签"
                64 -> "腾讯会议"
                68 -> "互动练习"
                74 -> "签退"
                77 -> "AI实践"
                else -> "未知类型"
            }
        }
    }
}
