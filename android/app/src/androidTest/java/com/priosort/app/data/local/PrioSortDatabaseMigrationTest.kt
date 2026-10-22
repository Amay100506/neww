package com.priosort.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrioSortDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PrioSortDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migratesVersionOneDataThroughVersionTenWithoutLosingRecords() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """INSERT INTO notifications (
                    eventKey, packageName, normalizedText, category, priorityLevel,
                    basePriorityScore, urgencyScore, actionScore, deadlineScore,
                    importanceScore, recencyScore, otpDetected, actionDetected,
                    deadlineResolutionStatus, explanationJson, receivedAt, processedAt,
                    isRead, isPinned, isExcluded, isArchived, isSnoozed, isVip
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                arrayOf(
                    EVENT_KEY, "example.bank", "Payment received", "FINANCIAL_TRANSACTIONAL", "P2",
                    72.0, 0.0, 0.0, 20.0, 60.0, 100.0, 0, 0, "UNRESOLVED", "{}",
                    1_800_000_000_000L, 1_800_000_000_001L, 0, 1, 0, 0, 0, 0
                )
            )
            execSQL(
                "INSERT INTO app_preferences (`key`, `value`) VALUES (?, ?)",
                arrayOf("theme_mode", "DARK")
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            PrioSortDatabase.MIGRATION_1_2,
            PrioSortDatabase.MIGRATION_2_3,
            PrioSortDatabase.MIGRATION_3_4,
            PrioSortDatabase.MIGRATION_4_5,
            PrioSortDatabase.MIGRATION_5_6,
            PrioSortDatabase.MIGRATION_6_7,
            PrioSortDatabase.MIGRATION_7_8,
            PrioSortDatabase.MIGRATION_8_9,
            PrioSortDatabase.MIGRATION_9_10
        )

        migrated.query(
            "SELECT eventKey, transactionMerchant, transactionCurrency, isActiveOnSystem, isManuallyOverridden, isBurst, burstCount FROM notifications WHERE eventKey = ?",
            arrayOf(EVENT_KEY)
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(EVENT_KEY, cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals("UNKNOWN", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
            assertEquals(0, cursor.getInt(4))
            assertEquals(0, cursor.getInt(5))
            assertEquals(1, cursor.getInt(6))
        }
        migrated.query("SELECT value FROM app_preferences WHERE `key` = ?", arrayOf("theme_mode")).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("DARK", cursor.getString(0))
        }
        migrated.close()
    }

    companion object {
        private const val TEST_DATABASE = "priosort-migration-test"
        private const val EVENT_KEY = "legacy-event"
    }
}
