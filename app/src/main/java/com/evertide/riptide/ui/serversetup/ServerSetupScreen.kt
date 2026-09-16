package com.evertide.riptide.ui.serversetup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.R

@Composable
fun ServerSetupScreen(
        navController: NavHostController,
        viewModel: ServerSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ServerSetupEvent.NavigateTo -> {
                    navController.navigate(event.route) { popUpTo(0) { inclusive = true } }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                modifier = Modifier.width(320.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    stringResource(R.string.server_connect),
                    style = MaterialTheme.typography.titleLarge
            )
            Text(
                    stringResource(R.string.server_address),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::onUrlChange,
                    placeholder = { Text(stringResource(R.string.server_address)) },
                    singleLine = true,
                    keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done,
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
                    onClick = viewModel::submit,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.continue_string))
                }
            }
        }
    }
}
