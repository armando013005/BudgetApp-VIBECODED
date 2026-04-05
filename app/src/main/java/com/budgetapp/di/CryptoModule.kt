package com.budgetapp.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule
// CryptoManager and PasswordHasher are provided via @Inject constructor
