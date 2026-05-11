package com.example.weathersnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.weathersnap.presentation.weather.WeatherScreen
import com.example.weathersnap.presentation.create_report.CreateReportScreen
import com.example.weathersnap.presentation.camera.CameraScreen
import com.example.weathersnap.presentation.reports.SavedReportsScreen
import com.example.weathersnap.ui.theme.WeatherSnapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherSnapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "weather",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("weather") {
                            WeatherScreen(
                                onNavigateToCreateReport = { city, temp, condition, humidity, wind, pressure ->
                                    navController.navigate("create_report/$city/$temp/$condition/$humidity/$wind/$pressure")
                                },
                                onNavigateToReports = {
                                    navController.navigate("saved_reports")
                                }
                            )
                        }
                        
                        composable(
                            route = "create_report/{cityName}/{temp}/{condition}/{humidity}/{wind}/{pressure}",
                            arguments = listOf(
                                navArgument("cityName") { type = NavType.StringType },
                                navArgument("temp") { type = NavType.StringType },
                                navArgument("condition") { type = NavType.StringType },
                                navArgument("humidity") { type = NavType.StringType },
                                navArgument("wind") { type = NavType.StringType },
                                navArgument("pressure") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val cityName = backStackEntry.arguments?.getString("cityName") ?: ""
                            val temp = backStackEntry.arguments?.getString("temp")?.toDoubleOrNull() ?: 0.0
                            val condition = backStackEntry.arguments?.getString("condition") ?: ""
                            val humidity = backStackEntry.arguments?.getString("humidity")?.toIntOrNull() ?: 0
                            val wind = backStackEntry.arguments?.getString("wind")?.toDoubleOrNull() ?: 0.0
                            val pressure = backStackEntry.arguments?.getString("pressure")?.toDoubleOrNull() ?: 0.0
                            
                            val savedStateHandle = backStackEntry.savedStateHandle
                            val capturedImagePath by savedStateHandle.getStateFlow<String?>("captured_image_path", null).collectAsState()

                            CreateReportScreen(
                                cityName = cityName,
                                temperature = temp,
                                condition = condition,
                                humidity = humidity,
                                windSpeed = wind,
                                pressure = pressure,
                                capturedImagePath = capturedImagePath,
                                onNavigateToCamera = { navController.navigate("camera_screen") },
                                onNavigateToSavedReports = { 
                                    navController.navigate("saved_reports") {
                                        popUpTo("weather") { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable("camera_screen") {
                            CameraScreen(
                                cacheDir = applicationContext.cacheDir,
                                onPhotoCaptured = { path ->
                                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_image_path", path)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("saved_reports") {
                            SavedReportsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}