package com.budgetapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // CHECKING, SAVINGS, CREDIT, CASH
    val balance: Double = 0.0,
    val institution: String? = null,
    val plaidAccessToken: String? = null, // encrypted
    val isManual: Boolean = true,
    val lastSynced: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
