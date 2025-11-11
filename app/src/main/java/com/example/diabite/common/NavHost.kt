package com.example.diabite.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.diabite.presentation.screen.AISuggestionsUI
import com.example.diabite.presentation.screen.DiabetesDetailsScreen
import com.example.diabite.presentation.screen.FoodDetailScreen
import com.example.diabite.presentation.screen.HomeScreen
import com.example.diabite.presentation.screen.LoginScreen
import com.example.diabite.presentation.screen.MedicalConditionsScreen
import com.example.diabite.presentation.screen.RegisterScreen
import com.example.diabite.presentation.screen.SearchScreen
import com.example.diabite.presentation.screen.SettingsScreen
import com.example.diabite.presentation.screen.TypeInfoUI
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.Resource

/**
 * The main entry point for the application's navigation.
 * It manages authentication state and navigates accordingly.
 */
@Composable
fun NavHostApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = when {
        authState is Resource.Success && authState.data != null -> Route.Home
        else -> Route.Login // Default to login while checking
    }

    // Show loading screen while checking authentication
    if (authState is Resource.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LaunchedEffect(authState) {
        if (authState is Resource.Success) {
            if (authState.data == null) {
                // When logged out, go to Login and clear backstack
                navController.navigate(Route.Login) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                // When logged in, go to Home and clear backstack
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
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }

        // Multi-step Registration Flow
        composable<Route.MedicalConditions> {
            MedicalConditionsScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable<Route.DiabetesDetails> {
            DiabetesDetailsScreen(
                navController = navController,
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
            HomeScreen(navController = navController, authViewModel = authViewModel)
        }
    }
}

/**
 * A simple placeholder screen for routes that haven't been implemented yet.
 */
@Composable
fun TextPlaceholderScreen(title: String) {
    // This is defined here to ensure NavHost.kt is fully self-contained and runnable
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
