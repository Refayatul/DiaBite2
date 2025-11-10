package com.example.diabite.data.repository

import com.example.diabite.data.model.Alternative
import com.example.diabite.data.model.ConditionRecommendation
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    override fun searchFood(query: String): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            val normalizedQuery = query.trim().lowercase()

            if (normalizedQuery.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            // Search with retry logic
            val result = retryOperation {
                // Search by normalized name (prefix matching)
                val querySnapshot = foodCollection
                    .whereGreaterThanOrEqualTo("normalizedName", normalizedQuery)
                    .whereLessThanOrEqualTo("normalizedName", normalizedQuery + "\uf8ff")
                    .limit(50)
                    .get()
                    .await()

                val foods = querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toFoodItem()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse food document")
                        null // Skip malformed documents
                    }
                }

                // If no results with prefix search, try contains search
                if (foods.isEmpty()) {
                    val containsQuerySnapshot = foodCollection
                        .whereArrayContains("searchTokens", normalizedQuery)
                        .limit(50)
                        .get()
                        .await()

                    containsQuerySnapshot.documents.mapNotNull { document ->
                        try {
                            document.toFoodItem()
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to parse food document")
                            null
                        }
                    }
                } else {
                    foods
                }
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

            // Get alternative food IDs from the original food
            val alternativeIds = originalFood.primaryAlternatives.map { it.foodId }

            if (alternativeIds.isEmpty()) {
                emit(Resource.success(emptyList()))
                return@flow
            }

            // Fetch alternative foods
            val alternatives = retryOperation {
                val documents = foodCollection
                    .whereIn("id", alternativeIds.take(10)) // Firestore limit
                    .get()
                    .await()

                documents.documents.mapNotNull { document ->
                    try {
                        document.toFoodItem()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse alternative food document")
                        null
                    }
                }
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
                        recommendation?.safetyLevel?.let { safety ->
                            safety.contains("Safe", ignoreCase = true) ||
                            safety.contains("Good", ignoreCase = true) ||
                            safety.contains("Recommended", ignoreCase = true)
                        } ?: false
                    }
                }

                // Sort by nutritional density and limit results
                recommendedFoods
                    .sortedByDescending { food ->
                        when (food.nutritionalDensity) {
                            "High" -> 3
                            "Medium" -> 2
                            "Low" -> 1
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

    /**
     * Map Firestore exceptions to more user-friendly exceptions
     */
    private fun mapFirestoreException(exception: Exception): Exception {
        return when (exception) {
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        Exception("Network connection error. Please check your internet connection.")
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        Exception("Access denied. Please check your permissions.")
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        Exception("Food data not found.")
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                        Exception("Too many requests. Please try again later.")
                    else ->
                        Exception("Database error occurred. Please try again.")
                }
            }
            else -> exception
        }
    }
}

/**
 * Extension function to convert Firestore document to FoodItem
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toFoodItem(): FoodItem {
    return FoodItem(
        id = getString("id") ?: id,
        name = getString("name") ?: "",
        normalizedName = getString("normalizedName") ?: "",
        category = getString("category") ?: "",
        calories = getLong("calories")?.toInt() ?: 0,
        carbs = getDouble("carbs") ?: 0.0,
        fiber = getDouble("fiber") ?: 0.0,
        sugars = getDouble("sugars") ?: 0.0,
        protein = getDouble("protein") ?: 0.0,
        totalFat = getDouble("totalFat") ?: 0.0,
        saturatedFat = getDouble("saturatedFat") ?: 0.0,
        sodium = getDouble("sodium") ?: 0.0,
        potassium = getDouble("potassium") ?: 0.0,
        glycemicIndex = getLong("glycemicIndex")?.toInt(),
        glycemicLoad = getDouble("glycemicLoad"),
        recommendations = (get("recommendations") as? Map<String, Map<String, Any>>)?.mapValues { (_, recMap) ->
            ConditionRecommendation(
                safetyLevel = recMap["safetyLevel"] as? String ?: "",
                reasoning = recMap["reasoning"] as? String ?: "",
                keyPoints = (recMap["keyPoints"] as? List<String>) ?: emptyList(),
                servingAdvice = recMap["servingAdvice"] as? String ?: "",
                timingAdvice = recMap["timingAdvice"] as? String,
                pairingSuggestions = (recMap["pairingSuggestions"] as? List<String>) ?: emptyList(),
                alternatives = (recMap["alternatives"] as? List<String>) ?: emptyList(),
                bloodSugarImpact = recMap["bloodSugarImpact"] as? String,
                bloodPressureImpact = recMap["bloodPressureImpact"] as? String,
                heartHealthImpact = recMap["heartHealthImpact"] as? String
            )
        } ?: emptyMap(),
        primaryAlternatives = (get("primaryAlternatives") as? List<Map<String, Any>>)?.map { altMap ->
            Alternative(
                foodId = altMap["foodId"] as? String ?: "",
                advantage = altMap["advantage"] as? String ?: "",
                improvement = altMap["improvement"] as? String ?: "",
                bestFor = (altMap["bestFor"] as? List<String>) ?: emptyList()
            )
        } ?: emptyList(),
        alternativeReasoning = getString("alternativeReasoning") ?: "",
        glycemicImpact = getString("glycemicImpact") ?: "",
        nutritionalDensity = getString("nutritionalDensity") ?: ""
    )
}
