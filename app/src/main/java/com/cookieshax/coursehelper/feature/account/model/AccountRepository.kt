package com.cookieshax.coursehelper.feature.account.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.cookieshax.coursehelper.core.database.AppDatabase
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.network.CookieManager
import com.cookieshax.coursehelper.core.utils.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "account_manager")

object AccountRepository {
    private const val KEY_ACTIVE_ACCOUNT_ID = "active_account_id"
    private val activeAccountIdKey = stringPreferencesKey(KEY_ACTIVE_ACCOUNT_ID)

    private val _expirationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expirationEvent = _expirationEvent.asSharedFlow()

    private val dataStore: DataStore<Preferences> by lazy {
        CourseHelperApplication.context.dataStore
    }
    private val applicationScope: CoroutineScope by lazy {
        CourseHelperApplication.applicationScope
    }

    private val db: AppDatabase by lazy { AppDatabase.getDatabase() }
    private val accountDao by lazy { db.accountDao() }
    private val tagDao by lazy { db.tagDao() }

    // 对外暴露状态流
    val accountList: StateFlow<List<Account>> by lazy {
        accountDao.getAllAccountsFlow()
            .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())
    }

    val accountsIdFlow: StateFlow<List<String>> by lazy {
        accountDao.getAllAccountsIdFlow()
            .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())
    }

    val accountsWithTagsFlow: StateFlow<List<AccountWithTags>> by lazy {
        accountDao.getAccountsWithTagsFlow()
            .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())
    }

    val accountsSizeFlow: StateFlow<Int> by lazy {
        accountDao.getAccountsSizeFlow()
            .stateIn(applicationScope, SharingStarted.Eagerly, 0)
    }

    val activeAccountIdFlow: StateFlow<String?> by lazy {
        dataStore.data
            .map { prefs -> prefs[activeAccountIdKey] }
            .stateIn(applicationScope, SharingStarted.Eagerly, null)
    }

    // 同步地获取当前账号数据
    fun getCurrentListSnapshot(): List<Account> = accountList.value

    suspend fun validateAccount(userId: String): AccountStatus {
        return try {
            when (val response = ApiManager.getUserInfo(userId)) {
                is ApiResult.Success -> {
                    val userInfo =
                        StringUtils.parseJson(response.data) ?: return AccountStatus.UNKNOWN

                    val result = StringUtils.getString(userInfo, "result", "0")
                    if (result == "1") {
                        AccountStatus.VALID
                    } else {
                        AccountStatus.EXPIRED
                    }
                }

                is ApiResult.Error -> {
                    AccountStatus.UNKNOWN
                }
            }
        } catch (_: Exception) {
            AccountStatus.UNKNOWN
        }
    }

    suspend fun syncAllAccountsStatus() {
        val currentAccountList = accountList.value
        val hasExpired = AtomicBoolean(false)
        coroutineScope {
            currentAccountList.forEach { account ->
                // 为每个账户开启一个独立的协程并行去抓取数据
                launch {
                    val status = validateAccount(account.uid)
                    if (status == AccountStatus.EXPIRED) {
                        hasExpired.set(true)
                    }

                    addOrUpdateAccount(account.copy(status = status))
                }
            }
        }
        if (hasExpired.get()) {
            _expirationEvent.tryEmit(Unit)
        }
    }

    // 添加或更新账户
    suspend fun addOrUpdateAccount(account: Account) {
        val existingAccount = accountDao.getAccountById(account.uid)
        val accountToSave = if (existingAccount != null) {
            // 如果账号已存在保留其原有的 order 属性 避免覆盖导致排序变动
            account.copy(order = existingAccount.order)
        } else {
            // 如果是新账号则设置其 order 为当前账号数量 即排在最后
            account.copy(order = accountDao.getAccountsSize())
        }

        accountDao.insertOrUpdateAccount(accountToSave)

        // 如果没有当前会话 自动设为当前账户
        if (activeAccountIdFlow.value == null) {
            switchActiveAccount(account.uid)
        }
    }

    // 删除账户
    suspend fun removeAccount(userId: String) {
        accountDao.deleteAccountById(userId)
        CookieManager.clearCookiesForUser(userId)

        // 如果删除的是当前账户
        if (activeAccountIdFlow.value == userId) {
            val currentList = accountDao.getAllAccounts()
            if (currentList.isNotEmpty()) {
                // 切换到列表中的第一个账户
                switchActiveAccount(currentList.first().uid)
            } else {
                // 没有账户了 清除当前活动账户ID
                switchActiveAccount(null)
            }
        }
    }

    // 切换会话
    suspend fun switchActiveAccount(userId: String?) {
        dataStore.edit { prefs ->
            if (userId != null) {
                prefs[activeAccountIdKey] = userId
            } else {
                prefs.remove(activeAccountIdKey)
            }
        }
    }

    // 重新排序账户
    suspend fun reorderAccounts(newList: List<Account>) {
        val updatedList = newList.mapIndexed { index, account ->
            account.copy(order = index)
        }
        accountDao.updateAccounts(updatedList)
    }

    // 为账号修改标签
    suspend fun updateAccountTags(uid: String, tagIds: List<Long>) {
        tagDao.clearAccountTags(uid)
        for (tagId in tagIds) {
            tagDao.insertAccountTagCrossRef(AccountTagCrossRef(uid, tagId))
        }
    }

    // 获取所有标签及其关联账号
    val allTagsWithAccountsFlow: StateFlow<List<TagWithAccounts>> by lazy {
        tagDao.getAllTagsWithAccountsFlow()
            .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())
    }

    // 创建标签
    suspend fun createTag(name: String, color: Int): Long {
        val maxOrder = tagDao.getMaxOrder() ?: -1
        return tagDao.insertOrUpdateTag(Tag(name = name, color = color, order = maxOrder + 1))
    }

    // 更新标签
    suspend fun updateTag(tag: Tag) {
        tagDao.insertOrUpdateTag(tag)
    }

    // 重新排序标签
    suspend fun reorderTags(newList: List<TagWithAccounts>) {
        val updatedList = newList.mapIndexed { index, tagWithAccounts ->
            tagWithAccounts.tag.copy(order = index)
        }
        tagDao.updateTags(updatedList)
    }

    // 删除标签 (同时删除关联关系)
    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag)
    }

    suspend fun associateTagWithAccounts(tagId: Long, accountIds: List<String>) {
        for (accountId in accountIds) {
            tagDao.insertAccountTagCrossRef(AccountTagCrossRef(accountId, tagId))
        }
    }

    suspend fun clearTagAssociations(tagId: Long) {
        for (accountId in accountList.value.map { it.uid }) {
            tagDao.removeTagFromAccount(accountId, tagId)
        }
    }

    // 清除所有元数据 (用于数据库重置等极端情况)
    suspend fun clearMetadata() {
        dataStore.edit { it.clear() }
    }
}
