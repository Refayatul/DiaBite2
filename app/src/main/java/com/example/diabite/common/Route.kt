package com.example.diabite.common

import kotlinx.serialization.Serializable

/**
 * Sealed class defining all top-level navigation routes in the application.
 * Using Kotlinx Serialization for type-safe navigation arguments (name and email).
 */
sealed interface Route {
    // Authentication Flow
    @Serializable
    data object Login : Route

    @Serializable
    data object Signup : Route

    // Multi-step Registration Flow
    @Serializable
    data class BasicInfo(val email: String = "", val password: String = "") : Route

    @Serializable
    data class MedicalConditions(
        val email: String,
        val password: String,
        val displayName: String,
        val dateOfBirth: String,
        val biologicalSex: String
    ) : Route

    @Serializable
    data class DiabetesDetails(
        val email: String,
        val password: String,
        val displayName: String,
        val dateOfBirth: String,
        val biologicalSex: String,
        val primaryConditions: List<String>,
        val diabetesType: String = "",
        val selectedMedications: List<String> = emptyList(),
        val otherMedication: String = ""
    ) : Route

    // Main App Flow (Home Dashboard)
    @Serializable
    data object Home : Route

    // Legacy route for backward compatibility
    @Serializable
    data class Detail(val name: String, val email: String) : Route

    // Core Feature Screens, navigated to from the HomeScreen dashboard
    @Serializable
    data object SearchFood : Route

    @Serializable
    data class FoodDetail(val foodId: String) : Route

    @Serializable
    data object AISuggestions : Route

    @Serializable
    data object TypeInfo : Route

    // Settings and Profile Management
    @Serializable
    data object Settings : Route
}
