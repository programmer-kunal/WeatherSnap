package com.example.weathersnap.presentation.community

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.presentation.common.FullScreenPhotoViewerDialog
import com.example.weathersnap.ui.theme.DarkGrey
import com.example.weathersnap.ui.theme.GreenPrimary
import com.example.weathersnap.ui.theme.HelperText
import com.example.weathersnap.ui.theme.SurfaceDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunityReportsScreen(
    onBack: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToSavedReports: () -> Unit = {},
    viewModel: CommunityReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewingPhotoModel by remember { mutableStateOf<Any?>(null) }
    var viewingPhotoTitle by remember { mutableStateOf<String?>(null) }

    val quickFilterCities = listOf("All", "Ghaziabad", "Jaipur", "Delhi", "Mumbai", "Bengaluru", "London")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Community Reports",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "Live crowd weather & field notes",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Action: My Reports button
            Button(
                onClick = onNavigateToMyReports,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = DarkGrey
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.myReportsCount > 0) "My Reports (${uiState.myReportsCount})" else "My Reports",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Search & Filter Input Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = {
                Text(
                    text = "Search city reports (e.g. Ghaziabad, Jaipur)...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty() || uiState.selectedCityFilter.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearFilter) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = SurfaceDark
            )
        )

        // Horizontal Quick-Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickFilterCities.forEach { city ->
                val isSelected = if (city.equals("All", ignoreCase = true)) {
                    uiState.selectedCityFilter.isBlank()
                } else {
                    uiState.selectedCityFilter.contains(city, ignoreCase = true)
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onCityFilterSelected(city) },
                    label = {
                        Text(
                            text = city,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceDark,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = GreenPrimary,
                        selectedLabelColor = DarkGrey
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) GreenPrimary else HelperText.copy(alpha = 0.3f),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        // Active Filter Banner
        if (uiState.selectedCityFilter.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Filtered by: \"${uiState.selectedCityFilter}\"",
                    color = GreenPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Clear filter",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { viewModel.clearFilter() }
                )
            }
        }

        // Feed Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to Load Community Reports",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    val safeError = uiState.errorMessage?.let { msg ->
                        if (msg.contains("Bearer", ignoreCase = true) ||
                            msg.contains("http", ignoreCase = true) ||
                            msg.contains("Authorization", ignoreCase = true) ||
                            msg.contains("failed to parse", ignoreCase = true) ||
                            msg.length > 120
                        ) {
                            "Please check your network connection and try again."
                        } else {
                            msg
                        }
                    } ?: "Please check your network connection and try again."

                    Text(
                        text = safeError,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = viewModel::retry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = DarkGrey
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (uiState.reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiState.selectedCityFilter.isNotBlank()) {
                            "No community reports found for \"${uiState.selectedCityFilter}\""
                        } else {
                            "No community reports available yet"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Be the first to publish a report for this city from the home screen, or clear filter to view other cities.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (uiState.selectedCityFilter.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = viewModel::clearFilter,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GreenPrimary)
                        ) {
                            Text("Show All Cities")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.reports, key = { it.id }) { report ->
                    CommunityReportCard(
                        report = report,
                        onPhotoClick = { model ->
                            viewingPhotoModel = model
                            viewingPhotoTitle = report.cityName
                        }
                    )
                }
            }
        }

        if (viewingPhotoModel != null) {
            FullScreenPhotoViewerDialog(
                imageModel = viewingPhotoModel,
                onDismiss = { viewingPhotoModel = null },
                title = viewingPhotoTitle
            )
        }
    }
}

@Composable
fun CommunityReportCard(
    report: CommunityReport,
    showDeleteAction: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    onPhotoClick: ((Any) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: City Name, Date & Temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = report.cityName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(report.timestamp)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "%.1f°C", report.temperature),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    textAlign = TextAlign.End,
                    softWrap = false,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Author & Origin Distinction Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (report.isLocalUser) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = GreenPrimary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "YOUR REPORT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGrey
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                color = DarkGrey,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = HelperText.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "COMMUNITY REPORT • ${report.authorName}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = HelperText
                        )
                    }
                }

                Text(
                    text = report.condition,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // User Field Notes
            if (report.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = report.notes,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Photograph Section (Remote URL, local captured file, or demo drawable)
            val imageModel: Any? = report.imageUrl
                ?: report.imagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file else null
                }
                ?: report.drawableResId

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkGrey)
                    .then(
                        if (imageModel != null && onPhotoClick != null) {
                            Modifier.clickable { onPhotoClick(imageModel) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Report Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = GreenPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Photo unavailable",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = "No photograph attached",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meteorological Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn("Humidity", "${report.humidity}%")
                MetricColumn("Wind", "${report.windSpeed} m/s")
                MetricColumn("Pressure", "${report.pressure} hPa")

                if (showDeleteAction && onDeleteClick != null) {
                    TextButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
