
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
import kotlinx.coroutines.tasks.await
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

    private var userDocListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        observeUser()
    }

    private fun observeUser() {
        // Listen to Firebase Auth state changes
        firebaseAuth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                // User logged out
                Timber.d("UserViewModel: User logged out")
                _user.value = null
                _favoriteFoodIds.value = emptyList()
                _searchHistory.value = emptyList()
                userDocListener?.remove()
                userDocListener = null
                return@addAuthStateListener
            }

            Timber.d("UserViewModel: User logged in: ${firebaseUser.uid}")
            // Run migration for legacy favorites
            migrateLegacyFavorites()
            // User logged in, set up listener on user document
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            
            // Remove existing listener
            userDocListener?.remove()
            
            userDocListener = userDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.w(error, "Listen for user profile failed.")
                    _user.value = null
                    _favoriteFoodIds.value = emptyList()
                    _searchHistory.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Timber.d("UserViewModel: Received user snapshot: data=${snapshot.data}")
                    val user = snapshot.toObject(User::class.java) ?: User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: ""
                    )
                    _user.value = user

                    // Load data directly from snapshot for real-time updates
                    @Suppress("UNCHECKED_CAST")
                    val favoriteIds = snapshot.get("favoriteFoodIds") as? List<String> ?: user.favoriteFoodIds
                    @Suppress("UNCHECKED_CAST")
                    val searchHistory = snapshot.get("searchHistory") as? List<String> ?: user.searchHistory

                    Timber.d("UserViewModel: Updated favoriteFoodIds: $favoriteIds")
                    Timber.d("UserViewModel: Updated searchHistory: $searchHistory")
                    
                    _favoriteFoodIds.value = favoriteIds
                    _searchHistory.value = searchHistory
                } else {
                    Timber.d("UserViewModel: User document does not exist, creating default")
                    val defaultUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: ""
                    )
                    _user.value = defaultUser
                    _favoriteFoodIds.value = emptyList()
                    _searchHistory.value = emptyList()
                }
            }
        }
    }

    private fun migrateLegacyData(uid: String) {
        // Migration no longer needed - data is stored in root arrays
        Timber.d("UserViewModel: Legacy data migration skipped - using root arrays")
    }

    private fun migrateLegacyFavorites() {
        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) return@launch

                // Check if there are any favorites in the old subcollection
                val favoritesCollection = firestore.collection("users").document(uid).collection("favorites")
                val snapshot = favoritesCollection.get().await()

                if (!snapshot.isEmpty) {
                    Timber.d("UserViewModel: Found ${snapshot.size()} legacy favorites in subcollection, migrating...")

                    // Get current favorites from root array
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    @Suppress("UNCHECKED_CAST")
                    val currentFavorites = userDoc.get("favoriteFoodIds") as? List<String> ?: emptyList()
                    val migratedFavorites = currentFavorites.toMutableList()

                    // Add any favorites from subcollection that aren't already in root array
                    val batch = firestore.batch()
                    snapshot.documents.forEach { doc ->
                        val foodId = doc.id
                        if (!migratedFavorites.contains(foodId)) {
                            migratedFavorites.add(foodId)
                            Timber.d("UserViewModel: Migrating favorite: $foodId")
                        }
                        // Delete the old subcollection document
                        batch.delete(doc.reference)
                    }

                    // Update root array with migrated favorites
                    batch.update(firestore.collection("users").document(uid), "favoriteFoodIds", migratedFavorites)
                    batch.commit().await()

                    Timber.d("UserViewModel: Successfully migrated ${snapshot.size()} legacy favorites")
                }
            } catch (e: Exception) {
                Timber.w(e, "UserViewModel: Failed to migrate legacy favorites")
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
            Timber.d("UserViewModel.toggleFavoriteFood: toggling favorite for $foodId (currently favorites=${_favoriteFoodIds.value})")
            val currentFavorites = _favoriteFoodIds.value.toMutableList()
            val wasFavorite = currentFavorites.contains(foodId)

            // Optimistic UI update
            if (wasFavorite) {
                currentFavorites.remove(foodId)
            } else {
                currentFavorites.add(foodId)
            }
            _favoriteFoodIds.value = currentFavorites
            Timber.d("UserViewModel.toggleFavoriteFood: optimistic update applied: wasFavorite=$wasFavorite, newFavorites=$currentFavorites")

            // Backend call
            val result = if (wasFavorite) {
                authRepository.removeFavoriteFood(foodId)
            } else {
                authRepository.addFavoriteFood(foodId)
            }

            result.collect { resource ->
                when (resource) {
                    is com.example.diabite.util.Resource.Loading -> {
                        Timber.d("UserViewModel.toggleFavoriteFood: loading state")
                    }
                    is com.example.diabite.util.Resource.Success -> {
                        Timber.d("UserViewModel.toggleFavoriteFood: success toggling favorite for $foodId wasFavorite=$wasFavorite")
                        // Success - the Firestore listener will update the UI with the latest data
                        // No need to do anything here as observeUser() will handle the update
                    }
                    is com.example.diabite.util.Resource.Error -> {
                        Timber.w("UserViewModel.toggleFavoriteFood: failed to toggle favorite foodId=$foodId, wasFavorite=$wasFavorite, error=${resource.error?.userMessage} | code=${resource.error?.code}")
                        // Revert optimistic update on error
                        val revertFavorites = _favoriteFoodIds.value.toMutableList()
                        if (wasFavorite) {
                            revertFavorites.add(foodId) // Add back if removal failed
                        } else {
                            revertFavorites.remove(foodId) // Remove if addition failed
                        }
                        _favoriteFoodIds.value = revertFavorites
                        Timber.d("UserViewModel.toggleFavoriteFood: reverted optimistic update due to error")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listener when ViewModel is destroyed
        userDocListener?.remove()
        Timber.d("UserViewModel: listener cleaned up on onCleared()")
    }
}
