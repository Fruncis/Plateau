package com.plateau.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.plateau.app.ui.theme.PlateauTheme
import com.plateau.app.ui.home.HomeScreen
import com.plateau.app.ui.session.SessionScreen
import com.plateau.app.ui.plans.PlansScreen
import com.plateau.app.ui.discover.DiscoverScreen
import com.plateau.app.ui.progress.ProgressScreen
import com.plateau.app.ui.photo.PhotoScreen
import com.plateau.app.ui.navigation.Destination
import com.plateau.app.ui.home.HomeViewModel
import com.plateau.app.ui.session.SessionViewModel
import com.plateau.app.ui.plans.PlansViewModel
import com.plateau.app.ui.discover.DiscoverViewModel
import com.plateau.app.ui.progress.ProgressViewModel
import com.plateau.app.ui.photo.PhotoViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlateauTheme {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val currentDestination = remember { mutableStateOf(Destination.Home) }

    androidx.navigation.compose.NavHost(navController, "home") {
        composable("home") {
            HomeScreen(
                viewModel = hiltViewModel<HomeViewModel>(),
                currentDestination = currentDestination.value,
                onNavigate = { dest ->
                    currentDestination.value = dest
                    navController.navigate(dest.route) { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                }
            )
        }
        composable("session") {
            SessionScreen(
                viewModel = hiltViewModel<SessionViewModel>(),
                currentDestination = currentDestination.value,
                onNavigate = { dest ->
                    currentDestination.value = dest
                    navController.navigate(dest.route) { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                }
            )
        }
        composable("plans") {
            PlansScreen(
                viewModel = hiltViewModel<PlansViewModel>(),
                currentDestination = currentDestination.value,
                onNavigate = { dest ->
                    currentDestination.value = dest
                    navController.navigate(dest.route) { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                }
            )
        }
        composable("discover") {
            DiscoverScreen(
                viewModel = hiltViewModel<DiscoverViewModel>(),
                currentDestination = currentDestination.value,
                onNavigate = { dest ->
                    currentDestination.value = dest
                    navController.navigate(dest.route) { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                }
            )
        }
        composable("progress") {
            ProgressScreen(
                viewModel = hiltViewModel<ProgressViewModel>(),
                currentDestination = currentDestination.value,
                onNavigate = { dest ->
                    currentDestination.value = dest
                    navController.navigate(dest.route) { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                }
            )
        }
        composable("photo") {
            PhotoScreen(
                viewModel = hiltViewModel<PhotoViewModel>(),
                currentDestination = currentDestination.value,
                onNavigate = { dest ->
                    currentDestination.value = dest
                    navController.navigate(dest.route) { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                }
            )
        }
    }
}