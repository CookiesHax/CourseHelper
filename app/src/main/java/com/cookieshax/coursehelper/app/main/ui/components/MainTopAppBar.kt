package com.cookieshax.coursehelper.app.main.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.app.main.MainTab
import com.cookieshax.coursehelper.feature.account.viewmodel.SelectionType
import com.cookieshax.coursehelper.feature.login.LoginType
import com.cookieshax.coursehelper.ui.items.IcTags
import com.cookieshax.coursehelper.ui.items.SearchInput
import com.cookieshax.coursehelper.ui.items.SearchTrigger

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MainTopAppBar(
    selectedTab: MainTab,
    isSearching: Boolean,
    onSearchStatusChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    searchBackProgress: Float,
    // Account Selection
    isSelectionMode: Boolean,
    selectedAccountCount: Int,
    totalAccountCount: Int,
    onToggleSelectAllAccounts: () -> Unit,
    onDeleteAccounts: () -> Unit,
    // Tag Selection
    isTagSelectionMode: Boolean,
    selectedTagCount: Int,
    totalTagCount: Int,
    selectionType: SelectionType,
    onToggleSelectAllTags: () -> Unit,
    onDeleteTags: () -> Unit,
    onAddTag: () -> Unit,
    onToggleTagSelectionType: () -> Unit,
    // Navigation/Actions
    useNavRail: Boolean,
    onNavigateToTags: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: (LoginType) -> Unit,
    onShowAddCourseDialog: () -> Unit,
) {
    var showLoginMenu by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = isSearching to selectedTab,
        transitionSpec = {
            fadeIn(tween(100)) togetherWith fadeOut(tween(100))
        },
        label = "top_bar_transition"
    ) { (searching, tab) ->
        if (searching) {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchInput(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onClose = {
                                onSearchStatusChange(false)
                                onClearSearch()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    val scale = 1f - (searchBackProgress * 0.08f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (searchBackProgress * 2f).coerceAtMost(1f)
                                },
                            hint = when (tab) {
                                MainTab.COURSE -> "搜索课程..."
                                MainTab.ACCOUNT -> "搜索账号..."
                                MainTab.TAGS -> "搜索标签..."
                                else -> "搜索..."
                            },
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                }
            )
        } else {
            TopAppBar(
                title = {
                    val title = when (tab) {
                        MainTab.COURSE -> "CourseHelper"
                        MainTab.ACCOUNT -> if (isSelectionMode) "$selectedAccountCount / $totalAccountCount" else "账号管理"
                        MainTab.TAGS -> if (isTagSelectionMode) "$selectedTagCount / $totalTagCount" else "标签管理"
                        MainTab.SETTINGS -> "系统设置"
                    }
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                },
                actions = {
                    when (tab) {
                        MainTab.COURSE, MainTab.ACCOUNT -> {
                            if (!useNavRail) {
                                IconButton(onClick = onNavigateToTags) {
                                    Icon(IcTags, contentDescription = "标签")
                                }
                            }

                            SearchTrigger(
                                onClick = { onSearchStatusChange(true) },
                                animatedVisibilityScope = this@AnimatedContent
                            )

                            if (tab == MainTab.COURSE || !isSelectionMode) {
                                Box {
                                    IconButton(onClick = { showLoginMenu = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "添加")
                                    }
                                    DropdownMenu(
                                        expanded = showLoginMenu,
                                        onDismissRequest = { showLoginMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("密码登录") },
                                            onClick = {
                                                showLoginMenu = false
                                                onNavigateToLogin(LoginType.PASSWORD)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("验证码登录") },
                                            onClick = {
                                                showLoginMenu = false
                                                onNavigateToLogin(LoginType.VERIFICATION_CODE)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("二维码登录") },
                                            onClick = {
                                                showLoginMenu = false
                                                onNavigateToLogin(LoginType.QRCODE)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("添加课程") },
                                            onClick = {
                                                showLoginMenu = false
                                                onShowAddCourseDialog()
                                            }
                                        )
                                    }
                                }
                            }

                            if (isSelectionMode && tab == MainTab.ACCOUNT) {
                                IconButton(onClick = onToggleSelectAllAccounts) {
                                    Icon(
                                        if (selectedAccountCount == totalAccountCount) Icons.Default.Deselect else Icons.Default.SelectAll,
                                        contentDescription = "选择"
                                    )
                                }
                                IconButton(onClick = onDeleteAccounts) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (!useNavRail && !isSelectionMode) {
                                IconButton(onClick = onNavigateToSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = "设置")
                                }
                            }
                        }

                        MainTab.TAGS -> {
                            SearchTrigger(
                                onClick = { onSearchStatusChange(true) },
                                animatedVisibilityScope = this@AnimatedContent
                            )

                            if (isTagSelectionMode) {
                                IconButton(onClick = onToggleSelectAllTags) {
                                    Icon(
                                        if (selectedTagCount == totalTagCount) Icons.Default.Deselect else Icons.Default.SelectAll,
                                        contentDescription = "选择"
                                    )
                                }
                                IconButton(onClick = onDeleteTags) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                IconButton(onClick = onAddTag) {
                                    Icon(Icons.Default.Add, contentDescription = "新建标签")
                                }
                                IconButton(onClick = onToggleTagSelectionType) {
                                    Icon(
                                        if (selectionType == SelectionType.TAG) Icons.Default.Person else IcTags,
                                        contentDescription = "切换视图"
                                    )
                                }
                            }
                        }

                        MainTab.SETTINGS -> {}
                    }
                }
            )
        }
    }
}
