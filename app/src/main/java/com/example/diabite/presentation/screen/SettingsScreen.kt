package com.example.diabite.presentation.screen

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.theme.TextDarkGray
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.presentation.viewmodel.ProfileState
import com.example.diabite.presentation.viewmodel.UpdateState
import com.example.diabite.presentation.viewmodel.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profileState by userProfileViewModel.profileState.collectAsState()
    val updateState by userProfileViewModel.updateState.collectAsState()

    // Local state for editing
    val selectedConditions = remember { mutableStateOf(setOf<String>()) }
    val diabetesType = remember { mutableStateOf("") }
    val selectedMedications = remember { mutableStateOf(setOf<String>()) }
    val otherMedication = remember { mutableStateOf("") }

    val typeExpanded = remember { mutableStateOf(false) }

    // Medical conditions options (same as registration)
    val diabetesConditions = listOf(
        "Diabetes Type 1",
        "Diabetes Type 2",
        "Prediabetes",
        "Gestational Diabetes"
    )

    val otherConditions = listOf(
        "Hypertension",
        "Hypotension",
        "High Cholesterol",
        "Coronary Artery Disease",
        "Kidney Disease",
        "Obesity",
        "PCOS",
        "Thyroid Disorders",
        "Pregnancy"
    )

    val diabetesTypeOptions = listOf(
        "Type 1 Diabetes",
        "Type 2 Diabetes",
        "Gestational Diabetes",
        "Other"
    )

    val commonMedications = listOf(
        "Metformin",
        "Insulin (various types)",
        "Glipizide",
        "Glyburide",
        "Pioglitazone",
        "Sitagliptin",
        "Empagliflozin",
        "Liraglutide",
        "DPP-4 inhibitors",
        "SGLT2 inhibitors"
    )

    // Initialize local state when profile loads
    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success) {
            val profile = (profileState as ProfileState.Success).profile
            selectedConditions.value = profile.primaryConditions.toSet()
            diabetesType.value = profile.diabetesType ?: ""
            selectedMedications.value = profile.diabetesMedications.toSet()
        }
    }

    // Handle update state changes
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.Success -> {
                Toast.makeText(context, (updateState as UpdateState.Success).message, Toast.LENGTH_SHORT).show()
                userProfileViewModel.resetUpdateState()
            }
            is UpdateState.Error -> {
                Toast.makeText(context, (updateState as UpdateState.Error).message, Toast.LENGTH_SHORT).show()
                userProfileViewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (profileState) {
            is ProfileState.Loading -> {
                CircularProgressIndicator()
            }
            is ProfileState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to load profile",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (profileState as ProfileState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = { userProfileViewModel.refreshProfile() }) {
                        Text("Retry")
                    }
                }
            }
            is ProfileState.Success -> {
                val profile = (profileState as ProfileState.Success).profile

                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Header with back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                            }

                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkGray
                                ),
                            )

                            // Logout button
                            IconButton(
                                onClick = {
                                    authViewModel.logout()
                                    navController.navigate(Route.Login) {
                                        popUpTo(Route.Home) { inclusive = true }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Personal Information Section (Read-only)
                        Text(
                            "Personal Information",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Display Name (read-only)
                        OutlinedTextField(
                            value = profile.displayName,
                            onValueChange = {},
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon") },
                            readOnly = true,
                            enabled = false
                        )

                        // Email (read-only)
                        OutlinedTextField(
                            value = profile.email,
                            onValueChange = {},
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                            readOnly = true,
                            enabled = false
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Medical Conditions Section (Editable)
                        Text(
                            "Medical Conditions",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Text(
                            "Update your medical conditions:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Diabetes Conditions Section
                        Text(
                            "Diabetes Related",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        diabetesConditions.forEach { condition ->
                            MedicalConditionCheckbox(
                                condition = condition,
                                isSelected = selectedConditions.value.contains(condition),
                                onCheckedChange = { checked: Boolean ->
                                    selectedConditions.value = if (checked) {
                                        selectedConditions.value + condition
                                    } else {
                                        selectedConditions.value - condition
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Other Conditions Section
                        Text(
                            "Other Conditions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        otherConditions.forEach { condition ->
                            MedicalConditionCheckbox(
                                condition = condition,
                                isSelected = selectedConditions.value.contains(condition),
                                onCheckedChange = { checked: Boolean ->
                                    selectedConditions.value = if (checked) {
                                        selectedConditions.value + condition
                                    } else {
                                        selectedConditions.value - condition
                                    }
                                }
                            )
                        }

                        // Diabetes Details Section (conditionally visible)
                        val hasDiabetesSelected = selectedConditions.value.any { condition ->
                            diabetesConditions.contains(condition)
                        }

                        if (hasDiabetesSelected) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                "Diabetes Details",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Diabetes Type Dropdown
                            ExposedDropdownMenuBox(
                                expanded = typeExpanded.value,
                                onExpandedChange = { typeExpanded.value = !typeExpanded.value }
                            ) {
                                OutlinedTextField(
                                    value = diabetesType.value,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Diabetes Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded.value) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = typeExpanded.value,
                                    onDismissRequest = { typeExpanded.value = false }
                                ) {
                                    diabetesTypeOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                diabetesType.value = option
                                                typeExpanded.value = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Medications Section
                            Text(
                                "Medications",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            commonMedications.forEach { medication ->
                                MedicationCheckbox(
                                    medication = medication,
                                    isSelected = selectedMedications.value.contains(medication),
                                    onCheckedChange = { checked: Boolean ->
                                        selectedMedications.value = if (checked) {
                                            selectedMedications.value + medication
                                        } else {
                                            selectedMedications.value - medication
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Other medication field
                            OutlinedTextField(
                                value = otherMedication.value,
                                onValueChange = { otherMedication.value = it },
                                label = { Text("Other Medication") },
                                placeholder = { Text("Enter other medication name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val allMedications = selectedMedications.value.toMutableList()
                                    if (otherMedication.value.isNotEmpty()) {
                                        allMedications.add(otherMedication.value)
                                    }

                                    userProfileViewModel.updateProfile(
                                        primaryConditions = selectedConditions.value.toList(),
                                        diabetesType = if (hasDiabetesSelected) diabetesType.value else null,
                                        diabetesMedications = if (hasDiabetesSelected) allMedications else emptyList()
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                enabled = updateState !is UpdateState.Loading
                            ) {
                                if (updateState is UpdateState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Save Changes")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logout Button (destructive action)
                        Button(
                            onClick = {
                                authViewModel.logout()
                                navController.navigate(Route.Login) {
                                    popUpTo(Route.Home) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }
}
