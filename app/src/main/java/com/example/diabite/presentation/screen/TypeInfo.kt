@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.diabite.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diabite.presentation.theme.TextDarkGray

@Composable
fun TypeInfoUI(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Your Type", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Text(
                    text = "Unsure of your diabetes type? Here are some key differences.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextDarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Type 1 Diabetes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This is an autoimmune condition where the body does not produce insulin. It typically develops in childhood or young adulthood. Symptoms can appear suddenly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Common symptoms include:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("• Increased thirst and frequent urination", style = MaterialTheme.typography.bodySmall)
                    Text("• Extreme hunger", style = MaterialTheme.typography.bodySmall)
                    Text("• Unintended weight loss", style = MaterialTheme.typography.bodySmall)
                    Text("• Fatigue and weakness", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Type 2 Diabetes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "In this condition, the body either doesn't produce enough insulin or resists it. It is the most common form of diabetes and often develops over many years.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Common symptoms include:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("• Slow-healing sores or frequent infections", style = MaterialTheme.typography.bodySmall)
                    Text("• Blurred vision", style = MaterialTheme.typography.bodySmall)
                    Text("• Numbness or tingling in hands/feet", style = MaterialTheme.typography.bodySmall)
                    Text("• Increased hunger and thirst", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Note: A proper diagnosis from a healthcare professional is crucial for managing your health effectively.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
