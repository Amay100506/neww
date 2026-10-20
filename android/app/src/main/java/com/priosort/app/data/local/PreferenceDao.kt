package com.priosort.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.priosort.app.data.local.entities.PackageSettingEntity
import com.priosort.app.data.local.entities.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferenceDao {

    @Query("SELECT * FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreference(key: String): UserPreferenceEntity?

    @Query("SELECT * FROM app_preferences WHERE `key` = :key LIMIT 1")
    fun observePreference(key: String): Flow<UserPreferenceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(entity: UserPreferenceEntity)

    @Query("SELECT * FROM package_settings")
    fun getAllPackageSettings(): Flow<List<PackageSettingEntity>>

    @Query("SELECT * FROM package_settings WHERE packageName = :packageName LIMIT 1")
    suspend fun getPackageSetting(packageName: String): PackageSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPackageSetting(entity: PackageSettingEntity)

    @Query("UPDATE package_settings SET isExcluded = :isExcluded WHERE packageName = :packageName")
    suspend fun setPackageExcluded(packageName: String, isExcluded: Boolean)
}
