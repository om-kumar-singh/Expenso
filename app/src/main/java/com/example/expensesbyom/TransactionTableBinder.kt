package com.example.expensesbyom

import android.view.LayoutInflater
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView

object TransactionTableBinder {
    fun bind(
        table: TableLayout,
        items: List<DatedTransaction>,
        showAccount: Boolean,
        dateText: (DatedTransaction) -> String = { it.transaction.date.dayOfMonth.toString() }
    ) {
        table.removeAllViews()
        table.isShrinkAllColumns = false
        table.isStretchAllColumns = false
        val inflater = LayoutInflater.from(table.context)
        table.addView(header(inflater, table, showAccount), rowParams())
        if (items.isEmpty()) {
            val empty = TextView(table.context)
            empty.text = table.context.getString(R.string.no_transactions)
            empty.setPadding(8, 16, 8, 8)
            table.addView(empty, rowParams())
            return
        }
        for (item in items) {
            val row = inflater.inflate(R.layout.item_transaction_row, table, false) as TableRow
            row.findViewById<TextView>(R.id.cellDate).text = dateText(item)
            val accountCell = row.findViewById<TextView>(R.id.cellAccount)
            if (showAccount) {
                accountCell.visibility = View.VISIBLE
                accountCell.text = item.accountName
            }
            row.findViewById<TextView>(R.id.cellDescription).text = item.transaction.description
            if (item.transaction.type == TransactionType.EXPENSE) {
                row.findViewById<TextView>(R.id.cellExpense).text = MoneyFormat.rupees(item.transaction.amount)
                row.findViewById<TextView>(R.id.cellReceived).text = ""
            } else {
                row.findViewById<TextView>(R.id.cellExpense).text = ""
                row.findViewById<TextView>(R.id.cellReceived).text = MoneyFormat.rupees(item.transaction.amount)
            }
            table.addView(row, rowParams())
        }
    }

    private fun rowParams(): TableLayout.LayoutParams {
        return TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun header(inflater: LayoutInflater, table: TableLayout, showAccount: Boolean): TableRow {
        val row = inflater.inflate(R.layout.item_transaction_row, table, false) as TableRow
        row.setBackgroundResource(R.drawable.bg_table_header)
        fun style(id: Int, text: String) {
            val view = row.findViewById<TextView>(id)
            view.text = text
            view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
        }
        style(R.id.cellDate, table.context.getString(R.string.table_date))
        val accountCell = row.findViewById<TextView>(R.id.cellAccount)
        if (showAccount) {
            accountCell.visibility = View.VISIBLE
            style(R.id.cellAccount, table.context.getString(R.string.table_account))
        }
        style(R.id.cellDescription, table.context.getString(R.string.table_description))
        style(R.id.cellExpense, table.context.getString(R.string.table_expense))
        style(R.id.cellReceived, table.context.getString(R.string.table_received))
        return row
    }
}

fun ExpenseDatabase.repository(): MonthRepository {
    return MonthRepository(accountDao(), monthlyNotesDao(), transactionDao())
}
