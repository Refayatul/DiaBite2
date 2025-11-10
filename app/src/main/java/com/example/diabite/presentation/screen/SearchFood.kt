package com.example.diabite.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Food") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Type 1 / Type 2 Toggle Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)),
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
                        ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary)
                    }
                ) {
                    Text("Type 1")
                }

                // Type 2 Button
                Button(
                    onClick = { selectedType = "Type 2" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = if (selectedType == "Type 2") {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary)
                    }
                ) {
                    Text("Type 2")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Search Bar and Search Button
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
                        Icon(Icons.Default.Search, contentDescription = "Search Food", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "Don't Know About Your Type?" Popup Button
            TextButton(
                onClick = {
                    navController.navigate(Route.TypeInfo)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Don't Know About Your Type?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
