package com.example.expensesbyom

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MonthlyNotesDao {
    @Query("SELECT * FROM monthly_notes WHERE accountId = :accountId AND year = :year AND month = :month LIMIT 1")
    fun getNotes(accountId: Long, year: Int, month: Int): MonthlyNotesEntity?

    @Query("SELECT * FROM monthly_notes WHERE accountId IN (:accountIds) ORDER BY year DESC, month DESC")
    fun getNotesForAccounts(accountIds: List<Long>): List<MonthlyNotesEntity>

    @Query("SELECT * FROM monthly_notes ORDER BY year DESC, month DESC")
    fun getAllNotes(): List<MonthlyNotesEntity>

    @Upsert
    fun upsertNotes(notes: MonthlyNotesEntity)

    @Query("DELETE FROM monthly_notes WHERE accountId = :accountId AND year = :year AND month = :month")
    fun deleteNotes(accountId: Long, year: Int, month: Int)

    @Query("DELETE FROM monthly_notes WHERE accountId = :accountId")
    fun deleteAllForAccount(accountId: Long)
}
