package com.cookieshax.coursehelper.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.dao.AccountDao
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.account.model.AccountTagCrossRef
import com.cookieshax.coursehelper.feature.account.model.Tag
import com.cookieshax.coursehelper.feature.account.dao.TagDao
import com.cookieshax.coursehelper.core.network.CookieManager
import kotlinx.coroutines.launch

@Database(
    entities = [Account::class, Tag::class, AccountTagCrossRef::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    CourseHelperApplication.context,
                    AppDatabase::class.java,
                    "course_helper_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .addCallback(object : Callback() {
                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            // 数据库重置时 清除所有关联的元数据和 Cookie
                            CourseHelperApplication.applicationScope.launch {
                                AccountRepository.switchActiveAccount(null)
                                AccountRepository.clearMetadata()
                                CookieManager.clearAllCookies()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
