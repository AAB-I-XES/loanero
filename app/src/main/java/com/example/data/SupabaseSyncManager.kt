package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SupabaseSyncManager {
    private const val TAG = "SupabaseSync"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    val supabaseUrl: String
        get() = try {
            BuildConfig.SUPABASE_URL
        } catch (e: Exception) {
            ""
        }

    val supabaseAnonKey: String
        get() = try {
            BuildConfig.SUPABASE_ANON_KEY
        } catch (e: Exception) {
            ""
        }

    fun isConfigured(): Boolean {
        val url = supabaseUrl
        val key = supabaseAnonKey
        return url.isNotEmpty() && 
               !url.contains("YOUR_SUPABASE_URL") && 
               key.isNotEmpty() && 
               !key.contains("YOUR_SUPABASE_ANON_KEY")
    }

    private fun getHeaders(request: Request.Builder): Request.Builder {
        return request
            .header("apikey", supabaseAnonKey)
            .header("Authorization", "Bearer $supabaseAnonKey")
    }

    suspend fun syncAll(repository: LoanRepository, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.w(TAG, "Supabase is not configured. Skipping full sync.")
            onProgress("Supabase not configured. Operating in local mode.")
            return@withContext
        }

        try {
            onProgress("Syncing members from cloud...")
            syncMembers(repository)
            onProgress("Syncing loans from cloud...")
            syncLoans(repository)
            onProgress("Syncing repayments from cloud...")
            syncRepayments(repository)
            onProgress("Cloud synchronization complete!")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing full sync", e)
            onProgress("Sync failed: ${e.localizedMessage}")
        }
    }

    private suspend fun syncMembers(repository: LoanRepository) {
        val url = "$supabaseUrl/rest/v1/members?select=*"
        val request = getHeaders(Request.Builder().url(url).get()).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch members: $response")
            val bodyString = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val member = Member(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    notes = obj.optString("notes", ""),
                    isMonthlyCollected = obj.optBoolean("isMonthlyCollected", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                // Save locally (Room has fallbackToDestructiveMigration or standard inserts)
                repository.insertMember(member)
            }
        }
    }

    private suspend fun syncLoans(repository: LoanRepository) {
        val url = "$supabaseUrl/rest/v1/loans?select=*"
        val request = getHeaders(Request.Builder().url(url).get()).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch loans: $response")
            val bodyString = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val loan = Loan(
                    id = obj.getInt("id"),
                    memberId = obj.getInt("memberId"),
                    principalAmount = obj.getDouble("principalAmount"),
                    interestRate = obj.getDouble("interestRate"),
                    interestType = obj.getString("interestType"),
                    purpose = obj.optString("purpose", ""),
                    notes = obj.optString("notes", ""),
                    startDate = obj.optLong("startDate", System.currentTimeMillis()),
                    isFinished = obj.optBoolean("isFinished", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                repository.insertLoan(loan)
            }
        }
    }

    private suspend fun syncRepayments(repository: LoanRepository) {
        val url = "$supabaseUrl/rest/v1/repayments?select=*"
        val request = getHeaders(Request.Builder().url(url).get()).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to fetch repayments: $response")
            val bodyString = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(bodyString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val repayment = Repayment(
                    id = obj.getInt("id"),
                    loanId = obj.getInt("loanId"),
                    amount = obj.getDouble("amount"),
                    paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                repository.insertRepayment(repayment)
            }
        }
    }

    suspend fun pushMember(member: Member) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val url = "$supabaseUrl/rest/v1/members"
        val json = JSONObject().apply {
            put("id", member.id)
            put("name", member.name)
            put("phone", member.phone)
            put("email", member.email)
            put("notes", member.notes)
            put("isMonthlyCollected", member.isMonthlyCollected)
            put("createdAt", member.createdAt)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = getHeaders(Request.Builder().url(url))
            .header("Prefer", "resolution=merge-duplicates")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to push member: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error pushing member", e)
        }
    }

    suspend fun deleteMember(memberId: Int) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val url = "$supabaseUrl/rest/v1/members?id=eq.$memberId"
        val request = getHeaders(Request.Builder().url(url).delete()).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to delete member: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting member", e)
        }
    }

    suspend fun pushLoan(loan: Loan) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val url = "$supabaseUrl/rest/v1/loans"
        val json = JSONObject().apply {
            put("id", loan.id)
            put("memberId", loan.memberId)
            put("principalAmount", loan.principalAmount)
            put("interestRate", loan.interestRate)
            put("interestType", loan.interestType)
            put("purpose", loan.purpose)
            put("notes", loan.notes)
            put("startDate", loan.startDate)
            put("isFinished", loan.isFinished)
            put("createdAt", loan.createdAt)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = getHeaders(Request.Builder().url(url))
            .header("Prefer", "resolution=merge-duplicates")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to push loan: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error pushing loan", e)
        }
    }

    suspend fun deleteLoan(loanId: Int) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val url = "$supabaseUrl/rest/v1/loans?id=eq.$loanId"
        val request = getHeaders(Request.Builder().url(url).delete()).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to delete loan: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting loan", e)
        }
    }

    suspend fun pushRepayment(repayment: Repayment) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val url = "$supabaseUrl/rest/v1/repayments"
        val json = JSONObject().apply {
            put("id", repayment.id)
            put("loanId", repayment.loanId)
            put("amount", repayment.amount)
            put("paymentDate", repayment.paymentDate)
            put("notes", repayment.notes)
            put("createdAt", repayment.createdAt)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)
        val request = getHeaders(Request.Builder().url(url))
            .header("Prefer", "resolution=merge-duplicates")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to push repayment: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error pushing repayment", e)
        }
    }

    suspend fun deleteRepayment(repaymentId: Int) = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext
        val url = "$supabaseUrl/rest/v1/repayments?id=eq.$repaymentId"
        val request = getHeaders(Request.Builder().url(url).delete()).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to delete repayment: ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting repayment", e)
        }
    }
}
