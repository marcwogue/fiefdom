package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface FirebaseApiService {
    @GET("users/{username}.json")
    suspend fun getUser(@Path("username") username: String): CommunityMember?

    @PUT("users/{username}.json")
    suspend fun saveUser(@Path("username") username: String, @Body user: CommunityMember): CommunityMember

    @GET("users.json")
    suspend fun getAllUsers(): Map<String, CommunityMember>?

    @GET("tasks/{username}.json")
    suspend fun getTasks(@Path("username") username: String): Map<String, Task>?

    @PUT("tasks/{username}.json")
    suspend fun saveTasks(@Path("username") username: String, @Body tasks: Map<String, Task>): Map<String, Task>

    @POST("tax_logs.json")
    suspend fun addTaxLog(@Body log: TaxLog): Map<String, String>

    @GET("tax_logs.json")
    suspend fun getTaxLogs(): Map<String, TaxLog>?

    @GET("community_debt.json")
    suspend fun getCommunityDebt(): Int?

    @PUT("community_debt.json")
    suspend fun saveCommunityDebt(@Body debt: Int): Int
}

object FirebaseClient {
    // Default fallback firebase URL to keep the app functional right out of the box
    const val DEFAULT_FIREBASE_URL = "https://fiefdom-quest-bvnxtq-default-rtdb.firebaseio.com/"

    private var currentUrl: String = DEFAULT_FIREBASE_URL
    private var cachedService: FirebaseApiService? = null

    fun getService(baseUrl: String): FirebaseApiService {
        val normalizedUrl = if (baseUrl.trim().endsWith("/")) baseUrl.trim() else "${baseUrl.trim()}/"
        val finalUrl = if (normalizedUrl.startsWith("http")) normalizedUrl else "https://$normalizedUrl"
        
        if (cachedService != null && currentUrl == finalUrl) {
            return cachedService!!
        }
        currentUrl = finalUrl
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(currentUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(FirebaseApiService::class.java)
        cachedService = service
        return service
    }
}
