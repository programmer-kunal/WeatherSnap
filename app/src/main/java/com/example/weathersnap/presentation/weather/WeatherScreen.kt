package com.example.weathersnap.presentation.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import com.example.weathersnap.ui.theme.GreenPrimary
import com.example.weathersnap.ui.theme.SurfaceDark
import com.example.weathersnap.ui.theme.DarkGrey
import com.example.weathersnap.ui.theme.HelperText
import com.example.weathersnap.domain.model.City
import com.example.weathersnap.domain.model.Weather

@Composable
fun WeatherScreen(
    onNavigateToCreateReport: (String, Double, String, Int, Double, Double, Int?) -> Unit = {_,_,_,_,_,_,_ -> },
    onNavigateToReports: (String, Int?) -> Unit = { _, _ -> },
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WeatherSnap",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = {
                    val city = uiState.selectedCity
                    onNavigateToReports(city?.name ?: "", city?.id)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(text = "Reports", color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Text(
            text = "Live weather reports with camera evidence",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search city", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            singleLine = true
        )
        
        if (uiState.searchQuery.length <= 2 && uiState.selectedCity == null) {
            Text(
                text = "Enter more than 2 letters to start city suggestions",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        AnimatedVisibility(visible = uiState.isSearching) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (uiState.searchError != null) {
            Text(
                text = uiState.searchError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        AnimatedVisibility(visible = uiState.citySuggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(uiState.citySuggestions) { city ->
                    Text(
                        text = city.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onCitySelected(city) }
                            .padding(16.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.background)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoadingWeather) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (uiState.weatherError != null) {
            Text(
                text = uiState.weatherError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        uiState.weather?.let { weather ->
            uiState.selectedCity?.let { city ->
                WeatherCard(
                    city = city,
                    weather = weather,
                    isDefaultLocation = uiState.isDefaultLocation,
                    onToggleDefaultLocation = { viewModel.onToggleDefaultCity(city) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    val city = uiState.selectedCity
                    onNavigateToCreateReport(
                        city?.displayName ?: "",
                        weather.temperature,
                        weather.conditionText,
                        weather.humidity,
                        weather.windSpeed,
                        weather.pressure,
                        city?.id
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Create Report", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeatherCard(
    city: City,
    weather: Weather,
    isDefaultLocation: Boolean = false,
    onToggleDefaultLocation: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Full city address & condition on the left
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = city.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weather.conditionText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Circular default-location icon next to the address
                IconButton(
                    onClick = onToggleDefaultLocation,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(36.dp)
                        .background(
                            color = if (isDefaultLocation) GreenPrimary else SurfaceDark,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDefaultLocation) GreenPrimary else HelperText.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isDefaultLocation) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
                        contentDescription = if (isDefaultLocation) "Remove default location" else "Make this the default location",
                        tint = if (isDefaultLocation) DarkGrey else GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. Temperature on the right
                Text(
                    text = String.format(Locale.getDefault(), "%.1f°C", weather.temperature),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    textAlign = TextAlign.End,
                    softWrap = false,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherInfoItem("Humidity", "${weather.humidity}%")
                WeatherInfoItem("Wind", "${weather.windSpeed} m/s")
                WeatherInfoItem("Pressure", "${weather.pressure} hPa")
            }
        }
    }
}

@Composable
fun WeatherInfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .widthIn(min = 80.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
