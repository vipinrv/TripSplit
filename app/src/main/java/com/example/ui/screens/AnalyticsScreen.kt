package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Currency
import com.example.ui.TripViewModel
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.MemberSpendingBarChart

@Composable
fun AnalyticsScreen(
    viewModel: TripViewModel,
    modifier: Modifier = Modifier
) {
    val currentTrip by viewModel.currentTrip.collectAsState()
    val detailedExpenses by viewModel.detailedExpenses.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val memberSpending by viewModel.memberSpending.collectAsState()

    val baseCurrency = currentTrip?.baseCurrency ?: "INR"
    val totalSpending = detailedExpenses.sumOf { it.expense.amountBaseCurrency }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Total Spending Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "${currentTrip?.title ?: "Trip"} Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Currency.format(totalSpending, baseCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${detailedExpenses.size} Expenses logged across ${categorySpending.size} categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Category Donut Chart
        item {
            CategoryDonutChart(
                categorySpending = categorySpending,
                totalSpendingBase = totalSpending,
                baseCurrency = baseCurrency
            )
        }

        // Member Spending Bar Chart
        item {
            MemberSpendingBarChart(
                memberSpendingList = memberSpending,
                baseCurrency = baseCurrency
            )
        }
    }
}
