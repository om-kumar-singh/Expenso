package com.example.expensesbyom

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.expensesbyom.databinding.ActivityMonthDetailBinding
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MonthDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonthDetailBinding
    private lateinit var repository: MonthRepository
    private var year: Int = YearMonth.now().year
    private var month: Int = YearMonth.now().monthValue
    private var accountIds: List<Long> = emptyList()
    private var showAccountColumn: Boolean = false
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMonthDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ScreenInsets.apply(this, binding.main)
        year = intent.getIntExtra(MonthKeys.YEAR, year)
        month = intent.getIntExtra(MonthKeys.MONTH, month)
        accountIds = intent.getLongArrayExtra(MonthKeys.ACCOUNT_IDS)?.toList().orEmpty()
        showAccountColumn = intent.getBooleanExtra(MonthKeys.SHOW_ACCOUNT_COLUMN, accountIds.size != 1)
        repository = ExpenseDatabase.getInstance(this).repository()
        binding.monthTitle.text = YearMonth.of(year, month).format(monthFormatter)
        binding.backButton.setOnClickListener { finish() }
        binding.editNotesButton.setOnClickListener { editNotes() }
    }

    override fun onResume() {
        super.onResume()
        loadDetail()
    }

    private fun editNotes() {
        if (accountIds.size == 1) {
            openNotes(accountIds[0])
            return
        }
        ExpenseDatabase.ioExecutor.execute {
            val accounts = repository.listAccounts().filter { accountIds.contains(it.id) }
            runOnUiThread {
                val names = accounts.map { it.name }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(R.string.choose_account_for_notes)
                    .setItems(names) { _, which ->
                        openNotes(accounts[which].id)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun openNotes(accountId: Long) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MonthKeys.YEAR, year)
                .putExtra(MonthKeys.MONTH, month)
                .putExtra(MonthKeys.ACCOUNT_ID, accountId)
        )
    }

    private fun loadDetail() {
        ExpenseDatabase.ioExecutor.execute {
            val ids = if (accountIds.isEmpty()) listOf(repository.ensureDefaultAccount().id) else accountIds
            accountIds = ids
            val summary = repository.loadSummary(ids, year, month)
            val transactions = repository.loadDatedTransactions(ids, year, month)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                binding.totalExpenses.text = getString(R.string.expenses_value, MoneyFormat.rupees(summary.totalExpenses))
                binding.totalReceived.text = getString(R.string.received_value, MoneyFormat.rupees(summary.totalIncome))
                binding.totalNet.text = getString(R.string.net_value, MoneyFormat.rupees(summary.netAmount))
                TransactionTableBinder.bind(binding.transactionTable, transactions, showAccountColumn)
            }
        }
    }
}
