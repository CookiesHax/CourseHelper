package com.cookieshax.coursehelper.app.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
    }

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery: StateFlow<String> = searchQuery
        .debounce(300.milliseconds)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val accounts: StateFlow<List<Account>> = AccountRepository.accountList
    val accountsId: StateFlow<List<String>> = AccountRepository.accountsIdFlow
    val activeAccountId: StateFlow<String?> = AccountRepository.activeAccountIdFlow

    fun updateSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun clearSearchQuery() {
        savedStateHandle[KEY_SEARCH_QUERY] = ""
    }

    fun syncAccounts() {
        viewModelScope.launch {
            AccountRepository.syncAllAccountsStatus()
        }
    }
}
