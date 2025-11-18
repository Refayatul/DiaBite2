package com.example.diabite.data.repository

import com.example.diabite.data.local.CacheManager
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedFoodRepository @Inject constructor(
    private val firestoreRepository: FirestoreFoodRepository,
    private val cacheManager: CacheManager,
    private val geminiRepository: GeminiRepository
) : FoodRepository {

    override fun searchFood(query: String, userConditions: List<String>, diabetesType: String?): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            // First, search Firestore for existing foods
            val firestoreResult = firestoreRepository.searchFood(query, userConditions, diabetesType)

            firestoreResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val foods = resource.data ?: emptyList()

                        if (foods.isNotEmpty()) {
                            // Found foods in Firestore - cache and return
                            foods.forEach { food ->
                                cacheManager.cacheFoodItemAsync(food)
                            }
                            emit(Resource.success(foods))
                        } else {
                            // No foods found in Firestore - try Gemini AI analysis
                            emitGeminiAnalysisResult(query, userConditions, diabetesType)
                        }
                    }
                    is Resource.Error -> {
                        // Firestore search failed - try Gemini as fallback
                        emitGeminiAnalysisResult(query, userConditions, diabetesType)
                    }
                    is Resource.Loading -> {
                        // Pass through loading state
                        emit(Resource.loading())
                    }
                }
            }
        } catch (e: Exception) {
            // Try Gemini as last resort
            emitGeminiAnalysisResult(query, userConditions, diabetesType)
        }
    }

    /**
     * Emit Gemini analysis result for a food query
     */
    private suspend fun kotlinx.coroutines.flow.FlowCollector<Resource<List<FoodItem>>>.emitGeminiAnalysisResult(query: String, userConditions: List<String>, diabetesType: String?) {
        try {
            geminiRepository.analyzeFood(query, userConditions, diabetesType).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val foodItem = resource.data
                        if (foodItem != null) {
                            // Cache the AI-generated food
                            cacheManager.cacheFoodItemAsync(foodItem)
                            emit(Resource.success(listOf(foodItem)))
                        } else {
                            emit(Resource.success(emptyList()))
                        }
                    }
                    is Resource.Error -> {
                        emit(Resource.error(resource.error ?: AppError.UnknownError("AI analysis failed")))
                    }
                    is Resource.Loading -> {
                        // Could emit loading state if needed
                        emit(Resource.loading())
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.error(AppError.fromException(e)))
        }
    }

    override fun getFoodById(foodId: String): Flow<Resource<FoodItem?>> = flow {
        emit(Resource.loading())

        try {
            // Check cache first
            val cachedFood = cacheManager.getCachedFoodItem(foodId)

            if (cachedFood != null) {
                // Cache hit - return cached data
                emit(Resource.success(cachedFood))
            } else {
                // Cache miss - fetch from Firestore
                val firestoreResult = firestoreRepository.getFoodById(foodId)

                firestoreResult.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val food = resource.data
                            if (food != null) {
                                // Cache the result for future use
                                cacheManager.cacheFoodItemAsync(food)
                            }
                            emit(Resource.success(food))
                        }
                        is Resource.Error -> {
                            emit(resource) // Pass through the error
                        }
                        is Resource.Loading -> {
                            emit(Resource.loading())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // If cache fails, try Firestore directly
            try {
                firestoreRepository.getFoodById(foodId).collect { resource ->
                    emit(resource)
                }
            } catch (firestoreException: Exception) {
                emit(Resource.error(AppError.fromException(Exception("Both cache and network failed: ${e.message}"))))
            }
        }
    }

    override fun getAlternatives(foodId: String): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            // For alternatives, we need to get the original food first to find alternatives
            // This is handled by Firestore repository, and we cache the results
            val firestoreResult = firestoreRepository.getAlternatives(foodId)

            firestoreResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val alternatives = resource.data ?: emptyList()

                        // Cache the alternative foods
                        alternatives.forEach { food ->
                            cacheManager.cacheFoodItemAsync(food)
                        }

                        emit(Resource.success(alternatives))
                    }
                    is Resource.Error -> {
                        emit(resource) // Pass through the error
                    }
                    is Resource.Loading -> {
                        emit(Resource.loading())
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to Firestore
            try {
                firestoreRepository.getAlternatives(foodId).collect { resource ->
                    emit(resource)
                }
            } catch (firestoreException: Exception) {
                emit(Resource.error(AppError.fromException(Exception("Both cache and network failed: ${e.message}"))))
            }
        }
    }

    override fun getFoodByCategory(category: String): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            // Category searches go through Firestore and cache results
            val firestoreResult = firestoreRepository.getFoodByCategory(category)

            firestoreResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val foods = resource.data ?: emptyList()

                        // Cache the results
                        foods.forEach { food ->
                            cacheManager.cacheFoodItemAsync(food)
                        }

                        emit(Resource.success(foods))
                    }
                    is Resource.Error -> {
                        emit(resource) // Pass through the error
                    }
                    is Resource.Loading -> {
                        emit(Resource.loading())
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to Firestore
            try {
                firestoreRepository.getFoodByCategory(category).collect { resource ->
                    emit(resource)
                }
            } catch (firestoreException: Exception) {
                emit(Resource.error(AppError.fromException(Exception("Both cache and network failed: ${e.message}"))))
            }
        }
    }

    override fun getPersonalizedRecommendations(
        userConditions: List<String>,
        limit: Int
    ): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            // Personalized recommendations are computed by Firestore repository
            val firestoreResult = firestoreRepository.getPersonalizedRecommendations(userConditions, limit)

            firestoreResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val foods = resource.data ?: emptyList()

                        // Cache the results
                        foods.forEach { food ->
                            cacheManager.cacheFoodItemAsync(food)
                        }

                        emit(Resource.success(foods))
                    }
                    is Resource.Error -> {
                        emit(resource) // Pass through the error
                    }
                    is Resource.Loading -> {
                        emit(Resource.loading())
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to Firestore
            try {
                firestoreRepository.getPersonalizedRecommendations(userConditions, limit).collect { resource ->
                    emit(resource)
                }
            } catch (firestoreException: Exception) {
                emit(Resource.error(AppError.fromException(Exception("Both cache and network failed: ${e.message}"))))
            }
        }
    }

    /**
     * Get cache statistics for debugging/monitoring
     */
    suspend fun getCacheStats() = cacheManager.getCacheStats()

    /**
     * Clear all cache (useful for debugging or user settings)
     */
    suspend fun clearCache() = cacheManager.clearAllCache()

    /**
     * Manually trigger cache cleanup
     */
    fun cleanupCache() = cacheManager.scheduleCleanup()
}
