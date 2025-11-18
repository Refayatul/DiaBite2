package com.example.diabite.util

object ConditionNormalizer {
    fun normalizeCondition(condition: String): String {
        return condition.lowercase().replace(" ", "_")
    }
}
