package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.local.CacheManager
import com.example.diabite.data.model.FoodItem
import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.FoodNormalizer
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val authRepository: AuthRepository,
    private val cacheManager: CacheManager,
    private val foodNormalizer: FoodNormalizer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _userConditions = MutableStateFlow<List<String>>(emptyList())
    val userConditions: StateFlow<List<String>> = _userConditions.asStateFlow()

    private val _userDiabetesType = MutableStateFlow<String?>("")
    val userDiabetesType: StateFlow<String?> = _userDiabetesType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isEmptyState = MutableStateFlow(true)
    val isEmptyState: StateFlow<Boolean> = _isEmptyState.asStateFlow()

    // State to track if user needs to select a diabetes type
    private val _isDiabetesTypeMissing = MutableStateFlow(false)
    val isDiabetesTypeMissing: StateFlow<Boolean> = _isDiabetesTypeMissing.asStateFlow()
    
    // Current user object to update
    private var currentUser: User? = null

    init {
        val initialQuery = savedStateHandle.get<String>("query") ?: ""
        _searchQuery.value = initialQuery

        observeUserChanges()
        setupSearchFlow()
    }

    private fun observeUserChanges() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .catch { e -> _error.value = "Failed to load user profile." }
                .collect { user ->
                    currentUser = user
                    val diabetesType = user?.diabetesType ?: ""
                    _userDiabetesType.value = diabetesType
                    _userConditions.value = if (diabetesType.isNotEmpty()) listOf(diabetesType) else emptyList()
                    _searchHistory.value = user?.searchHistory?.reversed() ?: emptyList()
                    
                    // Check if diabetes type is missing
                    _isDiabetesTypeMissing.value = user != null && diabetesType.isBlank()
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isEmptyState.value = true
        }
    }

    fun updateUserDiabetesType(type: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val updatedUser = user.copy(diabetesType = type)
                authRepository.updateUserProfile(updatedUser).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            // Explicitly update state to dismiss dialog immediately
                            _userDiabetesType.value = type
                            _isDiabetesTypeMissing.value = false
                        }
                        is Resource.Error -> {
                            _error.value = "Failed to update profile: ${result.error?.userMessage}"
                        }
                        else -> {
                            // Handle Loading if needed
                        }
                    }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchFlow() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.isNotBlank() && it.length >= 2 }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    saveSearchToHistory(query)
                    performSearch(query)
                }
                .collect { resource ->
                    _isLoading.value = resource is Resource.Loading
                    when (resource) {
                        is Resource.Success -> {
                            val foods = resource.data ?: emptyList()
                            _searchResults.value = foodNormalizer.removeDuplicates(foods).sortedByDescending {
                                foodNormalizer.getSearchRelevanceScore(searchQuery.value, it.name)
                            }
                            _isEmptyState.value = foods.isEmpty()
                        }
                        is Resource.Error -> {
                            _error.value = resource.error?.userMessage ?: "Search failed"
                            _searchResults.value = emptyList()
                            _isEmptyState.value = true
                        }
                        else -> Unit
                    }
                }
        }
    }

    private fun performSearch(query: String) = foodRepository.searchFood(
        query = query,
        userConditions = _userConditions.value,
        diabetesType = _userDiabetesType.value
    )

    private fun saveSearchToHistory(query: String) {
        viewModelScope.launch {
            authRepository.addSearchToHistory(query).collect {}
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            authRepository.clearSearchHistory().collect { result ->
                if (result is Resource.Success) {
                    // History cleared successfully from Firebase, local state will be updated via observeUserChanges
                } else if (result is Resource.Error) {
                    _error.value = "Failed to clear search history: ${result.error?.userMessage}"
                }
            }
        }
    }

    fun retrySearch() {
        updateSearchQuery(searchQuery.value)
    }

    fun clearSearch() {
        updateSearchQuery("")
    }

    fun searchFromHistory(query: String) {
        updateSearchQuery(query)
    }
}
