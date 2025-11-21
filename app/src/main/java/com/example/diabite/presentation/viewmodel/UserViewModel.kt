package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.User
import com.example.diabite.data.model.UserHistory
import com.example.diabite.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class UserViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _favoriteFoodIds = MutableStateFlow<List<String>>(emptyList())
    val favoriteFoodIds: StateFlow<List<String>> = _favoriteFoodIds.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _isClearingHistory = MutableStateFlow(false)
    val isClearingHistory: StateFlow<Boolean> = _isClearingHistory.asStateFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .collectLatest { user ->
                    _user.value = user
                    Timber.d("UserViewModel: Received user snapshot: uid=${user?.uid} email=${user?.email} name=${user?.name}")
                    Timber.d("UserViewModel: favoriteFoodIds=${user?.favoriteFoodIds}")
                    _favoriteFoodIds.value = user?.favoriteFoodIds ?: emptyList()

                    // Observe search history from subcollection
                    val uid = user?.uid ?: firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        val historyCollection = firestore.collection("users").document(uid).collection("history")
                        historyCollection.addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Timber.w(error, "Listen for search history failed")
                                return@addSnapshotListener
                            }

                            val historyItems = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(UserHistory::class.java)
                            }?.sortedByDescending { it.eatenAt }?.map { it.foodId } ?: emptyList()

                            Timber.d("UserViewModel: observed ${historyItems.size} history items: $historyItems")
                            _searchHistory.value = historyItems
                        }
                    } else {
                        _searchHistory.value = emptyList()
                    }
                }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            // Optimistic UI update
            _searchHistory.value = emptyList()
            _isClearingHistory.value = true
            authRepository.clearSearchHistory().collect { result ->
                _isClearingHistory.value = false
                // If this failed, the next observeUser will bring the old history back,
                // which is a reasonable fallback.
            }
        }
    }

    fun addSearchToHistory(query: String) {
        viewModelScope.launch {
            // Optimistic UI update
            val currentHistory = _searchHistory.value.toMutableList()
            currentHistory.remove(query) // Remove if exists to add it to the top
            currentHistory.add(0, query)
            _searchHistory.value = currentHistory

            authRepository.addSearchToHistory(query).collect { result ->
                when (result) {
                    is com.example.diabite.util.Resource.Error -> {
                        // Revert optimistic update on error
                        val revertHistory = _searchHistory.value.toMutableList()
                        revertHistory.remove(query)
                        _searchHistory.value = revertHistory
                    }
                    else -> {
                        // Success - the Firestore listener will update the UI with the latest data
                        // No need to do anything here as observeUser() will handle the update
                    }
                }
            }
        }
    }

    fun toggleFavoriteFood(foodId: String) {
        viewModelScope.launch {
            val currentFavorites = _favoriteFoodIds.value.toMutableList()
            val wasFavorite = currentFavorites.contains(foodId)

            // Optimistic UI update
            if (wasFavorite) {
                currentFavorites.remove(foodId)
            } else {
                currentFavorites.add(foodId)
            }
            _favoriteFoodIds.value = currentFavorites

            // Backend call
            val result = if (wasFavorite) {
                authRepository.removeFavoriteFood(foodId)
            } else {
                authRepository.addFavoriteFood(foodId)
            }

            result.collect { resource ->
                when (resource) {
                    is com.example.diabite.util.Resource.Error -> {
                        // Revert optimistic update on error
                        val revertFavorites = _favoriteFoodIds.value.toMutableList()
                        if (wasFavorite) {
                            revertFavorites.add(foodId) // Add back if removal failed
                        } else {
                            revertFavorites.remove(foodId) // Remove if addition failed
                        }
                        _favoriteFoodIds.value = revertFavorites
                    }
                    else -> {
                        // Success - the Firestore listener will update the UI with the latest data
                        // No need to do anything here as observeUser() will handle the update
                    }
                }
            }
        }
    }
}
