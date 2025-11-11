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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.diabite.util.Resource

@Composable
fun MedicalConditionsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val signUpState by authViewModel.signUpState.collectAsState()
    val registrationState by authViewModel.registrationState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

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

    val hasDiabetesSelected = registrationState.primaryConditions.any { condition ->
        diabetesConditions.contains(condition)
    }

    LaunchedEffect(signUpState) {
        when (val state = signUpState) {
            is Resource.Success -> {
                if (state.data != null) { // Only navigate on successful registration
                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    authViewModel.clearRegistrationData()
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Login) { inclusive = true }
                    }
                }
            }
            is Resource.Error -> {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Medical Conditions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkGray
                    ),
                )

                Text(
                    "Step 2 of 3 - Select any conditions that apply to you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Diabetes Related",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                diabetesConditions.forEach { condition ->
                    MedicalConditionCheckbox(
                        condition = condition,
                        isSelected = registrationState.primaryConditions.contains(condition),
                        onCheckedChange = { checked ->
                            val updatedConditions = if (checked) {
                                registrationState.primaryConditions + condition
                            } else {
                                registrationState.primaryConditions.filter { it != condition }
                            }
                            // Also update the diabetes type if a diabetes condition is selected
                            val diabetesType = updatedConditions.firstOrNull { it in diabetesConditions } ?: ""
                            authViewModel.updateRegistrationState(registrationState.copy(primaryConditions = updatedConditions, diabetesType = diabetesType))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Other Conditions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                otherConditions.forEach { condition ->
                    MedicalConditionCheckbox(
                        condition = condition,
                        isSelected = registrationState.primaryConditions.contains(condition),
                        onCheckedChange = { checked ->
                            val updatedConditions = if (checked) {
                                registrationState.primaryConditions + condition
                            } else {
                                registrationState.primaryConditions.filter { it != condition }
                            }
                            authViewModel.updateRegistrationState(registrationState.copy(primaryConditions = updatedConditions))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Select all that apply. You can change these later in your profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

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
                            if (hasDiabetesSelected) {
                                navController.navigate(Route.DiabetesDetails)
                            } else {
                                authViewModel.signUpWithProfile(
                                    email = registrationState.email,
                                    password = registrationState.password,
                                    displayName = registrationState.displayName,
                                    dateOfBirth = registrationState.dateOfBirth,
                                    biologicalSex = registrationState.biologicalSex,
                                    primaryConditions = registrationState.primaryConditions,
                                    diabetesType = "",
                                    diabetesMedications = emptyList()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = true
                    ) {
                        Text(if (hasDiabetesSelected) "Next: Diabetes Details" else "Complete Registration")
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalConditionCheckbox(
    condition: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            text = condition,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
