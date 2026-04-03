package com.android.multitimerpro.di

import android.content.Context
import androidx.room.Room
import com.android.multitimerpro.data.AppDatabase
import com.android.multitimerpro.data.TimerDao
import com.android.multitimerpro.data.GoogleAuthClient
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
    @Singleton
    fun provideGoogleAuthClient(@ApplicationContext context: Context): GoogleAuthClient {
        return GoogleAuthClient(context)
    }
}
