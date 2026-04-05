package com.budgetapp.data.repository

import com.budgetapp.data.local.dao.BudgetDao
import com.budgetapp.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month, year)

    suspend fun getBudgetForCategory(categoryId: Int, month: Int, year: Int): BudgetEntity? =
        budgetDao.getBudgetForCategory(categoryId, month, year)

    suspend fun addBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)
}
