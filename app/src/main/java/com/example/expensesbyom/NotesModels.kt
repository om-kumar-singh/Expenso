package com.example.expensesbyom

import java.math.BigDecimal
import java.time.LocalDate

enum class TransactionType {
    EXPENSE,
    INCOME
}

data class Transaction(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val originalLine: String
)

data class InvalidLine(
    val lineNumber: Int,
    val originalLine: String,
    val reason: String
)

data class ParseResult(
    val transactions: List<Transaction>,
    val invalidLines: List<InvalidLine>,
    val totalExpenses: BigDecimal,
    val totalIncome: BigDecimal,
    val netAmount: BigDecimal
)
