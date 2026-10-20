package com.priosort.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.priosort.app.data.local.entities.NotificationEntity
import com.priosort.app.data.local.entities.PackageSettingEntity
import com.priosort.app.data.local.entities.UserPreferenceEntity

@Database(
    entities = [
        NotificationEntity::class,
        UserPreferenceEntity::class,
        PackageSettingEntity::class,
        com.priosort.app.data.local.entities.CustomRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrioSortDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun customRuleDao(): CustomRuleDao

    companion object {
        @Volatile
        private var INSTANCE: PrioSortDatabase? = null

        fun getInstance(context: Context): PrioSortDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrioSortDatabase::class.java,
                    "priosort_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
