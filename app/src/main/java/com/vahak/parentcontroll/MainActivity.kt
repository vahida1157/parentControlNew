package com.vahak.parentcontroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (startDestination == null) {
                        // Optional: You can put a loading spinner or Splash screen Box here
                        Box(modifier = Modifier.fillMaxSize())
                    } else {
                        ParentControlNavGraph(
                            startDestination = startDestination!!,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}