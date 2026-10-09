package com.example.expensesbyom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class NotesParserTest {

    private val year = 2026
    private val month = 9

    @Test
    fun sampleNotes_expensesIncomeAndInheritedDates() {
        val text = """
            11- milk-46
            dosa-120
            samosa-10
            12- charity-51
            didi-1000
            prince=+1500
        """.trimIndent()

        val result = NotesParser.parse(text, year, month)

        assertEquals(6, result.transactions.size)
        assertTrue(result.invalidLines.isEmpty())
        assertEquals(BigDecimal("1227"), result.totalExpenses)
        assertEquals(BigDecimal("1500"), result.totalIncome)
        assertEquals(BigDecimal("273"), result.netAmount)

        assertEquals(LocalDate.of(2026, Month.SEPTEMBER, 11), result.transactions[0].date)
        assertEquals("milk", result.transactions[0].description)
        assertEquals(TransactionType.EXPENSE, result.transactions[0].type)
        assertEquals(LocalDate.of(2026, Month.SEPTEMBER, 11), result.transactions[1].date)
        assertEquals("dosa", result.transactions[1].description)
        assertEquals(LocalDate.of(2026, Month.SEPTEMBER, 12), result.transactions[3].date)
        assertEquals("prince", result.transactions[5].description)
        assertEquals(TransactionType.INCOME, result.transactions[5].type)
        assertEquals(BigDecimal("1500"), result.transactions[5].amount)
    }

    @Test
    fun sameLineDateAndTransaction() {
        val result = NotesParser.parse("11- milk-46", year, month)
        assertEquals(1, result.transactions.size)
        assertEquals(LocalDate.of(2026, 9, 11), result.transactions[0].date)
        assertEquals("milk", result.transactions[0].description)
        assertEquals(BigDecimal("46"), result.transactions[0].amount)
        assertEquals(TransactionType.EXPENSE, result.transactions[0].type)
    }

    @Test
    fun incomeVariants() {
        val text = """
            1-
            Prince=+1500
            Prince +200
            Prince-+50
        """.trimIndent()
        val result = NotesParser.parse(text, year, month)
        assertEquals(3, result.transactions.size)
        assertTrue(result.transactions.all { it.type == TransactionType.INCOME })
        assertEquals(BigDecimal("1750"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("1750"), result.netAmount)
    }

    @Test
    fun inheritedDates_multipleDays() {
        val text = """
            11-
            milk-46
            13-
            auto-196
            Dudh-46
        """.trimIndent()
        val result = NotesParser.parse(text, year, month)
        assertEquals(LocalDate.of(2026, 9, 11), result.transactions[0].date)
        assertEquals(LocalDate.of(2026, 9, 13), result.transactions[1].date)
        assertEquals(LocalDate.of(2026, 9, 13), result.transactions[2].date)
        assertEquals("Dudh", result.transactions[2].description)
    }

    @Test
    fun invalidLineDoesNotStopParsing() {
        val text = """
            11- milk-46
            this is broken
            dosa-120
        """.trimIndent()
        val result = NotesParser.parse(text, year, month)
        assertEquals(2, result.transactions.size)
        assertEquals(1, result.invalidLines.size)
        assertEquals("this is broken", result.invalidLines[0].originalLine.trim())
        assertEquals(BigDecimal("166"), result.totalExpenses)
    }

    @Test
    fun transactionBeforeDateIsInvalid() {
        val result = NotesParser.parse("milk-46\n11- dosa-120", year, month)
        assertEquals(1, result.invalidLines.size)
        assertEquals(1, result.transactions.size)
        assertEquals("dosa", result.transactions[0].description)
    }

    @Test
    fun negativeNetWhenExpensesExceedIncome() {
        val text = """
            11- rent-1000
            snack-50
        """.trimIndent()
        val result = NotesParser.parse(text, year, month)
        assertEquals(BigDecimal("1050"), result.totalExpenses)
        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal("-1050"), result.netAmount)
    }

    @Test
    fun descriptionWithSpacesAndUserSample() {
        val text = """
            11- dudh-46
            Dosa-120
            Samosa-10
            12- charity-51
            didi-1000
            underwear-229
            didi food-190
            13- auto-196
            Dudh-46
            Masala-30
            Prince=+1500
        """.trimIndent()
        val result = NotesParser.parse(text, year, month)
        assertTrue(result.invalidLines.isEmpty())
        assertEquals(11, result.transactions.size)
        assertEquals("didi food", result.transactions[6].description)
        val expenses = BigDecimal("46")
            .add(BigDecimal("120"))
            .add(BigDecimal("10"))
            .add(BigDecimal("51"))
            .add(BigDecimal("1000"))
            .add(BigDecimal("229"))
            .add(BigDecimal("190"))
            .add(BigDecimal("196"))
            .add(BigDecimal("46"))
            .add(BigDecimal("30"))
        assertEquals(expenses, result.totalExpenses)
        assertEquals(BigDecimal("1500"), result.totalIncome)
        assertEquals(expenses.negate().add(BigDecimal("1500")), result.netAmount)
    }

    @Test
    fun blankLinesIgnored() {
        val result = NotesParser.parse("11- milk-46\n\n\ndosa-10", year, month)
        assertEquals(2, result.transactions.size)
        assertTrue(result.invalidLines.isEmpty())
    }

    @Test
    fun decimalAmountsUseBigDecimal() {
        val result = NotesParser.parse("1- tea-12.50\nbonus=+1.25", year, month)
        assertEquals(BigDecimal("12.50"), result.transactions[0].amount)
        assertEquals(BigDecimal("1.25"), result.transactions[1].amount)
        assertEquals(BigDecimal("-11.25"), result.netAmount)
    }

    @Test
    fun invalidLineMissingAmount() {
        val result = NotesParser.parse("11- milk-46\ndidi food", year, month)
        assertEquals(1, result.transactions.size)
        assertEquals(1, result.invalidLines.size)
        assertEquals(2, result.invalidLines[0].lineNumber)
        assertEquals("didi food", result.invalidLines[0].originalLine.trim())
        assertEquals(NotesParser.REASON_MISSING_AMOUNT, result.invalidLines[0].reason)
        assertEquals(BigDecimal("46"), result.totalExpenses)
    }

    @Test
    fun invalidLineTrailingHyphenIsMissingAmount() {
        val result = NotesParser.parse("11- milk-", year, month)
        assertEquals(0, result.transactions.size)
        assertEquals(1, result.invalidLines.size)
        assertEquals(NotesParser.REASON_MISSING_AMOUNT, result.invalidLines[0].reason)
        assertEquals(1, result.invalidLines[0].lineNumber)
    }

    @Test
    fun invalidLineInvalidAmount() {
        val result = NotesParser.parse("11- abc-xyz", year, month)
        assertEquals(0, result.transactions.size)
        assertEquals(1, result.invalidLines.size)
        assertEquals(1, result.invalidLines[0].lineNumber)
        assertEquals("11- abc-xyz", result.invalidLines[0].originalLine.trim())
        assertEquals(NotesParser.REASON_INVALID_AMOUNT, result.invalidLines[0].reason)
    }

    @Test
    fun transactionBeforeDayHasLineNumberOne() {
        val result = NotesParser.parse("milk-46\n11- dosa-120", year, month)
        assertEquals(1, result.invalidLines.size)
        assertEquals(1, result.invalidLines[0].lineNumber)
        assertEquals(NotesParser.REASON_NO_DAY, result.invalidLines[0].reason)
        assertEquals("milk-46", result.invalidLines[0].originalLine.trim())
        assertEquals(1, result.transactions.size)
    }

    @Test
    fun multipleInvalidLinesKeepValidTotals() {
        val text = """
            11- milk-46
            didi food
            dosa-120
            abc-xyz
            milk-
        """.trimIndent()
        val result = NotesParser.parse(text, year, month)
        assertEquals(2, result.transactions.size)
        assertEquals(3, result.invalidLines.size)
        assertEquals(2, result.invalidLines[0].lineNumber)
        assertEquals(NotesParser.REASON_MISSING_AMOUNT, result.invalidLines[0].reason)
        assertEquals(4, result.invalidLines[1].lineNumber)
        assertEquals(NotesParser.REASON_INVALID_AMOUNT, result.invalidLines[1].reason)
        assertEquals(5, result.invalidLines[2].lineNumber)
        assertEquals(NotesParser.REASON_MISSING_AMOUNT, result.invalidLines[2].reason)
        assertEquals(BigDecimal("166"), result.totalExpenses)
    }

    @Test
    fun lineNumbersAreOneBasedAndCountBlankLines() {
        val text = "11- milk-46\n\n\ndidi food\n\nabc-xyz"
        val result = NotesParser.parse(text, year, month)
        assertEquals(1, result.transactions.size)
        assertEquals(2, result.invalidLines.size)
        assertEquals(4, result.invalidLines[0].lineNumber)
        assertEquals("didi food", result.invalidLines[0].originalLine.trim())
        assertEquals(NotesParser.REASON_MISSING_AMOUNT, result.invalidLines[0].reason)
        assertEquals(6, result.invalidLines[1].lineNumber)
        assertEquals("abc-xyz", result.invalidLines[1].originalLine.trim())
        assertEquals(NotesParser.REASON_INVALID_AMOUNT, result.invalidLines[1].reason)
    }
}
