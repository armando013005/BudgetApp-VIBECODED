package com.budgetapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.budgetapp.data.local.dao.AccountDao
import com.budgetapp.data.local.dao.BudgetDao
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.local.dao.TransactionDao
import com.budgetapp.data.local.dao.UserDao
import com.budgetapp.data.local.entity.AccountEntity
import com.budgetapp.data.local.entity.BudgetEntity
import com.budgetapp.data.local.entity.CategoryEntity
import com.budgetapp.data.local.entity.TransactionEntity
import com.budgetapp.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
}
