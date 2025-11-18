package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.local.CacheManager
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
                    _userConditions.value = user?.primaryConditions ?: emptyList()
                    _userDiabetesType.value = user?.diabetesType ?: ""
                    _searchHistory.value = user?.searchHistory?.reversed() ?: emptyList()
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
            // You would expand this to clear history in Firestore
            _searchHistory.value = emptyList()
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
