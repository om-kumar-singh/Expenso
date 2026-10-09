package com.example.expensesbyom

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

object AccountScopeDialog {
    fun show(
        context: Context,
        accounts: List<AccountEntity>,
        selectedIds: List<Long>,
        onPicked: (List<Long>) -> Unit
    ) {
        val names = mutableListOf(context.getString(R.string.scope_all))
        names.addAll(accounts.map { it.name })
        names.add(context.getString(R.string.scope_choose))
        AlertDialog.Builder(context)
            .setTitle(R.string.filter_accounts)
            .setItems(names.toTypedArray()) { _, which ->
                when (which) {
                    0 -> onPicked(accounts.map { it.id })
                    names.lastIndex -> showMulti(context, accounts, selectedIds, onPicked)
                    else -> onPicked(listOf(accounts[which - 1].id))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun promptName(context: Context, title: Int, initial: String, onOk: (String) -> Unit) {
        val input = EditText(context)
        input.hint = context.getString(R.string.account_name_hint)
        input.setText(initial)
        input.setSelection(initial.length)
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ -> onOk(input.text.toString()) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showMulti(
        context: Context,
        accounts: List<AccountEntity>,
        selectedIds: List<Long>,
        onPicked: (List<Long>) -> Unit
    ) {
        val checked = BooleanArray(accounts.size) { index -> selectedIds.contains(accounts[index].id) }
        AlertDialog.Builder(context)
            .setTitle(R.string.scope_choose)
            .setMultiChoiceItems(accounts.map { it.name }.toTypedArray(), checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                val ids = accounts.mapIndexedNotNull { index, account ->
                    if (checked[index]) account.id else null
                }
                if (ids.isNotEmpty()) onPicked(ids)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
