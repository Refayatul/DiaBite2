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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.theme.TextDarkGray
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {

    // State for all fields
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var biologicalSex by remember { mutableStateOf("") }
    var diabetesType by remember { mutableStateOf("") }

    // Dropdown states
    var sexExpanded by remember { mutableStateOf(false) }
    var diabetesExpanded by remember { mutableStateOf(false) }

    // State for validation
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var passwordMatchError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val signUpState by viewModel.signUpState.collectAsState()

    // Handle sign up state changes
    LaunchedEffect(signUpState) {
        when (signUpState) {
            is Resource.Success<*> -> {
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                navController.navigate(Route.Login) {
                    popUpTo(Route.Signup) { inclusive = true }
                }
            }
            is Resource.Error<*> -> {
                // Error is handled in the UI below
            }
            else -> {}
        }
    }

    val sexOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val diabetesOptions = listOf("Type 1", "Type 2", "Gestational", "Other", "None")

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
                // Back Button (Good UX)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Login", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Create Account",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkGray
                    ),
                )

                Text(
                    "Join DiaBite",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                    isError = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = { if (emailError) Text("Valid email is required") }
                )

                // Date of Birth Field
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth (Optional)") },
                    placeholder = { Text("MM/DD/YYYY") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
                        label = { Text("Biological Sex (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
                                }
                            )
                        }
                    }
                }

                // Diabetes Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = diabetesExpanded,
                    onExpandedChange = { diabetesExpanded = !diabetesExpanded }
                ) {
                    OutlinedTextField(
                        value = diabetesType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Diabetes Type (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diabetesExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = diabetesExpanded,
                        onDismissRequest = { diabetesExpanded = false }
                    ) {
                        diabetesOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    diabetesType = option
                                    diabetesExpanded = false
                                }
                            )
                        }
                    }
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = false; passwordMatchError = false },
                    label = { Text("Password") },
                    placeholder = { Text("Create a password (min 6 characters)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                    singleLine = true,
                    isError = passwordError || passwordMatchError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = {
                        if (passwordError) Text("Password is required")
                        else if (passwordMatchError) Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                    }
                )

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordError = false; passwordMatchError = false },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon") },
                    singleLine = true,
                    isError = confirmPasswordError || passwordMatchError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { if (confirmPasswordError) Text("Confirmation is required") }
                )

                // Error message
                if (signUpState is Resource.Error<*>) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val appError = (signUpState as Resource.Error<*>).error
                    val errorMessage = when (appError) {
                        is AppError.InvalidPasswordError -> "Password should be at least 6 characters"
                        is AppError.UserAlreadyExistsError -> "An account with this email already exists"
                        is AppError.InvalidEmailError -> "Invalid email format"
                        is AppError.NetworkError -> "Network error. Please check your connection"
                        else -> appError?.userMessage ?: "An unknown error occurred"
                    }
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Up Button
                Button(
                    onClick = {
                        // Validation logic
                        nameError = displayName.isEmpty()
                        emailError = email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        passwordError = password.length < 6
                        confirmPasswordError = confirmPassword.isEmpty()
                        passwordMatchError = password != confirmPassword

                        if (!nameError && !emailError && !passwordError && !confirmPasswordError && !passwordMatchError) {
                            val primaryConditions = if (diabetesType.isNotEmpty() && diabetesType != "None") {
                                listOf("Diabetes ($diabetesType)")
                            } else {
                                emptyList()
                            }

                            viewModel.signUp(
                                email = email,
                                password = password,
                                displayName = displayName,
                                dateOfBirth = dateOfBirth,
                                biologicalSex = biologicalSex.lowercase(),
                                primaryConditions = primaryConditions,
                                diabetesType = diabetesType.lowercase().replace(" ", "")
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = signUpState !is Resource.Loading<*>
                ) {
                    if (signUpState is Resource.Loading<*>) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Account")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Link back to login
                TextButton(onClick = { navController.navigate(Route.Login) }) {
                    Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = {
                    navController.navigate(Route.BasicInfo())
                }) {
                    Text("Complete Registration →", color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
