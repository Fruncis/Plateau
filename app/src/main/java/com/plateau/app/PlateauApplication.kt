package com.plateau.app

import android.app.Application
import com.plateau.app.data.repository.SeedRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PlateauApplication : Application() {

    @Inject lateinit var seedRepository: SeedRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { seedRepository.seedIfEmpty() }
    }
}
