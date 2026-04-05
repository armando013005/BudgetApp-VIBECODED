package com.budgetapp.data.repository

import com.budgetapp.data.local.dao.TransactionDao
import com.budgetapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByAccount(accountId: Int): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTotalSpending(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getTotalSpending(startDate, endDate)

    fun getSpendingByCategory(categoryId: Int, startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getSpendingByCategory(categoryId, startDate, endDate)

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    suspend fun addTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun addTransactions(transactions: List<TransactionEntity>) =
        transactionDao.insertTransactions(transactions)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    fun getManualTransactionSum(): Flow<Double> =
        transactionDao.getManualTransactionSum()

    suspend fun getExistingPlaidIds(): List<String> =
        transactionDao.getAllPlaidTransactionIds()
}
