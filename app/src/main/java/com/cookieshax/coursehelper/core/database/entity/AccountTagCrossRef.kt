package com.cookieshax.coursehelper.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_tag_cross_ref",
    primaryKeys = ["uid", "tagId"],
    indices = [Index("tagId")]
)
data class AccountTagCrossRef(
    val uid: String,
    val tagId: Long
)
