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
import com.vahak.parentcontroll.core.service.WebFilterVpnService
import com.vahak.parentcontroll.core.util.LauncherManager
import com.vahak.parentcontroll.ui.navigation.ParentControlNavGraph
import com.vahak.parentcontroll.ui.screens.launcher.ChildLauncherScreen // 🚀 ADD THIS IMPORT
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

            // 🚀 PRO FIX: Observe the Master Switch
            val activeChildId by mainViewModel.activeChildId.collectAsState()

            val navController = rememberNavController()

            com.vahak.parentcontroll.uiv2.theme.ParentControlTheme() {
                if (startDestination == null) {
                    // Splash / Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
                // 🚀 THE UI FORTRESS: If a child is active, ONLY draw the Launcher.
                else if (activeChildId != null) {
                    ChildLauncherScreen(
                        // If you have the child's name in the DB, you can fetch it here later
                        childName = "حالت کودک",
                        onExitLauncherClick = {
                            // 1. Flip the Master Switch (This instantly redraws the NavGraph!)
                            mainViewModel.clearActiveLauncherSession()

                            // 2. Disable OS Launcher
                            LauncherManager.disableLauncherMode(this@MainActivity)

                            // 3. Stop the Enforcer
                            val stopIntent = Intent(this@MainActivity, RestrictionEnforcerService::class.java).apply {
                                action = RestrictionEnforcerService.ACTION_STOP
                            }
                            startService(stopIntent)

                            // 4. Stop the VPN
                            val stopVpnIntent = Intent(this@MainActivity, WebFilterVpnService::class.java).apply {
                                action = WebFilterVpnService.ACTION_STOP
                            }
                            startService(stopVpnIntent)
                        }
                    )
                }
                // 🚀 NORMAL MODE: Draw the Parent Dashboard NavGraph
                else {
                    ParentControlNavGraph(
                        startDestination = startDestination!!,
                        modifier = Modifier.fillMaxSize(),
                        navController = navController,
                        // We pass an empty lambda since the NavGraph doesn't handle exiting anymore
                        onDisableLauncherRequested = { }
                    )
                }
            }
        }
    }
}