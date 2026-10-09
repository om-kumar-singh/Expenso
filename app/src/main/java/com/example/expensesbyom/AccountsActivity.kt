package com.example.expensesbyom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.expensesbyom.databinding.ActivityAccountsBinding

class AccountsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountsBinding
    private lateinit var repository: MonthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ScreenInsets.apply(this, binding.main)
        repository = ExpenseDatabase.getInstance(this).repository()
        AppNav.bind(
            this,
            binding.bottomNav.navOverview,
            binding.bottomNav.navAccounts,
            binding.bottomNav.navPeriod,
            AppNav.Screen.ACCOUNTS
        )
        binding.addAccountButton.setOnClickListener {
            AccountScopeDialog.promptName(this, R.string.add_account, "") { name ->
                if (name.trim().isEmpty()) return@promptName
                ExpenseDatabase.ioExecutor.execute {
                    repository.createAccount(name)
                    runOnUiThread { refresh() }
                }
            }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        ExpenseDatabase.ioExecutor.execute {
            val accounts = repository.listAccounts()
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                render(accounts)
            }
        }
    }

    private fun render(accounts: List<AccountEntity>) {
        binding.accountList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (account in accounts) {
            val card = inflater.inflate(R.layout.item_account, binding.accountList, false)
            card.findViewById<TextView>(R.id.accountName).text = account.name
            val badge = card.findViewById<TextView>(R.id.accountBadge)
            badge.visibility = if (account.isDefault) View.VISIBLE else View.GONE
            card.findViewById<View>(R.id.renameButton).setOnClickListener {
                AccountScopeDialog.promptName(this, R.string.rename, account.name) { name ->
                    ExpenseDatabase.ioExecutor.execute {
                        repository.renameAccount(account.id, name)
                        runOnUiThread { refresh() }
                    }
                }
            }
            card.findViewById<View>(R.id.deleteButton).setOnClickListener {
                if (account.isDefault) {
                    Toast.makeText(this, R.string.cannot_delete_default, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_account_title)
                    .setMessage(getString(R.string.delete_account_message, account.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        ExpenseDatabase.ioExecutor.execute {
                            repository.deleteAccount(account.id)
                            runOnUiThread { refresh() }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            binding.accountList.addView(card)
        }
    }
}
