package com.budgetapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetapp.data.local.AppDatabase
import com.budgetapp.data.local.dao.AccountDao
import com.budgetapp.data.local.dao.BudgetDao
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.local.dao.TransactionDao
import com.budgetapp.data.local.dao.UserDao
import com.budgetapp.data.remote.PlaidService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "budget_app.db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val defaultCategories = listOf(
                            "Food & Dining" to 0xFF4CAF50.toInt(),
                            "Transportation" to 0xFF2196F3.toInt(),
                            "Entertainment" to 0xFF9C27B0.toInt(),
                            "Bills & Utilities" to 0xFFFF9800.toInt(),
                            "Shopping" to 0xFFE91E63.toInt(),
                            "Health" to 0xFF00BCD4.toInt(),
                            "Education" to 0xFF3F51B5.toInt(),
                            "Income" to 0xFF8BC34A.toInt(),
                            "Other" to 0xFF607D8B.toInt()
                        )
                        // Insert directly via SQL since we don't have DAO access in callback
                        defaultCategories.forEach { (name, color) ->
                            db.execSQL(
                                "INSERT INTO categories (name, color, isDefault) VALUES (?, ?, 1)",
                                arrayOf(name, color)
                            )
                        }
                    }
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://sandbox.plaid.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun providePlaidService(retrofit: Retrofit): PlaidService =
        retrofit.create(PlaidService::class.java)

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
}
