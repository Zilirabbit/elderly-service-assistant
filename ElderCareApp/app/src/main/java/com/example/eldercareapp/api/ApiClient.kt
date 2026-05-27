package com.example.eldercareapp.api

import com.example.eldercareapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Android 模拟器访问宿主电脑本机：-PELDERCARE_BASE_URL=http://10.0.2.2:8080/
    // 真机联调访问电脑局域网 IP：-PELDERCARE_BASE_URL=http://192.168.1.23:8080/
    private val baseUrl = BuildConfig.ELDERCARE_BASE_URL.let { url ->
        if (url.endsWith("/")) url else "$url/"
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val assistantApi: AssistantApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AssistantApi::class.java)
    }
}
