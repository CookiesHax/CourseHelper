package com.cookieshax.coursehelper.feature.course.model

import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.StringUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object CourseRepository {
    private val courseMap = mutableMapOf<String, Course>()

    suspend fun getCourses(showUnnecessaryCourses: Boolean = false): ApiResult<List<Course>> {
        return try {
            when (val result = ApiManager.getCourses()) {
                is ApiResult.Success -> {
                    val data = StringUtils.parseJson(result.data)
                    if (data == null) {
                        ApiResult.Error("解析JSON失败")
                    } else {
                        val resultValue = data.get("result")?.asInt ?: 0
                        val msg = data.get("msg")?.asString ?: ""

                        if (resultValue == 1) {
                            val courses = Course.fromApiResponse(data, showUnnecessaryCourses)
                            // 自动更新缓存
                            courses.forEach { courseMap[it.courseId] = it }
                            ApiResult.Success(courses)
                        } else {
                            ApiResult.Error(msg)
                        }
                    }
                }

                is ApiResult.Error -> result
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "未知错误")
        }
    }

    fun saveCourse(course: Course) {
        courseMap[course.courseId] = course
    }

    fun getCourseById(courseId: String): Course? = courseMap[courseId]

    suspend fun getTasksForCourse(course: Course): ApiResult<List<CourseTask>> = coroutineScope {
        try {
            val mainTasksDeferred = async { ApiManager.getTaskList(course) }
            val webTasksDeferred = async { ApiManager.getTaskListWeb(course) }

            val mainResult = mainTasksDeferred.await()
            val webResult = webTasksDeferred.await()

            if (mainResult is ApiResult.Success) {
                var tasks = CourseTask.fromApiResponse(mainResult.data)

                // 进行补全
                if (webResult is ApiResult.Success) {
                    tasks = CourseTask.updateTasksWithWebApiResponse(tasks, webResult.data)
                }

                ApiResult.Success(tasks)
            } else {
                val errorMsg = (mainResult as? ApiResult.Error)?.message ?: "获取任务列表失败"
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "未知错误")
        }
    }
}
