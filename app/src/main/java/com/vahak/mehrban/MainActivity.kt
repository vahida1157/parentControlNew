package com.vahak.mehrban

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.navigation.compose.rememberNavController
import com.vahak.mehrban.core.service.RestrictionEnforcerService
import com.vahak.mehrban.core.util.AppSignatureHelper
import com.vahak.mehrban.core.util.LauncherManager
import com.vahak.mehrban.uiv2.components.UpdateCheckerWrapper
import com.vahak.mehrban.uiv2.navigation.ParentControlNavGraph
import com.vahak.mehrban.uiv2.screens.launcher.ChildLauncherScreen
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by mainViewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
            val startDestination by mainViewModel.startDestination.collectAsState()
            val activeChildId by mainViewModel.activeChildId.collectAsState()

            // 🚀 Observe the finished download
            val downloadedFilePath by mainViewModel.downloadedFilePath.collectAsState()

            val navController = rememberNavController()

            AppSignatureHelper(applicationContext).getAppSignatures()

            // 🚀 TRIGGER THE INSTALLER WHEN DOWNLOAD FINISHES
            LaunchedEffect(downloadedFilePath) {
                downloadedFilePath?.let { path ->
                    installApk(this@MainActivity, path)
                    mainViewModel.clearDownloadedFilePath() // Reset state so it doesn't loop
                }
            }

            ParentControlTheme(themeMode = currentTheme) {
                if (startDestination == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else if (activeChildId != null) {
                    // CHILD MODE: No update dialogs here!
                    ChildLauncherScreen(
                        onExitLauncherClick = {
                            LauncherManager.disableLauncherMode(this@MainActivity)
                            val stopIntent = Intent(
                                this@MainActivity,
                                RestrictionEnforcerService::class.java
                            ).apply {
                                action = RestrictionEnforcerService.ACTION_STOP
                            }
                            startService(stopIntent)
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            mainViewModel.clearActiveLauncherSession()
                        }
                    )
                } else {
                    // 🚀 PARENT MODE: Wrap the Parent Graph in the Update Checker
                    UpdateCheckerWrapper(viewModel = mainViewModel) {
                        ParentControlNavGraph(
                            startDestination = startDestination!!,
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            onDisableLauncherRequested = { }
                        )
                    }
                }
            }
        }
    }

    // --- STEP 6: THE INSTALLER FUNCTION ---
    private fun installApk(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        // Create a secure content:// URI using FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            // GRANT_READ_URI_PERMISSION is required so the OS Installer can read our private file
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }
}