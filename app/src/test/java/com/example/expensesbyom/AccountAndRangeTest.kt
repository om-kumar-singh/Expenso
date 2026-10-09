package com.example.expensesbyom

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountAndRangeTest {

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
    fun existingRecordsStayOnDefaultAccount() {
        repository.saveMonth(2026, 9, "11- milk-46")
        val default = repository.ensureDefaultAccount()
        assertEquals(ExpenseDatabase.DEFAULT_ACCOUNT_NAME, default.name)
        assertTrue(default.isDefault)
        val txs = database.transactionDao().getForMonth(default.id, 2026, 9)
        assertEquals(1, txs.size)
        assertEquals(default.id, txs[0].accountId)
        assertEquals("milk", txs[0].description)
    }

    @Test
    fun accountsIsolateNotesAndTransactions() {
        val mother = repository.createAccount("Mother's Account")
        repository.saveMonth(defaultId, 2026, 9, "11- milk-46")
        repository.saveMonth(mother.id, 2026, 9, "11- medicine-200\nhelp=+50")
        assertEquals("11- milk-46", repository.loadNotes(defaultId, 2026, 9))
        assertEquals(1, repository.loadTransactions(defaultId, 2026, 9).size)
        assertEquals(2, repository.loadTransactions(mother.id, 2026, 9).size)
        assertEquals(BigDecimal("46"), repository.loadSummary(listOf(defaultId), 2026, 9).totalExpenses)
        assertEquals(BigDecimal("200"), repository.loadSummary(listOf(mother.id), 2026, 9).totalExpenses)
    }

    @Test
    fun cannotDeleteDefaultAccount() {
        assertFalse(repository.deleteAccount(defaultId))
        assertNotNull(repository.ensureDefaultAccount())
    }

    @Test
    fun combinedTotalsDoNotDoubleCount() {
        val travel = repository.createAccount("Travel")
        repository.saveMonth(defaultId, 2026, 9, "15- taxi-100")
        repository.saveMonth(travel.id, 2026, 9, "16- hotel-900\nrefund=+50")
        val combined = repository.loadSummary(listOf(defaultId, travel.id), 2026, 9)
        assertEquals(BigDecimal("1000"), combined.totalExpenses)
        assertEquals(BigDecimal("50"), combined.totalIncome)
        assertEquals(BigDecimal("-950"), combined.netAmount)
        assertEquals(3, combined.transactionCount)
        val dated = repository.loadDatedTransactions(listOf(defaultId, travel.id), 2026, 9)
        assertEquals(3, dated.size)
        assertEquals(1, dated.count { it.accountId == defaultId })
        assertEquals(2, dated.count { it.accountId == travel.id })
    }

    @Test
    fun singleDayDateRange() {
        repository.saveMonth(defaultId, 2026, 10, "14- snack-10\n15- lunch-120\n16- tea-20")
        val report = repository.reportForRange(
            LocalDate.of(2026, 10, 15),
            LocalDate.of(2026, 10, 15),
            listOf(defaultId)
        )
        assertEquals(1, report.overall.transactionCount)
        assertEquals(BigDecimal("120"), report.overall.totalExpenses)
        assertEquals(LocalDate.of(2026, 10, 15), report.transactions[0].transaction.date)
    }

    @Test
    fun inclusiveMultiDayRange() {
        repository.saveMonth(
            defaultId,
            2026,
            9,
            "15- a-10\n16- b-20\n19- c-30\n20- d-40"
        )
        val report = repository.reportForRange(
            LocalDate.of(2026, 9, 15),
            LocalDate.of(2026, 9, 19),
            listOf(defaultId)
        )
        assertEquals(3, report.overall.transactionCount)
        assertEquals(BigDecimal("60"), report.overall.totalExpenses)
    }

    @Test
    fun rangeCrossingMonthBoundary() {
        repository.saveMonth(defaultId, 2026, 9, "30- a-15")
        repository.saveMonth(defaultId, 2026, 10, "1- b-25\n9- c-5")
        val report = repository.reportForRange(
            LocalDate.of(2026, 9, 30),
            LocalDate.of(2026, 10, 9),
            listOf(defaultId)
        )
        assertEquals(3, report.overall.transactionCount)
        assertEquals(BigDecimal("45"), report.overall.totalExpenses)
    }

    @Test
    fun perAccountTotalsInCombinedRange() {
        val mother = repository.createAccount("Mother's Account")
        repository.saveMonth(defaultId, 2026, 10, "1- lunch-80")
        repository.saveMonth(mother.id, 2026, 10, "2- meds-40\ngift=+10")
        val report = repository.reportForRange(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 9),
            listOf(defaultId, mother.id)
        )
        assertEquals(BigDecimal("120"), report.overall.totalExpenses)
        assertEquals(BigDecimal("10"), report.overall.totalIncome)
        assertEquals(BigDecimal("-110"), report.overall.netAmount)
        val mine = report.perAccount.first { it.accountId == defaultId }
        val hers = report.perAccount.first { it.accountId == mother.id }
        assertEquals(BigDecimal("80"), mine.totals.totalExpenses)
        assertEquals(BigDecimal("40"), hers.totals.totalExpenses)
        assertEquals(BigDecimal("10"), hers.totals.totalIncome)
    }

    @Test
    fun decimalAmountsAndIncomeNotTreatedAsExpense() {
        repository.saveMonth(defaultId, 2026, 9, "1- tea-12.50\nbonus=+1.25")
        val summary = repository.loadSummary(defaultId.let { listOf(it) }, 2026, 9)
        assertEquals(BigDecimal("12.50"), summary.totalExpenses)
        assertEquals(BigDecimal("1.25"), summary.totalIncome)
        assertEquals(BigDecimal("-11.25"), summary.netAmount)
    }

    @Test
    fun recalculateDoesNotDuplicateAcrossAccounts() {
        val other = repository.createAccount("Household")
        repository.saveMonth(defaultId, 2026, 9, "11- milk-46")
        repository.saveMonth(other.id, 2026, 9, "11- rice-30")
        repository.saveMonth(defaultId, 2026, 9, "11- milk-50")
        assertEquals(1, repository.loadTransactions(defaultId, 2026, 9).size)
        assertEquals(1, repository.loadTransactions(other.id, 2026, 9).size)
        assertEquals(BigDecimal("50"), repository.loadTransactions(defaultId, 2026, 9)[0].amount)
    }

    @Test
    fun invalidDateRangeIsRejected() {
        assertEquals(
            DateRangeCalculator.ERROR_START_AFTER_END,
            DateRangeCalculator.validate(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 15))
        )
        assertNull(DateRangeCalculator.validate(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15)))
    }
}
