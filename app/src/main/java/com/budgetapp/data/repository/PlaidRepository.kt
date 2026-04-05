package com.budgetapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.budgetapp.data.local.entity.AccountEntity
import com.budgetapp.data.local.entity.TransactionEntity
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.remote.PlaidExchangeRequest
import com.budgetapp.data.remote.PlaidService
import com.budgetapp.data.remote.PlaidTransaction
import com.budgetapp.data.remote.PlaidTransactionsRequest
import com.budgetapp.data.remote.SandboxPublicTokenRequest
import com.budgetapp.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaidRepository @Inject constructor(
    private val plaidService: PlaidService,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val cryptoManager: CryptoManager,
    private val categoryDao: CategoryDao,
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Maps common Plaid category names to app category names
    private val plaidCategoryMap = mapOf(
        "Food and Drink" to "Food & Dining",
        "Travel" to "Transportation",
        "Transportation" to "Transportation",
        "Recreation" to "Entertainment",
        "Entertainment" to "Entertainment",
        "Shops" to "Shopping",
        "Shopping" to "Shopping",
        "Healthcare" to "Health",
        "Health" to "Health",
        "Service" to "Bills & Utilities",
        "Utilities" to "Bills & Utilities",
        "Payment" to "Bills & Utilities",
        "Transfer" to "Other",
        "Education" to "Education",
        "Income" to "Income"
    )

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "plaid_prefs",
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encrypted not available
            context.getSharedPreferences("plaid_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun savePlaidCredentials(clientId: String, secret: String) {
        prefs.edit()
            .putString("plaid_client_id", clientId)
            .putString("plaid_secret", secret)
            .apply()
    }

    fun getPlaidClientId(): String = prefs.getString("plaid_client_id", "") ?: ""
    fun getPlaidSecret(): String = prefs.getString("plaid_secret", "") ?: ""

    fun hasCredentials(): Boolean =
        getPlaidClientId().isNotBlank() && getPlaidSecret().isNotBlank()

    suspend fun generateSandboxAccessToken(institutionId: String = "ins_109508"): Result<String> {
        return try {
            val clientId = getPlaidClientId()
            val secret = getPlaidSecret()
            if (clientId.isBlank() || secret.isBlank()) {
                return Result.failure(Exception("Plaid credentials not configured. Go to Settings first."))
            }
            val publicTokenResponse = plaidService.createSandboxPublicToken(
                SandboxPublicTokenRequest(
                    client_id = clientId,
                    secret = secret,
                    institution_id = institutionId
                )
            )
            val accessTokenResponse = plaidService.exchangePublicToken(
                PlaidExchangeRequest(
                    client_id = clientId,
                    secret = secret,
                    public_token = publicTokenResponse.public_token
                )
            )
            Result.success(accessTokenResponse.access_token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveCategoryId(plaidTransaction: PlaidTransaction): Int? {
        val plaidCategory = plaidTransaction.category?.firstOrNull() ?: return null
        val appCategoryName = plaidCategoryMap[plaidCategory] ?: return null
        return categoryDao.getCategoryByName(appCategoryName)?.id
    }

    suspend fun syncAccount(account: AccountEntity): Result<Int> {
        return try {
            val clientId = getPlaidClientId()
            val secret = getPlaidSecret()
            if (clientId.isBlank() || secret.isBlank()) {
                return Result.failure(Exception("Plaid credentials not configured"))
            }

            val accessToken = if (account.plaidAccessToken != null) {
                try {
                    cryptoManager.decryptFromBase64(account.plaidAccessToken)
                } catch (e: Exception) {
                    // Token might not be encrypted yet (entered manually)
                    account.plaidAccessToken
                }
            } else {
                return Result.failure(Exception("No access token for account"))
            }

            val calendar = Calendar.getInstance()
            val endDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -90)
            val startDate = dateFormat.format(calendar.time)

            val response = plaidService.getTransactions(
                PlaidTransactionsRequest(
                    client_id = clientId,
                    secret = secret,
                    access_token = accessToken,
                    start_date = startDate,
                    end_date = endDate
                )
            )

            // Update account balance from Plaid
            response.accounts.firstOrNull()?.balances?.current?.let { balance ->
                accountRepository.updateBalance(account.id, balance)
            }

            // Deduplicate and insert new transactions with auto-categorization
            val existingIds = transactionRepository.getExistingPlaidIds().toSet()
            val newPlaidTxs = response.transactions.filter { it.transaction_id !in existingIds }
            val newTransactions = newPlaidTxs.map { plaidTx ->
                val categoryId = resolveCategoryId(plaidTx)
                TransactionEntity(
                    accountId = account.id,
                    categoryId = categoryId,
                    amount = -plaidTx.amount, // Plaid: positive = expense, we use negative = expense
                    description = plaidTx.name,
                    date = try {
                        dateFormat.parse(plaidTx.date)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    },
                    isManual = false,
                    plaidTransactionId = plaidTx.transaction_id
                )
            }

            if (newTransactions.isNotEmpty()) {
                transactionRepository.addTransactions(newTransactions)
            }

            // Update lastSynced timestamp
            accountRepository.updateAccount(account.copy(lastSynced = System.currentTimeMillis()))

            Result.success(newTransactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAllAccounts(): Result<Int> {
        return try {
            val accounts = accountRepository.getAllAccounts().first()
            val plaidAccounts = accounts.filter { !it.isManual && it.plaidAccessToken != null }
            if (plaidAccounts.isEmpty()) return Result.success(0)

            var totalNew = 0
            plaidAccounts.forEach { account ->
                syncAccount(account).onSuccess { count -> totalNew += count }
            }
            Result.success(totalNew)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
