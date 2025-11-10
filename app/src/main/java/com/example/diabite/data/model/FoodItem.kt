package com.example.diabite.data.model

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val category: String = "",
    val calories: Int = 0,
    val carbs: Double = 0.0,
    val fiber: Double = 0.0,
    val sugars: Double = 0.0,
    val protein: Double = 0.0,
    val totalFat: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val glycemicIndex: Int? = null,
    val glycemicLoad: Double? = null,
    val recommendations: Map<String, ConditionRecommendation> = emptyMap(),
    val primaryAlternatives: List<Alternative> = emptyList(),
    val alternativeReasoning: String = "",
    val glycemicImpact: String = "",
    val nutritionalDensity: String = ""
)
