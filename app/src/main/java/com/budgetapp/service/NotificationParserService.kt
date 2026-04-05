package com.budgetapp.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.budgetapp.data.local.dao.AccountDao
import com.budgetapp.data.local.dao.TransactionDao
import com.budgetapp.data.local.entity.TransactionEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Listens to notifications from known banking apps and parses transaction
 * amounts + merchant names to automatically create transaction records.
 *
 * Requires the user to grant Notification Access in system settings:
 *   Settings → Apps → Special app access → Notification access
 */
@AndroidEntryPoint
class NotificationParserService : NotificationListenerService() {

    @Inject
    lateinit var transactionDao: TransactionDao

    @Inject
    lateinit var accountDao: AccountDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Known banking app package prefixes / exact packages
    private val bankPackages = setOf(
        "com.chase.sig.android",
        "com.bofa.android",
        "com.citi.mobile",
        "com.wellsfargo.mobile",
        "com.americanexpress.android.acctsvcs.us",
        "com.usbank.mobilebanking",
        "com.capitalone.mobile",
        "com.discover.mobileapp",
        "com.ally.mobile",
        "com.tdbank",
        "com.pnc.mobile",
        "com.schwab.mobile",
        "com.fidelity.mobileapp",
        "com.venmo",
        "com.paypal.android.p2pmobile",
        "com.squareup.cash"
    )

    // Patterns for extracting amounts from notification text
    // e.g. "$24.99", "24.99 USD", "charged $24.99", "purchase of $24.99"
    private val amountPatterns = listOf(
        Pattern.compile("""\$\s*([\d,]+\.?\d{0,2})"""),
        Pattern.compile("""([\d,]+\.\d{2})\s*USD"""),
        Pattern.compile("""charged\s+\$?([\d,]+\.?\d{0,2})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""purchase of\s+\$?([\d,]+\.?\d{0,2})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""payment of\s+\$?([\d,]+\.?\d{0,2})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""debit of\s+\$?([\d,]+\.?\d{0,2})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""spent\s+\$?([\d,]+\.?\d{0,2})""", Pattern.CASE_INSENSITIVE)
    )

    // Patterns to detect the merchant / payee name
    // e.g. "at Amazon", "to Starbucks", "from Netflix"
    private val merchantPatterns = listOf(
        Pattern.compile("""at\s+([A-Za-z0-9\s&'.\-]{2,30})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""to\s+([A-Za-z0-9\s&'.\-]{2,30})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""from\s+([A-Za-z0-9\s&'.\-]{2,30})""", Pattern.CASE_INSENSITIVE)
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (!isBankPackage(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        val fullText = "$title $bigText".trim()
        if (fullText.isBlank()) return

        val amount = parseAmount(fullText) ?: return
        val merchant = parseMerchant(fullText) ?: getAppLabel(packageName)

        serviceScope.launch {
            val accounts = accountDao.getAllAccountsList()
            val targetAccount = accounts.firstOrNull() ?: return@launch

            transactionDao.insertTransaction(
                TransactionEntity(
                    accountId = targetAccount.id,
                    amount = -amount,  // negative = expense
                    description = merchant,
                    date = System.currentTimeMillis(),
                    isManual = false
                )
            )
        }
    }

    private fun isBankPackage(packageName: String): Boolean {
        return packageName in bankPackages ||
            packageName.contains("bank", ignoreCase = true) ||
            packageName.contains("credit", ignoreCase = true) ||
            packageName.contains("finance", ignoreCase = true)
    }

    private fun parseAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.replace(",", "") ?: continue
                val value = raw.toDoubleOrNull() ?: continue
                if (value > 0.0) return value
            }
        }
        return null
    }

    private fun parseMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim() ?: continue
                if (merchant.isNotBlank()) return merchant
            }
        }
        return null
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager ?: return packageName
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
