package com.example.diabite.domain.repository

import com.example.diabite.data.model.FoodItem
import com.example.diabite.util.Resource
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    /**
     * Search for food items by query string
     * @param query The search query (food name)
     * @param userConditions List of user\'s medical conditions for personalized results
     * @param diabetesType The user\'s diabetes type for specific dietary considerations
     * @return Flow of Resource containing list of matching FoodItems
     */
    fun searchFood(query: String, userConditions: List<String> = emptyList(), diabetesType: String? = null): Flow<Resource<List<FoodItem>>>

    /**
     * Get a specific food item by its ID
     * @param foodId The unique identifier of the food item
     * @return Flow of Resource containing the FoodItem or null if not found
     */
    fun getFoodById(foodId: String): Flow<Resource<FoodItem?>>

    /**
     * Get alternative food suggestions for a given food item
     * @param foodId The food item to find alternatives for
     * @return Flow of Resource containing list of alternative FoodItems
     */
    fun getAlternatives(foodId: String): Flow<Resource<List<FoodItem>>>

    /**
     * Get food items by category
     * @param category The food category to filter by
     * @return Flow of Resource containing list of FoodItems in the category
     */
    fun getFoodByCategory(category: String): Flow<Resource<List<FoodItem>>>

    /**
     * Get personalized food recommendations based on user conditions
     * @param userConditions List of user\'s medical conditions
     * @param limit Maximum number of recommendations to return
     * @return Flow of Resource containing list of recommended FoodItems
     */
    fun getPersonalizedRecommendations(
        userConditions: List<String>,
        limit: Int = 20
    ): Flow<Resource<List<FoodItem>>>
}
