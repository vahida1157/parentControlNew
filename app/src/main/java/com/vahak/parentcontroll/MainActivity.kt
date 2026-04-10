package com.vahak.parentcontroll

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vahak.parentcontroll.core.service.RestrictionEnforcerService
import com.vahak.parentcontroll.core.util.LauncherManager
import com.vahak.parentcontroll.ui.navigation.ParentControlNavGraph
import com.vahak.parentcontroll.ui.navigation.Screen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val startDestination by mainViewModel.startDestination.collectAsState()

            // 1. CREATE THE NAV CONTROLLER HERE
            val navController = rememberNavController()

            ParentControlTheme {
                if (startDestination == null) {
                    // Splash / Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    // We don't use Scaffold padding for the NavGraph to ensure
                    // the Launcher background draws edge-to-edge.
                    ParentControlNavGraph(
                        startDestination = startDestination!!,
                        modifier = Modifier.fillMaxSize(),
                        navController = navController, // NOW THIS WORKS
                        onDisableLauncherRequested = {
                            mainViewModel.clearActiveLauncherSession()

                            LauncherManager.disableLauncherMode(this)

                            val stopIntent =
                                Intent(this, RestrictionEnforcerService::class.java).apply {
                                    action = RestrictionEnforcerService.ACTION_STOP
                                }
                            startService(stopIntent)

                            mainViewModel.refreshDestination()

                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}