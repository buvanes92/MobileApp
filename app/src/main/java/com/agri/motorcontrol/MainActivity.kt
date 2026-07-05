package com.agri.motorcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agri.motorcontrol.ui.TelemetryViewModel
import com.agri.motorcontrol.ui.screens.AnalyticsScreen
import com.agri.motorcontrol.ui.screens.DashboardScreen
import com.agri.motorcontrol.ui.screens.LoginScreen
import com.agri.motorcontrol.ui.screens.SchedulerScreen
import com.agri.motorcontrol.ui.screens.SimulatorPanel
import com.agri.motorcontrol.ui.theme.AgriMotorControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgriMotorControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TelemetryViewModel = viewModel()
                    MainLayout(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: TelemetryViewModel) {
    val navController = rememberNavController()
    var isSimulatorExpanded by remember { mutableStateOf(false) }
    val currentBackstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackstackEntry?.destination?.route ?: "login"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (currentRoute != "login") {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "dashboard",
                            onClick = {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "scheduler",
                            onClick = {
                                navController.navigate("scheduler") {
                                    popUpTo("dashboard")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
                            label = { Text("Schedule", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == "analytics",
                            onClick = {
                                navController.navigate("analytics") {
                                    popUpTo("dashboard")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(Icons.Default.List, contentDescription = "Analytics") },
                            label = { Text("Analytics", fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "login",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("dashboard") {
                    DashboardScreen(viewModel)
                }
                composable("scheduler") {
                    SchedulerScreen(viewModel)
                }
                composable("analytics") {
                    AnalyticsScreen(viewModel)
                }
            }
        }

        // Overlay Hardware Simulator Panel (Hidden on login screen)
        if (currentRoute != "login") {
            SimulatorPanel(
                viewModel = viewModel,
                isExpanded = isSimulatorExpanded,
                onToggleExpand = { isSimulatorExpanded = !isSimulatorExpanded },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
