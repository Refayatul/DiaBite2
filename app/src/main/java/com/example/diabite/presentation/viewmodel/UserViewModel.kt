package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Derived state flows for UI convenience
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
                .catch {
                    // In a real app, you might want to log this error
                }
                .collect { user ->
                    _user.value = user
                    _favoriteFoodIds.value = user?.favoriteFoodIds ?: emptyList()
                    _searchHistory.value = user?.searchHistory ?: emptyList()
                }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            _isClearingHistory.value = true
            authRepository.clearSearchHistory().collect { result ->
                _isClearingHistory.value = false
                // The local state will be updated via observeUserChanges
            }
        }
    }

    fun addSearchToHistory(query: String) {
        viewModelScope.launch {
            authRepository.addSearchToHistory(query).collect { result ->
                // The local state will be updated via observeUser
            }
        }
    }

    fun toggleFavoriteFood(foodId: String) {
        viewModelScope.launch {
            if (_favoriteFoodIds.value.contains(foodId)) {
                authRepository.removeFavoriteFood(foodId).collect { result ->
                    // The local state will be updated via observeUser
                }
            } else {
                authRepository.addFavoriteFood(foodId).collect { result ->
                    // The local state will be updated via observeUser
                }
            }
        }
    }
}
