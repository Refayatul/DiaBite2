package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.UserProfile
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                // Get current user first
                authRepository.getCurrentUser().collect { user ->
                    if (user != null) {
                        // In a real implementation, you would fetch the UserProfile from Firestore
                        // For now, we'll create a mock profile based on the user data
                        val mockProfile = UserProfile(
                            uid = user.uid,
                            email = user.email,
                            displayName = user.displayName,
                            dateOfBirth = null, // Would be fetched from Firestore
                            biologicalSex = user.biologicalSex,
                            primaryConditions = user.primaryConditions,
                            diabetesType = user.diabetesType,
                            diabetesMedications = emptyList() // Would be fetched from Firestore
                        )
                        _profileState.value = ProfileState.Success(mockProfile)
                    } else {
                        _profileState.value = ProfileState.Error("User not found")
                    }
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun updateProfile(
        primaryConditions: List<String>,
        diabetesType: String? = null,
        diabetesMedications: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            try {
                val currentState = _profileState.value
                if (currentState is ProfileState.Success) {
                    val currentProfile = currentState.profile

                    // Create updated profile
                    val updatedProfile = currentProfile.copy(
                        primaryConditions = primaryConditions,
                        diabetesType = diabetesType,
                        diabetesMedications = diabetesMedications,
                        lastUpdated = java.util.Date()
                    )

                    // Update user in repository (this would save to Firestore in real implementation)
                    val user = com.example.diabite.data.model.User(
                        uid = currentProfile.uid,
                        email = currentProfile.email,
                        displayName = currentProfile.displayName,
                        dateOfBirth = currentProfile.dateOfBirth?.let { java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()).format(it) } ?: "",
                        biologicalSex = currentProfile.biologicalSex,
                        primaryConditions = primaryConditions,
                        diabetesType = diabetesType ?: ""
                    )

                    authRepository.updateUserProfile(user).collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                _profileState.value = ProfileState.Success(updatedProfile)
                                _updateState.value = UpdateState.Success("Profile updated successfully")
                            }
                            is Resource.Error -> {
                                _updateState.value = UpdateState.Error(resource.error?.userMessage ?: "Failed to update profile")
                            }
                            is Resource.Loading -> {
                                // Keep loading state
                            }
                        }
                    }
                } else {
                    _updateState.value = UpdateState.Error("No profile loaded")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun refreshProfile() {
        loadUserProfile()
    }
}

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Loading : UpdateState()
    data class Success(val message: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
