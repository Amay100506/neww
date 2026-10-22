package com.priosort.app.domain

data class RuleBackup(
    val formatVersion: Int = 2,
    val soundRules: List<SoundRule> = emptyList(),
    val priorityRules: List<CustomRule> = emptyList(),
    val lightRules: List<LightRule> = emptyList()
)
