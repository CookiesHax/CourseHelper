package com.cookieshax.coursehelper.feature.course.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.feature.course.model.Course
import com.cookieshax.coursehelper.feature.course.model.CourseRepository
import com.cookieshax.coursehelper.feature.course.model.CourseTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TaskState {
    object Loading : TaskState()
    data class Success(val course: Course, val tasks: List<CourseTask>) : TaskState()
    data class Error(val msg: String) : TaskState()
}

class CourseTaskViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<TaskState>(TaskState.Loading)
    val uiState: StateFlow<TaskState> = _uiState
    val isRefreshing = MutableStateFlow(false)

    fun loadTasks(courseId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && _uiState.value !is TaskState.Success) {
                _uiState.value = TaskState.Loading
            }

            val course = CourseRepository.getCourseById(courseId)
            if (course == null) {
                _uiState.value = TaskState.Error("未找到课程信息")
                return@launch
            }

            when (val result = CourseRepository.getTasksForCourse(course)) {
                is ApiResult.Success -> _uiState.value = TaskState.Success(course, result.data)
                is ApiResult.Error -> _uiState.value = TaskState.Error(result.message)
            }
        }
    }

    fun refreshTasks(courseId: String) {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                loadTasks(courseId, forceRefresh = true)
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
