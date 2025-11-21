package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.local.CacheManager
import com.example.diabite.data.model.FoodItem
import com.example.diabite.data.model.User
import com.example.diabite.data.model.UserHistory
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.domain.repository.FoodRepository
import com.example.diabite.util.FoodNormalizer
import com.example.diabite.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val cachedFoodRepository: com.example.diabite.data.repository.CachedFoodRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
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

    private val _aiInProgress = MutableStateFlow(false)
    val aiInProgress: StateFlow<Boolean> = _aiInProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isEmptyState = MutableStateFlow(true)
    val isEmptyState: StateFlow<Boolean> = _isEmptyState.asStateFlow()

    private val _isDiabetesTypeMissing = MutableStateFlow(false)
    val isDiabetesTypeMissing: StateFlow<Boolean> = _isDiabetesTypeMissing.asStateFlow()

    private val _canDoAISearch = MutableStateFlow(false)
    val canDoAISearch: StateFlow<Boolean> = _canDoAISearch.asStateFlow()

    private var currentUser: User? = null

    init {
        val initialQuery = savedStateHandle.get<String>("query") ?: ""
        _searchQuery.value = initialQuery

        observeUserChanges()
        setupSearchFlow()
        observeAiProgress()
    }

    private fun observeAiProgress() {
        viewModelScope.launch {
            cachedFoodRepository.aiInProgress.collect { inProgress ->
                _aiInProgress.value = inProgress
            }
        }
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

                    // Listen to search history from subcollection
                    val uid = user?.uid ?: firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        // Migrate any legacy data first
                        migrateLegacyHistoryData(uid)

                        val historyCollection = firestore.collection("users").document(uid).collection("history")
                        historyCollection.addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Timber.w(error, "Listen for search history failed")
                                _searchHistory.value = emptyList()
                                return@addSnapshotListener
                            }

                            val historyItems = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(UserHistory::class.java)
                            }?.sortedByDescending { it.eatenAt }?.map { it.foodId } ?: emptyList()

                            Timber.d("SearchViewModel: observed ${historyItems.size} history items: $historyItems")
                            _searchHistory.value = historyItems
                        }
                    } else {
                        _searchHistory.value = emptyList()
                    }

                    // Check if diabetes type is missing
                    _isDiabetesTypeMissing.value = user != null && diabetesType.isBlank()
                }
        }
    }

    private fun migrateLegacyHistoryData(uid: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                    if (user != null && user.searchHistory.isNotEmpty()) {
                        Timber.d("SearchViewModel: Migrating ${user.searchHistory.size} legacy history items")
                        val batch = firestore.batch()
                        user.searchHistory.forEach { query ->
                            val historyRef = firestore.collection("users").document(uid)
                                .collection("history").document(query)
                            batch.set(historyRef, UserHistory(foodId = query, eatenAt = java.util.Date() as java.util.Date))
                        }
                        batch.commit().await()

                        // Clear the legacy array
                        firestore.collection("users").document(uid)
                            .update("searchHistory", emptyList<String>())
                            .await()
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to migrate legacy history data")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        // Clear any previous errors when starting a new search
        _error.value = null
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
                            // Save successful database search to history
                            if (foods.isNotEmpty()) {
                                saveSearchToHistory(searchQuery.value)
                            }
                            // Show AI search option if no results found in database
                            _canDoAISearch.value = foods.isEmpty()
                        }
                        is Resource.Error -> {
                            _error.value = resource.error?.userMessage ?: "Search failed"
                            _searchResults.value = emptyList()
                            _isEmptyState.value = true
                            // Allow AI search as fallback when database search fails
                            _canDoAISearch.value = true
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

    fun performAdvancedAISearch() {
        val currentQuery = searchQuery.value
        if (currentQuery.isNotBlank()) {
            _isLoading.value = true
            _error.value = null

            viewModelScope.launch {
                try {
                    // Perform AI search directly using the repository method
                    val aiResult = cachedFoodRepository.searchFoodWithAI(
                        query = currentQuery,
                        userConditions = _userConditions.value,
                        diabetesType = _userDiabetesType.value
                    )

                    aiResult.collect { resource ->
                        _isLoading.value = resource is Resource.Loading
                        when (resource) {
                            is Resource.Success -> {
                                val foods = resource.data ?: emptyList()
                                if (foods.isNotEmpty()) {
                                    _searchResults.value = foodNormalizer.removeDuplicates(foods).sortedByDescending {
                                        foodNormalizer.getSearchRelevanceScore(currentQuery, it.name)
                                    }
                                    _isEmptyState.value = false
                                    _canDoAISearch.value = false
                                    // Save successful AI search to history
                                    saveSearchToHistory(currentQuery)
                                } else {
                                    _error.value = "No food information found"
                                    _isEmptyState.value = true
                                }
                            }
                            is Resource.Error -> {
                                _error.value = resource.error?.userMessage ?: "AI search failed"
                                _isEmptyState.value = true
                            }
                            else -> Unit
                        }
                    }
                } catch (e: Exception) {
                    _error.value = "AI search failed: ${e.localizedMessage ?: "Unknown error"}"
                    _isLoading.value = false
                    _isEmptyState.value = true
                }
            }
        }
    }
}
