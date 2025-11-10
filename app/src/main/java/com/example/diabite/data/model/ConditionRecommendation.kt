package com.example.diabite.data.model

data class ConditionRecommendation(
    val safetyLevel: String = "",
    val reasoning: String = "",
    val keyPoints: List<String> = emptyList(),
    val servingAdvice: String = "",
    val timingAdvice: String? = null,
    val pairingSuggestions: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val bloodSugarImpact: String? = null,
    val bloodPressureImpact: String? = null,
    val heartHealthImpact: String? = null
)
