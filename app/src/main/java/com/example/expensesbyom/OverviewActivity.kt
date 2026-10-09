package com.example.expensesbyom

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.expensesbyom.databinding.ActivityOverviewBinding
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class OverviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverviewBinding
    private lateinit var repository: MonthRepository
    private lateinit var filterStore: AccountFilterStore
    private var accounts: List<AccountEntity> = emptyList()
    private var selectedIds: List<Long> = emptyList()
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ScreenInsets.apply(this, binding.main)
        repository = ExpenseDatabase.getInstance(this).repository()
        filterStore = AccountFilterStore(this)
        AppNav.bind(
            this,
            binding.bottomNav.navOverview,
            binding.bottomNav.navAccounts,
            binding.bottomNav.navPeriod,
            AppNav.Screen.OVERVIEW
        )
        binding.accountFilter.setOnClickListener {
            AccountScopeDialog.show(this, accounts, selectedIds) { ids ->
                selectedIds = ids
                if (ids.size == accounts.size) filterStore.saveAllAccounts() else filterStore.saveAccountIds(ids)
                updateFilterLabel()
                refreshMonths()
            }
        }
        binding.addNotesButton.setOnClickListener { openNewNotes() }
    }

    override fun onResume() {
        super.onResume()
        refreshMonths()
    }

    private fun openNewNotes() {
        ExpenseDatabase.ioExecutor.execute {
            val defaultId = repository.ensureDefaultAccount().id
            val accountId = if (selectedIds.size == 1) selectedIds[0] else defaultId
            runOnUiThread {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MonthKeys.NEW_NOTES, true)
                        .putExtra(MonthKeys.ACCOUNT_ID, accountId)
                )
            }
        }
    }

    private fun refreshMonths() {
        ExpenseDatabase.ioExecutor.execute {
            val loadedAccounts = repository.listAccounts()
            val ids = filterStore.resolveIds(loadedAccounts)
            val summaries = repository.loadSummaries(ids)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                accounts = loadedAccounts
                selectedIds = ids
                updateFilterLabel()
                renderSummaries(summaries)
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

    private fun renderSummaries(summaries: List<MonthSummary>) {
        binding.monthList.removeAllViews()
        binding.emptyState.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
        val inflater = LayoutInflater.from(this)
        val showAccount = selectedIds.size != 1
        for (summary in summaries) {
            val card = inflater.inflate(R.layout.item_month_card, binding.monthList, false)
            val title = YearMonth.of(summary.year, summary.month).format(monthFormatter)
            card.findViewById<TextView>(R.id.monthTitle).text = title
            card.findViewById<TextView>(R.id.monthExpenses).text =
                getString(R.string.card_expenses, MoneyFormat.rupees(summary.totalExpenses))
            card.findViewById<TextView>(R.id.monthReceived).text =
                getString(R.string.card_received, MoneyFormat.rupees(summary.totalIncome))
            card.findViewById<TextView>(R.id.monthNet).text =
                getString(R.string.card_net, MoneyFormat.rupees(summary.netAmount))
            card.findViewById<TextView>(R.id.monthCount).text =
                if (summary.transactionCount == 1) {
                    getString(R.string.transaction_count_one)
                } else {
                    getString(R.string.transaction_count_many, summary.transactionCount)
                }
            card.setOnClickListener {
                startActivity(
                    Intent(this, MonthDetailActivity::class.java)
                        .putExtra(MonthKeys.YEAR, summary.year)
                        .putExtra(MonthKeys.MONTH, summary.month)
                        .putExtra(MonthKeys.ACCOUNT_IDS, selectedIds.toLongArray())
                        .putExtra(MonthKeys.SHOW_ACCOUNT_COLUMN, showAccount)
                )
            }
            binding.monthList.addView(card)
        }
    }
}
