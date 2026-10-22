package com.priosort.app.data.repository

import com.priosort.app.data.local.LightRuleDao
import com.priosort.app.data.local.entities.LightRuleEntity
import com.priosort.app.domain.LightRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LightRuleRepository(private val dao: LightRuleDao) {
    val rules: Flow<List<LightRule>> = dao.observeRules().map { rows -> rows.map(LightRuleEntity::toDomain) }
    suspend fun enabledRules(): List<LightRule> = dao.getEnabledRules().map(LightRuleEntity::toDomain)
    suspend fun save(rule: LightRule) = dao.upsert(LightRuleEntity.fromDomain(rule))
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clear() = dao.deleteAll()
}
