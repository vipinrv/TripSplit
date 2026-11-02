package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Currency
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.MemberEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun parseDateStringToMillis(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

private fun formatMillisToDateString(millis: Long?): String {
    if (millis == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    tripBaseCurrency: String,
    members: List<MemberEntity>,
    editingExpense: ExpenseEntity?,
    onDismiss: () -> Unit,
    onSaveExpense: (
        expenseId: String?,
        title: String,
        amountOriginal: Double,
        originalCurrency: String,
        category: String,
        payerMemberId: String,
        date: String,
        location: String,
        notes: String,
        tags: String,
        receiptPath: String?,
        selectedMemberIdsForSplit: List<String>,
        customSplitsMap: Map<String, Double>?
    ) -> Unit
) {
    var title by remember { mutableStateOf(editingExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(editingExpense?.amountOriginal?.toString() ?: "") }
    var selectedCurrency by remember { mutableStateOf(Currency.fromCode(editingExpense?.originalCurrency ?: tripBaseCurrency)) }
    var currencyExpanded by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf(ExpenseCategory.fromId(editingExpense?.category ?: "Food")) }

    var selectedPayerId by remember {
        mutableStateOf(
            editingExpense?.payerMemberId ?: (members.firstOrNull()?.id ?: "")
        )
    }
    var payerExpanded by remember { mutableStateOf(false) }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var dateText by remember { mutableStateOf(editingExpense?.date ?: today) }
    var showDatePicker by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf(editingExpense?.location ?: "") }
    var notesText by remember { mutableStateOf(editingExpense?.notes ?: "") }
    var tagsText by remember { mutableStateOf(editingExpense?.tags ?: "") }

    var hasReceiptAttached by remember { mutableStateOf(editingExpense?.receiptAttachmentPath != null) }

    // Split Logic Mode
    var isCustomSplit by remember { mutableStateOf(editingExpense?.splitType == "CUSTOM") }
    var selectedSplitMemberIds by remember {
        mutableStateOf(members.map { it.id })
    }

    val customSplitsMap = remember { mutableStateMapOf<String, String>() }

    val amountDouble = amountText.toDoubleOrNull() ?: 0.0
    val convertedBaseAmount = Currency.convert(amountDouble, selectedCurrency.code, tripBaseCurrency)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateStringToMillis(dateText)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateText = formatMillisToDateString(millis)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingExpense != null) "Edit Expense" else "Log New Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title *") },
                    placeholder = { Text("e.g. Seafood Dinner, Hotel Booking") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount & Currency Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount *") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("expense_amount_input"),
                        singleLine = true
                    )

                    // Currency Dropdown
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "${selectedCurrency.code} (${selectedCurrency.symbol})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            Currency.entries.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${curr.code} (${curr.symbol})") },
                                    onClick = {
                                        selectedCurrency = curr
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Currency Conversion Live Rate Banner (if currency != trip base)
                if (!selectedCurrency.code.equals(tripBaseCurrency, ignoreCase = true) && amountDouble > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Live Conversion: ${Currency.format(amountDouble, selectedCurrency.code)} = ${Currency.format(convertedBaseAmount, tripBaseCurrency)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategory.entries.take(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.id, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExpenseCategory.entries.drop(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.id, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payer Dropdown
                ExposedDropdownMenuBox(
                    expanded = payerExpanded,
                    onExpandedChange = { payerExpanded = !payerExpanded }
                ) {
                    val payerMember = members.firstOrNull { it.id == selectedPayerId }
                    val payerDisplayName = payerMember?.getDisplayName() ?: "Select Payer"

                    OutlinedTextField(
                        value = payerDisplayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid By *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("expense_payer_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.getDisplayName()) },
                                onClick = {
                                    selectedPayerId = m.id
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date & Location
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_date_input"),
                            singleLine = true
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text("Location") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes & Custom Tags
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Optional notes or description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Custom Tags") },
                    placeholder = { Text("#Stay, #Food, #Fuel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt Attachment Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Receipt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attach Receipt",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = hasReceiptAttached,
                        onCheckedChange = { hasReceiptAttached = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Split Logic Section
                Text(
                    text = "Split Logic",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isCustomSplit,
                        onClick = { isCustomSplit = false }
                    )
                    Text("Equal Split", modifier = Modifier.clickable { isCustomSplit = false })

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = isCustomSplit,
                        onClick = { isCustomSplit = true }
                    )
                    Text("Custom Split", modifier = Modifier.clickable { isCustomSplit = true })
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!isCustomSplit) {
                    // Equal split - member checkboxes
                    Text(
                        text = "Split equally among selected members:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    members.forEach { m ->
                        val isChecked = selectedSplitMemberIds.contains(m.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSplitMemberIds = if (isChecked) {
                                        if (selectedSplitMemberIds.size > 1) selectedSplitMemberIds - m.id else selectedSplitMemberIds
                                    } else {
                                        selectedSplitMemberIds + m.id
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedSplitMemberIds = if (checked) {
                                        selectedSplitMemberIds + m.id
                                    } else {
                                        if (selectedSplitMemberIds.size > 1) selectedSplitMemberIds - m.id else selectedSplitMemberIds
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = m.getDisplayName(), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            if (isChecked && amountDouble > 0 && selectedSplitMemberIds.isNotEmpty()) {
                                val perPerson = amountDouble / selectedSplitMemberIds.size
                                Text(
                                    text = Currency.format(perPerson, selectedCurrency.code),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // Custom split inputs per member
                    Text(
                        text = "Specify custom amount for each member (${selectedCurrency.code}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    members.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = m.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customSplitsMap[m.id] ?: "",
                                onValueChange = { customSplitsMap[m.id] = it },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(56.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val mapToSend = if (isCustomSplit) {
                                customSplitsMap.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                            } else null

                            onSaveExpense(
                                editingExpense?.id,
                                title,
                                amountDouble,
                                selectedCurrency.code,
                                selectedCategory.id,
                                selectedPayerId,
                                dateText,
                                locationText,
                                notesText,
                                tagsText,
                                if (hasReceiptAttached) "receipt_sample.png" else null,
                                selectedSplitMemberIds,
                                mapToSend
                            )
                        },
                        enabled = title.isNotBlank() && amountDouble > 0 && selectedPayerId.isNotBlank(),
                        modifier = Modifier.testTag("save_expense_btn")
                    ) {
                        Text(if (editingExpense != null) "Update Expense" else "Log Expense")
                    }
                }
            }
        }
    }
}
