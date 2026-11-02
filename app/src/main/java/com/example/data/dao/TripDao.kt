package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseSplitEntity
import com.example.data.model.MemberEntity
import com.example.data.model.SettlementEntity
import com.example.data.model.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    // Trips
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    fun getTripById(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripByIdDirect(tripId: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Query("DELETE FROM expense_splits WHERE expenseId IN (SELECT id FROM expenses WHERE tripId = :tripId)")
    suspend fun deleteExpenseSplitsForTrip(tripId: String)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteExpensesForTrip(tripId: String)

    @Query("DELETE FROM trip_members WHERE tripId = :tripId")
    suspend fun deleteMembersForTrip(tripId: String)

    @Query("DELETE FROM settlements WHERE tripId = :tripId")
    suspend fun deleteSettlementsForTrip(tripId: String)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripOnly(tripId: String)

    @Transaction
    suspend fun deleteTripCompletely(tripId: String) {
        deleteExpenseSplitsForTrip(tripId)
        deleteExpensesForTrip(tripId)
        deleteMembersForTrip(tripId)
        deleteSettlementsForTrip(tripId)
        deleteTripOnly(tripId)
    }

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String)

    // Members
    @Query("SELECT * FROM trip_members WHERE tripId = :tripId ORDER BY fullName ASC")
    fun getMembersForTrip(tripId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM trip_members WHERE tripId = :tripId ORDER BY fullName ASC")
    suspend fun getMembersForTripDirect(tripId: String): List<MemberEntity>

    @Query("SELECT * FROM trip_members WHERE id = :memberId LIMIT 1")
    suspend fun getMemberById(memberId: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Query("DELETE FROM trip_members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)

    // Expenses
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC, createdAt DESC")
    fun getExpensesForTrip(tripId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC, createdAt DESC")
    suspend fun getExpensesForTripDirect(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    // Splits
    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsForExpense(expenseId: String): Flow<List<ExpenseSplitEntity>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpenseDirect(expenseId: String): List<ExpenseSplitEntity>

    @Query("SELECT * FROM expense_splits WHERE expenseId IN (SELECT id FROM expenses WHERE tripId = :tripId)")
    suspend fun getAllSplitsForTripDirect(tripId: String): List<ExpenseSplitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    // Settlements
    @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY date DESC, createdAt DESC")
    fun getSettlementsForTrip(tripId: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE tripId = :tripId ORDER BY date DESC, createdAt DESC")
    suspend fun getSettlementsForTripDirect(tripId: String): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)

    @Query("DELETE FROM settlements WHERE tripId = :tripId")
    suspend fun clearAllSettlementsForTrip(tripId: String)

    // Composite Transaction
    @Transaction
    suspend fun saveExpenseWithSplits(expense: ExpenseEntity, splits: List<ExpenseSplitEntity>) {
        deleteSplitsForExpense(expense.id)
        insertExpense(expense)
        insertSplits(splits)
    }

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int
}
