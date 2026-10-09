package com.example.expensesbyom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    fun getDefault(): AccountEntity?

    @Insert
    fun insert(account: AccountEntity): Long

    @Update
    fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id AND isDefault = 0")
    fun deleteIfNotDefault(id: Long): Int
}
