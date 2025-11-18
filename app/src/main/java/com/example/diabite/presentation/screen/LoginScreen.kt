package com.example.diabite.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.diabite.R
import com.example.diabite.common.Route
import com.example.diabite.presentation.theme.TextDarkGray
import com.example.diabite.presentation.viewmodel.AuthViewModel
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import timber.log.Timber

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {

    var useremail by remember { mutableStateOf("") }
    var userpass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoginInProgress by remember { mutableStateOf(false) }

    // State for interactive validation/error feedback
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()

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
                    // Reset loading state and show error
                    viewModel.resetAuthState()
                }
            }
            catch (e: ApiException) {
                Timber.e(e, "Google Sign-In failed")
                // Reset loading state and show error
                viewModel.resetAuthState()
            }
        }
    )

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is Resource.Success<*> -> {
                isLoginInProgress = false
                navController.navigate(Route.Home) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            }
            is Resource.Error<*> -> {
                isLoginInProgress = false
                // Error is handled in the UI below
            }
            else -> {}
        }
    }

    // Handle Google sign-in state changes
    LaunchedEffect(googleSignInState) {
        when (googleSignInState) {
            is Resource.Success<*> -> {
                isGoogleSignInLoading = false
                navController.navigate(Route.Home) {
                    popUpTo(Route.Login) { inclusive = true }
                }
            }
            is Resource.Error<*> -> {
                isGoogleSignInLoading = false
                // Error is handled in the UI below
                // Reset the state after showing error
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .padding(16.dp)
        ,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // --- REPLACED Icon with Image to use app_logo.png ---
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(120.dp), // Increased size for prominence
                    contentScale = ContentScale.Fit
                )
                // ---------------------------------------------------

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "DiaBite",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDarkGray
                    ),
                )

                Spacer(modifier=Modifier.height(32.dp))

                Text(
                    "User Login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = useremail,
                    onValueChange = {
                        useremail = it
                        emailError = false
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError,
                    supportingText = {
                        if (emailError) {
                            Text(text = "Email is required")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = userpass,
                    onValueChange = {
                        userpass = it
                        passwordError = false
                    },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your Password") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError,
                    supportingText = {
                        if (passwordError) {
                            Text(text = "Password is required")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button
                Button(
                    onClick = {
                        emailError = useremail.isEmpty()
                        passwordError = userpass.isEmpty()

                        if (!emailError && !passwordError) {
                            isLoginInProgress = true
                            viewModel.login(useremail, userpass)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoginInProgress
                ) {
                    if (isLoginInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Login")
                    }
                }

                // Google Sign-In Button
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

                // Google Sign-In Error message
                if (googleSignInState is Resource.Error<*>) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val googleError = (googleSignInState as Resource.Error<*>).error
                    val googleErrorMessage = when (googleError) {
                        is AppError.NetworkError -> "Network error. Please check your connection"
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

                // Error message
                if (loginState is Resource.Error<*>) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val appError = (loginState as Resource.Error<*>).error
                    val errorMessage = when (appError) {
                        is AppError.InvalidCredentialsError -> "Invalid email or password"
                        is AppError.UserNotFoundError -> "No account found with this email"
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

                // Forgot Password Link
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Forgot Password?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        // TODO: Navigate to forgot password screen
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "Don't have an account? Sign Up" Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val annotatedString = buildAnnotatedString {
                        append("Don't have an account? ")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Sign Up")
                        }
                    }
                    Text(
                        text = annotatedString,
                        modifier = Modifier
                            .clickable {
                                navController.navigate(Route.Signup)
                            }
                            .padding(4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}