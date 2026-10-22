package com.priosort.app.data.repository

import com.priosort.app.data.local.SoundRuleDao
import com.priosort.app.data.local.entities.SoundRuleEntity
import com.priosort.app.domain.SoundRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SoundRuleRepository(private val dao: SoundRuleDao) {
    val rules: Flow<List<SoundRule>> = dao.observeRules().map { rows -> rows.map(SoundRuleEntity::toDomain) }

    suspend fun save(rule: SoundRule) {
        if (rule.id == 0L) dao.insert(SoundRuleEntity.fromDomain(rule))
        else dao.update(SoundRuleEntity.fromDomain(rule))
    }

    suspend fun setEnabled(rule: SoundRule, enabled: Boolean) =
        dao.update(SoundRuleEntity.fromDomain(rule.copy(enabled = enabled)))

    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clear() = dao.deleteAll()
    suspend fun enabledRules(): List<SoundRule> = dao.getEnabledRules().map(SoundRuleEntity::toDomain)
}
