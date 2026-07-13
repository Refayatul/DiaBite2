package com.example.diabite.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diabite.util.AppError
import com.example.diabite.util.Resource

/**
 * Composable for displaying errors from Resource states
 */
@Composable
fun ErrorHandler(
    resource: Resource<*>?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    resource?.error?.let {
        ErrorCard(
            error = it,
            onRetry = if (it.retryable) onRetry else null,
            modifier = modifier
        )
    }
}

/**
 * Card component for displaying error information
 */
@Composable
fun ErrorCard(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error.userMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}

/**
 * Snackbar for displaying error messages
 */
@Composable
fun ErrorSnackbar(
    error: AppError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        action = {
            if (error.retryable) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    ) {
        Text(error.userMessage)
    }
}

/**
 * Extension function to show error as snackbar
 */
suspend fun SnackbarHostState.showError(error: AppError) {
    this.showSnackbar(
        message = error.userMessage,
        actionLabel = if (error.retryable) "Retry" else null
    )
}
