package com.example.diabite.data.repository

import com.example.diabite.data.model.Alternative
import com.example.diabite.data.model.ConditionRecommendation
import com.example.diabite.data.model.FoodItem
import com.example.diabite.data.model.FoodLink
import com.example.diabite.data.model.NutritionalBenefit
import com.example.diabite.data.model.PotentialConcern
import com.example.diabite.data.model.PreparationTip
import com.example.diabite.data.model.Serving
import com.example.diabite.data.model.Timing
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GeminiRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    /**
     * Analyze food using Gemini AI Cloud Function
     */
    fun analyzeFood(foodName: String, userConditions: List<String>, diabetesType: String?): Flow<Resource<FoodItem>> = callbackFlow {
        trySend(Resource.loading())

        val data = hashMapOf(
            "foodName" to foodName.trim(),
            "userConditions" to userConditions
        )

        // Add diabetesType to the payload if it's not null or blank
        if (!diabetesType.isNullOrBlank()) {
            data["diabetesType"] = diabetesType
        }

        val task = functions
            .getHttpsCallable("geminiFoodAnalysis")
            .call(data)

        task.addOnSuccessListener { result ->
            try {
                val response = result.data as? Map<*, *>
                if (response != null) {
                    val success = response["success"] as? Boolean ?: false
                    val foodData = response["data"] as? Map<*, *>

                    if (success && foodData != null) {
                        val foodItem = parseFoodItemFromResponse(foodData)
                        if (foodItem != null) {
                            trySend(Resource.success(foodItem))
                        } else {
                            trySend(Resource.error(AppError.UnknownError("Failed to parse AI response")))
                        }
                    } else {
                        trySend(Resource.error(AppError.UnknownError("AI analysis failed")))
                    }
                } else {
                    trySend(Resource.error(AppError.UnknownError("Invalid response format")))
                }
            } catch (e: Exception) {
                trySend(Resource.error(AppError.fromException(e)))
            }
        }

        task.addOnFailureListener { exception ->
            val errorMessage = handleGeminiError(exception)
            trySend(Resource.error(AppError.UnknownError(errorMessage)))
        }

        awaitClose { /* Cleanup if needed */ }
    }

    /**
     * Parse FoodItem from Firebase Functions response
     */
    private fun parseFoodItemFromResponse(data: Map<*, *>): FoodItem? {
        return try {
            FoodItem(
                id = data["id"] as? String ?: return null,
                name = data["name"] as? String ?: return null,
                normalizedName = data["normalizedName"] as? String ?: "",
                category = data["category"] as? String ?: "Unknown",
                calories = (data["calories"] as? Number)?.toInt() ?: 0,
                carbs = (data["totalCarbohydrates"] as? Number)?.toDouble() ?: 0.0,
                fiber = (data["fiber"] as? Number)?.toDouble() ?: 0.0,
                sugars = (data["sugars"] as? Number)?.toDouble() ?: 0.0,
                protein = (data["protein"] as? Number)?.toDouble() ?: 0.0,
                totalFat = (data["totalFat"] as? Number)?.toDouble() ?: 0.0,
                saturatedFat = (data["saturatedFat"] as? Number)?.toDouble() ?: 0.0,
                sodium = (data["sodium"] as? Number)?.toDouble() ?: 0.0,
                potassium = (data["potassium"] as? Number)?.toDouble() ?: 0.0,
                glycemicIndex = (data["glycemicIndex"] as? Number)?.toInt(),
                glycemicLoad = (data["glycemicLoad"] as? Number)?.toDouble(),
                recommendations = parseRecommendations(data["recommendations"] as? Map<*, *>),
                primaryAlternatives = parseAlternatives(data["primaryAlternatives"] as? List<*>),
                alternativeReasoning = data["alternativeReasoning"] as? String ?: "",
                glycemicImpact = data["glycemicImpact"] as? String ?: "Unknown",
                nutritionalDensity = data["nutritionalDensity"] as? String ?: "Medium",
                householdMeasure = data["householdMeasure"] as? String,
                netCarbs = (data["netCarbs"] as? Number)?.toDouble(),
                nutritionalBenefits = parseNutritionalBenefits(data["nutritionalBenefits"] as? List<*>),
                potentialConcerns = parsePotentialConcerns(data["potentialConcerns"] as? List<*>),
                preparationTips = parsePreparationTips(data["preparationTips"] as? List<*>),
                inflammatoryIndex = data["inflammatoryIndex"] as? String,
                dataSource = data["dataSource"] as? String,
                confidenceScore = (data["confidenceScore"] as? Number)?.toDouble()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse recommendations from response
     */
    private fun parseRecommendations(recommendations: Map<*, *>?) =
        recommendations?.mapNotNull { (key, value) ->
            if (key is String && value is Map<*, *>) {
                key to parseConditionRecommendation(value)
            } else null
        }?.toMap() ?: emptyMap()

    /**
     * Parse single condition recommendation
     */
    private fun parseConditionRecommendation(data: Map<*, *>) =
        ConditionRecommendation(
            status = data["status"] as? String ?: "",
            reasoning = data["reasoning"] as? String ?: "",
            serving = (data["serving"] as? Map<*, *>)?.let { parseServing(it) },
            timing = (data["timing"] as? Map<*, *>)?.let { parseTiming(it) },
            pairing = (data["pairing"] as? List<*>)?.let { parseFoodLinkList(it) } ?: emptyList(),
            alternatives = (data["alternatives"] as? List<*>)?.let { parseFoodLinkList(it) } ?: emptyList(),
            warnings = (data["warnings"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )

    private fun parseServing(data: Map<*, *>): Serving {
        return Serving(
            standard = data["standard"] as? String ?: "",
            adjusted = data["adjusted"] as? String
        )
    }

    private fun parseTiming(data: Map<*, *>): Timing {
        return Timing(
            bestTime = data["bestTime"] as? String,
            avoidWhen = data["avoidWhen"] as? String
        )
    }

    private fun parseFoodLinkList(data: List<*>): List<FoodLink> {
        return data.mapNotNull { item ->
            if (item is Map<*, *>) {
                FoodLink(
                    foodId = item["foodId"] as? String ?: "",
                    reason = item["reason"] as? String ?: ""
                )
            } else null
        }
    }

    private fun parseNutritionalBenefits(benefits: List<*>?): List<NutritionalBenefit> {
        return benefits?.mapNotNull { item ->
            if (item is Map<*, *>) {
                NutritionalBenefit(
                    category = item["category"] as? String ?: "",
                    description = item["description"] as? String ?: "",
                    strength = item["strength"] as? String ?: ""
                )
            } else null
        } ?: emptyList()
    }

    private fun parsePotentialConcerns(concerns: List<*>?): List<PotentialConcern> {
        return concerns?.mapNotNull { item ->
            if (item is Map<*, *>) {
                PotentialConcern(
                    category = item["category"] as? String ?: "",
                    description = item["description"] as? String ?: "",
                    severity = item["severity"] as? String ?: ""
                )
            } else null
        } ?: emptyList()
    }

    private fun parsePreparationTips(tips: List<*>?): List<PreparationTip> {
        return tips?.mapNotNull { item ->
            if (item is Map<*, *>) {
                PreparationTip(
                    category = item["category"] as? String ?: "",
                    description = item["description"] as? String ?: ""
                )
            } else null
        } ?: emptyList()
    }

    /**
     * Parse alternatives from response
     */
    private fun parseAlternatives(alternatives: List<*>?): List<Alternative> {
        return alternatives?.mapNotNull { item ->
            if (item is Map<*, *>) {
                Alternative(
                    foodId = item["foodId"] as? String ?: "",
                    advantage = item["advantage"] as? String ?: "",
                    improvement = item["improvement"] as? String ?: "",
                    bestFor = (item["bestFor"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            } else null
        } ?: emptyList()
    }

    /**
     * Handle Gemini-specific errors
     */
    private fun handleGeminiError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"

        return when {
            message.contains("unauthenticated") ->
                "Please log in to use AI food analysis"
            message.contains("invalid-argument") ->
                "Invalid food name. Please try a different search term"
            message.contains("resource-exhausted") ->
                "AI analysis limit reached. Please try again later"
            message.contains("unavailable") || message.contains("deadline") ->
                "AI service temporarily unavailable. Please try again"
            message.contains("internal") ->
                "AI analysis failed. Please try again or search for existing foods"
            message.contains("network") ->
                "Network error. Please check your connection and try again"
            else -> "AI analysis failed: $message"
        }
    }

    /**
     * Check if error is retryable
     */
    fun isRetryableError(error: String): Boolean {
        return error.contains("unavailable") ||
               error.contains("deadline") ||
               error.contains("network") ||
               error.contains("internal")
    }

    /**
     * Get user-friendly error message
     */
    fun getUserFriendlyError(error: String): String {
        return when {
            error.contains("limit reached") -> "You've reached your daily AI analysis limit"
            error.contains("unavailable") -> "AI service is temporarily busy"
            error.contains("network") -> "Please check your internet connection"
            else -> error
        }
    }
}