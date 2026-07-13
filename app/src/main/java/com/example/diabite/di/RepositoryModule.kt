package com.example.diabite.di

import android.content.Context
import androidx.room.Room
import com.example.diabite.data.local.CacheManager
import com.example.diabite.data.local.FoodDao
import com.example.diabite.data.local.FoodDatabase
import com.example.diabite.data.repository.AuthRepositoryImpl
import com.example.diabite.data.repository.CachedFoodRepository
import com.example.diabite.data.repository.FirestoreFoodRepository
import com.example.diabite.data.repository.GeminiRepository
import com.example.diabite.domain.repository.AuthRepository
import com.example.diabite.domain.repository.FoodRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFoodRepository(
        cachedFoodRepository: CachedFoodRepository
    ): FoodRepository

    companion object {
        @Provides
        @Singleton
        fun provideFoodDatabase(
            @ApplicationContext context: Context
        ): FoodDatabase {
            return Room.databaseBuilder(
                context,
                FoodDatabase::class.java,
                FoodDatabase.DATABASE_NAME
            ).build()
        }

        @Provides
        @Singleton
        fun provideFoodDao(database: FoodDatabase): FoodDao {
            return database.foodDao()
        }

        @Provides
        @Singleton
        fun provideCacheManager(foodDao: FoodDao): CacheManager {
            return CacheManager(foodDao)
        }

        @Provides
        @Singleton
        fun provideFirestoreRepository(
            firestore: FirebaseFirestore
        ): FirestoreFoodRepository {
            return FirestoreFoodRepository(firestore)
        }

        @Provides
        @Singleton
        fun provideGeminiRepository(): GeminiRepository {
            return GeminiRepository()
        }
    }
}
