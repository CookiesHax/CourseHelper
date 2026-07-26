package com.cookieshax.coursehelper.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.cookieshax.coursehelper.core.database.entity.Account
import com.cookieshax.coursehelper.core.database.entity.AccountTagCrossRef
import com.cookieshax.coursehelper.core.database.entity.Tag

data class TagWithAccounts(
    @Embedded val tag: Tag,
    @Relation(
        parentColumn = "tagId",
        entityColumn = "uid",
        associateBy = Junction(AccountTagCrossRef::class)
    )
    val accounts: List<Account>
)
