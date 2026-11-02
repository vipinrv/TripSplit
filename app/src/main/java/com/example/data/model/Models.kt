package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class Currency(val code: String, val symbol: String, val nameLabel: String, val usdRate: Double) {
    INR("INR", "₹", "Indian Rupee", 83.50),
    USD("USD", "$", "US Dollar", 1.00),
    EUR("EUR", "€", "Euro", 0.92),
    GBP("GBP", "£", "British Pound", 0.78),
    JPY("JPY", "¥", "Japanese Yen", 155.00),
    CAD("CAD", "C$", "Canadian Dollar", 1.36),
    AUD("AUD", "A$", "Australian Dollar", 1.50),
    SGD("SGD", "S$", "Singapore Dollar", 1.35),
    AED("AED", "AED", "UAE Dirham", 3.67);

    companion object {
        fun fromCode(code: String): Currency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }

        fun convert(amount: Double, fromCode: String, toCode: String): Double {
            val fromCurrency = fromCode(fromCode)
            val toCurrency = fromCode(toCode)
            if (fromCurrency == toCurrency) return amount
            val amountInUsd = amount / fromCurrency.usdRate
            return amountInUsd * toCurrency.usdRate
        }

        fun format(amount: Double, currencyCode: String): String {
            val curr = fromCode(currencyCode)
            return if (curr == JPY) {
                "${curr.symbol}${String.format("%.0f", amount)}"
            } else {
                "${curr.symbol}${String.format("%.2f", amount)}"
            }
        }
    }
}

enum class ExpenseCategory(val id: String, val displayName: String, val colorHex: String) {
    FOOD("Food", "Food & Dining", "#F59E0B"),
    STAY("Stay", "Accommodation", "#8B5CF6"),
    FUEL("Fuel", "Fuel & Transit", "#EF4444"),
    FLIGHT("Flight", "Flights & Travel", "#3B82F6"),
    ACTIVITY("Activity", "Activities & Sightseeing", "#10B981"),
    SHOPPING("Shopping", "Shopping", "#EC4899"),
    MISC("Misc", "Miscellaneous", "#6B7280");

    companion object {
        fun fromId(id: String): ExpenseCategory {
            return entries.firstOrNull { 
                it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) 
            } ?: MISC
        }
    }
}

data class DetailedExpense(
    val expense: ExpenseEntity,
    val payerName: String,
    val payerNickname: String?,
    val splits: List<ExpenseSplitWithMember>
)

data class ExpenseSplitWithMember(
    val split: ExpenseSplitEntity,
    val memberName: String
)

data class MemberBalance(
    val memberId: String,
    val memberName: String,
    val nickname: String?,
    val avatarColorHex: String,
    val totalPaidBase: Double,
    val totalShareBase: Double,
    val netBalanceBase: Double // > 0 means gets back, < 0 means owes
)

data class DebtPair(
    val fromMemberId: String,
    val fromName: String,
    val toMemberId: String,
    val toName: String,
    val amountBase: Double
)

data class SettlementStatusSummary(
    val isFullySettled: Boolean,
    val unsettledCount: Int,
    val totalPendingDebtBase: Double,
    val pairs: List<DebtPair>
)

data class CategorySpending(
    val category: ExpenseCategory,
    val totalAmountBase: Double,
    val percentage: Double
)

data class MemberSpending(
    val memberId: String,
    val memberName: String,
    val paidAmountBase: Double,
    val shareAmountBase: Double
)
