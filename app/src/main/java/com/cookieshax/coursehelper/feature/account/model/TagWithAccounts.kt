package com.cookieshax.coursehelper.feature.account.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TagWithAccounts(
    @Embedded val tag: Tag,
    @Relation(
        parentColumn = "tagId",
        entityColumn = "uid",
        associateBy = Junction(AccountTagCrossRef::class)
    )
    val accounts: List<Account>
)
