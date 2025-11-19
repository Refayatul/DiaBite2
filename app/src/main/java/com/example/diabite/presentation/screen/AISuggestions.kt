package com.example.diabite.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // Import LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale // Import scale modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISuggestionsUI(navController: NavController) {
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "AI Meal Suggestions",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface // Use surface color for better contrast
                    )
                )
            }
        ) { paddingValues ->
            // Trigger initial animation *inside* the content of the Scaffold
            LaunchedEffect(Unit) {
                visible = true
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // AnimatedVisibility *inside* the Column content
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated Icon
                        val scale by animateFloatAsState(
                            targetValue = if (visible) 1f else 0.5f,
                            animationSpec = tween(durationMillis = 500),
                            label = "scale"
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Icon",
                            modifier = Modifier
                                .scale(scale) // Apply the animated scale modifier
                                .padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = "Personalized AI Suggestions Coming Soon!",
                            style = MaterialTheme.typography.headlineLarge.copy( // Larger title
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // Colored title
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Here, the Gemini API will generate personalized, diabetes-friendly meal plans based on user preferences.",
                            style = MaterialTheme.typography.bodyLarge.copy( // Larger body text
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // Optional: Show a loading indicator or a placeholder for future functionality
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 4.dp
                        )
                    }
                }
            }
        }
    }
}
