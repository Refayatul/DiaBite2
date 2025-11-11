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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {

    val registrationState by viewModel.registrationState.collectAsState()
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // State for validation
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var passwordMatchError by remember { mutableStateOf(false) }
    var displayNameError by remember { mutableStateOf(false) }
    var dateOfBirthError by remember { mutableStateOf(false) }
    var biologicalSexError by remember { mutableStateOf(false) }

    var sexExpanded by remember { mutableStateOf(false) }

    val biologicalSexOptions = listOf("Male", "Female", "Other", "Prefer not to say")

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
            }
            is Resource.Error<*> -> {
                isGoogleSignInLoading = false
                viewModel.resetAuthState()
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

                Spacer(modifier = Modifier.height(32.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = registrationState.password,
                    onValueChange = { viewModel.updateRegistrationState(registrationState.copy(password = it)); passwordError = false; passwordMatchError = false },
                    label = { Text("Password") },
                    placeholder = { Text("Create a password (min 6 characters)") },
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

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = registrationState.displayName,
                    onValueChange = { viewModel.updateRegistrationState(registrationState.copy(displayName = it)); displayNameError = false },
                    label = { Text("Full Name") },
                    placeholder = { Text("Enter your full name") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon") },
                    singleLine = true,
                    isError = displayNameError,
                    supportingText = { if (displayNameError) Text("Name is required") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Date of Birth",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = registrationState.dateOfBirth.split("/").getOrElse(1) { "" },
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                val parts = registrationState.dateOfBirth.split("/").toMutableList()
                                while (parts.size < 3) parts.add("")
                                parts[1] = it
                                viewModel.updateRegistrationState(registrationState.copy(dateOfBirth = parts.joinToString("/")))
                                dateOfBirthError = false
                            }
                        },
                        label = { Text("Day") },
                        placeholder = { Text("DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)
                    )

                    OutlinedTextField(
                        value = registrationState.dateOfBirth.split("/").getOrElse(0) { "" },
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                val parts = registrationState.dateOfBirth.split("/").toMutableList()
                                while (parts.size < 3) parts.add("")
                                parts[0] = it
                                viewModel.updateRegistrationState(registrationState.copy(dateOfBirth = parts.joinToString("/")))
                                dateOfBirthError = false
                            }
                        },
                        label = { Text("Month") },
                        placeholder = { Text("MM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)
                    )

                    OutlinedTextField(
                        value = registrationState.dateOfBirth.split("/").getOrElse(2) { "" },
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                val parts = registrationState.dateOfBirth.split("/").toMutableList()
                                while (parts.size < 3) parts.add("")
                                parts[2] = it
                                viewModel.updateRegistrationState(registrationState.copy(dateOfBirth = parts.joinToString("/")))
                                dateOfBirthError = false
                            }
                        },
                        label = { Text("Year") },
                        placeholder = { Text("YYYY") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center)
                    )
                }

                if (dateOfBirthError) {
                    Text(
                        "Please enter a valid date of birth",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = sexExpanded,
                    onExpandedChange = { sexExpanded = !sexExpanded }
                ) {
                    OutlinedTextField(
                        value = registrationState.biologicalSex,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Biological Sex") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = biologicalSexError,
                        supportingText = { if (biologicalSexError) Text("Please select your biological sex") }
                    )
                    ExposedDropdownMenu(
                        expanded = sexExpanded,
                        onDismissRequest = { sexExpanded = false }
                    ) {
                        biologicalSexOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.updateRegistrationState(registrationState.copy(biologicalSex = option))
                                    sexExpanded = false
                                    biologicalSexError = false
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
                        displayNameError = registrationState.displayName.isEmpty()

                        val dobParts = registrationState.dateOfBirth.split("/")
                        val day = dobParts.getOrNull(1)?.toIntOrNull()
                        val month = dobParts.getOrNull(0)?.toIntOrNull()
                        val year = dobParts.getOrNull(2)?.toIntOrNull()

                        dateOfBirthError = registrationState.dateOfBirth.isEmpty() || day == null || month == null || year == null ||
                                day !in 1..31 || month !in 1..12 || year < 1900 || year > Calendar.getInstance().get(Calendar.YEAR) ||
                                !isValidDateComponents(day, month, year)

                        biologicalSexError = registrationState.biologicalSex.isEmpty()

                        if (!emailError && !passwordError && !confirmPasswordError && !passwordMatchError &&
                            !displayNameError && !dateOfBirthError && !biologicalSexError) {
                            val dateOfBirth = String.format(Locale.getDefault(), "%02d/%02d/%04d", month, day, year)
                            viewModel.updateRegistrationState(registrationState.copy(dateOfBirth = dateOfBirth))

                            navController.navigate(Route.MedicalConditions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Continue to Medical Conditions")
                }

                Spacer(modifier = Modifier.height(16.dp))
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

private fun isValidDate(dateString: String): Boolean {
    return try {
        val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        format.isLenient = false
        format.parse(dateString)
        true
    } catch (e: Exception) {
        false
    }
}

private fun isValidDateComponents(day: Int, month: Int, year: Int): Boolean {
    return try {
        val calendar = Calendar.getInstance()
        calendar.setLenient(false)
        calendar.set(year, month - 1, day) // Month is 0-based in Calendar
        calendar.get(Calendar.YEAR) == year &&
        calendar.get(Calendar.MONTH) == month - 1 &&
        calendar.get(Calendar.DAY_OF_MONTH) == day
    } catch (e: Exception) {
        false
    }
}
