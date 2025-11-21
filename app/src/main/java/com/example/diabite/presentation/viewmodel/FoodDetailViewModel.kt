package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.ConditionNormalizer
import com.example.diabite.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
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

    // Favorite status
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Expanded sections state - all sections expanded by default
    private val _expandedSections = MutableStateFlow(setOf("nutrition", "diabetes_advice", "alternatives"))
    val expandedSections: StateFlow<Set<String>> = _expandedSections.asStateFlow()

    init {
        loadUserAndFoodDetails()
    }

    private fun loadUserAndFoodDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch user data
                val user = authRepository.getCurrentUser().first()
                val diabetesType = user?.diabetesType?.let { ConditionNormalizer.normalizeCondition(it) }
                _userDiabetesType.value = diabetesType

                // User conditions derived from diabetes type only
                _userConditions.value = listOfNotNull(diabetesType)

                // Check favorite status from favorites subcollection
                checkFavoriteStatus()

                // Then load food details
                loadFoodDetails()
            } catch (e: Exception) {
                _error.value = "Could not load user profile. Advice may not be personalized."
                loadFoodDetails() // Still try to load food details even if user profile fails
            }
        }
    }

    private fun loadFoodDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            foodRepository.getFoodById(foodId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val food = resource.data
                        _foodItem.value = food
                        if (food != null) {
                            loadAlternatives(food.id)
                            // Save the actually opened item to user search history
                            try {
                                authRepository.addSearchToHistory(food.name).collect {}
                            } catch (e: Exception) {
                                // Ignore history write failures; non-critical
                            }
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
        }
    }

    private fun checkFavoriteStatus() {
        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) {
                    val favoriteDoc = firestore.collection("users")
                        .document(uid)
                        .collection("favorites")
                        .document(foodId)
                        .get()
                        .await()

                    _isFavorite.value = favoriteDoc.exists()
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to check favorite status")
                _isFavorite.value = false
            }
        }
    }

    private fun loadAlternatives(foodId: String) {
        viewModelScope.launch {
            foodRepository.getAlternatives(foodId).collect {
                _alternatives.value = if (it is Resource.Success) it.data ?: emptyList() else emptyList()
            }
        }
    }

    fun toggleFavoriteStatus() {
        viewModelScope.launch {
            val currentlyFavorite = _isFavorite.value
            val action = if (currentlyFavorite) {
                authRepository.removeFavoriteFood(foodId)
            } else {
                authRepository.addFavoriteFood(foodId)
            }

            action.collect { resource ->
                when (resource) {
                    is Resource.Loading -> Timber.d("toggleFavoriteStatus: loading")
                    is Resource.Success -> {
                        Timber.d("toggleFavoriteStatus: success")
                        _isFavorite.value = !currentlyFavorite
                    }
                    is Resource.Error -> {
                        val err = resource.error
                        _error.value = err?.userMessage ?: "Failed to update favorite status."
                        Timber.e(err?.technicalMessage ?: "Favorite update failed: ${err?.userMessage}")
                        Timber.d("Favorite toggle failed: code=${err?.code} userMessage=${err?.userMessage} technical=${err?.technicalMessage}")
                    }
                }
            }
        }
    }

    fun toggleSection(sectionKey: String) {
        val currentExpanded = _expandedSections.value.toMutableSet()
        if (currentExpanded.contains(sectionKey)) {
            currentExpanded.remove(sectionKey)
        } else {
            currentExpanded.add(sectionKey)
        }
        _expandedSections.value = currentExpanded
    }

    fun retry() {
        loadUserAndFoodDetails()
    }
}
