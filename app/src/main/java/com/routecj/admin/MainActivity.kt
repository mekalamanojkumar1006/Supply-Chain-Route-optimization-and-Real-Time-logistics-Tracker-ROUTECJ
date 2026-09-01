package com.routecj.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.routecj.admin.presentation.navigation.NavGraph
import com.routecj.admin.ui.theme.RouteCJAdminTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.routecj.admin.presentation.MainViewModel

import com.routecj.admin.core.security.SessionManager
import javax.inject.Inject

/**
 * MainActivity - Entry point of the application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RouteCJAdminTheme {
                MainContent(viewModel, sessionManager)
            }
        }
    }
}

/**
 * Main content composable.
 * Sets up the navigation graph and basic UI structure.
 */
@Composable
private fun MainContent(viewModel: MainViewModel, sessionManager: SessionManager) {
    val navController = rememberNavController()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavGraph(navController = navController, sessionManager = sessionManager)
    }
}
