package com.example.diabite.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Data class to hold the state for the multi-step registration process
data class RegistrationState(
    val email: String = "",
    val password: String = "",
    val name: String = "", // Renamed from displayName
    val dateOfBirth: String = "",
    val biologicalSex: String = "",
    val primaryConditions: List<String> = emptyList(),
    val diabetesType: String = "",
    val selectedMedications: List<String> = emptyList(),
    val otherMedication: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User?>>(Resource.loading())
    val authState: StateFlow<Resource<User?>> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _signUpState = MutableStateFlow<Resource<User?>>(Resource.loading())
    val signUpState: StateFlow<Resource<User?>> = _signUpState.asStateFlow()

    private val _loginState = MutableStateFlow<Resource<User?>>(Resource.loading())
    val loginState: StateFlow<Resource<User?>> = _loginState.asStateFlow()

    private val _googleSignInState = MutableStateFlow<Resource<User?>>(Resource.loading())
    val googleSignInState: StateFlow<Resource<User?>> = _googleSignInState.asStateFlow()

    private val _logoutState = MutableStateFlow<Resource<Unit>>(Resource.loading())
    val logoutState: StateFlow<Resource<Unit>> = _logoutState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<Resource<Unit>>(Resource.loading())
    val passwordResetState: StateFlow<Resource<Unit>> = _passwordResetState.asStateFlow()
    
    // State for the multi-step registration
    private val _registrationState = MutableStateFlow(RegistrationState())
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun updateRegistrationState(newState: RegistrationState) {
        _registrationState.value = newState
    }

    fun clearRegistrationData() {
        _registrationState.value = RegistrationState()
    }

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser().collect { user ->
                    _currentUser.value = user
                    _authState.value = Resource.success(user)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking current user")
                _authState.value = Resource.firebaseError(e)
            }
        }
    }

    // Simplified signUp method matching new schema
    fun signUp(
        email: String,
        password: String,
        name: String, // Renamed from displayName
        diabetesType: String = ""
    ) {
        // Validate input
        when {
            email.isBlank() -> {
                _signUpState.value = Resource.error(AppError.InvalidEmailError())
                return
            }
            password.length < 6 -> {
                _signUpState.value = Resource.error(AppError.InvalidPasswordError())
                return
            }
            name.isBlank() -> {
                _signUpState.value = Resource.error(AppError.MissingFieldError("name"))
                return
            }
        }

        _signUpState.value = Resource.loading()

        // Create User object with only required fields
        val user = User(
            email = email,
            name = name,
            diabetesType = diabetesType
        )

        viewModelScope.launch {
            try {
                authRepository.signUp(email, password, user).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val signedUpUser = resource.data
                            _currentUser.value = signedUpUser
                            _signUpState.value = Resource.success(signedUpUser)
                        }
                        is Resource.Error -> {
                            Timber.e(resource.error?.cause, "Sign up failed")
                            _signUpState.value = Resource.error(resource.error!!, resource.data)
                        }
                        is Resource.Loading -> {
                            _signUpState.value = Resource.loading()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Sign up error")
                _signUpState.value = Resource.firebaseError(e)
            }
        }
    }

    // Unified signUpWithProfile to act as main sign up entry point using RegistrationState
    fun signUpWithProfile(
        email: String,
        password: String,
        name: String, // Renamed
        dateOfBirth: String = "", // Kept for signature compatibility but not used
        biologicalSex: String = "", // Kept for signature compatibility but not used
        primaryConditions: List<String> = emptyList(), // Kept for signature compatibility but not used
        diabetesType: String = "",
        diabetesMedications: List<String> = emptyList() // Kept for signature compatibility but not used
    ) {
        // Forward to the simplified signUp
        signUp(email, password, name, diabetesType)
    }

    fun login(email: String, password: String) {
        // Validate input
        when {
            email.isBlank() -> {
                _loginState.value = Resource.error(AppError.InvalidEmailError())
                return
            }
            password.isBlank() -> {
                _loginState.value = Resource.error(AppError.MissingFieldError("password"))
                return
            }
        }

        _loginState.value = Resource.loading()

        viewModelScope.launch {
            try {
                authRepository.login(email, password).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val loggedInUser = resource.data
                            _currentUser.value = loggedInUser
                            _loginState.value = Resource.success(loggedInUser)
                        }
                        is Resource.Error -> {
                            Timber.e(resource.error?.cause, "Login failed")
                            _loginState.value = Resource.error(resource.error!!, resource.data)
                        }
                        is Resource.Loading -> {
                            _loginState.value = Resource.loading()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Login error")
                _loginState.value = Resource.firebaseError(e)
            }
        }
    }

    fun googleSignIn(idToken: String) {
        if (idToken.isBlank()) {
            _googleSignInState.value = Resource.error(AppError.MissingFieldError("idToken"))
            return
        }

        _googleSignInState.value = Resource.loading()

        viewModelScope.launch {
            try {
                authRepository.googleSignIn(idToken).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val googleUser = resource.data
                            _currentUser.value = googleUser
                            _googleSignInState.value = Resource.success(googleUser)
                        }
                        is Resource.Error -> {
                            Timber.e(resource.error?.cause, "Google sign-in failed")
                            _googleSignInState.value = Resource.error(resource.error!!, resource.data)
                        }
                        is Resource.Loading -> {
                            _googleSignInState.value = Resource.loading()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Google sign-in error")
                _googleSignInState.value = Resource.firebaseError(e)
            }
        }
    }

    fun logout() {
        // --- FIX: Reset login/signup states IMMEDIATELY to prevent navigation loops ---
        // This ensures that even if logout takes time or fails, the LoginScreen sees a clean state.
        _loginState.value = Resource.loading()
        _googleSignInState.value = Resource.loading()
        _signUpState.value = Resource.loading()
        clearRegistrationData()
        
        _logoutState.value = Resource.loading()

        viewModelScope.launch {
            try {
                authRepository.logout().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _currentUser.value = null
                        _authState.value = Resource.success(null) // Update auth state for navigation
                        _logoutState.value = Resource.success(Unit)
                    }
                        is Resource.Error -> {
                            Timber.e(resource.error?.cause, "Logout failed")
                            _logoutState.value = resource
                        }
                        is Resource.Loading -> {
                            _logoutState.value = Resource.loading()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Logout error")
                _logoutState.value = Resource.firebaseError(e)
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            try {
                authRepository.updateUserProfile(user).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _currentUser.value = resource.data
                        }
                        is Resource.Error -> {
                            Timber.e(resource.error?.cause, "Profile update failed")
                            // Could emit a separate state for profile updates if needed
                        }
                        is Resource.Loading -> {
                            // Could emit loading state if needed
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Profile update error")
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _passwordResetState.value = Resource.error(AppError.InvalidEmailError())
            return
        }

        _passwordResetState.value = Resource.loading()

        viewModelScope.launch {
            try {
                authRepository.resetPassword(email).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _passwordResetState.value = Resource.success(Unit)
                        }
                        is Resource.Error -> {
                            Timber.e(resource.error?.cause, "Password reset failed")
                            _passwordResetState.value = resource
                        }
                        is Resource.Loading -> {
                            _passwordResetState.value = Resource.loading()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Password reset error")
                _passwordResetState.value = Resource.firebaseError(e)
            }
        }
    }

    fun resetAuthState() {
        _signUpState.value = Resource.success(null)
        _loginState.value = Resource.success(null)
        _googleSignInState.value = Resource.success(null)
        _logoutState.value = Resource.success(Unit)
        _passwordResetState.value = Resource.success(Unit)
    }
}
