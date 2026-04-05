package com.budgetapp.data.repository

import com.budgetapp.data.local.dao.UserDao
import com.budgetapp.data.local.entity.UserEntity
import com.budgetapp.security.PasswordHasher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher
) {
    suspend fun isUserSetUp(): Boolean = userDao.getUser() != null

    suspend fun createUser(password: String) {
        val hash = passwordHasher.hash(password)
        userDao.insertUser(UserEntity(passwordHash = hash))
    }

    suspend fun verifyPassword(password: String): Boolean {
        val user = userDao.getUser() ?: return false
        return passwordHasher.verify(password, user.passwordHash)
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean {
        val user = userDao.getUser() ?: return false
        if (!passwordHasher.verify(oldPassword, user.passwordHash)) return false
        val newHash = passwordHasher.hash(newPassword)
        userDao.updatePassword(user.id, newHash)
        return true
    }
}
