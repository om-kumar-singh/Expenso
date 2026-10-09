package com.example.expensesbyom

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormat {
    private val symbols = DecimalFormatSymbols(Locale.US)

    fun rupees(amount: BigDecimal): String {
        val scaled = amount.abs().setScale(2, RoundingMode.HALF_UP)
        val pattern = if (scaled.stripTrailingZeros().scale() <= 0) "#,##0" else "#,##0.00"
        val formatted = DecimalFormat(pattern, symbols).format(scaled)
        val withRupee = "₹$formatted"
        return if (amount.signum() < 0) "-$withRupee" else withRupee
    }
}
