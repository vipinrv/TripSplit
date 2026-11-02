package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val baseCurrency: String,
    val isManuallySettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "trip_members",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class MemberEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val fullName: String,
    val nickname: String? = null,
    val avatarColorHex: String = "#0D9488"
) {
    fun getDisplayName(): String {
        return nickname?.takeIf { it.isNotBlank() } ?: fullName.ifBlank { "Member" }
    }
}

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId"), Index("payerMemberId")]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val title: String,
    val amountOriginal: Double,
    val originalCurrency: String,
    val amountBaseCurrency: Double,
    val exchangeRate: Double = 1.0,
    val category: String, // Food, Stay, Fuel, Flight, Activity, Shopping, Misc
    val payerMemberId: String,
    val date: String, // YYYY-MM-DD
    val location: String = "",
    val notes: String = "",
    val tags: String = "", // Comma-separated tags e.g. "#Stay,#Food"
    val receiptAttachmentPath: String? = null,
    val splitType: String = "EQUAL", // EQUAL, CUSTOM
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId"), Index("memberId")]
)
data class ExpenseSplitEntity(
    @PrimaryKey
    val id: String,
    val expenseId: String,
    val memberId: String,
    val amountBase: Double,
    val isCustomSplit: Boolean = false
)

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId"), Index("fromMemberId"), Index("toMemberId")]
)
data class SettlementEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountBase: Double,
    val date: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
