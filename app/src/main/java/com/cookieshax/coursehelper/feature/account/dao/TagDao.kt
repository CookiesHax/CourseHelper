package com.cookieshax.coursehelper.feature.account.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cookieshax.coursehelper.feature.account.model.AccountTagCrossRef
import com.cookieshax.coursehelper.feature.account.model.Tag
import com.cookieshax.coursehelper.feature.account.model.TagWithAccounts
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    fun getAllTagsFlow(): Flow<List<Tag>>

    @Transaction
    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    fun getAllTagsWithAccountsFlow(): Flow<List<TagWithAccounts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTag(tag: Tag): Long

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Update
    suspend fun updateTags(tags: List<Tag>)

    @Transaction
    @Query("SELECT * FROM tags WHERE name = :tagName")
    suspend fun getTagWithAccounts(tagName: String): TagWithAccounts?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccountTagCrossRef(crossRef: AccountTagCrossRef)

    @Query("DELETE FROM account_tag_cross_ref WHERE uid = :uid AND tagId = :tagId")
    suspend fun removeTagFromAccount(uid: String, tagId: Long)

    @Query("DELETE FROM account_tag_cross_ref WHERE uid = :uid")
    suspend fun clearAccountTags(uid: String)

    @Query("SELECT MAX(`order`) FROM tags")
    suspend fun getMaxOrder(): Int?

    @Transaction
    suspend fun addTagToAccount(uid: String, tagName: String, color: Int) {
        // 确保 Tag 存在
        val existing = getTagWithAccounts(tagName)
        val tagId = if (existing == null) {
            val maxOrder = getMaxOrder() ?: -1
            insertOrUpdateTag(Tag(name = tagName, color = color, order = maxOrder + 1))
        } else {
            existing.tag.tagId
        }
        insertAccountTagCrossRef(AccountTagCrossRef(uid, tagId))
    }
}
