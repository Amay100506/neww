package com.priosort.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class UserPreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "package_settings")
data class PackageSettingEntity(
    @PrimaryKey
    val packageName: String,
    val applicationName: String?,
    val importanceScore: Float = 50.0f,
    val isExcluded: Boolean = false,
    val customPriorityLevel: String? = null
)
