package com.example

import com.example.data.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripSplitMathUnitTest {

    @Test
    fun testCurrencyConversion() {
        // Test INR to USD and back
        val inrAmount = 8350.0
        val usdAmount = Currency.convert(inrAmount, "INR", "USD")
        assertEquals(100.0, usdAmount, 0.01)

        // Test USD to EUR
        val usd100 = 100.0
        val eurAmount = Currency.convert(usd100, "USD", "EUR")
        assertEquals(92.0, eurAmount, 0.01)

        // Test same currency conversion
        val sameCurr = Currency.convert(500.0, "INR", "INR")
        assertEquals(500.0, sameCurr, 0.01)
    }

    @Test
    fun testCurrencyFormat() {
        val formattedInr = Currency.format(1250.50, "INR")
        assertTrue(formattedInr.contains("₹") && formattedInr.contains("1250.50"))

        val formattedUsd = Currency.format(49.99, "USD")
        assertTrue(formattedUsd.contains("$") && formattedUsd.contains("49.99"))

        val formattedJpy = Currency.format(1500.0, "JPY")
        assertTrue(formattedJpy.contains("¥") && formattedJpy.contains("1500"))
    }

    @Test
    fun testDebtMinimizationAlgorithm() {
        // Member A paid 300, Member B paid 0, Member C paid 0
        // Total = 300, Each share = 100
        // Net balance: A = +200, B = -100, C = -100
        val netBalances = mapOf(
            "A" to 200.0,
            "B" to -100.0,
            "C" to -100.0
        )

        val debtors = netBalances.filter { it.value < -0.01 }.map { it.key to -it.value }.toMutableList()
        val creditors = netBalances.filter { it.value > 0.01 }.map { it.key to it.value }.toMutableList()

        var i = 0
        var j = 0
        var debtCount = 0
        var totalSettled = 0.0

        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]
            val creditor = creditors[j]
            val amount = minOf(debtor.second, creditor.second)

            if (amount > 0.01) {
                debtCount++
                totalSettled += amount
            }

            val remDebtor = debtor.second - amount
            val remCreditor = creditor.second - amount
            debtors[i] = debtor.first to remDebtor
            creditors[j] = creditor.first to remCreditor

            if (remDebtor <= 0.01) i++
            if (remCreditor <= 0.01) j++
        }

        assertEquals(2, debtCount) // B pays A 100, C pays A 100
        assertEquals(200.0, totalSettled, 0.01)
    }
}
