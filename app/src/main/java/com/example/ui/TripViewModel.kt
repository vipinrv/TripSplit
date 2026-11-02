package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TripRepository
import com.example.data.model.CategorySpending
import com.example.data.model.Currency
import com.example.data.model.DebtPair
import com.example.data.model.DetailedExpense
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.MemberBalance
import com.example.data.model.MemberEntity
import com.example.data.model.MemberSpending
import com.example.data.model.SettlementEntity
import com.example.data.model.SettlementStatusSummary
import com.example.data.model.TripEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = TripRepository(database.tripDao())

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty(application)
        }
    }

    val allTrips: StateFlow<List<TripEntity>> = repository.allTrips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val selectedTripId: StateFlow<String?> = _selectedTripId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTrip: StateFlow<TripEntity?> = selectedTripId.flatMapLatest { id ->
        if (id != null) repository.getTripById(id)
        else allTrips.flatMapLatest { trips ->
            val firstId = trips.firstOrNull()?.id
            if (firstId != null) {
                if (_selectedTripId.value == null) _selectedTripId.value = firstId
                repository.getTripById(firstId)
            } else flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMembers: StateFlow<List<MemberEntity>> = currentTrip.flatMapLatest { trip ->
        if (trip != null) repository.getMembersForTrip(trip.id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search and Filter State for Expenses
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<String?>(null)
    val payerFilter = MutableStateFlow<String?>(null)
    val tagFilter = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailedExpenses: StateFlow<List<DetailedExpense>> = combine(
        currentTrip.flatMapLatest { trip ->
            if (trip != null) repository.getDetailedExpenses(trip.id)
            else flowOf(emptyList())
        },
        searchQuery,
        categoryFilter,
        payerFilter,
        tagFilter
    ) { expenses, query, cat, payer, tag ->
        expenses.filter { detailed ->
            val exp = detailed.expense
            val matchesQuery = query.isBlank() ||
                    exp.title.contains(query, ignoreCase = true) ||
                    exp.notes.contains(query, ignoreCase = true) ||
                    exp.location.contains(query, ignoreCase = true) ||
                    exp.tags.contains(query, ignoreCase = true)

            val matchesCat = cat == null || exp.category.equals(cat, ignoreCase = true)
            val matchesPayer = payer == null || exp.payerMemberId == payer
            val matchesTag = tag == null || exp.tags.contains(tag, ignoreCase = true)

            matchesQuery && matchesCat && matchesPayer && matchesTag
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBalances: StateFlow<List<MemberBalance>> = currentTrip.flatMapLatest { trip ->
        if (trip != null) repository.getMemberBalances(trip.id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val settlementSummary: StateFlow<SettlementStatusSummary> = currentTrip.flatMapLatest { trip ->
        if (trip != null) repository.getSettlementStatusSummary(trip.id)
        else flowOf(SettlementStatusSummary(true, 0, 0.0, emptyList()))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettlementStatusSummary(true, 0, 0.0, emptyList())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val categorySpending: StateFlow<List<CategorySpending>> = currentTrip.flatMapLatest { trip ->
        if (trip != null) repository.getCategorySpending(trip.id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val memberSpending: StateFlow<List<MemberSpending>> = currentTrip.flatMapLatest { trip ->
        if (trip != null) repository.getMemberSpendingBreakdown(trip.id)
        else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User Message / Snackbar
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }

    // Trip operations
    fun updateTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.updateTrip(trip)
            _snackbarMessage.value = "Updated trip details"
        }
    }

    fun toggleManualSettlement(tripId: String, isSettled: Boolean) {
        viewModelScope.launch {
            repository.toggleManualSettlement(tripId, isSettled)
            _snackbarMessage.value = if (isSettled) "Trip marked as Fully Settled & Closed" else "Trip reopened as Ongoing"
        }
    }

    fun extendTripEndDate(tripId: String, newEndDate: String) {
        viewModelScope.launch {
            repository.extendTripEndDate(tripId, newEndDate)
            _snackbarMessage.value = "Extended trip end date to $newEndDate"
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            _snackbarMessage.value = "Member removed"
        }
    }

    fun createNewTrip(title: String, location: String, startDate: String, endDate: String, baseCurrency: String, memberNames: List<String>) {
        viewModelScope.launch {
            val tripId = "trip_" + UUID.randomUUID().toString().take(8)
            val newTrip = TripEntity(
                id = tripId,
                title = title.ifBlank { "Un-named Trip" },
                location = location.ifBlank { "Destination" },
                startDate = startDate,
                endDate = endDate,
                baseCurrency = baseCurrency
            )

            val colorPalette = listOf("#0D9488", "#8B5CF6", "#F59E0B", "#EC4899", "#3B82F6", "#10B981", "#EF4444")
            val members = memberNames.filter { it.isNotBlank() }.mapIndexed { idx, name ->
                val parts = name.trim().split(" ")
                val nickname = if (parts.size > 1) parts.first() else null
                MemberEntity(
                    id = "m_" + UUID.randomUUID().toString().take(8),
                    tripId = tripId,
                    fullName = name.trim(),
                    nickname = nickname,
                    avatarColorHex = colorPalette[idx % colorPalette.size]
                )
            }

            repository.createTrip(newTrip, members)
            _selectedTripId.value = tripId
            _snackbarMessage.value = "Created trip '${newTrip.title}'"
        }
    }

    fun deleteCurrentTrip() {
        val trip = currentTrip.value ?: return
        deleteTripById(trip.id)
    }

    fun deleteTripById(tripId: String) {
        viewModelScope.launch {
            val trip = repository.getTripByIdDirect(tripId)
            val title = trip?.title ?: "Trip"
            repository.deleteTrip(tripId)
            if (_selectedTripId.value == tripId) {
                _selectedTripId.value = null
            }
            _snackbarMessage.value = "Permanently deleted '$title' and all associated data"
        }
    }

    fun generateCsvExportForTrip(tripId: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.generateCsvExport(tripId)
            onResult(csv)
        }
    }

    fun restoreSampleData() {
        viewModelScope.launch {
            repository.forceSeedSampleData(getApplication())
            _snackbarMessage.value = "Sample trips restored successfully"
        }
    }

    fun addMemberToCurrentTrip(name: String, nickname: String?) {
        val trip = currentTrip.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val colorPalette = listOf("#0D9488", "#8B5CF6", "#F59E0B", "#EC4899", "#3B82F6", "#10B981")
            val member = MemberEntity(
                id = "m_" + UUID.randomUUID().toString().take(8),
                tripId = trip.id,
                fullName = name.trim(),
                nickname = nickname?.takeIf { it.isNotBlank() },
                avatarColorHex = colorPalette[(0..5).random()]
            )
            repository.addMemberToTrip(member)
            _snackbarMessage.value = "Added member '${member.getDisplayName()}'"
        }
    }

    // Expense Operations
    fun saveExpense(
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
        customSplitsMap: Map<String, Double>? // memberId -> custom base amount
    ) {
        val trip = currentTrip.value ?: return
        if (title.isBlank() || amountOriginal <= 0.0 || payerMemberId.isBlank()) {
            _snackbarMessage.value = "Please fill in title, valid amount, and payer"
            return
        }

        viewModelScope.launch {
            val amountBase = Currency.convert(amountOriginal, originalCurrency, trip.baseCurrency)
            val exchangeRate = Currency.convert(1.0, originalCurrency, trip.baseCurrency)

            val expId = expenseId ?: ("exp_" + UUID.randomUUID().toString().take(8))

            val expense = ExpenseEntity(
                id = expId,
                tripId = trip.id,
                title = title.trim(),
                amountOriginal = amountOriginal,
                originalCurrency = originalCurrency,
                amountBaseCurrency = amountBase,
                exchangeRate = exchangeRate,
                category = category,
                payerMemberId = payerMemberId,
                date = date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                location = location.trim(),
                notes = notes.trim(),
                tags = tags.trim(),
                receiptAttachmentPath = receiptPath,
                splitType = if (customSplitsMap != null) "CUSTOM" else "EQUAL"
            )

            val splits = mutableListOf<ExpenseSplitEntity>()

            if (customSplitsMap != null) {
                // Custom split
                customSplitsMap.forEach { (memberId, customAmountOriginal) ->
                    val customAmountBase = Currency.convert(customAmountOriginal, originalCurrency, trip.baseCurrency)
                    splits.add(
                        ExpenseSplitEntity(
                            id = "s_${expId}_$memberId",
                            expenseId = expId,
                            memberId = memberId,
                            amountBase = customAmountBase,
                            isCustomSplit = true
                        )
                    )
                }
            } else {
                // Equal split across selected member IDs
                val splitMembers = if (selectedMemberIdsForSplit.isNotEmpty()) selectedMemberIdsForSplit
                else currentMembers.value.map { it.id }

                val perPersonBase = if (splitMembers.isNotEmpty()) amountBase / splitMembers.size else 0.0

                splitMembers.forEach { mId ->
                    splits.add(
                        ExpenseSplitEntity(
                            id = "s_${expId}_$mId",
                            expenseId = expId,
                            memberId = mId,
                            amountBase = perPersonBase,
                            isCustomSplit = false
                        )
                    )
                }
            }

            repository.saveExpense(expense, splits)
            _snackbarMessage.value = "Saved expense '$title'"
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            _snackbarMessage.value = "Expense deleted"
        }
    }

    // Settlements
    fun recordSettlement(fromMemberId: String, toMemberId: String, amountBase: Double, notes: String) {
        val trip = currentTrip.value ?: return
        if (amountBase <= 0.0 || fromMemberId == toMemberId) return

        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val settlement = SettlementEntity(
                id = "st_" + UUID.randomUUID().toString().take(8),
                tripId = trip.id,
                fromMemberId = fromMemberId,
                toMemberId = toMemberId,
                amountBase = amountBase,
                date = today,
                notes = notes.trim()
            )
            repository.recordSettlement(settlement)
            _snackbarMessage.value = "Payment recorded! Settlement updated."
        }
    }

    fun settleUpAllGroupDebts() {
        val trip = currentTrip.value ?: return
        viewModelScope.launch {
            repository.settleUpAllDebts(trip.id)
            _snackbarMessage.value = "All group debts marked as fully settled!"
        }
    }

    fun clearSettlements() {
        val trip = currentTrip.value ?: return
        viewModelScope.launch {
            repository.clearAllSettlements(trip.id)
            _snackbarMessage.value = "Settlement history cleared"
        }
    }

    suspend fun getCsvExportContent(): String {
        val trip = currentTrip.value ?: return "No trip selected"
        return repository.generateCsvExport(trip.id)
    }
}

class TripViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
