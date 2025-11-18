package com.example.diabite.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.home_screen.HomeScreen
import com.example.diabite.presentation.screen.AISuggestionsUI
import com.example.diabite.presentation.screen.FoodDetailScreen
import com.example.diabite.presentation.screen.LoginScreen
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

    var showLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // 3 second timeout
        showLoading = false
    }

    // Derived states to prevent unnecessary navigation on user updates
    val isAuthenticated by remember(authState) {
        derivedStateOf { authState is Resource.Success && authState.data != null }
    }
    val isUnauthenticated by remember(authState) {
        derivedStateOf { authState is Resource.Success && authState.data == null }
    }

    val startDestination = when {
        isAuthenticated -> Route.Home
        else -> Route.Login
    }

    if (authState is Resource.Loading && showLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            navController.navigate(Route.Home) {
                popUpTo(Route.Login) { inclusive = true }
            }
        }
    }

    LaunchedEffect(isUnauthenticated) {
        if (isUnauthenticated) {
            navController.navigate(Route.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<Route.Login>() {
            LoginScreen(navController = navController)
        }
        composable<Route.Signup>() {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }

        composable<Route.Home>() {
            HomeScreen(navController = navController, authViewModel = authViewModel)
        }

        composable<Route.SearchFood>() {
            SearchScreen(
                onFoodItemClick = { foodItem ->
                    navController.navigate(Route.FoodDetail(foodItem.id))
                }
            )
        }

        composable<Route.FoodDetail>() {
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

        composable<Route.Detail>() {
            HomeScreen(navController = navController)
        }
    }
}
