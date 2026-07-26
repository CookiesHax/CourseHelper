package com.cookieshax.coursehelper.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.cookieshax.coursehelper.core.database.entity.Account
import com.cookieshax.coursehelper.core.database.entity.AccountTagCrossRef
import com.cookieshax.coursehelper.core.database.entity.Tag

data class AccountWithTags(
    @Embedded val account: Account,
    @Relation(
        parentColumn = "uid",
        entityColumn = "tagId",
        associateBy = Junction(AccountTagCrossRef::class)
    )
    val tags: List<Tag>
)
