package com.example.diabite.presentation.home_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

// Sealed class for bottom navigation items (unchanged)
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
fun HomeScreen(navController: NavHostController, authViewModel: AuthViewModel, userViewModel: UserViewModel) {
    val bottomNavController = rememberNavController()
    val showLogoutDialog = remember { mutableStateOf(false) }

    val items = listOf(BottomNavItem.Home, BottomNavItem.History, BottomNavItem.Favourite)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DiaBite", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = { navController.navigate(Route.Settings) }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = { showLogoutDialog.value = true }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
                HomeScreenUI(name = currentUser?.name ?: "User", email = currentUser?.email ?: "", mainNavController = navController)
            }
            composable(BottomNavItem.History.route) {
                HistoryScreenUI(viewModel = userViewModel) { query ->
                    navController.navigate("${Route.SearchFood}/$query")
                }
            }
            composable(BottomNavItem.Favourite.route) {
                FavouriteScreenUI(viewModel = userViewModel) { foodId ->
                    navController.navigate("${Route.FoodDetail}/$foodId")
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
                        navController.navigate(Route.Login) {
                            // FIX: Use popUpTo(0) for absolute stack clearing, consistent with NavHostApp
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog.value = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun HomeScreenUI(name: String, email: String, mainNavController: NavController) {
    val scrollState = rememberScrollState()
    var visible by remember { mutableStateOf(false) }

    // Extract colors outside Canvas
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Background Canvas for subtle decorative elements (e.g., floating circles)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Trigger initial animation *inside* the content of the Box
        LaunchedEffect(Unit) {
            visible = true
        }

        // Decorative Canvas (subtle, behind content)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val width = size.width
            val height = size.height
            val radius = 80.dp.toPx()

            // Draw some subtle, semi-transparent circles
            drawCircle(
                color = primaryColor.copy(alpha = 0.05f),
                radius = radius,
                center = Offset(width * 0.8f, height * 0.2f)
            )
            drawCircle(
                color = secondaryColor.copy(alpha = 0.05f),
                radius = radius * 0.7f,
                center = Offset(width * 0.2f, height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome Section
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    // Add a small decorative element before the text
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Welcome, $name!",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "What would you like to do today?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Feature Cards Section
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Search Card
                    FeatureCard(
                        title = "\uD83D\uDD0D Search Food",
                        description = "Get personalized safety advice for any food",
                        icon = Icons.Filled.Search,
                        iconColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            mainNavController.navigate("${Route.SearchFood}/")
                        }
                    )

                    // AI Suggestions Card
                    FeatureCard(
                        title = "\uD83E\uDD16 AI Meal Suggestions",
                        description = "Let our AI suggest personalized meal ideas for you.",
                        icon = Icons.Filled.SmartToy,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            mainNavController.navigate(Route.AISuggestions)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    // Animate the scale based on visibility state (if passed down) or a local state if needed for press effects
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 200
        ),
        label = "cardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // .graphicsLayer {
            //     scaleX = scale
            //     scaleY = scale
            // }
            .scale(scale)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container with Border
            Box(
                modifier = Modifier
                    .size(72.dp) // Slightly larger icon container
                    .clip(RoundedCornerShape(20.dp)) // Match card shape more closely
                    .background(
                        contentColor.copy(
                            alpha = 0.1f
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = iconColor.copy(
                            alpha = 0.3f
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp), // Slightly larger icon
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(20.dp)) // Increased space
            // Text Content
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = iconColor // Title color matches icon
                )
                Spacer(modifier = Modifier.height(6.dp)) // Increased space
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = contentColor.copy(
                        alpha = 0.9f
                    ) // Slightly less transparent content color
                )
            }
            // Chevron Icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = contentColor.copy(
                    alpha = 0.7f
                ) // Slightly more prominent navigation icon
            )
        }
    }
}
