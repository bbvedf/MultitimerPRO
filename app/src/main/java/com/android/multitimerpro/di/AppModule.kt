package com.android.multitimerpro.di

import android.content.Context
import androidx.room.Room
import com.android.multitimerpro.data.AppDatabase
import com.android.multitimerpro.data.TimerDao
import com.android.multitimerpro.data.HistoryDao
import com.android.multitimerpro.data.GoogleAuthClient
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGoogleAuthClient(@ApplicationContext context: Context): GoogleAuthClient {
        return GoogleAuthClient(context)
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        // Usamos el ID específico de tu base de datos que aparece en la captura
        return Firebase.firestore("ai-studio-ccfd95a4-1b33-4d90-8119-cff5243e3752")
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "multitimer_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTimerDao(db: AppDatabase): TimerDao {
        return db.timerDao()
    }

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao {
        return db.historyDao()
    }
}
