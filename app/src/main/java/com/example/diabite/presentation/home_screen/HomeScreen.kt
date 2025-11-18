package com.example.diabite.presentation.home_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.screen.FavouriteScreenUI
import com.example.diabite.presentation.screen.HistoryScreenUI
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.presentation.viewmodel.UserViewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("dashboard_home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object History : BottomNavItem("dashboard_history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Favourite : BottomNavItem("dashboard_favourite", "Favourite", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, authViewModel: AuthViewModel = hiltViewModel(), userViewModel: UserViewModel = hiltViewModel()) {
    val bottomNavController = rememberNavController()
    val showLogoutDialog = remember { mutableStateOf(false) }

    val items = listOf(BottomNavItem.Home, BottomNavItem.History, BottomNavItem.Favourite)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DiaBite", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                actions = {
                    IconButton(onClick = { navController.navigate(Route.Settings) }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showLogoutDialog.value = true }) {
                        Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(navController = bottomNavController, startDestination = BottomNavItem.Home.route, modifier = Modifier.padding(paddingValues)) {
            composable(BottomNavItem.Home.route) {
                val currentUser by authViewModel.currentUser.collectAsState()
                HomeScreenUI(name = currentUser?.displayName ?: "User", email = currentUser?.email ?: "", mainNavController = navController)
            }
            composable(BottomNavItem.History.route) {
                HistoryScreenUI(viewModel = userViewModel) { foodId ->
                    navController.navigate(Route.FoodDetail(foodId))
                }
            }
            composable(BottomNavItem.Favourite.route) {
                FavouriteScreenUI(viewModel = userViewModel) { foodId ->
                    navController.navigate(Route.FoodDetail(foodId))
                }
            }
        }
    }

    if (showLogoutDialog.value) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog.value = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog.value = false
                        authViewModel.logout()
                    }
                ) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog.value = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun HomeScreenUI(name: String, email: String, mainNavController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Welcome, $name!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "What would you like to do today?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            onClick = { mainNavController.navigate(Route.SearchFood) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Food", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Search Food", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Get personalized safety advice for any food", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            onClick = { mainNavController.navigate(Route.AISuggestions) }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(text = "✨ Want AI Suggestions?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Let our AI suggest personalized meal ideas for you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { mainNavController.navigate(Route.TypeInfo) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.SmartToy, contentDescription = "Info Icon", modifier = Modifier.size(28.dp).padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
                Text(text = "Click here to read about Diabetes Types", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
