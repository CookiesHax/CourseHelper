package com.cookieshax.coursehelper.feature.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.AccountWithTags
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.account.model.Tag
import com.cookieshax.coursehelper.feature.account.model.TagWithAccounts
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TagManagerViewModel : ViewModel() {
    val tagsWithAccounts: StateFlow<List<TagWithAccounts>> =
        AccountRepository.allTagsWithAccountsFlow

    val accountsWithTags: StateFlow<List<AccountWithTags>> =
        AccountRepository.accountsWithTagsFlow

    val accounts: StateFlow<List<Account>> =
        AccountRepository.accountList

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
