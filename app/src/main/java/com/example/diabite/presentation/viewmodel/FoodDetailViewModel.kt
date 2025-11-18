package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.ConditionNormalizer
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val foodId: String = checkNotNull(savedStateHandle["foodId"])

    // Food data state
    private val _foodItem = MutableStateFlow<FoodItem?>(null)
    val foodItem: StateFlow<FoodItem?> = _foodItem.asStateFlow()

    // Alternatives state
    private val _alternatives = MutableStateFlow<List<FoodItem>>(emptyList())
    val alternatives: StateFlow<List<FoodItem>> = _alternatives.asStateFlow()

    // User profile state
    private val _userConditions = MutableStateFlow<List<String>>(emptyList())
    val userConditions: StateFlow<List<String>> = _userConditions.asStateFlow()
    private val _userDiabetesType = MutableStateFlow<String?>(null)
    val userDiabetesType: StateFlow<String?> = _userDiabetesType.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Expanded sections state
    private val _expandedSections = MutableStateFlow(setOf("nutrition", "conditions", "alternatives"))
    val expandedSections: StateFlow<Set<String>> = _expandedSections.asStateFlow()

    init {
        loadUserConditions()
        loadFoodDetails()
    }

    private fun loadUserConditions() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .catch { e ->
                    _error.value = "Could not load user profile. Advice may not be personalized."
                }
                .collect { user ->
                    // Normalize conditions to match food data keys (lowercase with underscores)
                    _userConditions.value = user?.primaryConditions?.map { ConditionNormalizer.normalizeCondition(it) } ?: emptyList()
                    _userDiabetesType.value = user?.diabetesType?.let { ConditionNormalizer.normalizeCondition(it) }
                }
        }
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
                foodRepository.getFoodById(foodId).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val food = resource.data
                            _foodItem.value = food
                            if (food != null) {
                                // Load alternatives
                                loadAlternatives(food.id)
                            }
                            _isLoading.value = false
                        }
                        is Resource.Error -> {
                            _error.value = resource.error?.userMessage ?: "Failed to load food details"
                            _isLoading.value = false
                        }
                        is Resource.Loading -> {
                            // Keep loading state
                        }
                    }
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
                foodRepository.getAlternatives(foodId).collect { resource ->
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
}
