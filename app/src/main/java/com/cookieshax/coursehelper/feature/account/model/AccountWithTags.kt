package com.cookieshax.coursehelper.feature.account.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class AccountWithTags(
    @Embedded val account: Account,
    @Relation(
        parentColumn = "uid",
        entityColumn = "tagId",
        associateBy = Junction(AccountTagCrossRef::class)
    )
    val tags: List<Tag>
)
