package com.android.multitimerpro.di

import android.content.Context
import androidx.room.Room
import com.android.multitimerpro.data.local.AppDatabase
import com.android.multitimerpro.data.local.TimerDao
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
        ).build()
    }

    @Provides
    fun provideTimerDao(db: AppDatabase): TimerDao {
        return db.timerDao()
    }
}
