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
                    email = email
                )
                // Save user to Firestore
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
                val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                val userDoc = userDocRef.get().await()

                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java) ?: createDefaultUser(firebaseUser)
                } else {
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "",
                        diabetesType = ""
                    )
                    userDocRef.set(newUser).await()
                    newUser
                }
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
                val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                val userDoc = userDocRef.get().await()

                val user = if (userDoc.exists()) {
                    userDoc.toObject(User::class.java) ?: createDefaultUser(firebaseUser)
                } else {
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "",
                        diabetesType = ""
                    )
                    userDocRef.set(newUser).await()
                    newUser
                }
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
            googleSignInClient.signOut().await()
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
                trySend(createDefaultUser(firebaseUser))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java) ?: createDefaultUser(firebaseUser)
                trySend(user)
            } else {
                val newUser = createDefaultUser(firebaseUser)
                userDocRef.set(newUser).addOnSuccessListener {
                    trySend(newUser)
                }.addOnFailureListener {
                    Timber.w(it, "Failed to create user document for the first time.")
                    trySend(newUser)
                }
            }
        }

        awaitClose { listener.remove() }
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
                emit(Resource.success(Unit))
                return@flow
            }
            
            val userDocRef = firestore.collection("users").document(uid)
            
            // Remove if exists then add to front
            userDocRef.update("searchHistory", FieldValue.arrayRemove(query)).await()
            userDocRef.update("searchHistory", FieldValue.arrayUnion(query)).await()
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            Timber.w(e, "Failed to add search to history")
            emit(Resource.success(Unit))
        }
    }

    private fun createDefaultUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        return User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
            diabetesType = ""
        )
    }
}
