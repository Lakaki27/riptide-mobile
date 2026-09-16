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
import androidx.compose.ui.res.stringResource
import com.evertide.riptide.R

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
                    navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
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
                title = { Text(stringResource(R.string.forgot_password)) },
                text = { Text(stringResource(R.string.forgot_password_text)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.setForgotModalVisible(false) }) {
                        Text(stringResource(R.string.got_it))
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
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.sign_in), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = { Text(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Email
                        ),
                modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Password
                        ),
                modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
                onClick = viewModel::submitLogin,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.sign_in))
            }
        }

        TextButton(onClick = { viewModel.setForgotModalVisible(true) }) { Text("Forgot password?") }
    }
}

@Composable
private fun ResetPasswordForm(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
            modifier = Modifier.width(320.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.set_a_new_password), style = MaterialTheme.typography.titleLarge)
        Text(
                stringResource(R.string.your_password_was_reset),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                placeholder = { Text(stringResource(R.string.new_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholder = { Text(stringResource(R.string.new_password_confirm)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
        )

        state.resetError?.let {
            Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
                onClick = viewModel::submitReset,
                enabled = !state.resetLoading,
                modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.resetLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.set_password))
            }
        }
    }
}
