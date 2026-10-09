package com.example.expensesbyom

import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

/**
 * Parses Notes-style expense text for a selected month/year.
 *
 * Day lines: `11-` or `11- milk-46`. Later lines inherit that day.
 * Expense: `milk-46`. Income: `Prince=+1500`, `Prince +1500`, `Prince-+1500`.
 */
object NotesParser {

    const val REASON_MISSING_AMOUNT = "Missing amount"
    const val REASON_INVALID_AMOUNT = "Invalid amount"
    const val REASON_MISSING_DESCRIPTION = "Missing description"
    const val REASON_NO_DAY = "No day specified yet"
    const val REASON_INVALID_DAY = "Invalid day for the selected month"
    const val REASON_INVALID_MONTH = "Invalid month/year"

    private val dayPrefix = Regex("""^(\d{1,2})\s*-\s*(.*)$""")
    private val incomeEqualsPlus = Regex("""^(.+?)\s*=\s*\+\s*(\d+(?:\.\d+)?)\s*$""")
    private val incomeHyphenPlus = Regex("""^(.+?)\s*-\s*\+\s*(\d+(?:\.\d+)?)\s*$""")
    private val incomeSpacePlus = Regex("""^(.+?)\s+\+\s*(\d+(?:\.\d+)?)\s*$""")
    private val expenseHyphen = Regex("""^(.+?)\s*-\s*(\d+(?:\.\d+)?)\s*$""")

    fun parse(text: String, year: Int, month: Int): ParseResult {
        val yearMonth = try {
            YearMonth.of(year, month)
        } catch (_: DateTimeException) {
            return ParseResult(
                transactions = emptyList(),
                invalidLines = listOf(InvalidLine(1, text, REASON_INVALID_MONTH)),
                totalExpenses = BigDecimal.ZERO,
                totalIncome = BigDecimal.ZERO,
                netAmount = BigDecimal.ZERO
            )
        }

        val transactions = mutableListOf<Transaction>()
        val invalidLines = mutableListOf<InvalidLine>()
        var currentDate: LocalDate? = null

        val rawLines = text.split('\n')
        for (index in rawLines.indices) {
            val lineNumber = index + 1
            val originalLine = rawLines[index].trimEnd('\r')
            val line = originalLine.trim()
            if (line.isEmpty()) continue

            val dayMatch = dayPrefix.matchEntire(line)
            if (dayMatch != null) {
                val day = dayMatch.groupValues[1].toInt()
                val remainder = dayMatch.groupValues[2].trim()
                val date = dateOrNull(yearMonth, day)
                if (date == null) {
                    invalidLines += InvalidLine(lineNumber, originalLine, REASON_INVALID_DAY)
                    continue
                }
                currentDate = date
                if (remainder.isEmpty()) continue
                parseTransaction(remainder, date, lineNumber, originalLine, transactions, invalidLines)
                continue
            }

            val date = currentDate
            if (date == null) {
                invalidLines += InvalidLine(lineNumber, originalLine, REASON_NO_DAY)
                continue
            }
            parseTransaction(line, date, lineNumber, originalLine, transactions, invalidLines)
        }

        val totalExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(t.amount) }
        val netAmount = totalIncome.subtract(totalExpenses)

        return ParseResult(
            transactions = transactions,
            invalidLines = invalidLines,
            totalExpenses = totalExpenses,
            totalIncome = totalIncome,
            netAmount = netAmount
        )
    }

    private fun dateOrNull(yearMonth: YearMonth, day: Int): LocalDate? {
        if (day < 1 || day > yearMonth.lengthOfMonth()) return null
        return yearMonth.atDay(day)
    }

    private fun parseTransaction(
        content: String,
        date: LocalDate,
        lineNumber: Int,
        originalLine: String,
        transactions: MutableList<Transaction>,
        invalidLines: MutableList<InvalidLine>
    ) {
        val income = matchIncome(content)
        if (income != null) {
            val (description, amount) = income
            if (description.isEmpty()) {
                invalidLines += InvalidLine(lineNumber, originalLine, REASON_MISSING_DESCRIPTION)
                return
            }
            transactions += Transaction(
                date = date,
                description = description,
                amount = amount,
                type = TransactionType.INCOME,
                originalLine = originalLine
            )
            return
        }

        val expenseMatch = expenseHyphen.matchEntire(content)
        if (expenseMatch != null) {
            val description = expenseMatch.groupValues[1].trim()
            val amount = parseAmount(expenseMatch.groupValues[2])
            if (description.isEmpty()) {
                invalidLines += InvalidLine(lineNumber, originalLine, REASON_MISSING_DESCRIPTION)
                return
            }
            if (amount == null) {
                invalidLines += InvalidLine(lineNumber, originalLine, REASON_INVALID_AMOUNT)
                return
            }
            transactions += Transaction(
                date = date,
                description = description,
                amount = amount,
                type = TransactionType.EXPENSE,
                originalLine = originalLine
            )
            return
        }

        invalidLines += InvalidLine(lineNumber, originalLine, classifyInvalid(content))
    }

    /**
     * Specific reasons for lines that are not valid income/expense.
     * Does not change which lines succeed as valid transactions.
     */
    private fun classifyInvalid(content: String): String {
        val dash = content.lastIndexOf('-')
        if (dash < 0) return REASON_MISSING_AMOUNT
        val after = content.substring(dash + 1).trim()
        if (after.isEmpty()) return REASON_MISSING_AMOUNT
        val numericPart = if (after.startsWith("+")) after.substring(1) else after
        if (parseAmount(numericPart) == null) return REASON_INVALID_AMOUNT
        if (content.substring(0, dash).trim().isEmpty()) return REASON_MISSING_DESCRIPTION
        return REASON_MISSING_AMOUNT
    }

    private fun matchIncome(content: String): Pair<String, BigDecimal>? {
        val match = incomeEqualsPlus.matchEntire(content)
            ?: incomeHyphenPlus.matchEntire(content)
            ?: incomeSpacePlus.matchEntire(content)
            ?: return null
        val description = match.groupValues[1].trim()
        val amount = parseAmount(match.groupValues[2]) ?: return null
        return description to amount
    }

    private fun parseAmount(raw: String): BigDecimal? {
        return try {
            BigDecimal(raw)
        } catch (_: NumberFormatException) {
            null
        }
    }
}
