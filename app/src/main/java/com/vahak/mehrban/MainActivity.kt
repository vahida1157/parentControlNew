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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.service.RestrictionEnforcerService
import com.vahak.mehrban.core.service.SessionSyncEngine
import com.vahak.mehrban.core.util.AppSignatureHelper
import com.vahak.mehrban.core.util.LauncherManager
import com.vahak.mehrban.uiv2.components.UpdateCheckerWrapper
import com.vahak.mehrban.uiv2.navigation.ParentControlNavGraph
import com.vahak.mehrban.uiv2.screens.browser.safebrowser.SafeBrowserActivity
import com.vahak.mehrban.uiv2.screens.launcher.ChildLauncherScreen
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import com.vahak.mehrban.worker.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var sessionSyncEngine: SessionSyncEngine

    @Inject
    lateinit var analytics: AppAnalytics

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("destination")?.let { route ->
            analytics.logInactivityNotificationClicked(route)
            mainViewModel.onEvent(MainEvent.SetPendingRoute(route))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationScheduler.cancelTimer(applicationContext)
        sessionSyncEngine.start()
        analytics.setInstallSource(BuildConfig.INSTALL_SOURCE)
        analytics.setAnalyticsCollectionEnabled(BuildConfig.ENABLE_ANALYTICS)

        intent?.getStringExtra("destination")?.let { route ->
            mainViewModel.onEvent(MainEvent.SetPendingRoute(route))
        }

        setContent {
            // 🚀 ONE SINGLE STATE OBJECT
            val state by mainViewModel.state.collectAsState()
            val navController = rememberNavController()

            AppSignatureHelper(applicationContext).getAppSignatures()

            // Handle APK downloads
            LaunchedEffect(state.downloadedFilePath) {
                state.downloadedFilePath?.let { path ->
                    installApk(this@MainActivity, path)
                    mainViewModel.onEvent(MainEvent.ClearDownloadedFilePath)
                }
            }

            // Localization setup
            val locale = Locale.Builder().setLanguage(state.language).build()
            val configuration =
                android.content.res.Configuration(LocalConfiguration.current).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }

            val localizedContext = remember(state.language) {
                object : android.content.ContextWrapper(this@MainActivity) {
                    val configContext = createConfigurationContext(configuration)
                    override fun getResources() = configContext.resources
                }
            }

            val layoutDirection =
                if (state.language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides layoutDirection
            ) {
                ParentControlTheme(themeMode = state.theme) {

                    if (state.isInitializing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    } else {

                        // 🚀 ROUTING: Child Mode vs Parent Mode
                        if (state.activeChildId != null) {
                            LaunchedEffect(Unit) { analytics.logScreenView("child_launcher") }

                            ChildLauncherScreen(
                                onExitLauncherClick = {
                                    LauncherManager.disableLauncherMode(this@MainActivity)
                                    startService(
                                        Intent(
                                            this@MainActivity,
                                            RestrictionEnforcerService::class.java
                                        ).apply { action = RestrictionEnforcerService.ACTION_STOP })
                                    startActivity(Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(
                                            Intent.CATEGORY_HOME
                                        ); flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })

                                    mainViewModel.onEvent(MainEvent.ClearActiveLauncherSession)
                                },
                                onOpenBrowserClick = { SafeBrowserActivity.start(this@MainActivity) })
                        } else {
                            // 🚀 THE FIX PART 2: Tighter Lifecycle constraints (Resume/Pause)
                            val lifecycleOwner = LocalLifecycleOwner.current
                            val appContext = applicationContext

                            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                        // App is fully on screen
                                        NotificationScheduler.cancelTimer(appContext)
                                    } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                                        // App is leaving the screen (Home button pressed, Screen locked, etc.)
                                        NotificationScheduler.resetInactivityTimer(appContext)
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }

                            UpdateCheckerWrapper(viewModel = mainViewModel) {

                                // Handle Deep Links safely
                                LaunchedEffect(state.pendingRoute) {
                                    state.pendingRoute?.let { route ->
                                        navController.navigate(route) { launchSingleTop = true }
                                        mainViewModel.onEvent(MainEvent.ConsumePendingRoute)
                                    }
                                }

                                ParentControlNavGraph(
                                    startDestination = state.startDestination,
                                    modifier = Modifier.fillMaxSize(),
                                    navController = navController,
                                    analytics = analytics,
                                )
                            }
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