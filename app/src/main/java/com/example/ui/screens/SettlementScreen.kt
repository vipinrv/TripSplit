package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Currency
import com.example.data.model.DebtPair
import com.example.data.model.MemberBalance
import com.example.ui.TripViewModel
import com.example.ui.components.DebtSettlementCard
import com.example.ui.components.SettlementHeaderBanner
import com.example.ui.components.parseHexColor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.ExtendTripDateDialog

@Composable
fun SettlementScreen(
    viewModel: TripViewModel,
    onOpenRecordSettlementDialog: (fromMemberId: String?, toMemberId: String?, amount: Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrip by viewModel.currentTrip.collectAsState()
    val settlementSummary by viewModel.settlementSummary.collectAsState()
    val balances by viewModel.currentBalances.collectAsState()

    val baseCurrency = currentTrip?.baseCurrency ?: "INR"
    var showExtendDialog by remember { mutableStateOf(false) }

    if (showExtendDialog && currentTrip != null) {
        ExtendTripDateDialog(
            trip = currentTrip!!,
            onConfirm = { newEndDate ->
                viewModel.extendTripEndDate(currentTrip!!.id, newEndDate)
                showExtendDialog = false
            },
            onDismiss = { showExtendDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settlement_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Live Settlement Header Status Banner
        item {
            SettlementHeaderBanner(
                isManuallySettled = currentTrip?.isManuallySettled == true,
                summary = settlementSummary,
                baseCurrency = baseCurrency,
                onSettleUpAll = { viewModel.settleUpAllGroupDebts() },
                onToggleManualSettlement = {
                    val trip = currentTrip
                    if (trip != null) {
                        viewModel.toggleManualSettlement(trip.id, !trip.isManuallySettled)
                    }
                },
                onExtendTripDate = { showExtendDialog = true }
            )
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenRecordSettlementDialog(null, null, null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("log_payment_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Payment")
                }

                if (!settlementSummary.isFullySettled) {
                    OutlinedButton(
                        onClick = { viewModel.settleUpAllGroupDebts() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settle All")
                    }
                }
            }
        }

        // Section 1: Pairwise Debts ("Who owes whom")
        item {
            Text(
                text = "Optimal Debt Settlement Plan (${settlementSummary.pairs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (settlementSummary.isFullySettled || settlementSummary.pairs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All pairwise debts are settled!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Everyone in the group is completely even.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        } else {
            items(settlementSummary.pairs) { debtPair ->
                DebtSettlementCard(
                    debtPair = debtPair,
                    baseCurrency = baseCurrency,
                    onSettlePair = {
                        onOpenRecordSettlementDialog(
                            debtPair.fromMemberId,
                            debtPair.toMemberId,
                            debtPair.amountBase
                        )
                    }
                )
            }
        }

        // Section 2: Individual Net Balances
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Member Balance Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(balances, key = { it.memberId }) { balance ->
            MemberBalanceCard(balance = balance, baseCurrency = baseCurrency)
        }
    }
}

@Composable
fun MemberBalanceCard(
    balance: MemberBalance,
    baseCurrency: String
) {
    val net = balance.netBalanceBase
    val isGetsBack = net > 0.01
    val isOwes = net < -0.01
    val isEven = !isGetsBack && !isOwes

    val statusColor = if (isGetsBack) Color(0xFF10B981) else if (isOwes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_balance_${balance.memberId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(balance.avatarColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = balance.memberName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = balance.memberName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paid ${Currency.format(balance.totalPaidBase, baseCurrency)} • Share ${Currency.format(balance.totalShareBase, baseCurrency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isEven) "Settled Up"
                    else if (isGetsBack) "Gets back ${Currency.format(net, baseCurrency)}"
                    else "Owes ${Currency.format(-net, baseCurrency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
