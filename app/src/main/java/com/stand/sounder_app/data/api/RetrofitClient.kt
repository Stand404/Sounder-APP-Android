package com.stand.sounder_app.data.api

import com.stand.sounder_app.MyApp
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://stand.homes/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // release 包关闭网络日志，debug 包打印基本信息便于排查
        level = if (com.stand.sounder_app.BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BASIC
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val httpCacheDir: File by lazy {
        File(MyApp.instance.cacheDir, "http_cache").also { it.mkdirs() }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .cache(Cache(httpCacheDir, 10L * 1024 * 1024))     // 10MB HTTP 缓存，减少重复请求
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES)) // 增大连接池，图片和 API 可复用
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
