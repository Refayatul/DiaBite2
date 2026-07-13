package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val foodId: String = checkNotNull(savedStateHandle["foodId"])

    // Food data state
    private val _foodItem = MutableStateFlow<FoodItem?>(null)
    val foodItem: StateFlow<FoodItem?> = _foodItem.asStateFlow()

    // Alternatives state
    private val _alternatives = MutableStateFlow<List<FoodItem>>(emptyList())
    val alternatives: StateFlow<List<FoodItem>> = _alternatives.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Expanded sections state
    private val _expandedSections = MutableStateFlow<Set<String>>(emptySet())
    val expandedSections: StateFlow<Set<String>> = _expandedSections.asStateFlow()

    init {
        loadFoodDetails()
    }

    /**
     * Load food details and alternatives
     */
    private fun loadFoodDetails() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load main food item
                val foodResult = foodRepository.getFoodById(foodId)
                foodResult.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val food = resource.data
                            _foodItem.value = food
                            if (food != null) {
                                // Load alternatives
                                loadAlternatives(food.id)
                            }
                        }
                        is Resource.Error -> {
                            _error.value = resource.error?.userMessage ?: "Failed to load food details"
                        }
                        is Resource.Loading -> {
                            // Keep loading state
                        }
                    }
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load alternative foods
     */
    private fun loadAlternatives(foodId: String) {
        viewModelScope.launch {
            try {
                val alternativesResult = foodRepository.getAlternatives(foodId)
                alternativesResult.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _alternatives.value = resource.data ?: emptyList()
                        }
                        is Resource.Error -> {
                            // Alternatives are optional, don't show error for this
                            _alternatives.value = emptyList()
                        }
                        is Resource.Loading -> {
                            // Keep current state
                        }
                    }
                }
            } catch (e: Exception) {
                // Alternatives are optional
                _alternatives.value = emptyList()
            }
        }
    }

    /**
     * Toggle section expansion
     */
    fun toggleSection(sectionKey: String) {
        val currentExpanded = _expandedSections.value.toMutableSet()
        if (currentExpanded.contains(sectionKey)) {
            currentExpanded.remove(sectionKey)
        } else {
            currentExpanded.add(sectionKey)
        }
        _expandedSections.value = currentExpanded
    }

    /**
     * Check if section is expanded
     */
    fun isSectionExpanded(sectionKey: String): Boolean {
        return _expandedSections.value.contains(sectionKey)
    }

    /**
     * Retry loading food details
     */
    fun retry() {
        loadFoodDetails()
    }

    /**
     * Get safety rating for user's conditions
     */
    fun getSafetyRating(foodItem: FoodItem, userConditions: List<String>): String {
        if (userConditions.isEmpty()) return "Unknown"

        val relevantRecommendations = userConditions.mapNotNull { condition ->
            foodItem.recommendations[condition]
        }

        if (relevantRecommendations.isEmpty()) return "Unknown"

        // Determine overall safety based on recommendations
        val safetyLevels = relevantRecommendations.map { it.safetyLevel }

        return when {
            safetyLevels.any { it.contains("Avoid", ignoreCase = true) } -> "Avoid"
            safetyLevels.any { it.contains("Caution", ignoreCase = true) } -> "Caution"
            safetyLevels.any { it.contains("Safe", ignoreCase = true) } -> "Safe"
            safetyLevels.any { it.contains("Good", ignoreCase = true) } -> "Good"
            safetyLevels.any { it.contains("Recommended", ignoreCase = true) } -> "Recommended"
            else -> "Unknown"
        }
    }

    /**
     * Get primary concern for user's conditions
     */
    fun getPrimaryConcern(foodItem: FoodItem, userConditions: List<String>): String? {
        if (userConditions.isEmpty()) return null

        val relevantRecommendations = userConditions.mapNotNull { condition ->
            foodItem.recommendations[condition]
        }

        // Return the most concerning advice
        return relevantRecommendations
            .flatMap { it.keyPoints }
            .firstOrNull { point ->
                point.contains("high", ignoreCase = true) ||
                point.contains("concern", ignoreCase = true) ||
                point.contains("limit", ignoreCase = true) ||
                point.contains("avoid", ignoreCase = true)
            }
    }

    /**
     * Get serving advice for user's conditions
     */
    fun getServingAdvice(foodItem: FoodItem, userConditions: List<String>): String? {
        if (userConditions.isEmpty()) return null

        val relevantRecommendations = userConditions.mapNotNull { condition ->
            foodItem.recommendations[condition]
        }

        return relevantRecommendations
            .mapNotNull { it.servingAdvice }
            .firstOrNull { it.isNotBlank() }
    }
}
