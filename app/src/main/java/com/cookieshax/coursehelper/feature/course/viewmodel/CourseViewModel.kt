package com.cookieshax.coursehelper.feature.course.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.cookieshax.coursehelper.feature.course.model.Course
import com.cookieshax.coursehelper.feature.course.model.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 课程状态机
sealed class CourseState {
    object Idle : CourseState() // 未开始/未选择账号
    object Loading : CourseState() // 加载中
    data class Success(val list: List<Course>) : CourseState() // 成功
    data class Error(val msg: String) : CourseState() // 出错
}

class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow<CourseState>(CourseState.Idle)
    val uiState: StateFlow<CourseState> = _uiState

    val isRefreshing = MutableStateFlow(false)

    // 记录最后一次获取数据的账号ID 用于判断数据是否过期
    private val _loadedAccountId = MutableStateFlow<String?>(null)
    val loadedAccountId: StateFlow<String?> = _loadedAccountId

    // 账号切换时调用 立即重置状态防止渲染过期数据 再加载新课程
    suspend fun onAccountChanged(accountId: String?) {
        if (accountId == null) {
            _uiState.value = CourseState.Idle
            _loadedAccountId.value = null
            return
        }

        // 账号没变且有缓存 直接返回
        if (accountId == _loadedAccountId.value && _uiState.value is CourseState.Success) {
            return
        }

        // 立即清除旧数据 避免切回页面时闪一帧旧内容
        _uiState.value = CourseState.Loading
        _loadedAccountId.value = accountId
        fetchCourses()
    }

    // 下拉刷新等场景调用
    suspend fun loadCourses(accountId: String?, forceRefresh: Boolean = false) {
        if (accountId == null) {
            _uiState.value = CourseState.Idle
            _loadedAccountId.value = null
            return
        }

        // 如果账号没变已经有数据且不是强制刷新则直接返回缓存
        if (!forceRefresh && accountId == _loadedAccountId.value && _uiState.value is CourseState.Success) {
            return
        }

        // 只有在初次加载或账号切换时才显示中心 Loading
        if (!forceRefresh || _uiState.value !is CourseState.Success) {
            _uiState.value = CourseState.Loading
        }

        _loadedAccountId.value = accountId
        fetchCourses()
    }

    fun refreshCourses(accountId: String?) {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                loadCourses(accountId, forceRefresh = true)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun cacheCourse(course: Course) {
        CourseRepository.saveCourse(course)
    }

    private suspend fun fetchCourses() {
        val showUnnecessaryCourses = settingsRepository.showUnnecessaryCourses.first()
        when (val result = CourseRepository.getCourses(showUnnecessaryCourses)) {
            is ApiResult.Success -> _uiState.value = CourseState.Success(result.data)
            is ApiResult.Error -> _uiState.value = CourseState.Error(result.message)
        }
    }
}
