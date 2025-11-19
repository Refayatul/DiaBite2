package com.example.diabite.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    var visible by remember { mutableStateOf(false) }

    // Local state for editing
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var diabetesType by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    
    // State for Logout Confirmation Dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Initialize state when user loads or editing starts
    if (!isEditing && currentUser != null) {
        name = currentUser!!.name
        diabetesType = currentUser!!.diabetesType
    }

    val diabetesTypeOptions = listOf(
        "Diabetes Type 1",
        "Diabetes Type 2"
    )

    // Extract colors here to solve the "Context of @Composable" error
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error

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

            // Draw some subtle, semi-transparent circles using the extracted variables
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Trigger initial animation
            LaunchedEffect(Unit) {
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Personal Information",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Name
                        OutlinedTextField(
                            value = if (isEditing) name else currentUser?.name ?: "",
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                            readOnly = !isEditing,
                            enabled = isEditing,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Email (Always read-only)
                        OutlinedTextField(
                            value = currentUser?.email ?: "",
                            onValueChange = {},
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                            readOnly = true,
                            enabled = false,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Diabetes Type
                        if (isEditing) {
                            ExposedDropdownMenuBox(
                                expanded = typeExpanded,
                                onExpandedChange = { typeExpanded = !typeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = diabetesType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Diabetes Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false }
                                ) {
                                    diabetesTypeOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                diabetesType = option
                                                typeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = currentUser?.diabetesType ?: "",
                                onValueChange = {},
                                label = { Text("Diabetes Type") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditing = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        currentUser?.let { user ->
                                            authViewModel.updateUserProfile(
                                                user.copy(name = name, diabetesType = diabetesType)
                                            )
                                        }
                                        isEditing = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Save")
                                }
                            }
                        } else {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("Edit Profile")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- LOGOUT BUTTON ---
                        OutlinedButton(
                            onClick = { showLogoutDialog = true }, // Shows the dialog instead of immediate logout
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = errorColor
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = errorColor
                            )
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }
    
    // --- LOGOUT CONFIRMATION DIALOG ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out? You will be returned to the login screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        
                        // FIX: Use popUpTo(0) for absolute stack clearing
                        navController.navigate(Route.Login) {
                            popUpTo(0) { 
                                inclusive = true 
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}