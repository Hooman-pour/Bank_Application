package com.example.di

import com.example.core.security.EncryptedSessionManager
import com.example.core.security.SessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSessionManager(
        impl: EncryptedSessionManager
    ): SessionManager
}
