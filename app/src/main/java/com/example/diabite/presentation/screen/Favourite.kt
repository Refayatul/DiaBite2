package com.example.diabite.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diabite.presentation.viewmodel.FavoritesViewModel
import timber.log.Timber
import com.example.diabite.presentation.viewmodel.UserViewModel
import com.example.diabite.data.model.FoodItem

@Composable
fun FavouriteScreenUI(
    viewModel: UserViewModel,
    onFavoriteItemClick: (String) -> Unit
) {
    val favoriteFoodIds by viewModel.favoriteFoodIds.collectAsState()
    // New FavoritesViewModel loads FoodItem details for the favorite IDs
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val favoriteFoods by favoritesViewModel.favoriteFoods.collectAsState()
    var visible by remember { mutableStateOf(false) }

    // Extract colors outside Canvas
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Background Canvas for subtle decorative elements (e.g., floating circles)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Decorative Canvas (subtle, behind content)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val width = size.width
            val height = size.height
            val radius = 60.dp.toPx()

            // Draw some subtle, semi-transparent circles
            drawCircle(
                color = primaryColor.copy(alpha = 0.03f),
                radius = radius,
                center = Offset(width * 0.9f, height * 0.1f)
            )
            drawCircle(
                color = secondaryColor.copy(alpha = 0.03f),
                radius = radius * 0.8f,
                center = Offset(width * 0.1f, height * 0.9f)
            )
        }

        // Trigger initial animation *inside* the content of the Box
        LaunchedEffect(Unit) {
            visible = true
        }
        Timber.d("FavouriteScreenUI: favoriteFoodIds=$favoriteFoodIds | loadedFoodCount=${favoriteFoods.size}")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            ) {
                Text(
                    text = "Your Saved Favourites",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold, // Bolder title
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center // Center title
                )
            }

            if (favoriteFoodIds.isEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    EmptyFavouritesMessage()
                }
            } else {
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteFoodIds) { foodId ->
                            // Try to show detailed info if available from batch fetch
                            val food: FoodItem? = favoriteFoods.find { it.id == foodId }
                            FavouriteItemCard(
                                foodId = foodId,
                                foodName = food?.name,
                                onClick = { onFavoriteItemClick(foodId) },
                                onRemoveClick = { viewModel.toggleFavoriteFood(foodId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavouriteItemCard(
    foodId: String,
    foodName: String? = null,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium, // Use theme shape
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Standard surface color
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite Icon with themed color
            Icon(
                imageVector = Icons.Filled.Favorite, // Use filled icon for favourites
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // Use primary color
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp)) // Spacer with width

            // Food name (fallback to id if not loaded)
            Text(
                text = (foodName ?: foodId).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Remove Button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyFavouritesMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animate the empty state icon
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "Empty Favourites",
                modifier = Modifier.size(64.dp), // Larger icon
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) // Use primary color with transparency
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Animate the title
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
        ) {
            Text(
                text = "No Favourites Saved",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold, // Bolder title
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Animate the description
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = tween(700)) + fadeIn(animationSpec = tween(700))
        ) {
            Text(
                text = "You can mark food items as favourite after scanning them to quickly find them later!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
