package com.example.diabite.data.repository

import com.example.diabite.data.model.Alternative
import com.example.diabite.data.model.ConditionRecommendation
import com.example.diabite.data.model.FoodItem
import com.example.diabite.data.model.NutritionalBenefit
import com.example.diabite.data.model.PotentialConcern
import com.example.diabite.data.model.PreparationTip
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class FirestoreFoodRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : FoodRepository {

    private val foodCollection = firestore.collection("foodItems")
    private val maxRetries = 3
    private val baseDelayMs = 1000L

    override fun searchFood(query: String, userConditions: List<String>, diabetesType: String?): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            val normalizedQuery = query.trim().lowercase()

            if (normalizedQuery.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            // Combine userConditions and diabetesType for comprehensive filtering
            val allConditions = (userConditions + listOfNotNull(diabetesType)).distinct()

            // Search with retry logic - simplified approach
            val result = retryOperation {
                // Get all foods and filter in memory (more reliable than complex queries)
                val allFoodsQuery = foodCollection
                    .limit(200) // Reasonable limit to avoid loading too much data
                    .get()
                    .await()

                val allFoods = allFoodsQuery.documents.mapNotNull { document ->
                    try {
                        document.toFoodItem()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse food document: ${document.id}")
                        null
                    }
                }

                // Filter foods that match the search query
                var matchingFoods = allFoods.filter { food ->
                    val foodName = food.name.lowercase()
                    val foodNormalizedName = food.normalizedName.lowercase()

                    // Match if query is contained in name or normalized name
                    foodName.contains(normalizedQuery) ||
                    foodNormalizedName.contains(normalizedQuery) ||
                    // Also check if the food name starts with the query
                    foodName.startsWith(normalizedQuery)
                }

                // Sort by relevance (exact matches first, then prefix matches, then contains)
                matchingFoods = matchingFoods.sortedWith(compareByDescending { food ->
                    val foodName = food.name.lowercase()
                    when {
                        foodName == normalizedQuery -> 3 // Exact match
                        foodName.startsWith(normalizedQuery) -> 2 // Starts with
                        else -> 1 // Contains
                    }
                })

                // Limit results
                matchingFoods = matchingFoods.take(50)

                // If user conditions (including diabetesType) are provided, filter out explicitly unsafe foods
                if (allConditions.isNotEmpty()) {
                    matchingFoods = matchingFoods.filter { food ->
                        // The food is KEPT if it is NOT marked as 'bad' or 'avoid' for ANY condition
                        val isExplicitlyUnsafe = allConditions.any { condition ->
                            val recommendation = food.recommendations[condition]
                            recommendation?.safetyLevel?.let { level ->
                                level.contains("bad", ignoreCase = true) ||
                                level.contains("avoid", ignoreCase = true)
                            } ?: false
                        }
                        !isExplicitlyUnsafe // Keep food if it is NOT explicitly unsafe
                    }
                }

                Timber.d("Search for '$query' found ${matchingFoods.size} results")
                matchingFoods
            }

            emit(Resource.success(result))

        } catch (e: Exception) {
            Timber.e(e, "Search food failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getFoodById(foodId: String): Flow<Resource<FoodItem?>> = flow {
        emit(Resource.loading())

        try {
            if (foodId.isEmpty()) {
                emit(Resource.success(null))
                return@flow
            }

            val result = retryOperation {
                val document = foodCollection.document(foodId).get().await()
                if (document.exists()) {
                    document.toFoodItem()
                } else {
                    null
                }
            }

            emit(Resource.success(result))

        } catch (e: Exception) {
            Timber.e(e, "Get food by ID failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getAlternatives(foodId: String): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            if (foodId.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            // First get the original food to understand its alternatives
            val originalFood = retryOperation {
                val document = foodCollection.document(foodId).get().await()
                if (document.exists()) document.toFoodItem() else null
            }

            if (originalFood == null) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            // Get alternative food IDs from all recommendations
            val alternativeIds = originalFood.recommendations.values
                .flatMap { it.alternatives }
                .map { it.foodId }
                .distinct()

            if (alternativeIds.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            // Fetch alternative foods
            val alternatives = retryOperation {
                // Firestore 'in' queries are limited to 10 items
                val chunks = alternativeIds.chunked(10)
                val allDocs = mutableListOf<FoodItem>()
                
                for (chunk in chunks) {
                     val documents = foodCollection
                        .whereIn("id", chunk)
                        .get()
                        .await()
                     
                     allDocs.addAll(documents.documents.mapNotNull { document ->
                        try {
                            document.toFoodItem()
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to parse alternative food document")
                            null
                        }
                    })
                }
                allDocs
            }

            emit(Resource.success(alternatives))

        } catch (e: Exception) {
            Timber.e(e, "Get alternatives failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getFoodByCategory(category: String): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            val normalizedCategory = category.trim().lowercase()

            if (normalizedCategory.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            val result = retryOperation {
                val querySnapshot = foodCollection
                    .whereEqualTo("category", normalizedCategory)
                    .limit(100)
                    .get()
                    .await()

                querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toFoodItem()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse food document")
                        null
                    }
                }
            }

            emit(Resource.success(result))

        } catch (e: Exception) {
            Timber.e(e, "Get food by category failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getPersonalizedRecommendations(
        userConditions: List<String>,
        limit: Int
    ): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            if (userConditions.isEmpty()) {
                // Return general healthy foods if no conditions specified
                val result = retryOperation {
                    foodCollection
                        .whereEqualTo("nutritionalDensity", "High")
                        .limit(limit.toLong())
                        .get()
                        .await()
                        .documents
                        .mapNotNull { document ->
                            try {
                                document.toFoodItem()
                            } catch (e: Exception) {
                                Timber.w(e, "Failed to parse food document")
                                null
                            }
                        }
                }
                emit(Resource.success(result))
                return@flow
            }

            // Get foods that have recommendations for user's conditions
            val result = retryOperation {
                val querySnapshot = foodCollection
                    .limit(limit.toLong() * 2) // Get more to filter
                    .get()
                    .await()

                val allFoods = querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toFoodItem()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse food document")
                        null
                    }
                }

                // Filter foods that have safe/positive recommendations for user's conditions
                val recommendedFoods = allFoods.filter { food ->
                    userConditions.any { condition ->
                        val recommendation = food.recommendations[condition]
                        recommendation?.safetyLevel?.let { level ->
                            level.contains("good", ignoreCase = true) ||
                            level.contains("moderate", ignoreCase = true)
                        } ?: false
                    }
                }

                // Sort by nutritional density and limit results
                recommendedFoods
                    .sortedByDescending { food ->
                        when (food.nutritionalDensity) {
                            "high" -> 3
                            "medium" -> 2
                            "low" -> 1
                            else -> 0
                        }
                    }
                    .take(limit)
            }

            emit(Resource.success(result))

        } catch (e: Exception) {
            Timber.e(e, "Get personalized recommendations failed")
            emit(Resource.firebaseError(e))
        }
    }

    /**
     * Save or update a FoodItem into Firestore
     */
    fun saveFoodItem(food: FoodItem) = flow {
        emit(Resource.loading<Boolean>())

        try {
            retryOperation {
                // Use the food.id as the document id
                foodCollection.document(food.id).set(food).await()
            }

            emit(Resource.success(true))
        } catch (e: Exception) {
            Timber.e(e, "Save food item failed")
            emit(Resource.firebaseError<Boolean>(e))
        }
    }

    /**
     * Retry operation with exponential backoff
     */
    private suspend fun <T> retryOperation(
        maxRetries: Int = this.maxRetries,
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null

        for (attempt in 0 until maxRetries) {
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                // Don't retry on cancellation or certain Firestore errors
                if (e is CancellationException ||
                    (e is FirebaseFirestoreException &&
                     e.code in listOf(
                         FirebaseFirestoreException.Code.PERMISSION_DENIED,
                         FirebaseFirestoreException.Code.INVALID_ARGUMENT,
                         FirebaseFirestoreException.Code.NOT_FOUND
                     ))
                ) {
                    throw e
                }

                if (attempt < maxRetries - 1) {
                    val delayMs = baseDelayMs * (1L shl attempt) // Exponential backoff
                    delay(delayMs)
                }
            }
        }

        throw lastException ?: Exception("Operation failed after $maxRetries attempts")
    }
}

