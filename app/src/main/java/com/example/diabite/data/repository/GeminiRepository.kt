package com.example.diabite.data.repository

import com.example.diabite.BuildConfig
import com.example.diabite.data.model.*
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.util.Locale

class GeminiRepository(
    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 4096// Increased for full analysis
            responseMimeType = "application/json" // Force JSON mode (supported in newer Gemini models)
        }
    )
) {

    /**
     * Analyze food using Gemini AI directly
     */
    fun analyzeFood(foodName: String, userConditions: List<String>, diabetesType: String?): Flow<Resource<FoodItem>> = callbackFlow {
        trySend(Resource.loading())

        try {
            val prompt = createGeminiPrompt(foodName, userConditions, diabetesType)
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text

            if (!responseText.isNullOrBlank()) {
                // Clean up Markdown formatting (```json ... ```) if present
                val cleanJson = sanitizeJson(responseText)
                val foodItem = parseGeminiResponseToFoodItem(cleanJson, foodName, diabetesType)
                
                if (foodItem != null) {
                    trySend(Resource.success(foodItem))
                } else {
                    trySend(Resource.error(AppError.UnknownError("Failed to parse food data")))
                }
            } else {
                trySend(Resource.error(AppError.UnknownError("Empty AI response")))
            }
        } catch (e: Exception) {
            val errorMessage = handleGeminiError(e)
            trySend(Resource.error(AppError.UnknownError(errorMessage)))
        }

        awaitClose { }
    }.flowOn(Dispatchers.IO)

    /**
     * Generate personalized meal suggestions
     */
    fun generateMealSuggestions(userConditions: List<String>, diabetesType: String?): Flow<Resource<String>> = callbackFlow {
        trySend(Resource.loading())

        try {
            val prompt = createMealSuggestionPrompt(userConditions, diabetesType)
            val response = generativeModel.generateContent(prompt)
            
            if (!response.text.isNullOrBlank()) {
                trySend(Resource.success(response.text!!))
            } else {
                trySend(Resource.error(AppError.UnknownError("Empty AI response")))
            }
        } catch (e: Exception) {
            trySend(Resource.error(AppError.UnknownError(handleGeminiError(e))))
        }

        awaitClose { }
    }.flowOn(Dispatchers.IO)

    // -------------------------------------------------------------------------
    // PARSING LOGIC (The "Brains" of the operation)
    // -------------------------------------------------------------------------

    private fun parseGeminiResponseToFoodItem(jsonString: String, originalName: String, userDiabetesType: String?): FoodItem? {
        return try {
            val root = JSONObject(jsonString)
            val basicInfo = root.optJSONObject("basicInfo") ?: JSONObject()
            val nutrition = basicInfo.optJSONObject("estimatedNutrition") ?: JSONObject()
            val glycemic = basicInfo.optJSONObject("glycemicInfo") ?: JSONObject()
            
            // Extract Recommendations Map
            val medicalAnalysis = root.optJSONObject("medicalAnalysis")
            val recommendationsMap = parseRecommendationsMap(medicalAnalysis, userDiabetesType)

            FoodItem(
                id = generateFoodId(originalName),
                name = basicInfo.optString("name", originalName).replaceFirstChar { it.uppercase() },
                normalizedName = originalName.lowercase().trim(),
                category = basicInfo.optString("category", "General"),
                subcategory = "",
                servingSize = basicInfo.optString("servingSize", "100g"),
                householdMeasure = basicInfo.optString("householdMeasure", "1 cup/piece"),
                
                // Numeric Parsing with safety helper
                calories = extractInt(nutrition, "calories"),
                totalCarbohydrates = extractDouble(nutrition, "totalCarbohydrates", "carbs"),
                netCarbs = extractDouble(nutrition, "netCarbs"),
                fiber = extractDouble(nutrition, "fiber"),
                sugars = extractDouble(nutrition, "sugars"),
                addedSugars = extractDouble(nutrition, "addedSugars"),
                protein = extractDouble(nutrition, "protein"),
                totalFat = extractDouble(nutrition, "totalFat"),
                saturatedFat = extractDouble(nutrition, "saturatedFat"),
                transFat = extractDouble(nutrition, "transFat"),
                cholesterol = extractDouble(nutrition, "cholesterol"),
                sodium = extractDouble(nutrition, "sodium"),
                potassium = extractDouble(nutrition, "potassium"),
                
                glycemicIndex = extractInt(glycemic, "glycemicIndex"),
                glycemicLoad = extractDouble(glycemic, "glycemicLoad"),
                glycemicImpact = glycemic.optString("glycemicImpact", "Unknown"),
                
                // Metadata
                nutritionalDensity = root.optString("nutritionalDensity", "Medium"),
                recommendations = recommendationsMap,
                dataSource = "Gemini AI Analysis",
                lastVerified = System.currentTimeMillis().toString(),
                confidenceScore = 0.85,
                searchCount = 1,
                addedBy = "AI",
                createdAt = System.currentTimeMillis().toString(),
                
                // Defaults for fields not always in basic AI prompt to save tokens
                calcium = 0.0, iron = 0.0, magnesium = 0.0, omega3 = 0.0, omega6 = 0.0,
                antioxidantLevel = "Unknown", inflammatoryIndex = "Unknown",
                lastUpdated = "", diabetesTypes = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseRecommendationsMap(json: JSONObject?, userDiabetesType: String?): Map<String, ConditionRecommendation> {
        val map = mutableMapOf<String, ConditionRecommendation>()
        if (json == null) return map

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = json.optJSONObject(key)
            if (obj != null) {
                // If the user has a specific type (e.g. "type_2") and the key matches, 
                // or if we are parsing general conditions
                map[key] = ConditionRecommendation(
                    condition = key.replace("_", " ").uppercase(),
                    safetyLevel = obj.optString("safetyLevel", "Unknown"),
                    reasoning = obj.optString("reasoning"),
                    keyPoints = jsonArrayToStringList(obj.optJSONArray("keyPoints")),
                    servingAdvice = obj.optString("servingAdvice"),
                    timingAdvice = obj.optString("timingAdvice"),
                    pairingSuggestions = jsonArrayToStringList(obj.optJSONArray("pairingSuggestions")),
                    // We assume alternatives are global in the main JSON, but if nested:
                    alternatives = emptyList(), 
                    bloodSugarImpact = obj.optString("bloodSugarImpact"),
                    bloodPressureImpact = obj.optString("bloodPressureImpact"),
                    heartHealthImpact = obj.optString("heartHealthImpact"),
                    nutritionalBenefits = emptyList(), // Can be expanded if prompt requests it
                    potentialConcerns = emptyList(),
                    preparationTips = emptyList()
                )
            }
        }
        return map
    }

    // -------------------------------------------------------------------------
    // HELPER FUNCTIONS
    // -------------------------------------------------------------------------

    private fun sanitizeJson(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    private fun jsonArrayToStringList(jsonArray: org.json.JSONArray?): List<String> {
        val list = mutableListOf<String>()
        if (jsonArray != null) {
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.optString(i))
            }
        }
        return list
    }

    /**
     * Extracts a number from a JSON field that might be a string like "15g" or "10-12".
     */
    private fun extractDouble(json: JSONObject, vararg keys: String): Double {
        for (key in keys) {
            if (json.has(key)) {
                val raw = json.optString(key)
                // Remove non-numeric characters except dot
                val clean = raw.replace(Regex("[^0-9.]"), "")
                return clean.toDoubleOrNull() ?: 0.0
            }
        }
        return 0.0
    }

    private fun extractInt(json: JSONObject, vararg keys: String): Int {
        return extractDouble(json, *keys).toInt()
    }

    private fun generateFoodId(name: String): String {
        return name.lowercase(Locale.ROOT)
            .trim()
            .replace(Regex("[^a-z0-9]"), "_")
            .take(40)
    }

    private fun handleGeminiError(exception: Exception): String {
        val message = exception.message?.lowercase() ?: "unknown error"
        return when {
            message.contains("apikey") || message.contains("unauthenticated") -> "API Key Error. Please check settings."
            message.contains("resource exhausted") || message.contains("quota") -> "AI Daily Limit Reached."
            message.contains("unavailable") || message.contains("timeout") -> "AI Server Busy. Try again in a moment."
            message.contains("connect") || message.contains("network") -> "Internet connection failed."
            else -> "Analysis failed: ${message.take(50)}..."
        }
    }

    // -------------------------------------------------------------------------
    // PROMPTS
    // -------------------------------------------------------------------------

    private fun createGeminiPrompt(foodName: String, userConditions: List<String>, diabetesType: String?): String {
        val conditions = (userConditions + listOfNotNull(diabetesType)).joinToString(", ")

        return """
            Act as an expert dietitian and endocrinologist. Analyze "$foodName" for a patient with: $conditions.
            
            Return strictly valid JSON containing NO markdown formatting. Use this exact structure:
            {
              "basicInfo": {
                "name": "$foodName",
                "category": "Food Category",
                "servingSize": "e.g. 1 medium apple or 100g",
                "householdMeasure": "visual size",
                "estimatedNutrition": {
                  "calories": "number only",
                  "totalCarbohydrates": "number only",
                  "fiber": "number only",
                  "sugars": "number only",
                  "protein": "number only",
                  "totalFat": "number only",
                  "saturatedFat": "number only",
                  "sodium": "number only",
                  "potassium": "number only"
                },
                "glycemicInfo": {
                  "glycemicIndex": "number 0-100",
                  "glycemicLoad": "number",
                  "glycemicImpact": "Low/Medium/High"
                }
              },
              "medicalAnalysis": {
                "diabetes_summary": {
                  "safetyLevel": "Safe/Moderate/Avoid",
                  "reasoning": "Medical explanation regarding blood sugar",
                  "keyPoints": ["Short bullet point 1", "Short bullet point 2"],
                  "servingAdvice": "Specific portion advice",
                  "bloodSugarImpact": "Spike/Slow Rise/Neutral"
                },
                "heart_health": {
                   "safetyLevel": "Safe/Caution",
                   "reasoning": "Impact on BP/Cholesterol",
                   "pairingSuggestions": ["Food pairing idea"]
                }
              },
              "nutritionalDensity": "High/Medium/Low"
            }
        """.trimIndent()
    }

    private fun createMealSuggestionPrompt(userConditions: List<String>, diabetesType: String?): String {
        return """
            Create a simple 1-day meal plan for: ${diabetesType ?: "General Health"}, Conditions: ${userConditions.joinToString()}.
            Format nicely with:
            ## Breakfast
            ## Lunch
            ## Dinner
            ## Snacks
            *Keep it brief and focus on low-glycemic options.*
        """.trimIndent()
    }
}