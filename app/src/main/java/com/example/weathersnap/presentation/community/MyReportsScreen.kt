package com.example.weathersnap.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.presentation.common.FullScreenPhotoViewerDialog
import com.example.weathersnap.ui.theme.DarkGrey
import com.example.weathersnap.ui.theme.GreenPrimary
import com.example.weathersnap.ui.theme.SurfaceDark

@Composable
fun MyReportsScreen(
    onBack: () -> Unit,
    onNavigateToSavedReports: () -> Unit,
    viewModel: MyReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var reportToDelete by remember { mutableStateOf<CommunityReport?>(null) }
    var viewingPhotoModel by remember { mutableStateOf<Any?>(null) }
    var viewingPhotoTitle by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
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
                        text = "My Community Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Published by this device",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick jump to device-saved reports (Phase 5)
                IconButton(onClick = onNavigateToSavedReports) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = "Saved on This Device",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Body
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenPrimary)
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
                            text = "No Community Reports Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reports you publish from the Create Report screen will appear here and in the community feed.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
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
                            showDeleteAction = true,
                            onDeleteClick = { reportToDelete = report },
                            onPhotoClick = { model ->
                                viewingPhotoModel = model
                                viewingPhotoTitle = report.cityName
                            }
                        )
                    }
                }
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

    // Confirmation Dialog before deletion (Phase 4 requirement)
    reportToDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = {
                Text(
                    text = "Delete Report?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this report from the community feed? It will remain safely preserved in your local device history.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = report.id
                        reportToDelete = null
                        viewModel.deleteReport(id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reportToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceDark,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
}
