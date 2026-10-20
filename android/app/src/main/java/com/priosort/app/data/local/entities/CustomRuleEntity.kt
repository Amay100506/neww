package com.priosort.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel

@Entity(tableName = "custom_rules")
data class CustomRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleName: String,
    val keywordPattern: String,
    val targetField: String,
    val assignedPriority: String?,
    val assignedCategory: String?,
    val isEnabled: Boolean = true
) {
    fun toDomain(): CustomRule {
        return CustomRule(
            id = id,
            ruleName = ruleName,
            keywordPattern = keywordPattern,
            targetField = targetField,
            assignedPriority = assignedPriority?.let { PriorityLevel.fromCode(it) },
            assignedCategory = assignedCategory?.let { NotificationCategory.fromString(it) },
            isEnabled = isEnabled
        )
    }

    companion object {
        fun fromDomain(rule: CustomRule): CustomRuleEntity {
            return CustomRuleEntity(
                id = rule.id,
                ruleName = rule.ruleName,
                keywordPattern = rule.keywordPattern,
                targetField = rule.targetField,
                assignedPriority = rule.assignedPriority?.code,
                assignedCategory = rule.assignedCategory?.name,
                isEnabled = rule.isEnabled
            )
        }
    }
}
