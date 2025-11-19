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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.diabite.presentation.viewmodel.UserViewModel

@Composable
fun HistoryScreenUI(
    viewModel: UserViewModel,
    onSearchQueryClick: (String) -> Unit
) {
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isClearingHistory by viewModel.isClearingHistory.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }

    // Define colors here (Context of Composable) to use inside Canvas
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background

    // Background Canvas for subtle decorative elements
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
                color = primaryColor.copy(alpha = 0.03f), // Use the captured variable
                radius = radius,
                center = Offset(width * 0.8f, height * 0.1f)
            )
            drawCircle(
                color = secondaryColor.copy(alpha = 0.03f), // Use the captured variable
                radius = radius * 0.8f,
                center = Offset(width * 0.2f, height * 0.9f)
            )
        }

        // Trigger initial animation *inside* the content of the Box
        LaunchedEffect(Unit) {
            visible = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header with title and clear button
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Search History",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    if (searchHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            enabled = !isClearingHistory
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (searchHistory.isEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "No History",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No recent searches found.",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(searchHistory) { query ->
                            HistoryItemCard(
                                query = query,
                                onClick = { onSearchQueryClick(query) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Search History") },
            text = { Text("Are you sure you want to clear all your search history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearSearchHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isClearingHistory
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    query: String,
    onClick: () -> Unit
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
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // History Icon
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            // Query Text
            Text(
                text = query,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}