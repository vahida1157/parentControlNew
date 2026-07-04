// SafeBrowserActivity.kt
package com.vahak.mehrban.uiv2.screens.browser.safebrowser

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.vahak.mehrban.MainViewModel
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class SafeBrowserActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentTheme by mainViewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
            val appLanguage by mainViewModel.appLanguage.collectAsState()

            val locale = Locale.Builder().setLanguage(appLanguage).build()
            val configuration = Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }

            val localizedContext = remember(appLanguage) {
                object : ContextWrapper(this@SafeBrowserActivity) {
                    val configContext = createConfigurationContext(configuration)
                    override fun getResources() = configContext.resources
                }
            }

            val layoutDirection = if (appLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides layoutDirection
            ) {
                ParentControlTheme(themeMode = currentTheme) {
                    SafeBrowserScreen(
                        onCloseClick = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SafeBrowserActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}