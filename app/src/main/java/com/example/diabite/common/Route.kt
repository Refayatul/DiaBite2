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
    data object MedicalConditions : Route

    @Serializable
    data object DiabetesDetails : Route

    // Main App Flow (Home Dashboard)
    @Serializable
    data object Home : Route

    // Legacy route for backward compatibility
    @Serializable
    data class Detail(val name: String, val email: String) : Route

    // Core Feature Screens, navigated to from the HomeScreen dashboard
    @Serializable
    data class SearchFood(val query: String) : Route

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
