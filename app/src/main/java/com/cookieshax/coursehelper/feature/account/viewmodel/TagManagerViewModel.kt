package com.cookieshax.coursehelper.feature.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.core.database.entity.Account
import com.cookieshax.coursehelper.core.database.model.AccountWithTags
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.core.database.entity.Tag
import com.cookieshax.coursehelper.core.database.model.TagWithAccounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SelectionType {
    TAG,
    ACCOUNT
}

class TagManagerViewModel : ViewModel() {
    private val _selectionType = MutableStateFlow(SelectionType.TAG)
    val selectionType = _selectionType.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedTagIds = MutableStateFlow(setOf<String>())
    val selectedTagIds = _selectedTagIds.asStateFlow()

    private val _triggerAddTag = MutableStateFlow(0)
    val triggerAddTag = _triggerAddTag.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    val tagsWithAccounts: StateFlow<List<TagWithAccounts>> =
        AccountRepository.allTagsWithAccountsFlow

    val accountsWithTags: StateFlow<List<AccountWithTags>> =
        AccountRepository.accountsWithTagsFlow

    val accounts: StateFlow<List<Account>> =
        AccountRepository.accountList

    fun setSelectionType(type: SelectionType) {
        _selectionType.value = type
        _isSelectionMode.value = false
        _selectedTagIds.value = emptySet()
    }

    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) _selectedTagIds.value = emptySet()
    }

    fun setSelectedTagIds(ids: Set<String>) {
        _selectedTagIds.value = ids
    }

    fun triggerAddTag() {
        _triggerAddTag.value += 1
    }

    fun consumeAddTagTrigger() {
        _triggerAddTag.value = 0
    }

    fun setShowDeleteDialog(show: Boolean) {
        _showDeleteDialog.value = show
    }

    fun createTag(name: String, color: Int, accountUids: List<String>) {
        viewModelScope.launch {
            val tagId = AccountRepository.createTag(name, color)
            if (accountUids.isNotEmpty()) {
                AccountRepository.associateTagWithAccounts(tagId, accountUids)
            }
        }
    }

    fun updateTag(tag: Tag, accountUids: List<String>) {
        viewModelScope.launch {
            AccountRepository.updateTag(tag)
            if (accountUids.isNotEmpty()) {
                AccountRepository.associateTagWithAccounts(tag.tagId, accountUids)
            } else {
                // 如果未选择帐户则清除现有关联
                AccountRepository.clearTagAssociations(tag.tagId)
            }
        }
    }

    fun deleteTags(tags: List<Tag>) {
        viewModelScope.launch {
            tags.forEach { AccountRepository.deleteTag(it) }
        }
    }

    fun reorderTags(newList: List<TagWithAccounts>) {
        viewModelScope.launch {
            AccountRepository.reorderTags(newList)
        }
    }

    fun updateAccountTags(uid: String, tagIds: List<Long>) {
        viewModelScope.launch {
            AccountRepository.updateAccountTags(uid, tagIds)
        }
    }
}
