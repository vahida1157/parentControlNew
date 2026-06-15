package com.vahak.mehrban

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember // 🚀 Added
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by mainViewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
            val startDestination by mainViewModel.startDestination.collectAsState()
            val activeChildId by mainViewModel.activeChildId.collectAsState()
            val downloadedFilePath by mainViewModel.downloadedFilePath.collectAsState()

            val appLanguage by mainViewModel.appLanguage.collectAsState()
            val navController = rememberNavController()

            AppSignatureHelper(applicationContext).getAppSignatures()

            LaunchedEffect(downloadedFilePath) {
                downloadedFilePath?.let { path ->
                    installApk(this@MainActivity, path)
                    mainViewModel.clearDownloadedFilePath()
                }
            }

            // 1. Create the localized configuration
            val locale = Locale(appLanguage)
            val configuration = android.content.res.Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }

            // 🚀 THE FIX: Wrap the Activity Context!
            // This forces Compose to use localized strings, but allows Hilt to still find the Activity.
            val localizedContext = remember(appLanguage) {
                object : android.content.ContextWrapper(this@MainActivity) {
                    val configContext = createConfigurationContext(configuration)
                    override fun getResources() = configContext.resources
                }
            }

            val layoutDirection = if (appLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

            // 3. Inject it into Compose Globally
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides layoutDirection
            ) {
                ParentControlTheme(themeMode = currentTheme) {
                    if (startDestination == null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        )
                    } else if (activeChildId != null) {
                        ChildLauncherScreen(
                            onExitLauncherClick = {
                                LauncherManager.disableLauncherMode(this@MainActivity)
                                val stopIntent = Intent(this@MainActivity, RestrictionEnforcerService::class.java).apply {
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
    }

    private fun installApk(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}