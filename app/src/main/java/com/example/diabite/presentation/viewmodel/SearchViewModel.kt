package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.local.CacheManager
import com.example.diabite.data.model.CachedSearch
import com.example.diabite.data.model.FoodItem
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.FoodNormalizer
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val authRepository: AuthRepository,
    private val cacheManager: CacheManager,
    private val foodNormalizer: FoodNormalizer
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Search results state
    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults.asStateFlow()

    // Search history state
    private val _searchHistory = MutableStateFlow<List<CachedSearch>>(emptyList())
    val searchHistory: StateFlow<List<CachedSearch>> = _searchHistory.asStateFlow()

    // User conditions state
    private val _userConditions = MutableStateFlow<List<String>>(emptyList())
    val userConditions: StateFlow<List<String>> = _userConditions.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isEmptyState = MutableStateFlow(false)
    val isEmptyState: StateFlow<Boolean> = _isEmptyState.asStateFlow()

    private val _isAIGenerated = MutableStateFlow(false)
    val isAIGenerated: StateFlow<Boolean> = _isAIGenerated.asStateFlow()

    private val _confidenceScore = MutableStateFlow<Float?>(null)
    val confidenceScore: StateFlow<Float?> = _confidenceScore.asStateFlow()

    init {
        loadSearchHistory()
        loadUserConditions()
        setupSearchFlow()
    }

    /**
     * Load user conditions from auth repository
     */
    private fun loadUserConditions() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser().first()
                _userConditions.value = user?.primaryConditions ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Failed to load user profile"
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _error.value = null // Clear error when user starts typing

        // Show empty state when query is cleared
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isEmptyState.value = true
        }
    }

    /**
     * Perform search with debouncing
     */
    @OptIn(FlowPreview::class)
    private fun setupSearchFlow() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce
                .filter { it.isNotBlank() && it.length >= 2 } // Minimum 2 characters
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query, _userConditions.value).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                val foods = resource.data ?: emptyList()
                                val deduplicatedFoods = foodNormalizer.removeDuplicates(foods)
                                val sortedFoods = sortByRelevance(query, deduplicatedFoods)
                                _searchResults.value = sortedFoods
                                _isLoading.value = false
                                _isEmptyState.value = sortedFoods.isEmpty()
                            }
                            is Resource.Error -> {
                                _error.value = resource.error?.userMessage ?: "Search failed"
                                _searchResults.value = emptyList()
                                _isLoading.value = false
                                _isEmptyState.value = true
                            }
                            is Resource.Loading -> {
                                _isLoading.value = true
                            }
                        }
                    }
                }
        }
    }

    /**
     * Perform the actual search
     */
    private fun performSearch(query: String, userConditions: List<String>) = kotlinx.coroutines.flow.flow<Resource<List<FoodItem>>> { 
        try {
            emit(Resource.Loading<List<FoodItem>>()) 

            // Save search to history
            saveSearchToHistory(query)

            // Perform search with user conditions
            foodRepository.searchFood(query, userConditions).collect { result ->
                emit(result)
            }

        } catch (e: Exception) {
            emit(Resource.Error<List<FoodItem>>(com.example.diabite.util.AppError.fromException(e)))
        }
    }

    /**
     * Sort search results by relevance
     */
    private fun sortByRelevance(query: String, foods: List<FoodItem>): List<FoodItem> {
        return foods.sortedByDescending { food ->
            foodNormalizer.getSearchRelevanceScore(query, food.name)
        }
    }

    /**
     * Save search to history
     */
    private suspend fun saveSearchToHistory(query: String) {
        try {
            // Get current results for history (we'll use empty list for now, could be enhanced)
            val cachedSearch = CachedSearch(
                query = query,
                results = emptyList(), // Could be populated with result IDs
                resultCount = 0
            )

            // This would need to be added to FoodDao if we want to store search history
            // For now, we'll just keep it in memory
        } catch (e: Exception) {
            // Don't crash on history save failure
        }
    }

    /**
     * Load search history
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                // This would load from Room database
                // For now, we'll keep it empty
                _searchHistory.value = emptyList()
            } catch (e: Exception) {
                _searchHistory.value = emptyList()
            }
        }
    }

    /**
     * Clear search history
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            try {
                // Clear from database
                _searchHistory.value = emptyList()
            } catch (e: Exception) {
                _error.value = "Failed to clear search history"
            }
        }
    }

    /**
     * Retry last search
     */
    fun retrySearch() {
        val currentQuery = _searchQuery.value
        if (currentQuery.isNotBlank()) {
            updateSearchQuery(currentQuery)
        }
    }

    /**
     * Clear current search
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _error.value = null
        _isEmptyState.value = true
    }

    /**
     * Search from history
     */
    fun searchFromHistory(query: String) {
        updateSearchQuery(query)
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats() = cacheManager.getCacheStats()

    /**
     * Clear all cache
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                cacheManager.clearAllCache()
                _error.value = "Cache cleared successfully"
            } catch (e: Exception) {
                _error.value = "Failed to clear cache"
            }
        }
    }

    /**
     * Create success resource for testing
     */
    private fun <T> createSuccessResource(data: T): Resource<T> = Resource.success(data)
}
