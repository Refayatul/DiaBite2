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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.theme.TextDarkGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    navController: NavController,
    initialEmail: String = "",
    initialPassword: String = ""
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var biologicalSex by remember { mutableStateOf("") }

    var sexExpanded by remember { mutableStateOf(false) }

    // Validation states
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var dateOfBirthError by remember { mutableStateOf(false) }
    var sexError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sexOptions = listOf("Male", "Female", "Other", "Prefer not to say")

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
                modifier = Modifier.padding(32.dp),
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
                    "Basic Information",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkGray
                    ),
                )

                Text(
                    "Step 1 of 3 - Tell us about yourself",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Display Name Field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; nameError = false },
                    label = { Text("Full Name") },
                    placeholder = { Text("Enter your full name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name is required") }
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = false },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError,
                    supportingText = { if (emailError) Text("Valid email is required") }
                )

                // Date of Birth Field
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it; dateOfBirthError = false },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("MM/DD/YYYY") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = dateOfBirthError,
                    supportingText = { if (dateOfBirthError) Text("Date of birth is required") }
                )

                // Biological Sex Dropdown
                ExposedDropdownMenuBox(
                    expanded = sexExpanded,
                    onExpandedChange = { sexExpanded = !sexExpanded }
                ) {
                    OutlinedTextField(
                        value = biologicalSex,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Biological Sex") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = sexError,
                        supportingText = { if (sexError) Text("Please select your biological sex") }
                    )
                    ExposedDropdownMenu(
                        expanded = sexExpanded,
                        onDismissRequest = { sexExpanded = false }
                    ) {
                        sexOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    biologicalSex = option
                                    sexExpanded = false
                                    sexError = false
                                }
                            )
                        }
                    }
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = false },
                    label = { Text("Password") },
                    placeholder = { Text("Create a password (min 6 characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError,
                    supportingText = { if (passwordError) Text("Password must be at least 6 characters") }
                )

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordError = false },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPasswordError,
                    supportingText = { if (confirmPasswordError) Text("Passwords do not match") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Next Button
                Button(
                    onClick = {
                        // Validation
                        nameError = displayName.isEmpty()
                        emailError = email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        dateOfBirthError = dateOfBirth.isEmpty()
                        sexError = biologicalSex.isEmpty()
                        passwordError = password.length < 6
                        confirmPasswordError = password != confirmPassword

                        if (!nameError && !emailError && !dateOfBirthError && !sexError && !passwordError && !confirmPasswordError) {
                            navController.navigate(
                                Route.MedicalConditions(
                                    email = email,
                                    password = password,
                                    displayName = displayName,
                                    dateOfBirth = dateOfBirth,
                                    biologicalSex = biologicalSex
                                )
                            )
                        } else {
                            Toast.makeText(context, "Please correct the errors above", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Next: Medical Conditions")
                }
            }
        }
    }
}
