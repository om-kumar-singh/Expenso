package com.example.expensesbyom

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import androidx.core.content.ContextCompat

object AppNav {
    fun bind(activity: Activity, overview: TextView, accounts: TextView, period: TextView, current: Screen) {
        val gold = ContextCompat.getColor(activity, R.color.gold_deep)
        val ink = ContextCompat.getColor(activity, R.color.ink)
        overview.setTextColor(if (current == Screen.OVERVIEW) gold else ink)
        accounts.setTextColor(if (current == Screen.ACCOUNTS) gold else ink)
        period.setTextColor(if (current == Screen.PERIOD) gold else ink)
        overview.setOnClickListener {
            if (current != Screen.OVERVIEW) {
                activity.startActivity(Intent(activity, OverviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
        accounts.setOnClickListener {
            if (current != Screen.ACCOUNTS) {
                activity.startActivity(Intent(activity, AccountsActivity::class.java))
            }
        }
        period.setOnClickListener {
            if (current != Screen.PERIOD) {
                activity.startActivity(Intent(activity, PeriodActivity::class.java))
            }
        }
    }

    enum class Screen { OVERVIEW, ACCOUNTS, PERIOD }
}
