package com.example.diabite.data.repository

import com.example.diabite.data.model.User
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            // Clear local Firestore cache
            firestore.clearPersistence().await()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            emit(Resource.firebaseError(e))
        }
    }

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val userDocRef = firestore.collection("users").document(firebaseUser.uid)

        val listener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.w(error, "Listen for user profile failed.")
                // Don't close the flow, just emit a basic user and let it recover
                trySend(createDefaultUser(firebaseUser))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java) ?: createDefaultUser(firebaseUser)
                trySend(user)
            } else {
                // Document doesn't exist, create it for the first time
                val newUser = createDefaultUser(firebaseUser)
                userDocRef.set(newUser).addOnSuccessListener {
                    trySend(newUser) // Emit the new user after creation
                }.addOnFailureListener {
                    Timber.w(it, "Failed to create user document for the first time.")
                    trySend(newUser) // Still send a user object on failure
                }
            }
        }

        awaitClose { listener.remove() } // Unregister listener when flow is cancelled
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

    override fun addFavoriteFood(foodId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.loading())
        try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                emit(Resource.error(AppError.AuthenticationError("User not logged in")))
                return@flow
            }
            firestore.collection("users").document(uid)
                .update("favoriteFoodIds", FieldValue.arrayUnion(foodId))
                .await()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to add favorite food")
            emit(Resource.firebaseError(e))
        }
    }

    override fun removeFavoriteFood(foodId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.loading())
        try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                emit(Resource.error(AppError.AuthenticationError("User not logged in")))
                return@flow
            }
            firestore.collection("users").document(uid)
                .update("favoriteFoodIds", FieldValue.arrayRemove(foodId))
                .await()
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove favorite food")
            emit(Resource.firebaseError(e))
        }
    }

    override fun addSearchToHistory(query: String): Flow<Resource<Unit>> = flow {
        emit(Resource.loading())
        try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                // Silently succeed if user is not logged in
                emit(Resource.success(Unit))
                return@flow
            }
            val userDocRef = firestore.collection("users").document(uid)

            // To keep history clean and ordered by most recent, we remove and then add.
            userDocRef.update("searchHistory", FieldValue.arrayRemove(query)).await()
            userDocRef.update("searchHistory", FieldValue.arrayUnion(query)).await()

            // Optional: Trim the history to a certain size
            // This would require a transaction to be safe. For now, we'll let it grow.

            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.w(e, "Failed to add search to history (non-critical)")
            emit(Resource.success(Unit)) // Don't block user for this
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