/**
 * Extension function to convert Firestore document to FoodItem
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toFoodItem(): FoodItem? {
    return try {
        FoodItem(
            id = getString("id") ?: id,
            name = getString("name") ?: "",
            normalizedName = getString("normalizedName") ?: "",
            category = getString("category") ?: "",
            subcategory = getString("subcategory") ?: "",
            servingSize = getString("servingSize") ?: "",
            householdMeasure = getString("householdMeasure") ?: "",
            calories = getLong("calories")?.toInt() ?: 0,
            totalCarbohydrates = getDouble("totalCarbohydrates") ?: 0.0,
            netCarbs = getDouble("netCarbs") ?: 0.0,
            fiber = getDouble("fiber") ?: 0.0,
            sugars = getDouble("sugars") ?: 0.0,
            addedSugars = getDouble("addedSugars") ?: 0.0,
            protein = getDouble("protein") ?: 0.0,
            totalFat = getDouble("totalFat") ?: 0.0,
            saturatedFat = getDouble("saturatedFat") ?: 0.0,
            transFat = getDouble("transFat") ?: 0.0,
            cholesterol = getDouble("cholesterol") ?: 0.0,
            sodium = getDouble("sodium") ?: 0.0,
            potassium = getDouble("potassium") ?: 0.0,
            calcium = getDouble("calcium") ?: 0.0,
            iron = getDouble("iron") ?: 0.0,
            magnesium = getDouble("magnesium") ?: 0.0,
            glycemicIndex = getLong("glycemicIndex")?.toInt() ?: 0,
            glycemicLoad = getDouble("glycemicLoad") ?: 0.0,
            omega3 = getDouble("omega3") ?: 0.0,
            omega6 = getDouble("omega6") ?: 0.0,
            antioxidantLevel = getString("antioxidantLevel") ?: "",
            inflammatoryIndex = getString("inflammatoryIndex") ?: "",
            glycemicImpact = getString("glycemicImpact") ?: "",
            nutritionalDensity = getString("nutritionalDensity") ?: "",
            recommendations = (get("recommendations") as? Map<String, Any>)?.mapValues { (_, value) ->
                val recMap = value as? Map<String, Any> ?: emptyMap()
                ConditionRecommendation(
                    condition = recMap["condition"] as? String ?: "",
                    safetyLevel = recMap["safetyLevel"] as? String ?: "",
                    reasoning = recMap["reasoning"] as? String ?: "",
                    keyPoints = (recMap["keyPoints"] as? List<String>) ?: emptyList(),
                    servingAdvice = recMap["servingAdvice"] as? String ?: "",
                    timingAdvice = recMap["timingAdvice"] as? String ?: "",
                    pairingSuggestions = (recMap["pairingSuggestions"] as? List<String>) ?: emptyList(),
                    alternatives = (recMap["alternatives"] as? List<Map<String, Any>>)?.mapNotNull { altMap ->
                        Alternative(
                            foodId = altMap["foodId"] as? String ?: "",
                            advantage = altMap["advantage"] as? String ?: "",
                            improvement = altMap["improvement"] as? String ?: "",
                            bestFor = (altMap["bestFor"] as? List<String>) ?: emptyList()
                        )
                    } ?: emptyList(),
                    bloodSugarImpact = recMap["bloodSugarImpact"] as? String ?: "",
                    bloodPressureImpact = recMap["bloodPressureImpact"] as? String ?: "",
                    heartHealthImpact = recMap["heartHealthImpact"] as? String ?: "",
                    kidneyImpact = recMap["kidneyImpact"] as? String ?: "",
                    alternativeReasoning = recMap["alternativeReasoning"] as? String ?: "",
                    nutritionalBenefits = (recMap["nutritionalBenefits"] as? List<Map<String, Any>>)?.map { benMap ->
                        NutritionalBenefit(
                            category = benMap["category"] as? String ?: "",
                            description = benMap["description"] as? String ?: "",
                            strength = benMap["strength"] as? String ?: ""
                        )
                    } ?: emptyList(),
                    potentialConcerns = (recMap["potentialConcerns"] as? List<Map<String, Any>>)?.map { conMap ->
                        PotentialConcern(
                            category = conMap["category"] as? String ?: "",
                            description = conMap["description"] as? String ?: "",
                            severity = conMap["severity"] as? String ?: ""
                        )
                    } ?: emptyList(),
                    preparationTips = (recMap["preparationTips"] as? List<Map<String, Any>>)?.map { tipMap ->
                        PreparationTip(
                            category = tipMap["category"] as? String ?: "",
                            description = tipMap["description"] as? String ?: ""
                        )
                    } ?: emptyList()
                )
            } ?: emptyMap(),
            dataSource = getString("dataSource") ?: "",
            lastVerified = getString("lastVerified") ?: "",
            confidenceScore = getDouble("confidenceScore") ?: 0.0,
            searchCount = getLong("searchCount")?.toInt() ?: 0,
            addedBy = getString("addedBy") ?: "",
            createdAt = getString("createdAt") ?: "",
            lastUpdated = getString("lastUpdated") ?: "",
            diabetesTypes = (get("diabetesTypes") as? List<String>) ?: emptyList()
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to parse FoodItem document: $id")
        null
    }
}
