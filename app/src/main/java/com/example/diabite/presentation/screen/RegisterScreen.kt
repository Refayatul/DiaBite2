package com.example.diabite.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diabite.common.Route
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {

    val registrationState by viewModel.registrationState.collectAsState()
    val signUpState by viewModel.signUpState.collectAsState()

    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    // State for validation
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var passwordMatchError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var typeError by remember { mutableStateOf(false) }

    val diabetesTypes = listOf(
        "Diabetes Type 1",
        "Diabetes Type 2"
    )

    // Handle Email sign-up state
    LaunchedEffect(signUpState) {
        if (signUpState is Resource.Success) {
            // FIX: Explicitly navigate to Home on success
             navController.navigate(Route.Home) {
                 popUpTo(Route.Login) { inclusive = true }
             }
        }
        if (signUpState is Resource.Success || signUpState is Resource.Error) {
            isRegistering = false
        }
    }

    var visible by remember { mutableStateOf(false) }
    // Trigger initial animation
    LaunchedEffect(Unit) {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.95f), // Slightly wider card
                    shape = RoundedCornerShape(24.dp), // More rounded corners
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp), // Increased elevation for more depth
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface // Standard surface color
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp), // Increased padding inside card
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Login", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Create Account",
                            style = MaterialTheme.typography.headlineLarge.copy( // Larger title
                                fontWeight = FontWeight.ExtraBold, // Bolder title
                                color = MaterialTheme.colorScheme.primary // Colored title
                            ),
                        )

                        Text(
                            "Join DiaBite",
                            style = MaterialTheme.typography.bodyMedium.copy( // Styled subtitle
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Name Input
                        OutlinedTextField(
                            value = registrationState.name,
                            onValueChange = { viewModel.updateRegistrationState(registrationState.copy(name = it)); nameError = false },
                            label = { Text("Full Name") },
                            placeholder = { Text("Enter your full name") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon", tint = MaterialTheme.colorScheme.primary) }, // Colored icon
                            singleLine = true,
                            isError = nameError,
                            supportingText = { if (nameError) Text("Name is required") },
                            colors = TextFieldDefaults.colors( // Custom colors for text field
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Email Input
                        OutlinedTextField(
                            value = registrationState.email,
                            onValueChange = { viewModel.updateRegistrationState(registrationState.copy(email = it)); emailError = false },
                            label = { Text("Email Address") },
                            placeholder = { Text("Enter your email") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = MaterialTheme.colorScheme.primary) }, // Colored icon
                            singleLine = true,
                            isError = emailError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            supportingText = { if (emailError) Text("Valid email is required") },
                            colors = TextFieldDefaults.colors( // Custom colors for text field
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Password Input
                        OutlinedTextField(
                            value = registrationState.password,
                            onValueChange = { viewModel.updateRegistrationState(registrationState.copy(password = it)); passwordError = false; passwordMatchError = false },
                            label = { Text("Password") },
                            placeholder = { Text("Min 6 characters") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = MaterialTheme.colorScheme.primary) }, // Colored icon
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant // Colored icon
                                    )
                                }
                            },
                            singleLine = true,
                            isError = passwordError || passwordMatchError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            supportingText = {
                                if (passwordError) Text("Password must be at least 6 characters")
                                else if (passwordMatchError) Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                            },
                            colors = TextFieldDefaults.colors( // Custom colors for text field
                                focusedIndicatorColor = if (passwordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = if (passwordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = if (passwordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (passwordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = if (passwordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Confirm Password Input
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; confirmPasswordError = false; passwordMatchError = false },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon", tint = MaterialTheme.colorScheme.primary) }, // Colored icon
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant // Colored icon
                                    )
                                }
                            },
                            singleLine = true,
                            isError = confirmPasswordError || passwordMatchError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            supportingText = { if (confirmPasswordError) Text("Confirmation is required") },
                            colors = TextFieldDefaults.colors( // Custom colors for text field
                                focusedIndicatorColor = if (confirmPasswordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = if (confirmPasswordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = if (confirmPasswordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = if (confirmPasswordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = if (confirmPasswordError || passwordMatchError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Diabetes Type Selection
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            OutlinedTextField(
                                value = registrationState.diabetesType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Diabetes Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                isError = typeError,
                                supportingText = { if (typeError) Text("Please select your diabetes type") },
                                colors = TextFieldDefaults.colors( // Custom colors for dropdown
                                    focusedIndicatorColor = if (typeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = if (typeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = if (typeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = if (typeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    cursorColor = if (typeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                diabetesTypes.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            viewModel.updateRegistrationState(registrationState.copy(diabetesType = option))
                                            typeExpanded = false
                                            typeError = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (!isRegistering) {
                                    emailError = registrationState.email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(registrationState.email).matches()
                                    passwordError = registrationState.password.length < 6
                                    confirmPasswordError = confirmPassword.isEmpty()
                                    passwordMatchError = registrationState.password != confirmPassword
                                    nameError = registrationState.name.isEmpty()
                                    typeError = registrationState.diabetesType.isEmpty()

                                    if (!emailError && !passwordError && !confirmPasswordError && !passwordMatchError && !nameError && !typeError) {
                                        isRegistering = true
                                        viewModel.signUp(
                                            email = registrationState.email,
                                            password = registrationState.password,
                                            name = registrationState.name,
                                            diabetesType = registrationState.diabetesType
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp), // Increased height
                            enabled = !isRegistering,
                            colors = ButtonDefaults.buttonColors( // Custom button color
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isRegistering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Create Account", fontWeight = FontWeight.Bold) // Bolder text
                            }
                        }

                        if (signUpState is Resource.Error) {
                            Text(
                                text = (signUpState as Resource.Error).error?.userMessage ?: "Sign up failed",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { navController.navigate(Route.Login) }) {
                            Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall.copy( // Styled link text
                                    fontWeight = FontWeight.Medium
                                ))
                        }
                    }
                }
            }
        }
    }
}
