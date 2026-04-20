package com.android.multitimerpro

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.android.multitimerpro.data.GoogleAuthClient
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.navigation.MainNavigation
import com.android.multitimerpro.ui.theme.MultiTimerProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() { // Changed to AppCompatActivity for Locale support

    @Inject
    lateinit var googleAuthClient: GoogleAuthClient

    private val viewModel: TimerViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.showMessage(getString(R.string.msg_notif_granted))
        } else {
            viewModel.showMessage(getString(R.string.msg_notif_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Solicitar permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
            val darkTheme = isDarkModeOverride ?: isSystemInDarkTheme()

            MultiTimerProTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        viewModel = viewModel,
                        onGoogleSignIn = {
                            googleSignInLauncher.launch(googleAuthClient.getSignInIntent())
                        }
                    )
                }
            }
        }
    }
}
