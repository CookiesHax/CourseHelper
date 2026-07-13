package com.cookieshax.coursehelper.feature.account.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.AccountIdName
import com.cookieshax.coursehelper.feature.account.model.AccountWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY `order` ASC")
    fun getAllAccountsFlow(): Flow<List<Account>>

    @Query("SELECT uid, name FROM accounts ORDER BY `order` ASC")
    fun getAllAccountsIdNameFlow(): Flow<List<AccountIdName>>

    @Query("SELECT * FROM accounts ORDER BY `order` ASC")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT uid FROM accounts ORDER BY `order` ASC")
    fun getAllAccountsIdFlow(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM accounts ORDER BY `order` ASC")
    fun getAccountsWithTagsFlow(): Flow<List<AccountWithTags>>

    @Query("SELECT COUNT(*) FROM accounts")
    fun getAccountsSizeFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountsSize(): Int

    @Query("SELECT * FROM accounts WHERE uid = :uid")
    suspend fun getAccountById(uid: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE uid = :uid")
    suspend fun deleteAccountById(uid: String)

    @Update
    suspend fun updateAccounts(accounts: List<Account>)
}
