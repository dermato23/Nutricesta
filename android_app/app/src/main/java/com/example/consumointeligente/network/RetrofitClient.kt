package com.example.consumointeligente.network

import com.example.consumointeligente.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


object RetrofitClient {
    // Definida por build type en app/build.gradle.kts:
    // debug -> http://10.0.2.2:8003 (emulador), release -> servidor HTTPS
    private val BASE_URL = BuildConfig.BASE_URL

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
