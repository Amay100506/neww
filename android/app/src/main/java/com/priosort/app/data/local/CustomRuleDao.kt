package com.priosort.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.priosort.app.data.local.entities.CustomRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {

    @Query("SELECT * FROM custom_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<CustomRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CustomRuleEntity)

    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM custom_rules")
    suspend fun deleteAllRules()
}
