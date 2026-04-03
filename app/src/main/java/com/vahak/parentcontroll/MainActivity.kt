package com.vahak.parentcontroll

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
import com.vahak.parentcontroll.core.util.LauncherManager
import com.vahak.parentcontroll.ui.navigation.ParentControlNavGraph
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
                        onDisableLauncherRequested = {
                            // 1. Kill the Launcher Alias
                            LauncherManager.disableLauncherMode(this)

                            // 2. Tell ViewModel to update the startDestination state
                            mainViewModel.refreshDestination()

                            // 3. Optional: If you want to drop them on the phone's
                            // real home screen instead of the app dashboard:
                            // finish()
                        }
                    )
                }
            }
        }
    }
}