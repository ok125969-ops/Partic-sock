package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.UltronDatabase
import com.example.data.UltronRepository
import com.example.ui.screens.GatewayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.TeacherScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = UltronDatabase.getDatabase(applicationContext)
        val repository = UltronRepository(database.ultronDao())

        setContent {
            MyApplicationTheme {
                UltronMainApp(repository = repository)
            }
        }
    }
}

@Composable
fun UltronMainApp(repository: UltronRepository) {
    val navController = rememberNavController()

    val items = listOf(
        Triple("home", "Particle OS", Icons.Filled.BlurOn),
        Triple("teacher", "Teacher Mode", Icons.Filled.School),
        Triple("gateway", "OS Gateway", Icons.Filled.PhoneAndroid),
        Triple("memory", "Memory", Icons.Filled.Memory)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != "home") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(if (currentRoute == "home") androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    repository = repository,
                    onNavigateToTeacher = { navController.navigate("teacher") }
                )
            }
            composable("teacher") { TeacherScreen(repository) }
            composable("gateway") { GatewayScreen(repository) }
            composable("memory") { MemoryScreen(repository) }
        }
    }
}
