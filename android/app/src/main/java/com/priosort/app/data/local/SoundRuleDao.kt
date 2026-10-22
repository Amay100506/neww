package com.priosort.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.priosort.app.data.local.entities.SoundRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundRuleDao {
    @Query("SELECT * FROM sound_rules ORDER BY id DESC")
    fun observeRules(): Flow<List<SoundRuleEntity>>

    @Query("SELECT * FROM sound_rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<SoundRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SoundRuleEntity): Long

    @Update
    suspend fun update(rule: SoundRuleEntity)

    @Query("DELETE FROM sound_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sound_rules")
    suspend fun deleteAll()
}
