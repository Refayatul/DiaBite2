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
    val nutritionalDensity: String = "",
    val householdMeasure: String? = null,
    val netCarbs: Double? = null,
    val nutritionalBenefits: List<NutritionalBenefit> = emptyList(),
    val potentialConcerns: List<PotentialConcern> = emptyList(),
    val preparationTips: List<PreparationTip> = emptyList(),
    val inflammatoryIndex: String? = null,
    val dataSource: String? = null,
    val confidenceScore: Double? = null
)

data class ConditionRecommendation(
    val status: String = "",
    val reasoning: String = "",
    val serving: Serving? = null,
    val timing: Timing? = null,
    val pairing: List<FoodLink> = emptyList(),
    val alternatives: List<FoodLink> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class Serving(
    val standard: String = "",
    val adjusted: String? = null
)

data class Timing(
    val bestTime: String? = null,
    val avoidWhen: String? = null
)

data class FoodLink(
    val foodId: String = "",
    val reason: String = ""
)

data class NutritionalBenefit(
    val category: String = "",
    val description: String = "",
    val strength: String = ""
)

data class PotentialConcern(
    val category: String = "",
    val description: String = "",
    val severity: String = ""
)

data class PreparationTip(
    val category: String = "",
    val description: String = ""
)
