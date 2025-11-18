package com.example.diabite.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.diabite.R
import com.example.diabite.common.Route
import com.example.diabite.presentation.theme.TextDarkGray
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {

    val registrationState by viewModel.registrationState.collectAsState()
    val signUpState by viewModel.signUpState.collectAsState()
    
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    // State for validation
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var passwordMatchError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var typeError by remember { mutableStateOf(false) }

    val diabetesTypes = listOf(
        "Diabetes Type 1",
        "Diabetes Type 2",
        "Prediabetes",
        "Gestational Diabetes",
        "Other"
    )

    val context = LocalContext.current
    val googleSignInState by viewModel.googleSignInState.collectAsState()
    var isGoogleSignInLoading by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null && account.idToken != null) {
                    viewModel.googleSignIn(account.idToken!!)
                } else {
                    viewModel.resetAuthState()
                }
            } catch (e: ApiException) {
                Timber.e(e, "Google Sign-In failed")
                viewModel.resetAuthState()
            }
        }
    )

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Handle Google sign-in state changes
    LaunchedEffect(googleSignInState) {
        when (googleSignInState) {
            is Resource.Success<*> -> {
                isGoogleSignInLoading = false
                // Google Sign in successful, navigation handled by NavHost based on AuthState
            }
            is Resource.Error<*> -> {
                isGoogleSignInLoading = false
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }

    // Handle Email sign-up state
    LaunchedEffect(signUpState) {
        if (signUpState is Resource.Success) {
             // Auth state update will trigger navigation in NavHost
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
                modifier = Modifier.padding(32.dp),
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

                Spacer(modifier = Modifier.height(24.dp))

                // Name Input
                OutlinedTextField(
                    value = registrationState.name,
                    onValueChange = { viewModel.updateRegistrationState(registrationState.copy(name = it)); nameError = false },
                    label = { Text("Full Name") },
                    placeholder = { Text("Enter your full name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name is required") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email Input
                OutlinedTextField(
                    value = registrationState.email,
                    onValueChange = { viewModel.updateRegistrationState(registrationState.copy(email = it)); emailError = false },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                    singleLine = true,
                    isError = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = { if (emailError) Text("Valid email is required") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Input
                OutlinedTextField(
                    value = registrationState.password,
                    onValueChange = { viewModel.updateRegistrationState(registrationState.copy(password = it)); passwordError = false; passwordMatchError = false },
                    label = { Text("Password") },
                    placeholder = { Text("Min 6 characters") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
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
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordError = false; passwordMatchError = false },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password Icon") },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true,
                    isError = confirmPasswordError || passwordMatchError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    supportingText = { if (confirmPasswordError) Text("Confirmation is required") }
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
                            .menuAnchor(),
                        isError = typeError,
                        supportingText = { if (typeError) Text("Please select your diabetes type") }
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
                        emailError = registrationState.email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(registrationState.email).matches()
                        passwordError = registrationState.password.length < 6
                        confirmPasswordError = confirmPassword.isEmpty()
                        passwordMatchError = registrationState.password != confirmPassword
                        nameError = registrationState.name.isEmpty()
                        typeError = registrationState.diabetesType.isEmpty()

                        if (!emailError && !passwordError && !confirmPasswordError && !passwordMatchError && !nameError && !typeError) {
                            viewModel.signUp(
                                email = registrationState.email,
                                password = registrationState.password,
                                name = registrationState.name,
                                diabetesType = registrationState.diabetesType
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = signUpState !is Resource.Loading
                ) {
                    if (signUpState is Resource.Loading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Account")
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
                
                // Google Sign In Button
                Button(
                    onClick = {
                        isGoogleSignInLoading = true
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.LightGray
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    enabled = !isGoogleSignInLoading
                ) {
                    if (isGoogleSignInLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Signing in...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    } else {
                        Text(
                            "Continue with Google",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                if (googleSignInState is Resource.Error<*>) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val googleError = (googleSignInState as Resource.Error<*>).error
                    val googleErrorMessage = when (googleError) {
                        is com.example.diabite.util.AppError.NetworkError -> "Network error. Please check your connection"
                        else -> googleError?.userMessage ?: "Google sign-in failed. Please try again"
                    }
                    Text(
                        text = googleErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { navController.navigate(Route.Login) }) {
                    Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
