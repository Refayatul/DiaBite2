package com.example.diabite.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.diabite.common.Route
import com.example.diabite.presentation.screen.AISuggestionsUI
import com.example.diabite.presentation.screen.BasicInfoScreen
import com.example.diabite.presentation.screen.DiabetesDetailsScreen
import com.example.diabite.presentation.screen.FoodDetailScreen
import com.example.diabite.presentation.home_screen.HomeScreen
import com.example.diabite.presentation.screen.LoginScreen
import com.example.diabite.presentation.screen.MedicalConditionsScreen
import com.example.diabite.presentation.screen.ProfileScreen
import com.example.diabite.presentation.screen.RegisterScreen
import com.example.diabite.presentation.screen.SearchScreen
import com.example.diabite.presentation.screen.SettingsScreen
import com.example.diabite.presentation.screen.TypeInfoUI
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.Resource
import kotlinx.coroutines.delay

/**
 * The main entry point for the application's navigation.
 * It manages authentication state and navigates accordingly.
 */
@Composable
fun NavHostApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Timeout mechanism for auth checking
    var showLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // 3 second timeout
        showLoading = false
    }

    // Determine start destination based on auth state
    val startDestination = when {
        authState is Resource.Success && authState.data != null -> Route.Home
        authState is Resource.Success && authState.data == null -> Route.Login
        authState is Resource.Loading && showLoading -> Route.Login // Don't wait indefinitely
        else -> Route.Login // Default to login
    }

    // Show loading screen only briefly while checking authentication
    if (authState is Resource.Loading && showLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Handle navigation when auth state changes (e.g., after logout)
    LaunchedEffect(authState) {
        when {
            authState is Resource.Success && authState.data == null -> {
                // User is logged out, navigate to login
                navController.navigate(Route.Login) {
                    popUpTo(0) { inclusive = true } // Clear entire back stack
                }
            }
            authState is Resource.Success && authState.data != null -> {
                // User is logged in, navigate to home
                navController.navigate(Route.Home) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // Authentication Screens
        composable<Route.Login>() {
            LoginScreen(navController = navController)
        }
        composable<Route.Signup>() {
            RegisterScreen(navController = navController)
        }

        // Multi-step Registration Flow
        composable<Route.BasicInfo>() { backStackEntry ->
            val basicInfo = backStackEntry.toRoute<Route.BasicInfo>()
            BasicInfoScreen(
                navController = navController,
                initialEmail = basicInfo.email,
                initialPassword = basicInfo.password
            )
        }

        composable<Route.MedicalConditions>() { backStackEntry ->
            val medicalConditions = backStackEntry.toRoute<Route.MedicalConditions>()
            MedicalConditionsScreen(
                navController = navController,
                email = medicalConditions.email,
                password = medicalConditions.password,
                displayName = medicalConditions.displayName,
                dateOfBirth = medicalConditions.dateOfBirth,
                biologicalSex = medicalConditions.biologicalSex,
                authViewModel = authViewModel
            )
        }

        composable<Route.DiabetesDetails>() { backStackEntry ->
            val diabetesDetails = backStackEntry.toRoute<Route.DiabetesDetails>()
            DiabetesDetailsScreen(
                navController = navController,
                email = diabetesDetails.email,
                password = diabetesDetails.password,
                displayName = diabetesDetails.displayName,
                dateOfBirth = diabetesDetails.dateOfBirth,
                biologicalSex = diabetesDetails.biologicalSex,
                primaryConditions = diabetesDetails.primaryConditions,
                authViewModel = authViewModel
            )
        }

        // Main App Screens (Protected Routes)
        composable<Route.Home>() {
            HomeScreen(navController = navController, authViewModel = authViewModel)
        }

        // Feature Screens
        composable<Route.SearchFood>() {
            SearchScreen(
                onFoodItemClick = { foodItem ->
                    navController.navigate(Route.FoodDetail(foodItem.id))
                }
            )
        }

        composable<Route.FoodDetail>() { backStackEntry ->
            val foodId = backStackEntry.toRoute<Route.FoodDetail>().foodId
            FoodDetailScreen(navController = navController)
        }

        composable<Route.TypeInfo>() {
            TypeInfoUI(navController = navController)
        }

        composable<Route.AISuggestions>() {
            AISuggestionsUI(navController = navController)
        }

        composable<Route.Settings>() {
            SettingsScreen(navController = navController)
        }

        // Legacy route for backward compatibility
        composable<Route.Detail>() { backStackEntry ->
            val (name, email) = backStackEntry.toRoute<Route.Detail>()
            HomeScreen(navController = navController)
        }
    }
}

/**
 * A simple placeholder screen for routes that haven't been implemented yet.
 */
@Composable
fun TextPlaceholderScreen(title: String) {
    // This is defined here to ensure NavHostApp.kt is fully self-contained and runnable
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
