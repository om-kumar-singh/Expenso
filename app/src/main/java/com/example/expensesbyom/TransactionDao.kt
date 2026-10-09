package com.example.expensesbyom

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM month_transactions
        WHERE accountId = :accountId AND year = :year AND month = :month
        ORDER BY day ASC, id ASC
        """
    )
    fun getForMonth(accountId: Long, year: Int, month: Int): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM month_transactions
        WHERE accountId IN (:accountIds) AND year = :year AND month = :month
        ORDER BY day ASC, id ASC
        """
    )
    fun getForMonth(accountIds: List<Long>, year: Int, month: Int): List<TransactionEntity>

    @Query("DELETE FROM month_transactions WHERE accountId = :accountId AND year = :year AND month = :month")
    fun deleteForMonth(accountId: Long, year: Int, month: Int)

    @Query("DELETE FROM month_transactions WHERE accountId = :accountId")
    fun deleteAllForAccount(accountId: Long)

    @Insert
    fun insertAll(transactions: List<TransactionEntity>)

    @Query(
        """
        SELECT COUNT(*) FROM month_transactions
        WHERE accountId = :accountId AND year = :year AND month = :month
        """
    )
    fun countForMonth(accountId: Long, year: Int, month: Int): Int

    @Query(
        """
        SELECT * FROM month_transactions
        WHERE accountId IN (:accountIds)
        AND (year * 10000 + month * 100 + day) BETWEEN :startKey AND :endKey
        ORDER BY year ASC, month ASC, day ASC, id ASC
        """
    )
    fun getInRange(accountIds: List<Long>, startKey: Int, endKey: Int): List<TransactionEntity>
}
