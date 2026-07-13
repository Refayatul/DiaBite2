package com.example.diabite

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class DiaBiteApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Timber.d("Firebase initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase")
        }
    }
}
