package com.example.diabite.util

/**
 * Sealed class representing the state of asynchronous operations
 */
sealed class Resource<T>(
    val data: T? = null,
    val error: AppError? = null
) {

    /**
     * Loading state - operation in progress
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)

    /**
     * Success state - operation completed successfully
     */
    class Success<T>(data: T?) : Resource<T>(data)

    /**
     * Error state - operation failed with an error
     */
    class Error<T>(error: AppError, data: T? = null) : Resource<T>(data, error)

    /**
     * Check if the resource is in loading state
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Check if the resource is in success state
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Check if the resource is in error state
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Get the data if available, regardless of state
     */
    fun getDataOrNull(): T? = data

    /**
     * Get the error if available
     */
    fun getErrorOrNull(): AppError? = error

    /**
     * Execute action if in success state
     */
    inline fun onSuccess(action: (T?) -> Unit): Resource<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Execute action if in error state
     */
    inline fun onError(action: (AppError) -> Unit): Resource<T> {
        if (this is Error) {
            action(error!!)
        }
        return this
    }

    /**
     * Execute action if in loading state
     */
    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) {
            action()
        }
        return this
    }

    /**
     * Transform success data using mapper function
     */
    inline fun <R> mapSuccess(transform: (T?) -> R): Resource<R> {
        return when (this) {
            is Loading -> Loading(data?.let(transform))
            is Success -> Success(transform(data))
            is Error -> Error(error!!, data?.let(transform))
        }
    }

    /**
     * Transform error using mapper function
     */
    inline fun mapError(transform: (AppError) -> AppError): Resource<T> {
        return when (this) {
            is Loading -> this
            is Success -> this
            is Error -> Error(transform(error!!), data)
        }
    }

    companion object {
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)
        fun <T> success(data: T): Resource<T> = Success(data)
        fun <T> error(error: AppError, data: T? = null): Resource<T> = Error(error, data)

        fun <T> error(exception: Exception, data: T? = null): Resource<T> =
            Error(AppError.fromException(exception), data)

        fun <T> firebaseError(exception: Exception, data: T? = null): Resource<T> =
            Error(AppError.fromFirebaseException(exception), data)

        fun <T> networkError(exception: Exception, data: T? = null): Resource<T> =
            Error(AppError.fromNetworkException(exception), data)
    }
}
