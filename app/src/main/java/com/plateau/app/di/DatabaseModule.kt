package com.plateau.app.di

import android.content.Context
import androidx.room.Room
import com.plateau.app.data.local.PlateauDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.SingletonComponent
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlateauDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PlateauDatabase::class.java,
            "plateau.db"
        ).fallbackToDestructiveMigration().build()
    }
}