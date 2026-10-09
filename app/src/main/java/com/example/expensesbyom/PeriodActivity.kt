package com.example.expensesbyom

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.expensesbyom.databinding.ActivityPeriodBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class PeriodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeriodBinding
    private lateinit var repository: MonthRepository
    private lateinit var filterStore: AccountFilterStore
    private var accounts: List<AccountEntity> = emptyList()
    private var selectedIds: List<Long> = emptyList()
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val tableDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPeriodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ScreenInsets.apply(this, binding.main)
        repository = ExpenseDatabase.getInstance(this).repository()
        filterStore = AccountFilterStore(this)
        AppNav.bind(
            this,
            binding.bottomNav.navOverview,
            binding.bottomNav.navAccounts,
            binding.bottomNav.navPeriod,
            AppNav.Screen.PERIOD
        )
        binding.startDateValue.setOnClickListener { pickDate(true) }
        binding.endDateValue.setOnClickListener { pickDate(false) }
        binding.accountFilter.setOnClickListener {
            AccountScopeDialog.show(this, accounts, selectedIds) { ids ->
                selectedIds = ids
                if (ids.size == accounts.size) filterStore.saveAllAccounts() else filterStore.saveAccountIds(ids)
                updateFilterLabel()
            }
        }
        binding.calculatePeriodButton.setOnClickListener { calculate() }
        refreshDates()
        loadAccounts()
    }

    private fun loadAccounts() {
        ExpenseDatabase.ioExecutor.execute {
            val loaded = repository.listAccounts()
            runOnUiThread {
                accounts = loaded
                selectedIds = filterStore.resolveIds(loaded)
                updateFilterLabel()
            }
        }
    }

    private fun updateFilterLabel() {
        binding.accountFilter.text = when {
            selectedIds.size == accounts.size -> getString(R.string.scope_all)
            selectedIds.size == 1 -> accounts.firstOrNull { it.id == selectedIds[0] }?.name ?: getString(R.string.filter_accounts)
            else -> getString(R.string.scope_choose) + " (${selectedIds.size})"
        }
    }

    private fun refreshDates() {
        binding.startDateValue.text = getString(R.string.start_date) + ": " + startDate.format(dateFormatter)
        binding.endDateValue.text = getString(R.string.end_date) + ": " + endDate.format(dateFormatter)
    }

    private fun pickDate(start: Boolean) {
        val current = if (start) startDate else endDate
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = LocalDate.of(year, month + 1, day)
                if (start) startDate = picked else endDate = picked
                refreshDates()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    private fun calculate() {
        val error = DateRangeCalculator.validate(startDate, endDate)
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            return
        }
        val ids = selectedIds
        ExpenseDatabase.ioExecutor.execute {
            val report = repository.reportForRange(startDate, endDate, ids)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                render(report, ids.size != 1)
            }
        }
    }

    private fun render(report: DateRangeCalculator.Report, showAccount: Boolean) {
        binding.rangeLabel.visibility = View.VISIBLE
        binding.totalExpenses.visibility = View.VISIBLE
        binding.totalReceived.visibility = View.VISIBLE
        binding.totalNet.visibility = View.VISIBLE
        binding.transactionCount.visibility = View.VISIBLE
        binding.rangeLabel.text = getString(
            R.string.period_range,
            report.start.format(dateFormatter),
            report.end.format(dateFormatter)
        )
        binding.totalExpenses.text = getString(R.string.expenses_value, MoneyFormat.rupees(report.overall.totalExpenses))
        binding.totalReceived.text = getString(R.string.received_value, MoneyFormat.rupees(report.overall.totalIncome))
        binding.totalNet.text = getString(R.string.net_value, MoneyFormat.rupees(report.overall.netAmount))
        binding.transactionCount.text = if (report.overall.transactionCount == 1) {
            getString(R.string.transaction_count_one)
        } else {
            getString(R.string.transaction_count_many, report.overall.transactionCount)
        }
        binding.perAccountList.removeAllViews()
        if (showAccount && report.perAccount.size > 1) {
            val heading = TextView(this)
            heading.text = getString(R.string.per_account_totals)
            heading.setPadding(0, 8, 0, 4)
            binding.perAccountList.addView(heading)
            for (item in report.perAccount) {
                val line = TextView(this)
                line.text = item.accountName + "  " +
                    getString(R.string.card_expenses, MoneyFormat.rupees(item.totals.totalExpenses)) + "  " +
                    getString(R.string.card_received, MoneyFormat.rupees(item.totals.totalIncome)) + "  " +
                    getString(R.string.card_net, MoneyFormat.rupees(item.totals.netAmount))
                line.setPadding(0, 4, 0, 4)
                binding.perAccountList.addView(line)
            }
        }
        TransactionTableBinder.bind(
            binding.transactionTable,
            report.transactions,
            showAccount
        ) { item -> item.transaction.date.format(tableDateFormatter) }
    }
}
