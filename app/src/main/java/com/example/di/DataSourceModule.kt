package com.example.di

import com.example.core.network.SessionTokenRefresher
import com.example.data.remote.source.FaraboomRemoteDataSource
import com.example.data.remote.source.FinotechRemoteDataSource
import com.example.data.remote.source.FreeSandboxRemoteDataSource
import com.example.data.remote.source.OpenBankingRemoteDataSource
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.BankingRepositoryImpl
import com.example.data.repository.TransferRepositoryImpl
import com.example.domain.model.OpenBankingProvider
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BankingRepository
import com.example.domain.repository.TransferRepository
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OpenBankingProviderKey(val value: OpenBankingProvider)

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @IntoMap
    @OpenBankingProviderKey(OpenBankingProvider.FREE_SANDBOX)
    abstract fun bindFreeSandboxDataSource(
        impl: FreeSandboxRemoteDataSource
    ): OpenBankingRemoteDataSource

    @Binds
    @IntoMap
    @OpenBankingProviderKey(OpenBankingProvider.FINOTECH)
    abstract fun bindFinotechDataSource(
        impl: FinotechRemoteDataSource
    ): OpenBankingRemoteDataSource

    @Binds
    @IntoMap
    @OpenBankingProviderKey(OpenBankingProvider.FARABOOM)
    abstract fun bindFaraboomDataSource(
        impl: FaraboomRemoteDataSource
    ): OpenBankingRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionTokenRefresher(
        impl: AuthRepositoryImpl
    ): SessionTokenRefresher

    @Binds
    @Singleton
    abstract fun bindBankingRepository(
        impl: BankingRepositoryImpl
    ): BankingRepository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(
        impl: TransferRepositoryImpl
    ): TransferRepository
}
