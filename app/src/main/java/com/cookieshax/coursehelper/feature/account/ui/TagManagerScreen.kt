package com.cookieshax.coursehelper.feature.account.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cookieshax.coursehelper.feature.account.model.Tag
import com.cookieshax.coursehelper.feature.account.ui.components.AccountSelectDialog
import com.cookieshax.coursehelper.feature.account.ui.components.AccountWithTagsListContent
import com.cookieshax.coursehelper.feature.account.ui.components.TagEditDialog
import com.cookieshax.coursehelper.feature.account.ui.components.TagListContent
import com.cookieshax.coursehelper.feature.account.ui.components.TagSelectDialog
import com.cookieshax.coursehelper.feature.account.viewmodel.TagManagerViewModel
import com.cookieshax.coursehelper.ui.items.HctColorPickerDialog
import com.cookieshax.coursehelper.ui.items.HctColorState
import com.cookieshax.coursehelper.ui.items.IcTags
import com.cookieshax.coursehelper.ui.items.SearchInput
import com.cookieshax.coursehelper.ui.items.SearchTrigger
import kotlin.coroutines.cancellation.CancellationException

enum class SelectionType {
    TAG,
    ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun TagManagerScreen(navController: NavHostController) {
    val viewModel: TagManagerViewModel = viewModel()
    val tagsWithAccounts by viewModel.tagsWithAccounts.collectAsState()
    val accountsWithTags by viewModel.accountsWithTags.collectAsState()

    val isNavigating = remember { mutableStateOf(false) }

    var selectionType by rememberSaveable { mutableStateOf(SelectionType.TAG) }

    // 搜索状态
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchBackProgress by remember { mutableFloatStateOf(0f) }

    // 多选状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }
    var selectBackProgress by remember { mutableFloatStateOf(0f) }

    // 新建/编辑标签对话框状态
    var showTagDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var tempSelectedAccountUids by remember { mutableStateOf<List<String>>(emptyList()) }

    // 账号选择对话框状态
    var showAccountSelectDialog by remember { mutableStateOf(false) }

    // 标签选择对话框状态
    var showTagSelectDialog by remember { mutableStateOf(false) }
    var tempSelectedTagIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var editingAccountUid by remember { mutableStateOf<String?>(null) }

    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 颜色选择器状态
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerState by remember { mutableStateOf(HctColorState(263.3, 42.7, 64.1)) }
    var pendingColorApply by remember { mutableStateOf<(HctColorState) -> Unit>({}) }

    // 过滤标签列表
    val filteredTags = remember(tagsWithAccounts, searchQuery) {
        val list = if (searchQuery.isBlank()) {
            tagsWithAccounts
        } else {
            tagsWithAccounts.filter {
                it.tag.name.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    // 过滤账号列表
    val filteredAccounts = remember(accountsWithTags, searchQuery) {
        if (searchQuery.isBlank()) {
            accountsWithTags
        } else {
            accountsWithTags.filter {
                it.account.name.contains(searchQuery, ignoreCase = true) ||
                        it.account.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    SharedTransitionLayout {
        Scaffold(
            topBar = {
                AnimatedContent(
                    targetState = isSearching,
                    transitionSpec = {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    },
                    label = "tag_top_bar"
                ) { searching ->
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
                                        onQueryChange = { searchQuery = it },
                                        onClose = {
                                            isSearching = false
                                            searchQuery = ""
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                val scale = 1f - (searchBackProgress * 0.08f)
                                                scaleX = scale
                                                scaleY = scale
                                                alpha =
                                                    1f - (searchBackProgress * 2f).coerceAtMost(1f)
                                            },
                                        hint = if (selectionType == SelectionType.TAG) "搜索标签..." else "搜索账号...",
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                if (isSelectionMode && selectionType == SelectionType.TAG) {
                                    Text(
                                        text = "${selectedTagIds.size} / ${tagsWithAccounts.size}",
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                } else {
                                    Text("标签管理")
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        if (!isNavigating.value) {
                                            isNavigating.value = true
                                            navController.popBackStack()
                                        }
                                    },
                                    enabled = !isNavigating.value
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            },
                            actions = {
                                // 搜索
                                SearchTrigger(
                                    onClick = { isSearching = true },
                                    animatedVisibilityScope = this@AnimatedContent
                                )

                                if (isSelectionMode) {
                                    // 全选/取消全选
                                    if (selectionType == SelectionType.TAG) {
                                        if (selectedTagIds.size == tagsWithAccounts.size) {
                                            IconButton(onClick = { selectedTagIds = setOf() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Deselect,
                                                    contentDescription = "取消全选"
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = {
                                                selectedTagIds =
                                                    tagsWithAccounts.map { it.tag.tagId.toString() }
                                                        .toSet()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.SelectAll,
                                                    contentDescription = "全选"
                                                )
                                            }
                                        }

                                        // 删除
                                        IconButton(onClick = {
                                            if (selectedTagIds.isNotEmpty()) showDeleteDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else {
                                    // 新建标签
                                    IconButton(
                                        onClick = {
                                            editingTag = null
                                            tempSelectedAccountUids = emptyList()
                                            showTagDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "新建标签"
                                        )
                                    }

                                    if (selectionType == SelectionType.TAG) {
                                        IconButton(
                                            onClick = {
                                                selectionType = SelectionType.ACCOUNT
                                                isSelectionMode = false
                                                selectedTagIds = emptySet()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "按账号管理标签"
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                selectionType = SelectionType.TAG
                                                isSelectionMode = false
                                                selectedTagIds = emptySet()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = IcTags,
                                                contentDescription = "按标签管理账号"
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            PredictiveBackHandler(
                enabled = isSearching || isSelectionMode
            ) { progress ->
                try {
                    progress.collect { backEvent ->
                        if (isSearching) {
                            searchBackProgress = backEvent.progress
                        } else if (isSelectionMode) {
                            selectBackProgress = backEvent.progress
                        }
                    }

                    if (isSearching) {
                        isSearching = false
                        searchQuery = ""
                        searchBackProgress = 0f
                    } else if (isSelectionMode) {
                        isSelectionMode = false
                        selectedTagIds = emptySet()
                        selectBackProgress = 0f
                    }
                } catch (_: CancellationException) {
                    searchBackProgress = 0f
                    selectBackProgress = 0f
                }
            }

            if (selectionType == SelectionType.TAG) {
                TagListContent(
                    tags = tagsWithAccounts,
                    filteredTags = filteredTags,
                    searchQuery = searchQuery,
                    isSelectionMode = isSelectionMode,
                    selectedTagIds = selectedTagIds,
                    onTagClick = { tagWithAccounts ->
                        val tagId = tagWithAccounts.tag.tagId.toString()
                        if (isSelectionMode) {
                            selectedTagIds = if (selectedTagIds.contains(tagId)) {
                                selectedTagIds - tagId
                            } else {
                                selectedTagIds + tagId
                            }
                        } else {
                            editingTag = tagWithAccounts.tag
                            tempSelectedAccountUids = tagWithAccounts.accounts.map { it.uid }
                            showAccountSelectDialog = true
                        }
                    },
                    onTagEdit = { tagWithAccounts ->
                        editingTag = tagWithAccounts.tag
                        tempSelectedAccountUids = tagWithAccounts.accounts.map { it.uid }
                        showTagDialog = true
                    },
                    onMove = { fromIndex, toIndex ->
                        val currentList = tagsWithAccounts.toMutableList()
                        val movedItem = currentList.removeAt(fromIndex)
                        currentList.add(toIndex, movedItem)
                        viewModel.reorderTags(currentList)
                    },
                    onSelectionModeChanged = { isSelectionMode = it },
                    onSelectedIdsChanged = { selectedTagIds = it },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                AccountWithTagsListContent(
                    accountsWithTags = accountsWithTags,
                    filteredAccounts = filteredAccounts,
                    onAccountClick = { accountWithTags ->
                        editingAccountUid = accountWithTags.account.uid
                        tempSelectedTagIds = accountWithTags.tags.map { it.tagId }
                        showTagSelectDialog = true
                    },
                    onEditTag = { accountWithTags ->
                        editingAccountUid = accountWithTags.account.uid
                        tempSelectedTagIds = accountWithTags.tags.map { it.tagId }
                        showTagSelectDialog = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // 新建/编辑标签对话框
        if (showTagDialog) {
            TagEditDialog(
                tag = editingTag,
                onDismiss = { showTagDialog = false },
                onConfirm = { name, color, selectedAccountUids ->
                    if (editingTag != null) {
                        viewModel.updateTag(
                            editingTag!!.copy(name = name, color = color),
                            selectedAccountUids
                        )
                    } else {
                        viewModel.createTag(name, color, selectedAccountUids)
                    }
                    showTagDialog = false
                },
                onPickColor = { currentState, onApply ->
                    colorPickerState = currentState
                    pendingColorApply = onApply
                    showColorPicker = true
                },
                onSelectAccounts = {
                    showAccountSelectDialog = true
                },
                initialSelectedAccounts = tempSelectedAccountUids,
                existingColors = tagsWithAccounts.map { it.tag.color },
            )
        }

        // 账号选择对话框
        if (showAccountSelectDialog) {
            val allAccounts by viewModel.accounts.collectAsState()
            AccountSelectDialog(
                accounts = allAccounts,
                initialSelected = tempSelectedAccountUids,
                onDismiss = { showAccountSelectDialog = false },
                onConfirm = { uids ->
                    tempSelectedAccountUids = uids
                    if (!showTagDialog && editingTag != null) {
                        // 如果是从列表直接点击进入的 立即保存
                        viewModel.updateTag(editingTag!!, uids)
                    }
                    showAccountSelectDialog = false
                }
            )
        }

        // 标签选择对话框
        if (showTagSelectDialog) {
            val allTagsWithAccounts by viewModel.tagsWithAccounts.collectAsState()
            val allTags = allTagsWithAccounts.map { it.tag }
            TagSelectDialog(
                tags = allTags,
                initialSelected = tempSelectedTagIds,
                onDismiss = { showTagSelectDialog = false },
                onConfirm = { tagIds ->
                    editingAccountUid?.let { uid ->
                        viewModel.updateAccountTags(uid, tagIds)
                    }
                    showTagSelectDialog = false
                }
            )
        }

        // 颜色选择器
        if (showColorPicker) {
            HctColorPickerDialog(
                state = colorPickerState,
                onStateChange = { colorPickerState = it },
                onDismissRequest = { showColorPicker = false },
                onApply = {
                    pendingColorApply(colorPickerState)
                    showColorPicker = false
                }
            )
        }

        // 删除确认对话框
        if (showDeleteDialog) {
            val tagsToDelete =
                tagsWithAccounts.filter { selectedTagIds.contains(it.tag.tagId.toString()) }
            val totalAccounts = tagsToDelete.flatMap { it.accounts }.distinctBy { it.uid }.size
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除标签") },
                text = {
                    if (totalAccounts > 0) {
                        Text("确定要删除选中的 ${selectedTagIds.size} 个标签吗？这些标签将从共 $totalAccounts 个账号中移除，此操作无法撤销。")
                    } else {
                        Text("确定要删除选中的 ${selectedTagIds.size} 个标签吗？")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTags(tagsToDelete.map { it.tag })
                            isSelectionMode = false
                            selectedTagIds = emptySet()
                            showDeleteDialog = false
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
