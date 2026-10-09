package com.example.expensesbyom

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class MonthRepository(
    private val accountDao: AccountDao,
    private val notesDao: MonthlyNotesDao,
    private val transactionDao: TransactionDao
) {
    fun ensureDefaultAccount(): AccountEntity {
        accountDao.getDefault()?.let { return it }
        val id = accountDao.insert(
            AccountEntity(name = ExpenseDatabase.DEFAULT_ACCOUNT_NAME, isDefault = true)
        )
        return accountDao.getById(id) ?: AccountEntity(
            id = id,
            name = ExpenseDatabase.DEFAULT_ACCOUNT_NAME,
            isDefault = true
        )
    }

    fun listAccounts(): List<AccountEntity> {
        ensureDefaultAccount()
        return accountDao.getAll()
    }

    fun createAccount(name: String): AccountEntity {
        ensureDefaultAccount()
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Account name is required" }
        val id = accountDao.insert(AccountEntity(name = trimmed, isDefault = false))
        return accountDao.getById(id)!!
    }

    fun renameAccount(id: Long, name: String) {
        val account = accountDao.getById(id) ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        accountDao.update(account.copy(name = trimmed))
    }

    fun deleteAccount(id: Long): Boolean {
        val account = accountDao.getById(id) ?: return false
        if (account.isDefault) return false
        notesDao.deleteAllForAccount(id)
        transactionDao.deleteAllForAccount(id)
        return accountDao.deleteIfNotDefault(id) > 0
    }

    fun saveMonth(year: Int, month: Int, notesText: String): ParseResult {
        return saveMonth(ensureDefaultAccount().id, year, month, notesText)
    }

    fun saveMonth(accountId: Long, year: Int, month: Int, notesText: String): ParseResult {
        ensureDefaultAccount()
        val result = NotesParser.parse(notesText, year, month)
        notesDao.upsertNotes(
            MonthlyNotesEntity(
                accountId = accountId,
                year = year,
                month = month,
                notesText = notesText
            )
        )
        transactionDao.deleteForMonth(accountId, year, month)
        val entities = result.transactions.map { it.toEntity(accountId) }
        if (entities.isNotEmpty()) {
            transactionDao.insertAll(entities)
        }
        return result
    }

    fun loadNotes(year: Int, month: Int): String {
        return loadNotes(ensureDefaultAccount().id, year, month)
    }

    fun loadNotes(accountId: Long, year: Int, month: Int): String {
        return notesDao.getNotes(accountId, year, month)?.notesText.orEmpty()
    }

    fun loadTransactions(year: Int, month: Int): List<Transaction> {
        return loadTransactions(ensureDefaultAccount().id, year, month)
    }

    fun loadTransactions(accountId: Long, year: Int, month: Int): List<Transaction> {
        return transactionDao.getForMonth(accountId, year, month).map { it.toTransaction() }
    }

    fun loadDatedTransactions(accountIds: List<Long>, year: Int, month: Int): List<DatedTransaction> {
        if (accountIds.isEmpty()) return emptyList()
        val names = accountNames()
        accountIds.forEach { ensureParsed(it, year, month) }
        return transactionDao.getForMonth(accountIds, year, month).map { entity ->
            DatedTransaction(
                accountId = entity.accountId,
                accountName = names[entity.accountId] ?: "Account",
                transaction = entity.toTransaction()
            )
        }
    }

    fun loadSummary(year: Int, month: Int): MonthSummary {
        return loadSummary(listOf(ensureDefaultAccount().id), year, month)
    }

    fun loadSummary(accountIds: List<Long>, year: Int, month: Int): MonthSummary {
        val txs = loadDatedTransactions(accountIds, year, month).map { it.transaction }
        return MonthTotals.of(year, month, txs)
    }

    fun loadSummaries(): List<MonthSummary> {
        return loadSummaries(listOf(ensureDefaultAccount().id))
    }

    fun loadSummaries(accountIds: List<Long>): List<MonthSummary> {
        if (accountIds.isEmpty()) return emptyList()
        val months = notesDao.getNotesForAccounts(accountIds)
            .filter { it.notesText.isNotBlank() }
            .map { YearMonth.of(it.year, it.month) }
            .distinct()
            .sortedDescending()
        return months.map { ym ->
            loadSummary(accountIds, ym.year, ym.monthValue)
        }
    }

    fun reportForRange(
        start: LocalDate,
        end: LocalDate,
        accountIds: List<Long>
    ): DateRangeCalculator.Report {
        val error = DateRangeCalculator.validate(start, end)
        require(error == null) { error!! }
        if (accountIds.isEmpty()) {
            return DateRangeCalculator.Report(
                start = start,
                end = end,
                overall = MonthTotals.of(emptyList()),
                perAccount = emptyList(),
                transactions = emptyList()
            )
        }
        seedParsedNotesInRange(start, end, accountIds)
        val names = accountNames()
        val entities = transactionDao.getInRange(
            accountIds,
            DateRangeCalculator.dateKey(start),
            DateRangeCalculator.dateKey(end)
        )
        val dated = entities.map { entity ->
            DatedTransaction(
                accountId = entity.accountId,
                accountName = names[entity.accountId] ?: "Account",
                transaction = entity.toTransaction()
            )
        }
        val overall = MonthTotals.of(dated.map { it.transaction })
        val perAccount = accountIds.map { id ->
            val accountTx = dated.filter { it.accountId == id }.map { it.transaction }
            AccountTotals(id, names[id] ?: "Account", MonthTotals.of(accountTx))
        }
        return DateRangeCalculator.Report(
            start = start,
            end = end,
            overall = overall,
            perAccount = perAccount,
            transactions = dated
        )
    }

    private fun seedParsedNotesInRange(start: LocalDate, end: LocalDate, accountIds: List<Long>) {
        var cursor = YearMonth.from(start)
        val last = YearMonth.from(end)
        while (!cursor.isAfter(last)) {
            accountIds.forEach { ensureParsed(it, cursor.year, cursor.monthValue) }
            cursor = cursor.plusMonths(1)
        }
    }

    private fun accountNames(): Map<Long, String> {
        return listAccounts().associate { it.id to it.name }
    }

    private fun ensureParsed(accountId: Long, year: Int, month: Int) {
        val notes = notesDao.getNotes(accountId, year, month) ?: return
        if (notes.notesText.isBlank()) return
        if (transactionDao.countForMonth(accountId, year, month) > 0) return
        saveMonth(accountId, year, month, notes.notesText)
    }
}

fun Transaction.toEntity(accountId: Long): TransactionEntity {
    return TransactionEntity(
        accountId = accountId,
        year = date.year,
        month = date.monthValue,
        day = date.dayOfMonth,
        description = description,
        amount = amount.toPlainString(),
        type = type.name,
        originalLine = originalLine
    )
}

fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        date = LocalDate.of(year, month, day),
        description = description,
        amount = BigDecimal(amount),
        type = TransactionType.valueOf(type),
        originalLine = originalLine
    )
}
