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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.R
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
        item {
            Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.titleLarge)
        }

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
            ) { Text(stringResource(R.string.logout)) }
        }
    }

    state.credentialModal?.let { (email, password) ->
        AlertDialog(
                onDismissRequest = viewModel::dismissCredentialModal,
                title = { Text(stringResource(R.string.account_credentials)) },
                text = {
                    Column {
                        Text(stringResource(R.string.account_credentials_text))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.email_info, email))
                        Text(stringResource(R.string.password_info, password))
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissCredentialModal) {
                        Text(stringResource(R.string.done))
                    }
                },
        )
    }

    state.confirmResetUser?.let { user ->
        AlertDialog(
                onDismissRequest = { viewModel.setConfirmResetUser(null) },
                title = { Text(stringResource(R.string.reset_password_for_q, user.email)) },
                text = { Text(stringResource(R.string.reset_password_for_q_text)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmResetUserPassword) {
                        Text(stringResource(R.string.reset))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setConfirmResetUser(null) }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
        )
    }

    state.confirmDeleteUser?.let { user ->
        AlertDialog(
                onDismissRequest = { viewModel.setConfirmDeleteUser(null) },
                title = { Text(stringResource(R.string.delete_email_q, user.email)) },
                text = { Text(stringResource(R.string.delete_email_q_text)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteUserAction) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setConfirmDeleteUser(null) }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
        )
    }

    state.confirmDeleteSong?.let { music ->
        AlertDialog(
                onDismissRequest = { viewModel.setConfirmDeleteSong(null) },
                title = { Text(stringResource(R.string.delete_song_q, music.title)) },
                text = { Text(stringResource(R.string.delete_song_q_text)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDeleteSongAction) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setConfirmDeleteSong(null) }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
        )
    }

    if (state.showCreateUserModal) {
        AlertDialog(
                onDismissRequest = { viewModel.setCreateUserModalVisible(false) },
                title = { Text(stringResource(R.string.new_user)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                                value = state.newUserEmail,
                                onValueChange = viewModel::onNewUserEmailChange,
                                placeholder = { Text(stringResource(R.string.email)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                        )
                        var roleExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { roleExpanded = true }) {
                                Text(
                                        if (state.newUserRole == "admin")
                                                stringResource(R.string.role_admin)
                                        else stringResource(R.string.role_user)
                                )
                            }
                            DropdownMenu(
                                    expanded = roleExpanded,
                                    onDismissRequest = { roleExpanded = false }
                            ) {
                                DropdownMenuItem(
                                        text = { Text(stringResource(R.string.role_user)) },
                                        onClick = {
                                            viewModel.onNewUserRoleChange("user")
                                            roleExpanded = false
                                        }
                                )
                                DropdownMenuItem(
                                        text = { Text(stringResource(R.string.role_admin)) },
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
                    TextButton(onClick = viewModel::submitCreateUser) {
                        Text(stringResource(R.string.create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setCreateUserModalVisible(false) }) {
                        Text(stringResource(R.string.cancel))
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
                    Text(
                            stringResource(R.string.delete_songs),
                            style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { viewModel.setDeleteSongModalVisible(false) }) {
                        Text(stringResource(R.string.close))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                        value = state.songQuery,
                        onValueChange = viewModel::onSongQueryChange,
                        placeholder = { Text(stringResource(R.string.search_songs)) },
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
                                Text(stringResource(R.string.delete))
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
        Text(stringResource(R.string.preferences), style = MaterialTheme.typography.titleMedium)
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
                        stringResource(R.string.theme),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(onClick = { themeExpanded = true }) {
                        Text(
                                when (themePreference) {
                                    ThemePreference.SYSTEM -> stringResource(R.string.theme_system)
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
                                text = { Text(stringResource(R.string.theme_system)) },
                                onClick = {
                                    viewModel.setTheme(ThemePreference.SYSTEM)
                                    themeExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_light)) },
                                onClick = {
                                    viewModel.setTheme(ThemePreference.LIGHT)
                                    themeExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.theme_dark)) },
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
                        stringResource(R.string.language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    OutlinedButton(onClick = { langExpanded = true }) {
                        Text(
                                if (language == "fr") stringResource(R.string.language_fr)
                                else stringResource(R.string.language_en)
                        )
                    }
                    DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                    ) {
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_en)) },
                                onClick = {
                                    viewModel.setLanguage("en")
                                    langExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.language_fr)) },
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
        Text(stringResource(R.string.change_password), style = MaterialTheme.typography.titleMedium)
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
                        stringResource(
                                R.string.password_length_requirement,
                                state.minPasswordLength
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                    value = state.currentPassword,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    placeholder = { Text(stringResource(R.string.current_password)) },
                    singleLine = true,
                    visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    placeholder = { Text(stringResource(R.string.new_password)) },
                    singleLine = true,
                    visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    placeholder = { Text(stringResource(R.string.new_password_confirm)) },
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
            Button(onClick = viewModel::submitPasswordChange) {
                Text(stringResource(R.string.update_password))
            }
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
            Text(stringResource(R.string.users), style = MaterialTheme.typography.titleMedium)
            Button(onClick = { viewModel.setCreateUserModalVisible(true) }) {
                Text(stringResource(R.string.new_user))
            }
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
                                user.role +
                                        if (user.mustResetPassword)
                                                " · " + stringResource(R.string.pending_reset)
                                        else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { viewModel.setConfirmResetUser(user) }) {
                        Text(stringResource(R.string.reset_password))
                    }
                    TextButton(onClick = { viewModel.setConfirmDeleteUser(user) }) {
                        Text(
                                stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error
                        )
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
        Text(
                stringResource(R.string.library_management),
                style = MaterialTheme.typography.titleMedium
        )
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
            ) {
                Text(
                        if (consumeProgress.running) stringResource(R.string.ingesting)
                        else stringResource(R.string.ingest_consume_folder)
                )
            }

            Column {
                OutlinedButton(
                        onClick = viewModel.libraryJobsRepository::startResync,
                        enabled = !resyncProgress.running,
                ) {
                    Text(
                            if (resyncProgress.running) stringResource(R.string.resyncing)
                            else stringResource(R.string.resync_library)
                    )
                }
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
                            stringResource(R.string.song_progress_count, resyncProgress.processed, resyncProgress.total),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(onClick = { viewModel.setDeleteSongModalVisible(true) }) {
                Text(stringResource(R.string.delete_songs))
            }
        }
    }
}
