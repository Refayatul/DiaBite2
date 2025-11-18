package com.example.diabite.data.repository

import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInClient
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
    private val firestore: FirebaseFirestore,
    private val googleSignInClient: GoogleSignInClient
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
                val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                val userDoc = userDocRef.get().await()

                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java)?.copy(
                        lastLoginAt = System.currentTimeMillis()
                    ) ?: run {
                        // Document exists but mapping failed, return default user without overwriting
                        Timber.w("Failed to map user profile during login. Returning basic user.")
                        createDefaultUser(firebaseUser)
                    }
                } else {
                    // Document does not exist, create a new User and save it
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                    userDocRef.set(newUser).await()
                    newUser
                }

                // Update last login (if it was an existing user)
                userDocRef.update("lastLoginAt", System.currentTimeMillis()).await()

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
                val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                val userDoc = userDocRef.get().await()

                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java)?.copy(
                        lastLoginAt = System.currentTimeMillis()
                    ) ?: run {
                        // Document exists but mapping failed, return default user without overwriting
                        Timber.w("Failed to map user profile during Google sign-in. Returning basic user.")
                        createDefaultUser(firebaseUser)
                    }
                } else {
                    // Document does not exist, create a new User profile for Google sign-in and save it
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "",
                        createdAt = System.currentTimeMillis(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                    userDocRef.set(newUser).await()
                    newUser
                }

                // Update last login
                userDocRef.update("lastLoginAt", System.currentTimeMillis()).await()

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
            // Sign out from Firebase Auth
            firebaseAuth.signOut()
            // Sign out from Google Sign-In to force account selection on next login
            googleSignInClient.signOut().await()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getCurrentUser(): Flow<User?> = flow {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            try {
                val userDoc = userDocRef.get().await()

                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java) ?: run {
                        // Document exists but mapping failed. DO NOT OVERWRITE.
                        Timber.w("Failed to map user profile from existing document. Falling back to basic user.")
                        createDefaultUser(firebaseUser)
                    }
                } else {
                    // Document does not exist. Create, save, and return the new user.
                    val newUser = createDefaultUser(firebaseUser)
                    userDocRef.set(newUser).await()
                    newUser
                }
                emit(user)
            } catch (e: Exception) {
                // Network/Firebase error on read. DO NOT OVERWRITE.
                Timber.w(e, "Failed to read user profile from Firestore. Returning basic user data as temporary fallback.")
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

    override fun saveUserProfile(userProfile: com.example.diabite.data.model.UserProfile): Flow<Resource<Unit>> = flow {
        emit(Resource.loading())

        try {
            firestore.collection("userProfiles").document(userProfile.uid).set(userProfile).await()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "UserProfile save failed")
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