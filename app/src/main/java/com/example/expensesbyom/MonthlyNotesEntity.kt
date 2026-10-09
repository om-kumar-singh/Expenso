package com.example.expensesbyom

import androidx.room.Entity

@Entity(
    tableName = "monthly_notes",
    primaryKeys = ["accountId", "year", "month"]
)
data class MonthlyNotesEntity(
    val accountId: Long,
    val year: Int,
    val month: Int,
    val notesText: String
)
