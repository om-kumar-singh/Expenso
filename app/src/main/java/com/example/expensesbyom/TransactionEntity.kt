package com.example.expensesbyom

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "month_transactions",
    indices = [Index(value = ["accountId", "year", "month"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val year: Int,
    val month: Int,
    val day: Int,
    val description: String,
    val amount: String,
    val type: String,
    val originalLine: String
)
