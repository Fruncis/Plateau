package com.plateau.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.android.scopes.InstallIn
import dagger.hilt.android.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class.java)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.preferencesDataStore("plateau_prefs")
    }
}