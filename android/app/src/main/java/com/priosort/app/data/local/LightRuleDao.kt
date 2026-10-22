package com.priosort.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.priosort.app.data.local.entities.LightRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LightRuleDao {
    @Query("SELECT * FROM light_rules ORDER BY id DESC")
    fun observeRules(): Flow<List<LightRuleEntity>>

    @Query("SELECT * FROM light_rules WHERE enabled = 1 ORDER BY id DESC")
    suspend fun getEnabledRules(): List<LightRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: LightRuleEntity): Long

    @Query("UPDATE light_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM light_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM light_rules")
    suspend fun deleteAll()
}
