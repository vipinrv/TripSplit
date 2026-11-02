package com.example.data

import com.example.data.dao.TripDao
import com.example.data.model.CategorySpending
import com.example.data.model.Currency
import com.example.data.model.DebtPair
import com.example.data.model.DetailedExpense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.ExpenseSplitWithMember
import com.example.data.model.MemberBalance
import com.example.data.model.MemberEntity
import com.example.data.model.MemberSpending
import com.example.data.model.SettlementEntity
import com.example.data.model.SettlementStatusSummary
import com.example.data.model.TripEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    fun getTripById(tripId: String): Flow<TripEntity?> = tripDao.getTripById(tripId)

    fun getMembersForTrip(tripId: String): Flow<List<MemberEntity>> = tripDao.getMembersForTrip(tripId)

    fun getExpensesForTrip(tripId: String): Flow<List<ExpenseEntity>> = tripDao.getExpensesForTrip(tripId)

    fun getSettlementsForTrip(tripId: String): Flow<List<SettlementEntity>> = tripDao.getSettlementsForTrip(tripId)

    suspend fun createTrip(trip: TripEntity, members: List<MemberEntity>) {
        tripDao.insertTrip(trip)
        tripDao.insertMembers(members)
    }

    suspend fun updateTrip(trip: TripEntity) {
        tripDao.insertTrip(trip)
    }

    suspend fun toggleManualSettlement(tripId: String, isSettled: Boolean) {
        val trip = tripDao.getTripByIdDirect(tripId) ?: return
        val updated = trip.copy(isManuallySettled = isSettled)
        tripDao.insertTrip(updated)
    }

    suspend fun extendTripEndDate(tripId: String, newEndDate: String) {
        val trip = tripDao.getTripByIdDirect(tripId) ?: return
        val updated = trip.copy(endDate = newEndDate)
        tripDao.insertTrip(updated)
    }

    suspend fun addMemberToTrip(member: MemberEntity) {
        tripDao.insertMember(member)
    }

    suspend fun deleteMember(memberId: String) {
        tripDao.deleteMember(memberId)
    }

    suspend fun getTripByIdDirect(tripId: String): TripEntity? {
        return tripDao.getTripByIdDirect(tripId)
    }

    suspend fun deleteTrip(tripId: String) {
        tripDao.deleteTripCompletely(tripId)
    }

    suspend fun saveExpense(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>) {
        tripDao.saveExpenseWithSplits(expense, splits)
    }

    suspend fun deleteExpense(expenseId: String) {
        tripDao.deleteExpense(expenseId)
    }

    suspend fun recordSettlement(settlement: SettlementEntity) {
        tripDao.insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlementId: String) {
        tripDao.deleteSettlement(settlementId)
    }

    suspend fun clearAllSettlements(tripId: String) {
        tripDao.clearAllSettlementsForTrip(tripId)
    }

    // Detailed expenses with payer name and split details
    fun getDetailedExpenses(tripId: String): Flow<List<DetailedExpense>> {
        return combine(
            tripDao.getExpensesForTrip(tripId),
            tripDao.getMembersForTrip(tripId)
        ) { expenses, members ->
            val memberMap = members.associateBy { it.id }
            expenses.map { expense ->
                val payer = memberMap[expense.payerMemberId]
                val payerName = payer?.getDisplayName() ?: "Unknown Payer"
                val payerNickname = payer?.nickname

                val splits = tripDao.getSplitsForExpenseDirect(expense.id).map { split ->
                    val member = memberMap[split.memberId]
                    val mName = member?.getDisplayName() ?: "Member"
                    ExpenseSplitWithMember(split, mName)
                }

                DetailedExpense(
                    expense = expense,
                    payerName = payerName,
                    payerNickname = payerNickname,
                    splits = splits
                )
            }
        }
    }

    // Balance Calculation Engine
    fun getMemberBalances(tripId: String): Flow<List<MemberBalance>> {
        return combine(
            tripDao.getMembersForTrip(tripId),
            tripDao.getExpensesForTrip(tripId),
            tripDao.getSettlementsForTrip(tripId)
        ) { members, expenses, settlements ->
            val allSplits = tripDao.getAllSplitsForTripDirect(tripId)
            
            val paidMap = mutableMapOf<String, Double>()
            val shareMap = mutableMapOf<String, Double>()
            val settlementsSentMap = mutableMapOf<String, Double>()
            val settlementsReceivedMap = mutableMapOf<String, Double>()

            members.forEach { m ->
                paidMap[m.id] = 0.0
                shareMap[m.id] = 0.0
                settlementsSentMap[m.id] = 0.0
                settlementsReceivedMap[m.id] = 0.0
            }

            expenses.forEach { exp ->
                val currentPaid = paidMap[exp.payerMemberId] ?: 0.0
                paidMap[exp.payerMemberId] = currentPaid + exp.amountBaseCurrency
            }

            allSplits.forEach { split ->
                val currentShare = shareMap[split.memberId] ?: 0.0
                shareMap[split.memberId] = currentShare + split.amountBase
            }

            settlements.forEach { st ->
                val sent = settlementsSentMap[st.fromMemberId] ?: 0.0
                settlementsSentMap[st.fromMemberId] = sent + st.amountBase

                val recv = settlementsReceivedMap[st.toMemberId] ?: 0.0
                settlementsReceivedMap[st.toMemberId] = recv + st.amountBase
            }

            members.map { m ->
                val totalPaid = paidMap[m.id] ?: 0.0
                val totalShare = shareMap[m.id] ?: 0.0
                val sent = settlementsSentMap[m.id] ?: 0.0
                val recv = settlementsReceivedMap[m.id] ?: 0.0

                // Net balance = (Paid + Sent to others) - (Share owed + Received from others)
                val net = (totalPaid + sent) - (totalShare + recv)

                MemberBalance(
                    memberId = m.id,
                    memberName = m.fullName,
                    nickname = m.nickname,
                    avatarColorHex = m.avatarColorHex,
                    totalPaidBase = totalPaid,
                    totalShareBase = totalShare,
                    netBalanceBase = net
                )
            }
        }
    }

    // Pairwise Simplified Debt Engine ("Who owes whom")
    fun getSettlementStatusSummary(tripId: String): Flow<SettlementStatusSummary> {
        return getMemberBalances(tripId).map { balances ->
            val members = tripDao.getMembersForTripDirect(tripId)
            val memberMap = members.associateBy { it.id }

            // Debt solver using greedy matching of debtors and creditors
            val debtors = mutableListOf<Pair<String, Double>>() // memberId to abs(netBalance)
            val creditors = mutableListOf<Pair<String, Double>>() // memberId to netBalance

            balances.forEach { b ->
                val net = b.netBalanceBase
                if (net < -0.01) {
                    debtors.add(b.memberId to -net)
                } else if (net > 0.01) {
                    creditors.add(b.memberId to net)
                }
            }

            val pairs = mutableListOf<DebtPair>()
            var i = 0
            var j = 0

            val debtorsList = debtors.map { it.first to it.second }.toMutableList()
            val creditorsList = creditors.map { it.first to it.second }.toMutableList()

            while (i < debtorsList.size && j < creditorsList.size) {
                val debtor = debtorsList[i]
                val creditor = creditorsList[j]

                val amount = minOf(debtor.second, creditor.second)
                if (amount > 0.01) {
                    val fromMember = memberMap[debtor.first]
                    val toMember = memberMap[creditor.first]

                    val fromName = fromMember?.getDisplayName() ?: "Member"
                    val toName = toMember?.getDisplayName() ?: "Member"

                    pairs.add(
                        DebtPair(
                            fromMemberId = debtor.first,
                            fromName = fromName,
                            toMemberId = creditor.first,
                            toName = toName,
                            amountBase = amount
                        )
                    )
                }

                val remainingDebtor = debtor.second - amount
                val remainingCreditor = creditor.second - amount

                debtorsList[i] = debtor.first to remainingDebtor
                creditorsList[j] = creditor.first to remainingCreditor

                if (remainingDebtor <= 0.01) i++
                if (remainingCreditor <= 0.01) j++
            }

            val totalPending = pairs.sumOf { it.amountBase }
            val isFully = pairs.isEmpty() || totalPending < 0.01

            SettlementStatusSummary(
                isFullySettled = isFully,
                unsettledCount = pairs.size,
                totalPendingDebtBase = totalPending,
                pairs = pairs
            )
        }
    }

    // Auto-settle all debts by inserting settlements for each pairwise debt
    suspend fun settleUpAllDebts(tripId: String) {
        val members = tripDao.getMembersForTripDirect(tripId)
        val memberMap = members.associateBy { it.id }
        val expenses = tripDao.getExpensesForTripDirect(tripId)
        val allSplits = tripDao.getAllSplitsForTripDirect(tripId)
        val settlements = tripDao.getSettlementsForTripDirect(tripId)

        val paidMap = mutableMapOf<String, Double>()
        val shareMap = mutableMapOf<String, Double>()
        val sentMap = mutableMapOf<String, Double>()
        val recvMap = mutableMapOf<String, Double>()

        members.forEach { m ->
            paidMap[m.id] = 0.0
            shareMap[m.id] = 0.0
            sentMap[m.id] = 0.0
            recvMap[m.id] = 0.0
        }

        expenses.forEach { e -> paidMap[e.payerMemberId] = (paidMap[e.payerMemberId] ?: 0.0) + e.amountBaseCurrency }
        allSplits.forEach { s -> shareMap[s.memberId] = (shareMap[s.memberId] ?: 0.0) + s.amountBase }
        settlements.forEach { st ->
            sentMap[st.fromMemberId] = (sentMap[st.fromMemberId] ?: 0.0) + st.amountBase
            recvMap[st.toMemberId] = (recvMap[st.toMemberId] ?: 0.0) + st.amountBase
        }

        val netMap = members.associate { m ->
            val net = ((paidMap[m.id] ?: 0.0) + (sentMap[m.id] ?: 0.0)) - ((shareMap[m.id] ?: 0.0) + (recvMap[m.id] ?: 0.0))
            m.id to net
        }.toMutableMap()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Pairwise settlement generation
        while (true) {
            val debtor = netMap.filter { it.value < -0.01 }.minByOrNull { it.value }
            val creditor = netMap.filter { it.value > 0.01 }.maxByOrNull { it.value }

            if (debtor == null || creditor == null) break

            val debtAmount = -debtor.value
            val creditAmount = creditor.value
            val settleAmount = minOf(debtAmount, creditAmount)

            if (settleAmount <= 0.01) break

            val st = SettlementEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                fromMemberId = debtor.key,
                toMemberId = creditor.key,
                amountBase = settleAmount,
                date = today,
                notes = "Auto settled all group debts"
            )
            tripDao.insertSettlement(st)

            netMap[debtor.key] = debtor.value + settleAmount
            netMap[creditor.key] = creditor.value - settleAmount
        }
    }

    // Category spending analytics
    fun getCategorySpending(tripId: String): Flow<List<CategorySpending>> {
        return tripDao.getExpensesForTrip(tripId).map { expenses ->
            val total = expenses.sumOf { it.amountBaseCurrency }
            if (total == 0.0) return@map emptyList()

            expenses.groupBy { ExpenseCategory.fromId(it.category) }
                .map { (cat, expList) ->
                    val catTotal = expList.sumOf { it.amountBaseCurrency }
                    CategorySpending(
                        category = cat,
                        totalAmountBase = catTotal,
                        percentage = (catTotal / total) * 100.0
                    )
                }
                .sortedByDescending { it.totalAmountBase }
        }
    }

    // Member spending breakdown (who paid vs who consumed)
    fun getMemberSpendingBreakdown(tripId: String): Flow<List<MemberSpending>> {
        return combine(
            tripDao.getMembersForTrip(tripId),
            tripDao.getExpensesForTrip(tripId)
        ) { members, expenses ->
            val allSplits = tripDao.getAllSplitsForTripDirect(tripId)
            val paidMap = expenses.groupBy { it.payerMemberId }
                .mapValues { it.value.sumOf { e -> e.amountBaseCurrency } }
            val shareMap = allSplits.groupBy { it.memberId }
                .mapValues { it.value.sumOf { s -> s.amountBase } }

            members.map { m ->
                MemberSpending(
                    memberId = m.id,
                    memberName = m.getDisplayName(),
                    paidAmountBase = paidMap[m.id] ?: 0.0,
                    shareAmountBase = shareMap[m.id] ?: 0.0
                )
            }.sortedByDescending { it.paidAmountBase }
        }
    }

    // CSV Export
    suspend fun generateCsvExport(tripId: String): String {
        val trip = tripDao.getTripByIdDirect(tripId) ?: return "Trip not found"
        val members = tripDao.getMembersForTripDirect(tripId).associateBy { it.id }
        val expenses = tripDao.getExpensesForTripDirect(tripId)
        val settlements = tripDao.getSettlementsForTripDirect(tripId)

        val sb = StringBuilder()
        sb.append("TRIPSPLIT EXPENSE & SETTLEMENT REPORT\n")
        sb.append("Trip Title,${trip.title}\n")
        sb.append("Location,${trip.location}\n")
        sb.append("Date Range,${trip.startDate} to ${trip.endDate}\n")
        sb.append("Base Currency,${trip.baseCurrency}\n\n")

        sb.append("--- EXPENSES ---\n")
        sb.append("Date,Title,Category,Paid By,Original Amount,Currency,Base Amount (${trip.baseCurrency}),Location,Notes,Tags\n")

        expenses.forEach { e ->
            val payer = members[e.payerMemberId]?.getDisplayName() ?: "Unknown"
            sb.append("\"${e.date}\",\"${e.title}\",\"${e.category}\",\"$payer\",${e.amountOriginal},${e.originalCurrency},${e.amountBaseCurrency},\"${e.location}\",\"${e.notes}\",\"${e.tags}\"\n")
        }

        sb.append("\n--- SETTLEMENT HISTORY ---\n")
        sb.append("Date,From Member,To Member,Amount (${trip.baseCurrency}),Notes\n")

        settlements.forEach { s ->
            val fromM = members[s.fromMemberId]?.getDisplayName() ?: "Member"
            val toM = members[s.toMemberId]?.getDisplayName() ?: "Member"
            sb.append("\"${s.date}\",\"$fromM\",\"$toM\",${s.amountBase},\"${s.notes}\"\n")
        }

        return sb.toString()
    }

    // Pre-populate sample trips if empty (only once on first install)
    suspend fun seedSampleDataIfEmpty(context: android.content.Context) {
        val prefs = context.getSharedPreferences("tripsplit_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_seeded_sample_data", false)) {
            return
        }

        if (tripDao.getTripCount() > 0) {
            prefs.edit().putBoolean("has_seeded_sample_data", true).apply()
            return
        }

        seedSampleTripsInternal()
        prefs.edit().putBoolean("has_seeded_sample_data", true).apply()
    }

    suspend fun forceSeedSampleData(context: android.content.Context) {
        seedSampleTripsInternal()
        val prefs = context.getSharedPreferences("tripsplit_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seeded_sample_data", true).apply()
    }

    private suspend fun seedSampleTripsInternal() {

        // Trip 1: Goa Beach Getaway
        val trip1Id = "trip_goa_001"
        val t1 = TripEntity(
            id = trip1Id,
            title = "Goa Beach Getaway",
            location = "North Goa, India",
            startDate = "2026-03-10",
            endDate = "2026-03-15",
            baseCurrency = "INR"
        )

        val m1 = MemberEntity(id = "m_alex", tripId = trip1Id, fullName = "Alex Johnson", nickname = "Alex", avatarColorHex = "#0D9488")
        val m2 = MemberEntity(id = "m_priya", tripId = trip1Id, fullName = "Priya Sharma", nickname = "Priya", avatarColorHex = "#8B5CF6")
        val m3 = MemberEntity(id = "m_rahul", tripId = trip1Id, fullName = "Rahul Verma", nickname = "Rahul", avatarColorHex = "#F59E0B")
        val m4 = MemberEntity(id = "m_maya", tripId = trip1Id, fullName = "Maya Lin", nickname = "Maya", avatarColorHex = "#EC4899")

        val members1 = listOf(m1, m2, m3, m4)

        tripDao.insertTrip(t1)
        tripDao.insertMembers(members1)

        // Expenses for Goa
        val e1 = ExpenseEntity(
            id = "exp_001",
            tripId = trip1Id,
            title = "Beachside Villa Booking",
            amountOriginal = 32000.0,
            originalCurrency = "INR",
            amountBaseCurrency = 32000.0,
            exchangeRate = 1.0,
            category = "Stay",
            payerMemberId = m1.id,
            date = "2026-03-10",
            location = "Calangute Beach",
            notes = "5 nights private villa with pool",
            tags = "#Stay,#Villa"
        )
        val e1Splits = members1.map { ExpenseSplitEntity("s1_${it.id}", e1.id, it.id, 8000.0) }

        val e2 = ExpenseEntity(
            id = "exp_002",
            tripId = trip1Id,
            title = "Seafood Shack Dinner & Drinks",
            amountOriginal = 9600.0,
            originalCurrency = "INR",
            amountBaseCurrency = 9600.0,
            exchangeRate = 1.0,
            category = "Food",
            payerMemberId = m2.id,
            date = "2026-03-11",
            location = "Britto's Shack, Baga",
            notes = "Dinner, lobster platter and Mocktails",
            tags = "#Food,#Dinner"
        )
        val e2Splits = members1.map { ExpenseSplitEntity("s2_${it.id}", e2.id, it.id, 2400.0) }

        val e3 = ExpenseEntity(
            id = "exp_003",
            tripId = trip1Id,
            title = "Scooter Rentals & Petrol",
            amountOriginal = 4800.0,
            originalCurrency = "INR",
            amountBaseCurrency = 4800.0,
            exchangeRate = 1.0,
            category = "Fuel",
            payerMemberId = m3.id,
            date = "2026-03-12",
            location = "Anjuna Market",
            notes = "4 Scooters for 3 days + full tanks",
            tags = "#Fuel,#Scooter"
        )
        val e3Splits = members1.map { ExpenseSplitEntity("s3_${it.id}", e3.id, it.id, 1200.0) }

        val e4 = ExpenseEntity(
            id = "exp_004",
            tripId = trip1Id,
            title = "Sunset Catamaran Cruise",
            amountOriginal = 12000.0,
            originalCurrency = "INR",
            amountBaseCurrency = 12000.0,
            exchangeRate = 1.0,
            category = "Activity",
            payerMemberId = m4.id,
            date = "2026-03-13",
            location = "Panjim Jetty",
            notes = "Water sports and catamaran cruise",
            tags = "#Activity,#Cruise"
        )
        val e4Splits = members1.map { ExpenseSplitEntity("s4_${it.id}", e4.id, it.id, 3000.0) }

        tripDao.saveExpenseWithSplits(e1, e1Splits)
        tripDao.saveExpenseWithSplits(e2, e2Splits)
        tripDao.saveExpenseWithSplits(e3, e3Splits)
        tripDao.saveExpenseWithSplits(e4, e4Splits)


        // Trip 2: Euro Summer Tour
        val trip2Id = "trip_euro_002"
        val t2 = TripEntity(
            id = trip2Id,
            title = "Euro Summer Tour",
            location = "Paris & Rome",
            startDate = "2026-06-01",
            endDate = "2026-06-12",
            baseCurrency = "EUR"
        )

        val em1 = MemberEntity(id = "m_emma", tripId = trip2Id, fullName = "Emma Watson", nickname = "Em", avatarColorHex = "#3B82F6")
        val em2 = MemberEntity(id = "m_liam", tripId = trip2Id, fullName = "Liam Neeson", nickname = "Liam", avatarColorHex = "#10B981")
        val em3 = MemberEntity(id = "m_lucas", tripId = trip2Id, fullName = "Lucas Vance", nickname = "Luke", avatarColorHex = "#F59E0B")
        val em4 = MemberEntity(id = "m_sophia", tripId = trip2Id, fullName = "Sophia Rodriguez", nickname = "Soph", avatarColorHex = "#EF4444")

        val members2 = listOf(em1, em2, em3, em4)

        tripDao.insertTrip(t2)
        tripDao.insertMembers(members2)

        val ee1 = ExpenseEntity(
            id = "exp_euro_01",
            tripId = trip2Id,
            title = "Parisian Montmartre Loft",
            amountOriginal = 1600.0,
            originalCurrency = "EUR",
            amountBaseCurrency = 1600.0,
            exchangeRate = 1.0,
            category = "Stay",
            payerMemberId = em1.id,
            date = "2026-06-01",
            location = "Paris, France",
            notes = "Charming 2BR apartment near Sacre Coeur",
            tags = "#Stay,#Paris"
        )
        val ee1Splits = members2.map { ExpenseSplitEntity("se1_${it.id}", ee1.id, it.id, 400.0) }

        val ee2 = ExpenseEntity(
            id = "exp_euro_02",
            tripId = trip2Id,
            title = "TGV High-Speed Train to Milan",
            amountOriginal = 520.0,
            originalCurrency = "EUR",
            amountBaseCurrency = 520.0,
            exchangeRate = 1.0,
            category = "Flight",
            payerMemberId = em2.id,
            date = "2026-06-05",
            location = "Gare de Lyon",
            notes = "First class seats with luggage allowance",
            tags = "#Flight,#Transit"
        )
        val ee2Splits = members2.map { ExpenseSplitEntity("se2_${it.id}", ee2.id, it.id, 130.0) }

        val ee3 = ExpenseEntity(
            id = "exp_euro_03",
            tripId = trip2Id,
            title = "Trattoria Pasta & Wine Dinner",
            amountOriginal = 360.0,
            originalCurrency = "USD", // Paid in USD
            amountBaseCurrency = 331.2, // Converted to EUR (360 * 0.92)
            exchangeRate = 0.92,
            category = "Food",
            payerMemberId = em3.id,
            date = "2026-06-08",
            location = "Trastevere, Rome",
            notes = "Paid via US Credit Card ($360 USD)",
            tags = "#Food,#Dinner"
        )
        val ee3Splits = members2.map { ExpenseSplitEntity("se3_${it.id}", ee3.id, it.id, 82.8) }

        tripDao.saveExpenseWithSplits(ee1, ee1Splits)
        tripDao.saveExpenseWithSplits(ee2, ee2Splits)
        tripDao.saveExpenseWithSplits(ee3, ee3Splits)
    }
}
