package com.budgetapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.budgetapp.data.local.dao.UserDao
import com.budgetapp.data.local.entity.UserEntity
import com.budgetapp.security.PasswordHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TRACKING_METHOD_MANUAL = "MANUAL"
        const val TRACKING_METHOD_PLAID = "PLAID"
        const val TRACKING_METHOD_NOTIFICATIONS = "NOTIFICATIONS"
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_TRACKING_METHOD = "tracking_method"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun isUserSetUp(): Boolean = userDao.getUser() != null

    fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun saveTrackingMethod(method: String) {
        prefs.edit()
            .putString(KEY_TRACKING_METHOD, method)
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }

    fun getTrackingMethod(): String =
        prefs.getString(KEY_TRACKING_METHOD, TRACKING_METHOD_MANUAL) ?: TRACKING_METHOD_MANUAL

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
