package com.example.expensesbyom

import java.time.LocalDate

object DateRangeCalculator {
    const val ERROR_START_AFTER_END = "Start date cannot be after the end date"

    fun dateKey(date: LocalDate): Int {
        return date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
    }

    fun validate(start: LocalDate, end: LocalDate): String? {
        return if (start.isAfter(end)) ERROR_START_AFTER_END else null
    }

    data class Report(
        val start: LocalDate,
        val end: LocalDate,
        val overall: MoneyTotals,
        val perAccount: List<AccountTotals>,
        val transactions: List<DatedTransaction>
    )
}
