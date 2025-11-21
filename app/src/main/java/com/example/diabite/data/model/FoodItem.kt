package com.example.diabite.data.model

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val category: String = "",
    val subcategory: String = "",
    val servingSize: String = "",
    val householdMeasure: String = "",
    val calories: Int = 0,
    val totalCarbohydrates: Double = 0.0,
    val netCarbs: Double = 0.0,
    val fiber: Double = 0.0,
    val sugars: Double = 0.0,
    val addedSugars: Double = 0.0,
    val protein: Double = 0.0,
    val totalFat: Double = 0.0,
    val saturatedFat: Double = 0.0,
    val transFat: Double = 0.0,
    val cholesterol: Double = 0.0,
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val magnesium: Double = 0.0,
    val glycemicIndex: Int = 0,
    val glycemicLoad: Double = 0.0,
    val omega3: Double = 0.0,
    val omega6: Double = 0.0,
    val antioxidantLevel: String = "",
    val inflammatoryIndex: String = "",
    val glycemicImpact: String = "",
    val nutritionalDensity: String = "",
    val recommendations: Map<String, ConditionRecommendation> = emptyMap(),
    val dataSource: String = "",
    val lastVerified: String = "",
    val confidenceScore: Double = 0.0,
    val searchCount: Int = 0,
    val addedBy: String = "",
    val createdAt: String = "",
    val lastUpdated: String = "",
    val diabetesTypes: List<String> = emptyList(),
    // Optional upload/provenance fields
    val uploadedAt: String = "",
    val uploadedBy: String = "",
    val version: Int = 1
)

data class ConditionRecommendation(
    val condition: String = "",
    val safetyLevel: String = "",
    val reasoning: String = "",
    val personalizedAdvice: String = "",
    val keyPoints: List<String> = emptyList(),
    val servingAdvice: String = "",
    val timingAdvice: String = "",
    val pairingSuggestions: List<String> = emptyList(),
    val alternatives: List<Alternative> = emptyList(),
    val bloodSugarImpact: String = "",
    val bloodPressureImpact: String = "",
    val heartHealthImpact: String = "",
    val kidneyImpact: String = "",
    val alternativeReasoning: String = "",
    val nutritionalBenefits: List<NutritionalBenefit> = emptyList(),
    val potentialConcerns: List<PotentialConcern> = emptyList(),
    val preparationTips: List<PreparationTip> = emptyList()
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
