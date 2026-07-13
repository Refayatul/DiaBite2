package com.example.diabite.domain.repository

import com.example.diabite.data.model.User
import com.example.diabite.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signUp(email: String, password: String, user: User): Flow<Resource<User>>
    fun login(email: String, password: String): Flow<Resource<User>>
    fun googleSignIn(idToken: String): Flow<Resource<User>>
    fun logout(): Flow<Resource<Unit>>
    fun getCurrentUser(): Flow<User?>
    fun updateUserProfile(user: User): Flow<Resource<User>>
    fun resetPassword(email: String): Flow<Resource<Unit>>
}
