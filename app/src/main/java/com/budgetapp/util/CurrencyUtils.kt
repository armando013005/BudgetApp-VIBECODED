package com.budgetapp.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun formatAmount(amount: Double): String = currencyFormat.format(amount)

    fun formatAmountSigned(amount: Double): String {
        val formatted = currencyFormat.format(kotlin.math.abs(amount))
        return if (amount < 0) "-$formatted" else "+$formatted"
    }
}
