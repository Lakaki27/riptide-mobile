package com.evertide.riptide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.evertide.riptide.data.local.ThemePreference
import com.evertide.riptide.data.local.ThemePreferenceStore
import com.evertide.riptide.navigation.RiptideNavHost
import com.evertide.riptide.ui.RootUiState
import com.evertide.riptide.ui.RootViewModel
import com.evertide.riptide.ui.common.ToastBusHolder
import com.evertide.riptide.ui.theme.RiptideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferenceStore: ThemePreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepSplashOn = true
        splashScreen.setKeepOnScreenCondition { keepSplashOn }

        setContent {
            val themePref by
                    themePreferenceStore.theme.collectAsState(initial = ThemePreference.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme =
                    when (themePref) {
                        ThemePreference.SYSTEM -> systemDark
                        ThemePreference.LIGHT -> false
                        ThemePreference.DARK -> true
                    }

            // Dynamically adjust status bar icon colors based on current theme state
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as android.app.Activity).window
                    androidx.core.view.WindowCompat.getInsetsController(window, view)
                            .isAppearanceLightStatusBars = !darkTheme
                }
            }

            RequestNotificationPermission()

            RiptideTheme(themePreference = themePref) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootContent(onReady = { keepSplashOn = false })
                }
            }
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                    ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
            if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun RootContent(
        onReady: () -> Unit,
        viewModel: RootViewModel = hiltViewModel(),
        toastBusHolder: ToastBusHolder = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    when (val current = state) {
        is RootUiState.Loading -> Unit // splash screen stays visible via keepSplashOn
        is RootUiState.Ready -> {
            LaunchedEffect(Unit) { onReady() }
            RiptideNavHost(
                    startDestination = current.startDestination,
                    toastBus = toastBusHolder.bus
            )
        }
    }
}
