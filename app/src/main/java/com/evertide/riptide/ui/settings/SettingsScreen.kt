package com.evertide.riptide.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.data.local.ThemePreference
import com.evertide.riptide.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        navController: NavHostController,
        viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()

    LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.titleLarge) }

        item { PreferencesSection(themePreference, state.language, viewModel) }

        item { ChangePasswordSection(state, viewModel) }

        if (isAdmin) {
            item { UsersSection(state, viewModel) }
            item { LibraryManagementSection(viewModel) }
        }

        item {
            OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        navController.navigate(Routes.AUTH) { popUpTo(0) { inclusive = true } }
                    }
            ) { Text("Logout") }
        }
    }

    state.credentialModal?.let { (email, password) ->
        AlertDialog(
                onDismissRequest = viewModel::dismissCredentialModal,
                title = { Text("Account credentials") },
                text = {
                    Column {
                        Text("Share these with the user securely.")
                        Spacer(Modifier.height(8.dp))
                        Text("Email: $email")
                        Text("Password: $password")
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissCredentialModal) { Text("Done") }
                },
        )
    }

    state.confirmResetUser?.let { user ->
        AlertDialog(
                onDismissRequest = { viewModel.setConfirmResetUser(null) },
                title = { Text("Reset password for ${user.email}?") },
                text = { Text("A new temporary password will be generated.") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmResetUserPassword) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setConfirmResetUser(null) }) { Text("Cancel") }
                },
        )
    }

    state.confirmDeleteUser?.let { user ->
        AlertDialog(
                onDismissRequest = { viewModel.setConfirmDeleteUser(null) },
                title = { Text("Delete ${user.email}?") },
                text = { Text("This permanently deletes the account.") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteUserAction) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setConfirmDeleteUser(null) }) {
                        Text("Cancel")
                    }
                },
        )
    }

    state.confirmDeleteSong?.let { music ->
        AlertDialog(
                onDismissRequest = { viewModel.setConfirmDeleteSong(null) },
                title = { Text("Delete ${music.title}?") },
                text = { Text("This permanently removes the file and its data.") },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteSongAction) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setConfirmDeleteSong(null) }) {
                        Text("Cancel")
                    }
                },
        )
    }

    if (state.showCreateUserModal) {
        AlertDialog(
                onDismissRequest = { viewModel.setCreateUserModalVisible(false) },
                title = { Text("New user") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                                value = state.newUserEmail,
                                onValueChange = viewModel::onNewUserEmailChange,
                                placeholder = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                        )
                        var roleExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { roleExpanded = true }) {
                                Text(if (state.newUserRole == "admin") "Admin" else "User")
                            }
                            DropdownMenu(
                                    expanded = roleExpanded,
                                    onDismissRequest = { roleExpanded = false }
                            ) {
                                DropdownMenuItem(
                                        text = { Text("User") },
                                        onClick = {
                                            viewModel.onNewUserRoleChange("user")
                                            roleExpanded = false
                                        }
                                )
                                DropdownMenuItem(
                                        text = { Text("Admin") },
                                        onClick = {
                                            viewModel.onNewUserRoleChange("admin")
                                            roleExpanded = false
                                        }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::submitCreateUser) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setCreateUserModalVisible(false) }) {
                        Text("Cancel")
                    }
                },
        )
    }

    if (state.showDeleteSongModal) {
        ModalBottomSheet(onDismissRequest = { viewModel.setDeleteSongModalVisible(false) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).height(500.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Delete songs", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.setDeleteSongModalVisible(false) }) {
                        Text("Close")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                        value = state.songQuery,
                        onValueChange = viewModel::onSongQueryChange,
                        placeholder = { Text("Search songs...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.songResults, key = { it.id }) { music ->
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(music.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                        music.artist.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.setConfirmDeleteSong(music) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferencesSection(
        themePreference: ThemePreference,
        language: String,
        viewModel: SettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Preferences", style = MaterialTheme.typography.titleMedium)
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var themeExpanded by remember { mutableStateOf(false) }
            Column {
                Text(
                        "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(onClick = { themeExpanded = true }) {
                        Text(
                                when (themePreference) {
                                    ThemePreference.SYSTEM -> "System"
                                    ThemePreference.LIGHT -> "Light"
                                    ThemePreference.DARK -> "Dark"
                                }
                        )
                    }
                    DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                    ) {
                        DropdownMenuItem(
                                text = { Text("System") },
                                onClick = {
                                    viewModel.setTheme(ThemePreference.SYSTEM)
                                    themeExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Light") },
                                onClick = {
                                    viewModel.setTheme(ThemePreference.LIGHT)
                                    themeExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Dark") },
                                onClick = {
                                    viewModel.setTheme(ThemePreference.DARK)
                                    themeExpanded = false
                                }
                        )
                    }
                }
            }

            var langExpanded by remember { mutableStateOf(false) }
            Column {
                Text(
                        "Language",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(onClick = { langExpanded = true }) {
                        Text(if (language == "fr") "French" else "English")
                    }
                    DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                    ) {
                        DropdownMenuItem(
                                text = { Text("English") },
                                onClick = {
                                    viewModel.setLanguage("en")
                                    langExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("French") },
                                onClick = {
                                    viewModel.setLanguage("fr")
                                    langExpanded = false
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangePasswordSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Change password", style = MaterialTheme.typography.titleMedium)
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.minPasswordLength > 0) {
                Text(
                        "New password must be at least ${state.minPasswordLength} characters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                    value = state.currentPassword,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    placeholder = { Text("Current password") },
                    singleLine = true,
                    visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    placeholder = { Text("New password") },
                    singleLine = true,
                    visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    placeholder = { Text("Confirm new password") },
                    singleLine = true,
                    visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
            )
            state.passwordError?.let {
                Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(onClick = viewModel::submitPasswordChange) { Text("Update password") }
        }
    }
}

@Composable
private fun UsersSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Users", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { viewModel.setCreateUserModalVisible(true) }) { Text("New user") }
        }
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp),
        ) {
            state.users.forEach { user ->
                Row(
                        modifier =
                                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.email, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                                user.role + if (user.mustResetPassword) " · Pending reset" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { viewModel.setConfirmResetUser(user) }) {
                        Text("Reset password")
                    }
                    TextButton(onClick = { viewModel.setConfirmDeleteUser(user) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryManagementSection(viewModel: SettingsViewModel) {
    val consumeProgress by viewModel.libraryJobsRepository.consumeProgress.collectAsState()
    val resyncProgress by viewModel.libraryJobsRepository.resyncProgress.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Library management", style = MaterialTheme.typography.titleMedium)
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                    onClick = viewModel.libraryJobsRepository::startConsume,
                    enabled = !consumeProgress.running,
            ) { Text(if (consumeProgress.running) "Ingesting..." else "Ingest consume folder") }

            Column {
                OutlinedButton(
                        onClick = viewModel.libraryJobsRepository::startResync,
                        enabled = !resyncProgress.running,
                ) { Text(if (resyncProgress.running) "Resyncing..." else "Resync library") }
                if (resyncProgress.running) {
                    Spacer(Modifier.height(4.dp))
                    val progress =
                            if (resyncProgress.total > 0)
                                    resyncProgress.processed.toFloat() / resyncProgress.total
                            else 0f
                    LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                            "${resyncProgress.processed} / ${resyncProgress.total} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(onClick = { viewModel.setDeleteSongModalVisible(true) }) {
                Text("Delete songs")
            }
        }
    }
}
