package com.example.diabite.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.diabite.presentation.screen.GeminiScreen
import com.example.diabite.presentation.screen.LoginScreen
import com.example.diabite.presentation.screen.RegisterScreen
import com.example.diabite.presentation.screen.SearchScreen
import com.example.diabite.presentation.screen.SettingsScreen
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.presentation.viewmodel.SearchViewModel
import com.example.diabite.presentation.viewmodel.UserViewModel
import com.example.diabite.util.Resource

@Composable
fun NavHostApp(authViewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    val userViewModel = hiltViewModel<UserViewModel>()
    val searchViewModel = hiltViewModel<SearchViewModel>()

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

    if (authState is Resource.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
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
        composable(Route.Login) {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable(Route.Signup) {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }

        composable(Route.Home) {
            HomeScreen(navController = navController, authViewModel = authViewModel, userViewModel = userViewModel)
        }

        composable("${Route.SearchFood}/{query}") { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            // Update the search query in the view model when navigating with a parameter
            searchViewModel.updateSearchQuery(query)
            SearchScreen(
                onFoodItemClick = { foodItem ->
                    // Save the opened food item to history
                    userViewModel.addSearchToHistory(foodItem.name)
                    navController.navigate("${Route.FoodDetail}/${foodItem.id}")
                },
                viewModel = searchViewModel,
                userViewModel = userViewModel
            )
        }

        composable("${Route.FoodDetail}/{foodId}") { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId") ?: ""
            FoodDetailScreen(onBackClick = { navController.navigateUp() })
        }

        composable(Route.AISuggestions) {
            AISuggestionsUI(navController = navController)
        }

        composable(Route.Gemini) {
            GeminiScreen()
        }

        composable(Route.Settings) {
            SettingsScreen(navController = navController, authViewModel = authViewModel)
        }

        composable("${Route.Detail}/{name}/{email}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            HomeScreen(navController = navController, authViewModel = authViewModel, userViewModel = userViewModel)
        }
    }
}
