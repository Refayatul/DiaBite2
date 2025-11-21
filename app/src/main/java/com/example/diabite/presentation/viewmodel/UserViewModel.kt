
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

    private var favoritesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var historyListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        observeUser()

        // Also set up listeners if user is already authenticated
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            setupSubcollectionListeners(currentUser.uid)
        }
    }

    private fun observeUser() {
        // Listen to Firebase Auth state changes
        firebaseAuth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                // User logged out
                _user.value = null
                _favoriteFoodIds.value = emptyList()
                _searchHistory.value = emptyList()
                setupSubcollectionListeners(null)
                return@addAuthStateListener
            }

            // User logged in, set up listener on user document
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            userDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.w(error, "Listen for user profile failed.")
                    _user.value = null
                    _favoriteFoodIds.value = emptyList()
                    _searchHistory.value = emptyList()
                    setupSubcollectionListeners(null)
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

                    // Load data from user document (legacy support)
                    _favoriteFoodIds.value = user.favoriteFoodIds
                    _searchHistory.value = user.searchHistory

                    // Set up listeners for the current user (will override with subcollection data if available)
                    setupSubcollectionListeners(user.uid)
                } else {
                    val defaultUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: ""
                    )
                    _user.value = defaultUser
                    _favoriteFoodIds.value = emptyList()
                    _searchHistory.value = emptyList()
                    setupSubcollectionListeners(defaultUser.uid)
                }
            }
        }
    }

    private fun setupSubcollectionListeners(uid: String?) {
        if (uid == null) {
            // Remove existing listeners if user is null
            favoritesListener?.remove()
            favoritesListener = null
            historyListener?.remove()
            historyListener = null
            return
        }

        Timber.d("UserViewModel: Setting up listeners for user $uid")

        // Remove existing listeners
        favoritesListener?.remove()
        historyListener?.remove()

        // First, check if there's existing data in user document that needs migration
        migrateLegacyData(uid)

        // Listen to favorites subcollection
        val favoritesCollection = firestore.collection("users").document(uid).collection("favorites")
        favoritesListener = favoritesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.w(error, "Listen for favorites failed")
                _favoriteFoodIds.value = emptyList()
                return@addSnapshotListener
            }

            val favoriteIds = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(com.example.diabite.data.model.UserFavorite::class.java)?.foodId
            } ?: emptyList()

            Timber.d("UserViewModel: observed ${favoriteIds.size} favorites: $favoriteIds")
            _favoriteFoodIds.value = favoriteIds
        }

        // Listen to history subcollection
        val historyCollection = firestore.collection("users").document(uid).collection("history")
        historyListener = historyCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.w(error, "Listen for search history failed")
                _searchHistory.value = emptyList()
                return@addSnapshotListener
            }

            val historyItems = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(UserHistory::class.java)
            }?.sortedByDescending { it.eatenAt }?.map { it.foodId } ?: emptyList()

            Timber.d("UserViewModel: observed ${historyItems.size} history items: $historyItems")
            _searchHistory.value = historyItems
        }
    }

    private fun migrateLegacyData(uid: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    val user = userDoc.toObject(User::class.java)
                    if (user != null) {
                        // Migrate favorites from array to subcollection
                        val legacyFavorites = user.favoriteFoodIds
                        if (legacyFavorites.isNotEmpty()) {
                            Timber.d("UserViewModel: Migrating ${legacyFavorites.size} legacy favorites")
                            val batch = firestore.batch()
                            legacyFavorites.forEach { foodId ->
                                val favoriteRef = firestore.collection("users").document(uid)
                                    .collection("favorites").document(foodId)
                                batch.set(favoriteRef, com.example.diabite.data.model.UserFavorite(foodId = foodId, addedAt = java.util.Date() as java.util.Date))
                            }
                            batch.commit().await()

                            // Clear the legacy array
                            firestore.collection("users").document(uid)
                                .update("favoriteFoodIds", emptyList<String>())
                                .await()
                        }

                        // Migrate search history from array to subcollection
                        val legacyHistory = user.searchHistory
                        if (legacyHistory.isNotEmpty()) {
                            Timber.d("UserViewModel: Migrating ${legacyHistory.size} legacy history items")
                            val batch = firestore.batch()
                            legacyHistory.forEach { query ->
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
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to migrate legacy data")
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

            // Backend call
            val result = if (wasFavorite) {
                authRepository.removeFavoriteFood(foodId)
            } else {
                authRepository.addFavoriteFood(foodId)
            }

            result.collect { resource ->
                when (resource) {
                    is com.example.diabite.util.Resource.Error -> {
                        Timber.w("UserViewModel.toggleFavoriteFood: failed to toggle favorite foodId=$foodId, wasFavorite=$wasFavorite, error=${resource.error?.userMessage}")
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
                        Timber.d("UserViewModel.toggleFavoriteFood: success toggling favorite for $foodId wasFavorite=$wasFavorite")
                        // Success - the Firestore listener will update the UI with the latest data
                        // No need to do anything here as observeUser() will handle the update
                    }
                }
            }
        }
    }
}
