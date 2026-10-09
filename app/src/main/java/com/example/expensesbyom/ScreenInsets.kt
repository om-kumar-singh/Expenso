package com.example.expensesbyom

import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object ScreenInsets {
    fun apply(activity: ComponentActivity, root: View, extraDp: Int = 16) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extra = (extraDp * activity.resources.displayMetrics.density).toInt()
            v.setPadding(
                systemBars.left + extra,
                systemBars.top + extra,
                systemBars.right + extra,
                systemBars.bottom + extra
            )
            insets
        }
    }
}
