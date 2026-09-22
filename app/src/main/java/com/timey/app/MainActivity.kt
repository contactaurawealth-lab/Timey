package com.timey.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.timey.app.core.ui.theme.TimeyTheme
import com.timey.app.features.timer.ui.TimerMainScreen
import com.timey.app.features.timer.viewmodel.TimerViewModel
import com.timey.app.features.timer.viewmodel.TimerViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TimerViewModel

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            TimerViewModelFactory(applicationContext)
        )[TimerViewModel::class.java]

        // Strict mode lifecycle monitoring
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppLeftForeground()
                Lifecycle.Event.ON_START -> viewModel.onAppReturnedToForeground()
                else -> Unit
            }
        })

        checkNotificationPermission()

        setContent {
            TimeyTheme {
                TimerMainScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
