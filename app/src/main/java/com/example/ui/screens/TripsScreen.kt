package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.R
import com.example.data.model.TripEntity
import com.example.ui.TripViewModel
import com.example.ui.components.DeleteBlockedDialog
import com.example.ui.components.DeleteTripConfirmationDialog
import com.example.ui.components.ExportTripDialog
import com.example.ui.components.ExtendTripDateDialog
import com.example.ui.components.TripCard

@Composable
fun TripsScreen(
    viewModel: TripViewModel,
    onOpenAddTripDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.allTrips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val settlementSummary by viewModel.settlementSummary.collectAsState()

    var tripPendingDelete by remember { mutableStateOf<TripEntity?>(null) }
    var tripPendingBlockedDelete by remember { mutableStateOf<TripEntity?>(null) }
    var tripToExtend by remember { mutableStateOf<TripEntity?>(null) }
    var tripToExport by remember { mutableStateOf<TripEntity?>(null) }
    var exportedCsvText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddTripDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_trip_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Trip")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("trips_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Art Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_trip_hero_1786454104534),
                            contentDescription = "Trip Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column {
                                Text(
                                    text = "Group Expense Toolkit",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                                Text(
                                    text = "Multi-currency splits & live debt settlement",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Trips (${trips.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = onOpenAddTripDialog,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "New Trip")
                    }
                }
            }

            // Trip Cards List
            if (trips.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No trips created yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap 'New Trip' to start tracking group expenses.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(trips, key = { it.id }) { trip ->
                    val isSelected = trip.id == selectedTripId
                    TripCard(
                        trip = trip,
                        isSelected = isSelected,
                        memberCount = 4,
                        totalSpendingBase = 0.0,
                        settlementSummary = settlementSummary,
                        onSelectTrip = { viewModel.selectTrip(trip.id) },
                        onExportTrip = {
                            viewModel.generateCsvExportForTrip(trip.id) { csv ->
                                exportedCsvText = csv
                                tripToExport = trip
                            }
                        },
                        onExtendTripDate = {
                            tripToExtend = trip
                        },
                        onDeleteTrip = {
                            if (!trip.isManuallySettled) {
                                tripPendingBlockedDelete = trip
                            } else {
                                tripPendingDelete = trip
                            }
                        }
                    )
                }
            }
        }

        // Delete Blocked Dialog (if trip is ongoing or unsettled)
        tripPendingBlockedDelete?.let { trip ->
            DeleteBlockedDialog(
                trip = trip,
                unsettledCount = settlementSummary.unsettledCount,
                onMarkSettledAndProceed = {
                    viewModel.toggleManualSettlement(trip.id, true)
                    tripPendingBlockedDelete = null
                    tripPendingDelete = trip
                },
                onDismiss = {
                    tripPendingBlockedDelete = null
                }
            )
        }

        // Extend Date Dialog
        tripToExtend?.let { trip ->
            ExtendTripDateDialog(
                trip = trip,
                onConfirm = { newEndDate ->
                    viewModel.extendTripEndDate(trip.id, newEndDate)
                    tripToExtend = null
                },
                onDismiss = {
                    tripToExtend = null
                }
            )
        }

        // Delete Confirmation Dialog (allowed because trip is manually settled)
        tripPendingDelete?.let { trip ->
            DeleteTripConfirmationDialog(
                trip = trip,
                isSettled = trip.isManuallySettled,
                pendingDebtCount = settlementSummary.unsettledCount,
                onConfirmDelete = {
                    viewModel.deleteTripById(trip.id)
                    tripPendingDelete = null
                },
                onExportBackup = {
                    viewModel.generateCsvExportForTrip(trip.id) { csv ->
                        exportedCsvText = csv
                        tripToExport = trip
                    }
                },
                onDismiss = {
                    tripPendingDelete = null
                }
            )
        }

        // Export Selected Trip Dialog
        tripToExport?.let { trip ->
            ExportTripDialog(
                trip = trip,
                csvContent = exportedCsvText,
                onDismiss = {
                    tripToExport = null
                }
            )
        }
    }
}
