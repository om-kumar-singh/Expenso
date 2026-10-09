package com.example.expensesbyom

import java.math.BigDecimal

data class MonthSummary(
    val year: Int,
    val month: Int,
    val totalExpenses: BigDecimal,
    val totalIncome: BigDecimal,
    val netAmount: BigDecimal,
    val transactionCount: Int
)

data class MoneyTotals(
    val totalExpenses: BigDecimal,
    val totalIncome: BigDecimal,
    val netAmount: BigDecimal,
    val transactionCount: Int
)

data class AccountTotals(
    val accountId: Long,
    val accountName: String,
    val totals: MoneyTotals
)

data class DatedTransaction(
    val accountId: Long,
    val accountName: String,
    val transaction: Transaction
)

object MonthTotals {
    fun of(year: Int, month: Int, transactions: List<Transaction>): MonthSummary {
        val money = of(transactions)
        return MonthSummary(
            year = year,
            month = month,
            totalExpenses = money.totalExpenses,
            totalIncome = money.totalIncome,
            netAmount = money.netAmount,
            transactionCount = money.transactionCount
        )
    }

    fun of(transactions: List<Transaction>): MoneyTotals {
        val expenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        return MoneyTotals(
            totalExpenses = expenses,
            totalIncome = income,
            netAmount = income.subtract(expenses),
            transactionCount = transactions.size
        )
    }
}
