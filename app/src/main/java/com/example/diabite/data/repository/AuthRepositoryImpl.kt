package com.example.diabite.data.repository

import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override fun signUp(email: String, password: String, user: User): Flow<Resource<User>> = flow {
        emit(Resource.loading())

        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val newUser = user.copy(
                    uid = firebaseUser.uid,
                    email = email,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )
                // Save user profile to Firestore
                firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                emit(Resource.success(newUser))
            } else {
                emit(Resource.error(AppError.UnknownError("Registration failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Sign up failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun login(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.loading())

        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Get user profile from Firestore
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java)?.copy(
                        lastLoginAt = System.currentTimeMillis()
                    ) ?: createDefaultUser(firebaseUser)
                } else {
                    createDefaultUser(firebaseUser)
                }

                // Update last login
                firestore.collection("users").document(firebaseUser.uid)
                    .update("lastLoginAt", System.currentTimeMillis()).await()

                emit(Resource.success(user))
            } else {
                emit(Resource.error(AppError.AuthenticationError("Login failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun googleSignIn(idToken: String): Flow<Resource<User>> = flow {
        emit(Resource.loading())

        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Check if user profile exists
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java)?.copy(
                        lastLoginAt = System.currentTimeMillis()
                    ) ?: createDefaultUser(firebaseUser)
                } else {
                    // Create new user profile for Google sign-in
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                    firestore.collection("users").document(firebaseUser.uid).set(newUser).await()
                    newUser
                }

                // Update last login
                firestore.collection("users").document(firebaseUser.uid)
                    .update("lastLoginAt", System.currentTimeMillis()).await()

                emit(Resource.success(user))
            } else {
                emit(Resource.error(AppError.AuthenticationError("Google sign-in failed")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Google sign-in failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.loading())

        try {
            firebaseAuth.signOut()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getCurrentUser(): Flow<User?> = flow {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            try {
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)
                emit(user)
            } catch (e: Exception) {
                Timber.w(e, "Failed to get user profile from Firestore, using fallback")
                // Fallback to basic user info if Firestore fails
                emit(createDefaultUser(firebaseUser))
            }
        } else {
            emit(null)
        }
    }

    override fun updateUserProfile(user: User): Flow<Resource<User>> = flow {
        emit(Resource.loading())

        try {
            firestore.collection("users").document(user.uid).set(user).await()
            emit(Resource.success(user))
        } catch (e: Exception) {
            Timber.e(e, "Profile update failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun resetPassword(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.loading())

        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Password reset failed")
            emit(Resource.firebaseError(e))
        }
    }

    private fun createDefaultUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
    }
}
