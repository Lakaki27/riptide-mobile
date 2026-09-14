package com.evertide.riptide.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.navigation.Routes

@Composable
fun AuthScreen(
    navController: NavHostController,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateHome -> {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.resetToken != null) {
            ResetPasswordForm(state, viewModel)
        } else {
            LoginForm(state, viewModel)
        }
    }

    if (state.showForgotModal) {
        AlertDialog(
            onDismissRequest = { viewModel.setForgotModalVisible(false) },
            title = { Text("Forgot password") },
            text = { Text("Please contact your administrator to have your password reset.") },
            confirmButton = {
                TextButton(onClick = { viewModel.setForgotModalVisible(false) }) {
                    Text("Got it")
                }
            },
        )
    }
}

@Composable
private fun LoginForm(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.width(320.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Phase 11: swap for the real app logo drawable.
        Text("Riptide", style = MaterialTheme.typography.titleLarge)
        Text("Sign in", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = { Text("Email") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = viewModel::submitLogin,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign in")
            }
        }

        TextButton(onClick = { viewModel.setForgotModalVisible(true) }) {
            Text("Forgot password?")
        }
    }
}

@Composable
private fun ResetPasswordForm(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.width(320.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Set a new password", style = MaterialTheme.typography.titleLarge)
        Text(
            "Your password was reset by an administrator. Choose a new one to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            placeholder = { Text("New password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            placeholder = { Text("Confirm new password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        state.resetError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = viewModel::submitReset,
            enabled = !state.resetLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.resetLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Set password")
            }
        }
    }
}
