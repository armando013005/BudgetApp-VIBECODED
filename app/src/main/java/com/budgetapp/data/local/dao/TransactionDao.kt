package com.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budgetapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `transactions` ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transactions` WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transactions` WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transactions` WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transactions` WHERE date BETWEEN :startDate AND :endDate AND categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategoryAndDateRange(categoryId: Int, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM `transactions` WHERE amount < 0 AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpending(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM `transactions` WHERE amount < 0 AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    fun getSpendingByCategory(categoryId: Int, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM `transactions` ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT plaidTransactionId FROM `transactions` WHERE plaidTransactionId IS NOT NULL")
    suspend fun getAllPlaidTransactionIds(): List<String>
}
