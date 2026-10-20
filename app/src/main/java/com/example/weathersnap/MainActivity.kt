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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.weathersnap.presentation.weather.WeatherScreen
import com.example.weathersnap.presentation.create_report.CreateReportScreen
import com.example.weathersnap.presentation.camera.CameraScreen
import com.example.weathersnap.presentation.community.CommunityReportsScreen
import com.example.weathersnap.presentation.community.MyReportsScreen
import com.example.weathersnap.presentation.reports.SavedReportsScreen
import com.example.weathersnap.presentation.splash.SplashScreen
import com.example.weathersnap.ui.theme.WeatherSnapTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

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
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onTimeout = {
                                    navController.navigate("weather") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("weather") {
                            WeatherScreen(
                                onNavigateToCreateReport = { city, temp, condition, humidity, wind, pressure, cityId ->
                                    val safeCity = try {
                                        URLEncoder.encode(city, "UTF-8")
                                    } catch (e: Exception) {
                                        city
                                    }
                                    val safeCondition = try {
                                        URLEncoder.encode(condition, "UTF-8")
                                    } catch (e: Exception) {
                                        condition
                                    }
                                    val idParam = cityId ?: 0
                                    navController.navigate("create_report/$safeCity/$temp/$safeCondition/$humidity/$wind/$pressure?cityId=$idParam")
                                },
                                onNavigateToReports = { city, cityId ->
                                    val safeCity = if (city.isNotBlank()) {
                                        try {
                                            URLEncoder.encode(city, "UTF-8")
                                        } catch (e: Exception) {
                                            city
                                        }
                                    } else ""
                                    val idParam = cityId ?: 0
                                    navController.navigate("community_reports?city=$safeCity&cityId=$idParam")
                                }
                            )
                        }
                        
                        composable(
                            route = "create_report/{cityName}/{temp}/{condition}/{humidity}/{wind}/{pressure}?cityId={cityId}",
                            arguments = listOf(
                                navArgument("cityName") { type = NavType.StringType },
                                navArgument("temp") { type = NavType.StringType },
                                navArgument("condition") { type = NavType.StringType },
                                navArgument("humidity") { type = NavType.StringType },
                                navArgument("wind") { type = NavType.StringType },
                                navArgument("pressure") { type = NavType.StringType },
                                navArgument("cityId") { type = NavType.IntType; defaultValue = 0 }
                            )
                        ) { backStackEntry ->
                            val rawCity = backStackEntry.arguments?.getString("cityName") ?: ""
                            val cityName = try {
                                URLDecoder.decode(rawCity, "UTF-8")
                            } catch (e: Exception) {
                                rawCity
                            }

                            val temp = backStackEntry.arguments?.getString("temp")?.toDoubleOrNull() ?: 0.0

                            val rawCondition = backStackEntry.arguments?.getString("condition") ?: ""
                            val condition = try {
                                URLDecoder.decode(rawCondition, "UTF-8")
                            } catch (e: Exception) {
                                rawCondition
                            }

                            val humidity = backStackEntry.arguments?.getString("humidity")?.toIntOrNull() ?: 0
                            val wind = backStackEntry.arguments?.getString("wind")?.toDoubleOrNull() ?: 0.0
                            val pressure = backStackEntry.arguments?.getString("pressure")?.toDoubleOrNull() ?: 0.0
                            val cityId = backStackEntry.arguments?.getInt("cityId") ?: 0
                            
                            val savedStateHandle = backStackEntry.savedStateHandle
                            val capturedImagePath by savedStateHandle.getStateFlow<String?>("captured_image_path", null).collectAsState()

                            CreateReportScreen(
                                cityName = cityName,
                                temperature = temp,
                                condition = condition,
                                humidity = humidity,
                                windSpeed = wind,
                                pressure = pressure,
                                cityId = cityId,
                                capturedImagePath = capturedImagePath,
                                onNavigateToCamera = { navController.navigate("camera_screen") },
                                onNavigateToCommunityReports = { publishedCity, publishedCityId ->
                                    val safeCity = try {
                                        URLEncoder.encode(publishedCity, "UTF-8")
                                    } catch (e: Exception) {
                                        publishedCity
                                    }
                                    val idArg = publishedCityId ?: cityId
                                    navController.navigate("community_reports?city=$safeCity&cityId=$idArg") {
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

                        composable(
                            route = "community_reports?city={city}&cityId={cityId}",
                            arguments = listOf(
                                navArgument("city") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("cityId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) {
                            CommunityReportsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToMyReports = { navController.navigate("my_reports") },
                                onNavigateToSavedReports = { navController.navigate("saved_reports") }
                            )
                        }

                        composable("my_reports") {
                            MyReportsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToSavedReports = { navController.navigate("saved_reports") }
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