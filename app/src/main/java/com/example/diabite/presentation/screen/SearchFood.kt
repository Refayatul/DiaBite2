package com.example.diabite.presentation.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diabite.common.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFoodUI(navController: NavController) {

    var foodSearchText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Type 2") } // Type 2 is default

    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    // Trigger initial animation
    androidx.compose.runtime.LaunchedEffect(Unit) {
        visible = true
    }

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
                            "Search Food",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Reduced vertical padding
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp)) // Reduced space

                // Type 1 / Type 2 Toggle Buttons
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(24.dp)) // Clip for overall border
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Type 1 Button
                        Button(
                            onClick = { selectedType = "Type 1" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = if (selectedType == "Type 1") {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Text(
                                "Type 1",
                                color = if (selectedType == "Type 1") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Type 2 Button
                        Button(
                            onClick = { selectedType = "Type 2" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = if (selectedType == "Type 2") {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Text(
                                "Type 2",
                                color = if (selectedType == "Type 2") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Search Bar and Search Button
                AnimatedVisibility(
                    visible = visible,
                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
                ) {
                    OutlinedTextField(
                        value = foodSearchText,
                        onValueChange = { foodSearchText = it },
                        label = { Text("Search for a food item...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                if (foodSearchText.isNotEmpty()) {
                                    Toast.makeText(context, "Searching for '$foodSearchText' for $selectedType...", Toast.LENGTH_SHORT).show()
                                    // TODO: Add logic to display search results here
                                }
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search Food",
                                    tint = MaterialTheme.colorScheme.primary // Use primary color
                                )
                            }
                        },
                        colors = TextFieldDefaults.colors( // Custom colors for search bar
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), // Subtle background when focused
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f) // Subtle background when unfocused
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
