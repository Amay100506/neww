package com.priosort.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.priosort.app.data.local.entities.NotificationEntity
import com.priosort.app.data.local.entities.PackageSettingEntity
import com.priosort.app.data.local.entities.UserPreferenceEntity

@Database(
    entities = [
        NotificationEntity::class,
        UserPreferenceEntity::class,
        PackageSettingEntity::class,
        com.priosort.app.data.local.entities.CustomRuleEntity::class,
        com.priosort.app.data.local.entities.SoundRuleEntity::class,
        com.priosort.app.data.local.entities.LightRuleEntity::class,
        com.priosort.app.data.local.entities.NotificationFeedbackEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class PrioSortDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun soundRuleDao(): SoundRuleDao
    abstract fun lightRuleDao(): LightRuleDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: PrioSortDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `sound_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `matchType` TEXT NOT NULL,
                        `matchValue` TEXT NOT NULL,
                        `soundUri` TEXT,
                        `volume` REAL NOT NULL,
                        `vibrationEnabled` INTEGER NOT NULL,
                        `silent` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `light_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `matchType` TEXT NOT NULL,
                        `matchValue` TEXT NOT NULL,
                        `colorArgb` INTEGER NOT NULL,
                        `pattern` TEXT NOT NULL,
                        `repeatPolicy` TEXT NOT NULL,
                        `speed` REAL NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `intensity` REAL NOT NULL,
                        `edges` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_light_rules_enabled_matchType` ON `light_rules` (`enabled`, `matchType`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `notification_feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `notificationId` INTEGER NOT NULL,
                        `feedbackType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`notificationId`) REFERENCES `notifications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_feedback_notificationId` ON `notification_feedback` (`notificationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_feedback_feedbackType` ON `notification_feedback` (`feedbackType`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `isActiveOnSystem` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `isManuallyOverridden` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sound_rules` ADD COLUMN `vibrationPatternMs` TEXT NOT NULL DEFAULT '0,140'")
                db.execSQL("ALTER TABLE `sound_rules` ADD COLUMN `cooldownMs` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sound_rules` ADD COLUMN `repeatCount` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `isBurst` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `burstCount` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `transactionCurrency` TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sound_rules` ADD COLUMN `allowDuringQuietHours` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notifications` ADD COLUMN `transactionMerchant` TEXT")
            }
        }

        fun getInstance(context: Context): PrioSortDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrioSortDatabase::class.java,
                    "priosort_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}
