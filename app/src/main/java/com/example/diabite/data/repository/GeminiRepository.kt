package com.example.diabite.data.repository

import com.example.diabite.data.model.FoodItem
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GeminiRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    /**
     * Analyze food using Gemini AI Cloud Function
     */
    fun analyzeFood(foodName: String): Flow<Resource<FoodItem>> = callbackFlow {
        trySend(Resource.loading())

        val data = hashMapOf(
            "foodName" to foodName.trim()
        )

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
                carbs = (data["carbs"] as? Number)?.toDouble() ?: 0.0,
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
                nutritionalDensity = data["nutritionalDensity"] as? String ?: "Medium"
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
        com.example.diabite.data.model.ConditionRecommendation(
            safetyLevel = data["safetyLevel"] as? String ?: "Unknown",
            reasoning = data["reasoning"] as? String ?: "",
            keyPoints = (data["keyPoints"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            servingAdvice = data["servingAdvice"] as? String ?: "",
            timingAdvice = data["timingAdvice"] as? String,
            pairingSuggestions = (data["pairingSuggestions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            alternatives = (data["alternatives"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            bloodSugarImpact = data["bloodSugarImpact"] as? String,
            bloodPressureImpact = data["bloodPressureImpact"] as? String,
            heartHealthImpact = data["heartHealthImpact"] as? String
        )

    /**
     * Parse alternatives from response
     */
    private fun parseAlternatives(alternatives: List<*>?) =
        alternatives?.mapNotNull { item ->
            if (item is Map<*, *>) {
                com.example.diabite.data.model.Alternative(
                    foodId = item["foodId"] as? String ?: "",
                    advantage = item["advantage"] as? String ?: "",
                    improvement = item["improvement"] as? String ?: "",
                    bestFor = (item["bestFor"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            } else null
        } ?: emptyList()

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
