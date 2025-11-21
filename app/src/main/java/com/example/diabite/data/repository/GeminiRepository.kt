package com.example.diabite.data.repository

import com.example.diabite.BuildConfig
import com.example.diabite.data.model.*
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import timber.log.Timber
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.time.Instant
import java.util.Locale

class GeminiRepository(
        private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            // Lower temperature to make JSON output more deterministic
            temperature = 0.2f
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
                Timber.d("=== GEMINI RAW RESPONSE ===\n$cleanJson")
                var foodItem = parseGeminiResponseToFoodItem(cleanJson, foodName, diabetesType)

                if (foodItem != null) {
                    Timber.d("=== PARSED FOODITEM ===\nID: ${foodItem.id}\nName: ${foodItem.name}\nRecommendations Keys: ${foodItem.recommendations.keys}\nRecommendations Count: ${foodItem.recommendations.size}")
                    foodItem.recommendations.forEach { (k, v) ->
                        Timber.d("  Key: $k | SafetyLevel: ${v.safetyLevel} | PersonalizedAdvice: ${v.personalizedAdvice}")
                    }
                }

                // Post-parse validation: if important nested fields are missing, attempt one targeted re-query
                if (foodItem != null) {
                    foodItem = validateAndPopulateMissingFields(foodItem, cleanJson, foodName, userConditions, diabetesType)
                }
                
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
            // First try 'medicalAnalysis', if null try 'recommendations', otherwise empty
            var medicalAnalysis = root.optJSONObject("medicalAnalysis")
            if (medicalAnalysis == null) {
                medicalAnalysis = root.optJSONObject("recommendations")
            }
            Timber.d("parseGeminiResponseToFoodItem: medicalAnalysis = $medicalAnalysis")
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
                
                    // Metadata and provenance
                    nutritionalDensity = root.optString("nutritionalDensity", "medium"),
                    recommendations = recommendationsMap,
                    dataSource = root.optString("dataSource", "Gemini AI Analysis"),
                    // Prefer ISO timestamps if provided, otherwise use now
                    lastVerified = root.optString("lastVerified", Instant.now().toString()),
                    confidenceScore = root.optDouble("confidenceScore", 0.85),
                    searchCount = root.optInt("searchCount", 1),
                    addedBy = root.optString("addedBy", "AI"),
                    createdAt = root.optString("createdAt", Instant.now().toString()),
                    uploadedAt = root.optString("uploadedAt", Instant.now().toString()),
                    uploadedBy = root.optString("uploadedBy", ""),
                    version = root.optInt("version", 1),

                    // Nutrient fields (try to parse from AI response if available)
                    calcium = extractDouble(nutrition, "calcium"),
                    iron = extractDouble(nutrition, "iron"),
                    magnesium = extractDouble(nutrition, "magnesium"),
                    omega3 = extractDouble(nutrition, "omega3"),
                    omega6 = extractDouble(nutrition, "omega6"),

                    antioxidantLevel = root.optString("antioxidantLevel", "medium"),
                    inflammatoryIndex = root.optString("inflammatoryIndex", "unknown"),
                    lastUpdated = root.optString("lastUpdated", ""),
                    diabetesTypes = jsonArrayToStringList(root.optJSONArray("diabetesTypes"))
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseRecommendationsMap(json: JSONObject?, userDiabetesType: String?): Map<String, ConditionRecommendation> {
        val map = mutableMapOf<String, ConditionRecommendation>()
        if (json == null) {
            Timber.d("parseRecommendationsMap: json is null, returning empty map")
            return map
        }

        val keys = json.keys()
        Timber.d("parseRecommendationsMap: processing ${json.length()} keys from medicalAnalysis")
        
        while (keys.hasNext()) {
            val key = keys.next()
            Timber.d("  Parsing key: '$key' (userDiabetesType: '$userDiabetesType')")
            
            val obj = json.optJSONObject(key)
            if (obj != null) {
                // If the user has a specific type (e.g. "type_2") and the key matches, 
                // or if we are parsing general conditions
                // Parse nested arrays/maps for a richer ConditionRecommendation
                val alternatives = mutableListOf<Alternative>()
                val altArray = obj.optJSONArray("alternatives")
                if (altArray != null) {
                    for (i in 0 until altArray.length()) {
                        val altObj = altArray.optJSONObject(i)
                        if (altObj != null) {
                            alternatives.add(
                                Alternative(
                                    foodId = altObj.optString("foodId", ""),
                                    advantage = altObj.optString("advantage", ""),
                                    improvement = altObj.optString("improvement", ""),
                                    bestFor = jsonArrayToStringList(altObj.optJSONArray("bestFor"))
                                )
                            )
                        }
                    }
                }

                val nutritionalBenefits = mutableListOf<NutritionalBenefit>()
                val benefitsArray = obj.optJSONArray("nutritionalBenefits")
                if (benefitsArray != null) {
                    for (i in 0 until benefitsArray.length()) {
                        val ben = benefitsArray.optJSONObject(i)
                        if (ben != null) {
                            nutritionalBenefits.add(
                                NutritionalBenefit(
                                    category = ben.optString("category", ""),
                                    description = ben.optString("description", ""),
                                    strength = ben.optString("strength", "")
                                )
                            )
                        }
                    }
                }

                val potentialConcerns = mutableListOf<PotentialConcern>()
                val concernsArray = obj.optJSONArray("potentialConcerns")
                if (concernsArray != null) {
                    for (i in 0 until concernsArray.length()) {
                        val con = concernsArray.optJSONObject(i)
                        if (con != null) {
                            potentialConcerns.add(
                                PotentialConcern(
                                    category = con.optString("category", ""),
                                    description = con.optString("description", ""),
                                    severity = con.optString("severity", "")
                                )
                            )
                        }
                    }
                }

                val preparationTips = mutableListOf<PreparationTip>()
                val tipsArray = obj.optJSONArray("preparationTips")
                if (tipsArray != null) {
                    for (i in 0 until tipsArray.length()) {
                        val tip = tipsArray.optJSONObject(i)
                        if (tip != null) {
                            preparationTips.add(
                                PreparationTip(
                                    category = tip.optString("category", ""),
                                    description = tip.optString("description", "")
                                )
                            )
                        }
                    }
                }

                // Normalize the incoming key so UI expects keys like `diabetes_type1` / `diabetes_type2`
                val normalizedKey = run {
                    val k = key.lowercase()
                    // If model returned a generic "diabetes_summary", map it to the user's diabetes type when available
                    if (k.contains("diabetes") && (k.contains("summary") || k.contains("diabetes_summary"))) {
                        userDiabetesType?.let { // replicate UI normalization here
                            when (it.lowercase()) {
                                "diabetes type 1", "type 1", "diabetes_type_1", "diabetes_type1" -> "diabetes_type1"
                                "diabetes type 2", "type 2", "diabetes_type_2", "diabetes_type2" -> "diabetes_type2"
                                else -> it.lowercase().replace(" ", "_")
                            }
                        } ?: k.replace(" ", "_")
                    } else if (k.contains("type") && k.contains("1")) {
                        "diabetes_type1"
                    } else if (k.contains("type") && k.contains("2")) {
                        "diabetes_type2"
                    } else {
                        k.replace(" ", "_")
                    }
                }

                Timber.d("    Original key: '$key' -> Normalized: '$normalizedKey'")

                // Extract personalized advice if present
                val personalized = obj.optString("personalizedDiabetesAdvice", obj.optString("personalizedAdvice", ""))

                map[normalizedKey] = ConditionRecommendation(
                    condition = normalizedKey.replace("_", " ").uppercase(),
                    safetyLevel = obj.optString("safetyLevel", "Unknown"),
                    reasoning = obj.optString("reasoning"),
                    personalizedAdvice = personalized,
                    keyPoints = jsonArrayToStringList(obj.optJSONArray("keyPoints")),
                    servingAdvice = obj.optString("servingAdvice"),
                    timingAdvice = obj.optString("timingAdvice"),
                    pairingSuggestions = jsonArrayToStringList(obj.optJSONArray("pairingSuggestions")),
                    alternatives = alternatives,
                    bloodSugarImpact = obj.optString("bloodSugarImpact"),
                    bloodPressureImpact = obj.optString("bloodPressureImpact"),
                    heartHealthImpact = obj.optString("heartHealthImpact"),
                    nutritionalBenefits = nutritionalBenefits,
                    potentialConcerns = potentialConcerns,
                    preparationTips = preparationTips
                )
            }
        }
        return map
    }

    /**
     * If the parsed FoodItem is missing important nested `medicalAnalysis` details,
     * attempt one targeted re-query to the model asking only for the missing part
     * (e.g., `medicalAnalysis`) and merge results. This keeps the number of calls low
     * while improving completeness.
     */
    private suspend fun validateAndPopulateMissingFields(
        item: FoodItem,
        originalJsonString: String,
        foodName: String,
        userConditions: List<String>,
        diabetesType: String?
    ): FoodItem {
        try {
            // Quick checks: if recommendations map is empty or doesn't contain expected keys
            if (item.recommendations.isEmpty()) {
                Timber.w("validateAndPopulateMissingFields: Recommendations are EMPTY for $foodName. Re-querying Gemini...")
                // Build a short prompt asking only for medicalAnalysis for the food
                val prompt = buildString {
                    append("Return ONLY a JSON object named \"medicalAnalysis\" for \"")
                    append(foodName)
                    append("\" with keys: diabetes_summary (for Type 1/Type 2 advice with personalizedDiabetesAdvice, alternatives, nutritionalBenefits, potentialConcerns, preparationTips), and optionally heart_health. Return STRICT JSON ONLY, no markdown, no explanation.")
                }

                val resp = try {
                    generativeModel.generateContent(prompt)
                } catch (e: Exception) {
                    Timber.e(e, "Re-query failed")
                    null
                }

                val text = resp?.text
                if (!text.isNullOrBlank()) {
                    val clean = sanitizeJson(text)
                    Timber.d("Re-query response: $clean")
                    try {
                        val newRoot = JSONObject("{\"medicalAnalysis\": $clean }")
                        val newMed = newRoot.optJSONObject("medicalAnalysis")
                        if (newMed != null) {
                            val parsed = parseRecommendationsMap(newMed, diabetesType)
                            Timber.d("Re-query parsed into ${parsed.size} recommendation(s)")
                            // Merge: prefer existing keys, but fill missing ones
                            val merged = item.recommendations.toMutableMap()
                            for ((k, v) in parsed) {
                                if (!merged.containsKey(k)) merged[k] = v
                            }
                            return item.copy(recommendations = merged)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Re-query parsing failed")
                    }
                } else {
                    Timber.w("Re-query returned empty response")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Validation error")
        }

        return item
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

            Return strictly valid JSON containing NO markdown formatting. Use this exact structure with comprehensive medical analysis:
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
                "diabetes_type1": {
                  "condition": "DIABETES TYPE 1",
                  "safetyLevel": "good",
                  "reasoning": "Detailed medical explanation regarding blood sugar impact for Type 1 diabetes",
                  "personalizedAdvice": "Specific advice for Type 1 diabetes management with this food",
                  "keyPoints": [
                    "Detailed point about insulin dosing",
                    "Point about blood sugar monitoring",
                    "Point about meal timing",
                    "Point about portion control"
                  ],
                  "servingAdvice": "Specific portion advice for Type 1 diabetes",
                  "timingAdvice": "Best timing for consumption",
                  "pairingSuggestions": ["protein sources", "healthy fats", "non-starchy vegetables"],
                  "bloodSugarImpact": "medium",
                  "bloodPressureImpact": "neutral",
                  "heartHealthImpact": "positive",
                  "kidneyImpact": "low",
                  "alternativeReasoning": "Detailed explanation of why alternatives are recommended for Type 1 diabetes",
                  "alternatives": [
                    {
                      "foodId": "alternative_food_name",
                      "advantage": "Why this alternative is better for blood sugar control",
                      "improvement": "How to prepare or what makes it better",
                      "bestFor": ["diabetes_type1", "diabetes_type2"]
                    },
                    {
                      "foodId": "another_alternative",
                      "advantage": "Different advantage for variety",
                      "improvement": "Different preparation method",
                      "bestFor": ["diabetes_type1"]
                    }
                  ],
                  "nutritionalBenefits": [
                    {
                      "category": "blood_sugar",
                      "description": "How this food affects blood glucose levels",
                      "strength": "strong"
                    },
                    {
                      "category": "fiber",
                      "description": "Fiber content and digestive benefits",
                      "strength": "moderate"
                    },
                    {
                      "category": "vitamins",
                      "description": "Vitamin content and health benefits",
                      "strength": "moderate"
                    }
                  ],
                  "potentialConcerns": [
                    {
                      "category": "portion_control",
                      "description": "Risk of overconsumption affecting blood sugar",
                      "severity": "moderate"
                    },
                    {
                      "category": "glycemic_variability",
                      "description": "How preparation method affects blood sugar response",
                      "severity": "low"
                    }
                  ],
                  "preparationTips": [
                    {
                      "category": "portion",
                      "description": "How to measure appropriate portions"
                    },
                    {
                      "category": "pairing",
                      "description": "Best foods to combine with for blood sugar control"
                    },
                    {
                      "category": "timing",
                      "description": "Optimal timing relative to insulin dosing"
                    }
                  ]
                },
                "diabetes_type2": {
                  "condition": "DIABETES TYPE 2",
                  "safetyLevel": "good",
                  "reasoning": "Detailed medical explanation regarding blood sugar and insulin sensitivity for Type 2 diabetes",
                  "personalizedAdvice": "Specific advice for Type 2 diabetes management with this food",
                  "keyPoints": [
                    "Point about insulin resistance",
                    "Point about weight management",
                    "Point about plate method",
                    "Point about monitoring"
                  ],
                  "servingAdvice": "Specific portion advice for Type 2 diabetes",
                  "timingAdvice": "Best timing for blood sugar control",
                  "pairingSuggestions": ["lean proteins", "healthy fats", "high-fiber foods"],
                  "bloodSugarImpact": "medium",
                  "bloodPressureImpact": "neutral",
                  "heartHealthImpact": "positive",
                  "kidneyImpact": "low",
                  "alternativeReasoning": "Detailed explanation of why alternatives help with Type 2 diabetes management",
                  "alternatives": [
                    {
                      "foodId": "alternative_food_name",
                      "advantage": "Why this alternative is better for insulin sensitivity",
                      "improvement": "How to prepare for better blood sugar control",
                      "bestFor": ["diabetes_type2", "weight_management"]
                    },
                    {
                      "foodId": "another_alternative",
                      "advantage": "Different advantage for metabolic health",
                      "improvement": "Different preparation method",
                      "bestFor": ["diabetes_type2"]
                    }
                  ],
                  "nutritionalBenefits": [
                    {
                      "category": "blood_sugar",
                      "description": "Impact on postprandial glucose and insulin response",
                      "strength": "strong"
                    },
                    {
                      "category": "weight_management",
                      "description": "Satiety and calorie density benefits",
                      "strength": "moderate"
                    },
                    {
                      "category": "heart_health",
                      "description": "Cardiovascular benefits for diabetes patients",
                      "strength": "moderate"
                    }
                  ],
                  "potentialConcerns": [
                    {
                      "category": "overconsumption",
                      "description": "Risk of excessive carbohydrate intake",
                      "severity": "moderate"
                    },
                    {
                      "category": "preparation_method",
                      "description": "How cooking method affects nutrient profile",
                      "severity": "low"
                    }
                  ],
                  "preparationTips": [
                    {
                      "category": "portion",
                      "description": "How to control portions for blood sugar management"
                    },
                    {
                      "category": "pairing",
                      "description": "Best combinations for stable blood sugar"
                    },
                    {
                      "category": "timing",
                      "description": "Optimal timing for metabolic health"
                    }
                  ]
                },
                "heart_health": {
                  "condition": "HEART HEALTH",
                  "safetyLevel": "good",
                  "reasoning": "Impact on cardiovascular health, blood pressure, and cholesterol",
                  "pairingSuggestions": ["foods that support heart health", "anti-inflammatory foods"]
                }
              },
              "nutritionalDensity": "High/Medium/Low",
              "dataSource": "Gemini AI Analysis",
              "confidenceScore": 0.85,
              "lastVerified": "${java.time.Instant.now()}",
              "addedBy": "AI"
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
