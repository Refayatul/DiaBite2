package com.example.diabite.util

/**
 * Sealed class for all application-specific exceptions
 * Provides consistent error handling and user-friendly messages
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    // Authentication Exceptions
    sealed class AuthException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class InvalidCredentials(message: String = "Invalid email or password") : AuthException(message)
        class UserNotFound(message: String = "Account not found") : AuthException(message)
        class UserAlreadyExists(message: String = "Account already exists") : AuthException(message)
        class WeakPassword(message: String = "Password is too weak") : AuthException(message)
        class InvalidEmail(message: String = "Invalid email format") : AuthException(message)
        class AccountDisabled(message: String = "Account has been disabled") : AuthException(message)
        class TooManyRequests(message: String = "Too many login attempts. Try again later") : AuthException(message)
        class NetworkError(message: String = "Network connection error", cause: Throwable? = null) : AuthException(message, cause)
        class GoogleSignInFailed(message: String = "Google sign-in failed", cause: Throwable? = null) : AuthException(message, cause)
    }

    // Network Exceptions
    sealed class NetworkException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class NoInternet(message: String = "No internet connection") : NetworkException(message)
        class Timeout(message: String = "Request timed out", cause: Throwable? = null) : NetworkException(message, cause)
        class ServerError(message: String = "Server error occurred", cause: Throwable? = null) : NetworkException(message, cause)
        class UnknownHost(message: String = "Unable to connect to server") : NetworkException(message)
    }

    // Data Exceptions
    sealed class DataException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class NotFound(message: String = "Data not found") : DataException(message)
        class ValidationError(message: String) : DataException(message)
        class ParseError(message: String = "Failed to parse data", cause: Throwable? = null) : DataException(message, cause)
        class SaveError(message: String = "Failed to save data", cause: Throwable? = null) : DataException(message, cause)
        class DeleteError(message: String = "Failed to delete data", cause: Throwable? = null) : DataException(message, cause)
    }

    // Cache Exceptions
    sealed class CacheException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class CacheCorrupted(message: String = "Cache data is corrupted") : CacheException(message)
        class CacheFull(message: String = "Cache storage is full") : CacheException(message)
        class CacheWriteError(message: String = "Failed to write to cache", cause: Throwable? = null) : CacheException(message, cause)
        class CacheReadError(message: String = "Failed to read from cache", cause: Throwable? = null) : CacheException(message, cause)
    }

    // AI/Gemini Exceptions
    sealed class AIException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class GeminiUnavailable(message: String = "AI service temporarily unavailable") : AIException(message)
        class RateLimitExceeded(message: String = "AI analysis limit reached. Try again later") : AIException(message)
        class InvalidInput(message: String = "Invalid input for AI analysis") : AIException(message)
        class AnalysisFailed(message: String = "AI analysis failed", cause: Throwable? = null) : AIException(message, cause)
        class ResponseParseError(message: String = "Failed to understand AI response", cause: Throwable? = null) : AIException(message, cause)
    }

    // Permission Exceptions
    sealed class PermissionException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class AccessDenied(message: String = "Access denied") : PermissionException(message)
        class InsufficientPermissions(message: String = "Insufficient permissions") : PermissionException(message)
        class AuthenticationRequired(message: String = "Authentication required") : PermissionException(message)
    }

    // UI Exceptions
    sealed class UIException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class InvalidState(message: String = "Application is in an invalid state") : UIException(message)
        class NavigationError(message: String = "Navigation failed", cause: Throwable? = null) : UIException(message, cause)
        class ConfigurationError(message: String = "Configuration error", cause: Throwable? = null) : UIException(message, cause)
    }

    // Generic Exceptions
    class UnknownError(message: String = "An unknown error occurred", cause: Throwable? = null) : AppException(message, cause)

    companion object {
        /**
         * Convert Firebase exceptions to AppException
         */
        fun fromFirebaseException(exception: Exception): AppException {
            val message = exception.message ?: "Unknown Firebase error"

            return when {
                message.contains("auth/invalid-email") -> AuthException.InvalidEmail()
                message.contains("auth/user-disabled") -> AuthException.AccountDisabled()
                message.contains("auth/user-not-found") -> AuthException.UserNotFound()
                message.contains("auth/wrong-password") -> AuthException.InvalidCredentials()
                message.contains("auth/email-already-in-use") -> AuthException.UserAlreadyExists()
                message.contains("auth/weak-password") -> AuthException.WeakPassword()
                message.contains("auth/too-many-requests") -> AuthException.TooManyRequests()
                message.contains("permission-denied") -> PermissionException.AccessDenied()
                message.contains("not-found") -> DataException.NotFound()
                message.contains("deadline-exceeded") -> NetworkException.Timeout(message, exception)
                message.contains("unavailable") -> NetworkException.ServerError(message, exception)
                else -> UnknownError(message, exception)
            }
        }

        /**
         * Convert network exceptions to AppException
         */
        fun fromNetworkException(exception: Exception): AppException {
            val message = exception.message ?: "Network error"

            return when {
                message.contains("timeout") -> NetworkException.Timeout(message, exception)
                message.contains("unknown host") -> NetworkException.UnknownHost()
                message.contains("no internet") -> NetworkException.NoInternet()
                else -> NetworkException.ServerError(message, exception)
            }
        }

        /**
         * Get user-friendly error message
         */
        fun getUserFriendlyMessage(exception: AppException): String {
            return when (exception) {
                is AuthException.InvalidCredentials -> "Invalid email or password. Please check and try again."
                is AuthException.UserNotFound -> "No account found with this email. Please sign up first."
                is AuthException.UserAlreadyExists -> "An account with this email already exists. Please sign in instead."
                is AuthException.WeakPassword -> "Password must be at least 6 characters long."
                is AuthException.InvalidEmail -> "Please enter a valid email address."
                is AuthException.AccountDisabled -> "This account has been disabled. Please contact support."
                is AuthException.TooManyRequests -> "Too many login attempts. Please wait a few minutes and try again."
                is AuthException.NetworkError -> "Network connection error. Please check your internet and try again."
                is AuthException.GoogleSignInFailed -> "Google sign-in failed. Please try again."

                is NetworkException.NoInternet -> "No internet connection. Please check your network and try again."
                is NetworkException.Timeout -> "Request timed out. Please try again."
                is NetworkException.ServerError -> "Server error occurred. Please try again later."
                is NetworkException.UnknownHost -> "Unable to connect to server. Please check your internet connection."

                is DataException.NotFound -> "The requested information was not found."
                is DataException.ValidationError -> exception.message ?: "Validation error occurred"
                is DataException.ParseError -> "Failed to process the data. Please try again."
                is DataException.SaveError -> "Failed to save your changes. Please try again."
                is DataException.DeleteError -> "Failed to delete the item. Please try again."

                is CacheException.CacheCorrupted -> "App cache is corrupted. Please restart the app."
                is CacheException.CacheFull -> "App storage is full. Please free up space and try again."
                is CacheException.CacheWriteError -> "Failed to save data locally. Some features may not work."
                is CacheException.CacheReadError -> "Failed to load saved data. Some features may not work."

                is AIException.GeminiUnavailable -> "AI nutritionist is temporarily unavailable. Please try again later."
                is AIException.RateLimitExceeded -> "You've reached your daily AI analysis limit. Please try again tomorrow."
                is AIException.InvalidInput -> "Invalid food name. Please try a different search term."
                is AIException.AnalysisFailed -> "AI analysis failed. Please try again or search for existing foods."
                is AIException.ResponseParseError -> "AI response was unclear. Please try again."

                is PermissionException.AccessDenied -> "You don't have permission to perform this action."
                is PermissionException.InsufficientPermissions -> "You need additional permissions to perform this action."
                is PermissionException.AuthenticationRequired -> "Please sign in to continue."

                is UIException.InvalidState -> "App is in an unexpected state. Please restart the app."
                is UIException.NavigationError -> "Navigation failed. Please try again."
                is UIException.ConfigurationError -> "App configuration error. Please restart the app."

                is UnknownError -> "An unexpected error occurred. Please try again."
            }
        }

        /**
         * Check if exception is retryable
         */
        fun isRetryable(exception: AppException): Boolean {
            return when (exception) {
                is NetworkException.Timeout,
                is NetworkException.ServerError,
                is NetworkException.UnknownHost,
                is AIException.GeminiUnavailable,
                is AIException.AnalysisFailed,
                is DataException.SaveError,
                is DataException.DeleteError -> true

                else -> false
            }
        }

        /**
         * Get suggested action for exception
         */
        fun getSuggestedAction(exception: AppException): String? {
            return when (exception) {
                is NetworkException.NoInternet -> "Check your internet connection"
                is NetworkException.Timeout -> "Try again in a few moments"
                is NetworkException.ServerError -> "Try again later or contact support"
                is AuthException.TooManyRequests -> "Wait a few minutes before trying again"
                is AIException.RateLimitExceeded -> "Try again tomorrow or search for existing foods"
                is AIException.GeminiUnavailable -> "Try again later or search for existing foods"
                is CacheException.CacheCorrupted -> "Restart the app to clear corrupted cache"
                is CacheException.CacheFull -> "Free up storage space on your device"
                else -> null
            }
        }
    }
}
