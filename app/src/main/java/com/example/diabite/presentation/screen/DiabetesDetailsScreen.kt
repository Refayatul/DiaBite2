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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.diabite.common.Route
import com.example.diabite.data.model.UserProfile
import com.example.diabite.presentation.theme.TextDarkGray
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiabetesDetailsScreen(
    navController: NavController,
    email: String,
    password: String,
    displayName: String,
    dateOfBirth: String,
    biologicalSex: String,
    primaryConditions: List<String>,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    // Diabetes details state
    var diabetesType by remember { mutableStateOf("") }
    var selectedMedications by remember { mutableStateOf(setOf<String>()) }
    var otherMedication by remember { mutableStateOf("") }

    var typeExpanded by remember { mutableStateOf(false) }

    // Validation
    var diabetesTypeError by remember { mutableStateOf(false) }

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

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is Resource.Success<*> -> {
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navController.navigate(Route.Home) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            }
            is Resource.Error<*> -> {
                // Error is handled in the UI below
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
                // Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Diabetes Details",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkGray
                    ),
                )

                Text(
                    "Step 3 of 3 - Tell us more about your diabetes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Diabetes Type Dropdown
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
                        isError = diabetesTypeError,
                        supportingText = { if (diabetesTypeError) Text("Please select your diabetes type") }
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
                                    diabetesTypeError = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Medications Section
                Text(
                    "Medications (Optional)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Text(
                    "Select any diabetes medications you take:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                commonMedications.forEach { medication ->
                    MedicationCheckbox(
                        medication = medication,
                        isSelected = selectedMedications.contains(medication),
                        onCheckedChange = { checked ->
                            selectedMedications = if (checked) {
                                selectedMedications + medication
                            } else {
                                selectedMedications - medication
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Other medication field
                OutlinedTextField(
                    value = otherMedication,
                    onValueChange = { otherMedication = it },
                    label = { Text("Other Medication (Optional)") },
                    placeholder = { Text("Enter other medication name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "This information helps us provide personalized recommendations. You can update it later in your profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Error message
                if (authState is Resource.Error<*>) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val errorMessage = (authState as Resource.Error<*>).error?.userMessage ?: "An error occurred during registration"
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            diabetesTypeError = diabetesType.isEmpty()

                            if (!diabetesTypeError) {
                                val allMedications = selectedMedications.toMutableList()
                                if (otherMedication.isNotEmpty()) {
                                    allMedications.add(otherMedication)
                                }

                                // Create UserProfile and complete registration
                                val userProfile = UserProfile(
                                    email = email,
                                    displayName = displayName,
                                    dateOfBirth = try {
                                        java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                                            .parse(dateOfBirth)
                                    } catch (e: Exception) {
                                        null
                                    },
                                    biologicalSex = biologicalSex,
                                    primaryConditions = primaryConditions,
                                    diabetesType = diabetesType,
                                    diabetesMedications = allMedications
                                )

                                // Call signUpWithProfile to save complete profile
                                authViewModel.signUpWithProfile(
                                    email = email,
                                    password = password,
                                    displayName = displayName,
                                    dateOfBirth = dateOfBirth,
                                    biologicalSex = biologicalSex,
                                    primaryConditions = primaryConditions,
                                    diabetesType = diabetesType,
                                    diabetesMedications = allMedications
                                )
                            } else {
                                Toast.makeText(context, "Please select your diabetes type", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = authState !is Resource.Loading<*>
                    ) {
                        if (authState is Resource.Loading<*>) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Complete Registration")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationCheckbox(
    medication: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = medication,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
