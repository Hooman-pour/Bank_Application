package com.example.di

import com.example.BuildConfig
import com.example.core.network.AuthInterceptor
import com.example.core.network.TokenRefreshAuthenticator
import com.example.data.remote.faraboom.FaraboomApiService
import com.example.data.remote.finotech.FinotechApiService
import com.example.domain.model.OpenBankingProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FinotechRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FaraboomRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("App-Key")
            redactHeader("Token-Id")
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(tokenRefreshAuthenticator)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @FinotechRetrofit
    fun provideFinotechRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(OpenBankingProvider.FINOTECH.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    @FaraboomRetrofit
    fun provideFaraboomRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit = Retrofit.Builder()
        .baseUrl(OpenBankingProvider.FARABOOM.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideFinotechApiService(
        @FinotechRetrofit retrofit: Retrofit
    ): FinotechApiService = retrofit.create(FinotechApiService::class.java)

    @Provides
    @Singleton
    fun provideFaraboomApiService(
        @FaraboomRetrofit retrofit: Retrofit
    ): FaraboomApiService = retrofit.create(FaraboomApiService::class.java)
}
