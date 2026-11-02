package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.AddEditTripDialog
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.RecordSettlementDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.SettlementScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripsScreen

enum class AppTab(val title: String, val icon: ImageVector) {
    TRIPS("Trips", Icons.Default.FlightTakeoff),
    EXPENSES("Expenses", Icons.AutoMirrored.Filled.ReceiptLong),
    SETTLEMENT("Settlement", Icons.Default.Payments),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSplitApp(
    viewModel: TripViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.TRIPS) }

    val trips by viewModel.allTrips.collectAsState()
    val currentTrip by viewModel.currentTrip.collectAsState()
    val members by viewModel.currentMembers.collectAsState()

    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Dialog States
    var showAddTripDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    var showRecordSettlementDialog by remember { mutableStateOf(false) }
    var settlementFromMemberId by remember { mutableStateOf<String?>(null) }
    var settlementToMemberId by remember { mutableStateOf<String?>(null) }
    var settlementAmount by remember { mutableStateOf<Double?>(null) }

    var tripDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { tripDropdownExpanded = true }
                    ) {
                        Text(
                            text = currentTrip?.title ?: "TripSplit",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        DropdownMenu(
                            expanded = tripDropdownExpanded,
                            onDismissRequest = { tripDropdownExpanded = false }
                        ) {
                            trips.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = t.title,
                                                fontWeight = if (t.id == currentTrip?.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (t.id == currentTrip?.id) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectTrip(t.id)
                                        tripDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Create New Trip", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    tripDropdownExpanded = false
                                    showAddTripDialog = true
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddTripDialog = true },
                        modifier = Modifier.testTag("top_app_bar_add_trip")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Trip")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation")
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.TRIPS -> TripsScreen(
                    viewModel = viewModel,
                    onOpenAddTripDialog = { showAddTripDialog = true }
                )
                AppTab.EXPENSES -> ExpensesScreen(
                    viewModel = viewModel,
                    onOpenAddExpenseDialog = {
                        editingExpense = null
                        showAddExpenseDialog = true
                    },
                    onOpenEditExpenseDialog = { exp ->
                        editingExpense = exp
                        showAddExpenseDialog = true
                    }
                )
                AppTab.SETTLEMENT -> SettlementScreen(
                    viewModel = viewModel,
                    onOpenRecordSettlementDialog = { from, to, amount ->
                        settlementFromMemberId = from
                        settlementToMemberId = to
                        settlementAmount = amount
                        showRecordSettlementDialog = true
                    }
                )
                AppTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onOpenAddMemberDialog = { showAddMemberDialog = true }
                )
            }
        }
    }

    // Dialog Modals
    if (showAddTripDialog) {
        AddEditTripDialog(
            onDismiss = { showAddTripDialog = false },
            onConfirm = { title, location, startDate, endDate, baseCurrency, memberNames ->
                viewModel.createNewTrip(title, location, startDate, endDate, baseCurrency, memberNames)
                showAddTripDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        val baseCurr = currentTrip?.baseCurrency ?: "INR"
        AddEditExpenseDialog(
            tripBaseCurrency = baseCurr,
            members = members,
            editingExpense = editingExpense,
            onDismiss = {
                showAddExpenseDialog = false
                editingExpense = null
            },
            onSaveExpense = { expId, title, amountOriginal, originalCurr, category, payerId, date, loc, notes, tags, receipt, selectedSplitMemberIds, customSplits ->
                viewModel.saveExpense(
                    expenseId = expId,
                    title = title,
                    amountOriginal = amountOriginal,
                    originalCurrency = originalCurr,
                    category = category,
                    payerMemberId = payerId,
                    date = date,
                    location = loc,
                    notes = notes,
                    tags = tags,
                    receiptPath = receipt,
                    selectedMemberIdsForSplit = selectedSplitMemberIds,
                    customSplitsMap = customSplits
                )
                showAddExpenseDialog = false
                editingExpense = null
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAddMember = { fullName, nickname ->
                viewModel.addMemberToCurrentTrip(fullName, nickname)
                showAddMemberDialog = false
            }
        )
    }

    if (showRecordSettlementDialog) {
        val baseCurr = currentTrip?.baseCurrency ?: "INR"
        RecordSettlementDialog(
            members = members,
            baseCurrency = baseCurr,
            initialFromMemberId = settlementFromMemberId,
            initialToMemberId = settlementToMemberId,
            initialAmount = settlementAmount,
            onDismiss = { showRecordSettlementDialog = false },
            onConfirm = { from, to, amount, notes ->
                viewModel.recordSettlement(from, to, amount, notes)
                showRecordSettlementDialog = false
            }
        )
    }
}
