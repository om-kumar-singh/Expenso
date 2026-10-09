package com.example.expensesbyom

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MonthPersistenceTest {

    private lateinit var database: ExpenseDatabase
    private lateinit var repository: MonthRepository
    private var defaultId: Long = 0

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = database.repository()
        defaultId = repository.ensureDefaultAccount().id
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveParsedTransactionsFromNotes() {
        val notes = """
            11- milk-46
            dosa-120
            samosa-10
            12- charity-51
            didi-1000
            prince=+1500
        """.trimIndent()
        repository.saveMonth(2026, 9, notes)
        val txs = repository.loadTransactions(2026, 9)
        assertEquals(6, txs.size)
        assertEquals("milk", txs[0].description)
        assertEquals(TransactionType.EXPENSE, txs[0].type)
        assertEquals(BigDecimal("46"), txs[0].amount)
        assertEquals("prince", txs[5].description)
        assertEquals(TransactionType.INCOME, txs[5].type)
    }

    @Test
    fun recalculatingReplacesTransactionsInsteadOfDuplicating() {
        repository.saveMonth(2026, 9, "11- milk-46")
        repository.saveMonth(2026, 9, "11- milk-50\ndosa-120")
        val txs = repository.loadTransactions(2026, 9)
        assertEquals(2, txs.size)
        assertEquals("milk", txs[0].description)
        assertEquals(BigDecimal("50"), txs[0].amount)
        assertEquals(2, database.transactionDao().countForMonth(defaultId, 2026, 9))
        assertEquals("11- milk-50\ndosa-120", repository.loadNotes(2026, 9))
    }

    @Test
    fun septemberAndOctoberStaySeparate() {
        repository.saveMonth(2026, 9, "11- milk-46\nprince=+100")
        repository.saveMonth(2026, 10, "1- rent-8000")
        assertEquals(2, repository.loadTransactions(2026, 9).size)
        assertEquals(1, repository.loadTransactions(2026, 10).size)
        assertEquals("milk", repository.loadTransactions(2026, 9)[0].description)
        assertEquals("rent", repository.loadTransactions(2026, 10)[0].description)
    }

    @Test
    fun expenseIncomeNetAndCount() {
        repository.saveMonth(
            2026,
            9,
            """
            11- milk-46
            dosa-120
            prince=+1500
            """.trimIndent()
        )
        val summary = repository.loadSummary(2026, 9)
        assertEquals(BigDecimal("166"), summary.totalExpenses)
        assertEquals(BigDecimal("1500"), summary.totalIncome)
        assertEquals(BigDecimal("1334"), summary.netAmount)
        assertEquals(3, summary.transactionCount)
    }

    @Test
    fun summariesNewestMonthFirst() {
        repository.saveMonth(2026, 9, "11- milk-46")
        repository.saveMonth(2026, 10, "1- rent-100")
        val summaries = repository.loadSummaries()
        assertEquals(2, summaries.size)
        assertEquals(10, summaries[0].month)
        assertEquals(9, summaries[1].month)
        assertEquals(1, summaries[0].transactionCount)
        assertEquals(1, summaries[1].transactionCount)
    }

    @Test
    fun negativeNetFromStoredTransactions() {
        repository.saveMonth(2026, 9, "11- rent-1000\nsnack-50")
        val summary = repository.loadSummary(2026, 9)
        assertEquals(BigDecimal("1050"), summary.totalExpenses)
        assertEquals(BigDecimal.ZERO, summary.totalIncome)
        assertEquals(BigDecimal("-1050"), summary.netAmount)
        assertTrue(summary.transactionCount == 2)
    }
}
