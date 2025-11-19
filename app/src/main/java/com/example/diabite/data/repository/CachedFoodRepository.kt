package com.example.diabite.data.repository

import com.example.diabite.data.local.CacheManager
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedFoodRepository @Inject constructor(
    private val firestoreRepository: FirestoreFoodRepository,
    private val cacheManager: CacheManager,
    private val geminiRepository: GeminiRepository
) : FoodRepository {

    // Expose AI progress so ViewModel/UI can react to Gemini calls
    private val _aiInProgress = kotlinx.coroutines.flow.MutableStateFlow(false)
    val aiInProgress: kotlinx.coroutines.flow.StateFlow<Boolean> = _aiInProgress.asStateFlow()

    override fun searchFood(query: String, userConditions: List<String>, diabetesType: String?): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        try {
            // 1) Check local cache first (fast)
            val cached = cacheManager.searchCachedFoods(query)
            if (cached.isNotEmpty()) {
                // Return cached items immediately
                emit(Resource.success(cached))
                return@flow
            }

            // 2) Query Firestore
            val firestoreResult = firestoreRepository.searchFood(query, userConditions, diabetesType)

            var foundInFirestore = false

            firestoreResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val foods = resource.data ?: emptyList()

                        if (foods.isNotEmpty()) {
                            // Cache and return results
                            foods.forEach { food ->
                                cacheManager.cacheFoodItemAsync(food)
                            }
                            emit(Resource.success(foods))
                            foundInFirestore = true
                        } else {
                            // No results in Firestore; will fallthrough to AI below
                            emit(Resource.success(emptyList()))
                        }
                    }
                    is Resource.Error -> {
                        // Pass through the error but allow AI fallback
                        emit(Resource.error(AppError.UnknownError("Search failed: ${resource.error?.userMessage ?: "unknown"}")))
                    }
                    is Resource.Loading -> {
                        emit(Resource.loading())
                    }
                }
            }

            // 3) If not found in Firestore or cache, call Gemini automatically and persist result
            if (!foundInFirestore) {
                try {
                    // signal AI start
                    _aiInProgress.value = true

                    geminiRepository.analyzeFood(query, userConditions, diabetesType).collect { aiResource ->
                        when (aiResource) {
                            is Resource.Success -> {
                                val item = aiResource.data
                                if (item != null) {
                                    // Persist to Firestore and cache for future searches
                                    try {
                                        firestoreRepository.saveFoodItem(item).collect {}
                                    } catch (e: Exception) {
                                        // Log/passthrough — persistence failure shouldn't block showing the result
                                    }

                                    cacheManager.cacheFoodItemAsync(item)
                                    emit(Resource.success(listOf(item)))
                                } else {
                                    emit(Resource.success(emptyList()))
                                }
                            }
                            is Resource.Error -> {
                                emit(Resource.error(aiResource.error!!))
                            }
                            is Resource.Loading -> emit(Resource.loading())
                        }
                    }
                } catch (e: Exception) {
                    emit(Resource.error(AppError.fromException(e)))
                } finally {
                    // signal AI finished
                    _aiInProgress.value = false
                }
            }
        } catch (e: Exception) {
            emit(Resource.error(AppError.fromException(e)))
        }
    }

    /**
     * Search for food using AI analysis (manual trigger)
     */
    fun searchFoodWithAI(query: String, userConditions: List<String>, diabetesType: String?): Flow<Resource<List<FoodItem>>> = flow {
        emit(Resource.loading())

        // Perform AI analysis directly
        _aiInProgress.value = true
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
                        emit(Resource.error(resource.error!!)) // Pass through AI error
                    }
                    is Resource.Loading -> {
                        emit(Resource.loading())
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.error(AppError.fromException(e)))
        } finally {
            _aiInProgress.value = false
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
