package com.example.expensesbyom

import android.os.Bundle
import android.text.Spanned
import android.text.TextWatcher
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.expensesbyom.databinding.ActivityMainBinding
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: MonthRepository
    private var selectedYearMonth: YearMonth = YearMonth.now()
    private var accountId: Long = 0L
    private var notesLoaded = false
    private var loadGeneration = 0
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = ExpenseDatabase.getInstance(this).repository()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ScreenInsets.apply(this, binding.main)

        if (savedInstanceState != null) {
            val year = savedInstanceState.getInt(STATE_YEAR, selectedYearMonth.year)
            val month = savedInstanceState.getInt(STATE_MONTH, selectedYearMonth.monthValue)
            selectedYearMonth = YearMonth.of(year, month)
            accountId = savedInstanceState.getLong(STATE_ACCOUNT, 0L)
        } else if (intent.hasExtra(MonthKeys.YEAR) && intent.hasExtra(MonthKeys.MONTH)) {
            selectedYearMonth = YearMonth.of(
                intent.getIntExtra(MonthKeys.YEAR, selectedYearMonth.year),
                intent.getIntExtra(MonthKeys.MONTH, selectedYearMonth.monthValue)
            )
        }
        if (intent.hasExtra(MonthKeys.ACCOUNT_ID)) {
            accountId = intent.getLongExtra(MonthKeys.ACCOUNT_ID, accountId)
        }

        updateMonthYearLabel()
        binding.monthYearValue.isEnabled = false
        binding.monthYearValue.setOnClickListener { showMonthYearPicker() }
        binding.calculateButton.setOnClickListener { calculate() }
        binding.monthsLink.setOnClickListener { finish() }

        val startNewNotes = savedInstanceState == null &&
            intent.getBooleanExtra(MonthKeys.NEW_NOTES, false) &&
            !(intent.hasExtra(MonthKeys.YEAR) && intent.hasExtra(MonthKeys.MONTH))
        ExpenseDatabase.ioExecutor.execute {
            if (accountId == 0L) {
                accountId = repository.ensureDefaultAccount().id
            }
            val accountName = repository.listAccounts().firstOrNull { it.id == accountId }?.name
                ?: ExpenseDatabase.DEFAULT_ACCOUNT_NAME
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                binding.accountNameLabel.text = accountName
                if (startNewNotes) {
                    selectedYearMonth = YearMonth.now()
                    updateMonthYearLabel()
                    binding.notesInput.setText("")
                    binding.monthYearValue.isEnabled = true
                    notesLoaded = false
                    binding.notesInput.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                        override fun afterTextChanged(s: Editable?) {
                            notesLoaded = true
                        }
                    })
                    renderTotals()
                    binding.notesInput.requestFocus()
                } else {
                    loadNotesFor(selectedYearMonth, persistCurrentFirst = false)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (notesLoaded && accountId != 0L) {
            persistMonthAsync(selectedYearMonth, binding.notesInput.text.toString())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_YEAR, selectedYearMonth.year)
        outState.putInt(STATE_MONTH, selectedYearMonth.monthValue)
        outState.putLong(STATE_ACCOUNT, accountId)
    }

    private fun updateMonthYearLabel() {
        binding.monthYearValue.text = selectedYearMonth.format(monthYearFormatter)
    }

    private fun showMonthYearPicker() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_month_year, null, false)
        val monthPicker = view.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = view.findViewById<NumberPicker>(R.id.yearPicker)
        val monthNames = java.time.Month.values().map {
            it.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                .replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }
        }
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = monthNames.toTypedArray()
        monthPicker.value = selectedYearMonth.monthValue - 1
        monthPicker.wrapSelectorWheel = false
        yearPicker.minValue = 2000
        yearPicker.maxValue = 2100
        yearPicker.value = selectedYearMonth.year
        yearPicker.wrapSelectorWheel = false

        AlertDialog.Builder(this)
            .setTitle(R.string.pick_month_year)
            .setView(view)
            .setPositiveButton(R.string.ok) { _, _ ->
                val next = YearMonth.of(yearPicker.value, monthPicker.value + 1)
                if (next == selectedYearMonth) return@setPositiveButton
                loadNotesFor(next, persistCurrentFirst = true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun calculate() {
        notesLoaded = true
        persistMonthAsync(selectedYearMonth, binding.notesInput.text.toString())
        renderTotals()
    }

    private fun renderTotals() {
        val result = NotesParser.parse(
            binding.notesInput.text.toString(),
            selectedYearMonth.year,
            selectedYearMonth.monthValue
        )
        binding.totalExpenses.text = getString(R.string.expenses_value, MoneyFormat.rupees(result.totalExpenses))
        binding.totalReceived.text = getString(R.string.received_value, MoneyFormat.rupees(result.totalIncome))
        binding.totalNet.text = getString(R.string.net_value, MoneyFormat.rupees(result.netAmount))
        val invalidCount = result.invalidLines.size
        if (invalidCount == 0) {
            binding.invalidWarningCard.visibility = View.GONE
            binding.invalidWarningItems.removeAllViews()
            clearInvalidHighlights()
        } else {
            binding.invalidWarningCard.visibility = View.VISIBLE
            binding.invalidWarningHeader.text = if (invalidCount == 1) {
                getString(R.string.invalid_lines_one)
            } else {
                getString(R.string.invalid_lines_many, invalidCount)
            }
            renderInvalidLineItems(result.invalidLines)
            applyInvalidHighlights(result.invalidLines)
        }
    }

    private fun renderInvalidLineItems(invalidLines: List<InvalidLine>) {
        binding.invalidWarningItems.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (item in invalidLines) {
            val row = inflater.inflate(R.layout.item_invalid_line, binding.invalidWarningItems, false) as TextView
            row.text = getString(
                R.string.invalid_line_item,
                item.lineNumber,
                item.originalLine.trim(),
                item.reason
            )
            row.setOnClickListener { jumpToNotesLine(item.lineNumber) }
            binding.invalidWarningItems.addView(row)
        }
    }

    private fun applyInvalidHighlights(invalidLines: List<InvalidLine>) {
        clearInvalidHighlights()
        val editable = binding.notesInput.text ?: return
        val color = ContextCompat.getColor(this, R.color.invalid_highlight)
        for (item in invalidLines) {
            val range = lineOffsets(editable, item.lineNumber) ?: continue
            if (range.first >= range.second) continue
            editable.setSpan(
                InvalidLineHighlightSpan(color),
                range.first,
                range.second,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun clearInvalidHighlights() {
        val editable = binding.notesInput.text ?: return
        val spans = editable.getSpans(0, editable.length, InvalidLineHighlightSpan::class.java)
        for (span in spans) {
            editable.removeSpan(span)
        }
    }

    private fun jumpToNotesLine(lineNumber: Int) {
        val editable = binding.notesInput.text ?: return
        val range = lineOffsets(editable, lineNumber) ?: return
        binding.notesInput.requestFocus()
        binding.notesInput.setSelection(range.first)
        binding.notesInput.post {
            binding.notesInput.bringPointIntoView(range.first)
        }
    }

    private fun lineOffsets(text: CharSequence, lineNumber: Int): Pair<Int, Int>? {
        if (lineNumber < 1) return null
        var current = 1
        var start = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                if (current == lineNumber) return start to i
                current++
                start = i + 1
            }
        }
        return if (current == lineNumber) start to text.length else null
    }

    private fun persistMonthAsync(yearMonth: YearMonth, text: String) {
        ExpenseDatabase.ioExecutor.execute {
            repository.saveMonth(accountId, yearMonth.year, yearMonth.monthValue, text)
        }
    }

    private fun loadNotesFor(target: YearMonth, persistCurrentFirst: Boolean) {
        val outgoingMonth = selectedYearMonth
        val outgoingText = binding.notesInput.text.toString()
        val generation = ++loadGeneration
        ExpenseDatabase.ioExecutor.execute {
            if (persistCurrentFirst) {
                repository.saveMonth(accountId, outgoingMonth.year, outgoingMonth.monthValue, outgoingText)
            }
            val loaded = repository.loadNotes(accountId, target.year, target.monthValue)
            runOnUiThread {
                if (isDestroyed || generation != loadGeneration) return@runOnUiThread
                selectedYearMonth = target
                updateMonthYearLabel()
                binding.notesInput.setText(loaded)
                notesLoaded = true
                binding.monthYearValue.isEnabled = true
                renderTotals()
            }
        }
    }

    companion object {
        private const val STATE_YEAR = "selected_year"
        private const val STATE_MONTH = "selected_month"
        private const val STATE_ACCOUNT = "selected_account"
    }
}
