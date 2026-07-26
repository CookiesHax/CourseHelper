package com.cookieshax.coursehelper.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0,
    val name: String,
    val color: Int,
    val order: Int = 0 // 用于排序
)
