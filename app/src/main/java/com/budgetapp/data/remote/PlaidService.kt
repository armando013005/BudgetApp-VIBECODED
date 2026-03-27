package com.budgetapp.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class PlaidLinkTokenRequest(
    val client_id: String,
    val secret: String,
    val user: PlaidUser,
    val client_name: String = "BudgetApp",
    val products: List<String> = listOf("transactions"),
    val country_codes: List<String> = listOf("US"),
    val language: String = "en"
)

data class PlaidUser(val client_user_id: String)

data class PlaidLinkTokenResponse(val link_token: String)

data class PlaidExchangeRequest(
    val client_id: String,
    val secret: String,
    val public_token: String
)

data class PlaidAccessTokenResponse(val access_token: String)

data class PlaidTransactionsRequest(
    val client_id: String,
    val secret: String,
    val access_token: String,
    val start_date: String,
    val end_date: String
)

data class PlaidTransactionsResponse(
    val accounts: List<PlaidAccount>,
    val transactions: List<PlaidTransaction>
)

data class PlaidAccount(
    val account_id: String,
    val name: String,
    val type: String,
    val balances: PlaidBalances
)

data class PlaidBalances(val current: Double?)

data class PlaidTransaction(
    val transaction_id: String,
    val account_id: String,
    val amount: Double,
    val name: String,
    val date: String,
    val category: List<String>?
)

interface PlaidService {
    @POST("link/token/create")
    suspend fun createLinkToken(@Body request: PlaidLinkTokenRequest): PlaidLinkTokenResponse

    @POST("item/public_token/exchange")
    suspend fun exchangePublicToken(@Body request: PlaidExchangeRequest): PlaidAccessTokenResponse

    @POST("transactions/get")
    suspend fun getTransactions(@Body request: PlaidTransactionsRequest): PlaidTransactionsResponse
}
