package com.example.expensesbyom

import android.content.Context

class AccountFilterStore(context: Context) {
    private val prefs = context.getSharedPreferences("account_filter", Context.MODE_PRIVATE)

    fun saveAllAccounts() {
        prefs.edit().putBoolean(KEY_ALL, true).remove(KEY_IDS).apply()
    }

    fun saveAccountIds(ids: List<Long>) {
        prefs.edit()
            .putBoolean(KEY_ALL, false)
            .putString(KEY_IDS, ids.joinToString(","))
            .apply()
    }

    fun resolveIds(allAccounts: List<AccountEntity>): List<Long> {
        if (allAccounts.isEmpty()) return emptyList()
        if (prefs.getBoolean(KEY_ALL, true)) return allAccounts.map { it.id }
        val stored = prefs.getString(KEY_IDS, null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
            .filter { id -> allAccounts.any { it.id == id } }
        return stored.ifEmpty { allAccounts.map { it.id } }
    }

    fun isAllSelected(allAccounts: List<AccountEntity>): Boolean {
        return resolveIds(allAccounts).toSet() == allAccounts.map { it.id }.toSet()
    }

    companion object {
        private const val KEY_ALL = "all"
        private const val KEY_IDS = "ids"
    }
}
