package com.plateau.app.di

import com.plateau.app.data.repository.ExerciseRepository
import com.plateau.app.data.repository.PlanRepository
import com.plateau.app.data.repository.ProgressRepository
import com.plateau.app.data.repository.SessionRepository
import com.plateau.app.data.repository.UserProfileRepository
import com.plateau.app.data.repository.SeedRepository
import com.plateau.app.domain.usecase.GetActivePlanUseCase
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePlanRepository(database: com.plateau.app.data.local.PlateauDatabase): PlanRepository =
        PlanRepository(database)

    @Provides
    @Singleton
    fun provideExerciseRepository(database: com.plateau.app.data.local.PlateauDatabase): ExerciseRepository =
        ExerciseRepository(database)

    @Provides
    @Singleton
    fun provideProgressRepository(database: com.plateau.app.data.local.PlateauDatabase): ProgressRepository =
        ProgressRepository(database)

    @Provides
    @Singleton
    fun provideSessionRepository(database: com.plateau.app.data.local.PlateauDatabase): SessionRepository =
        SessionRepository(database)

    @Provides
    @Singleton
    fun provideUserProfileRepository(database: com.plateau.app.data.local.PlateauDatabase): UserProfileRepository =
        UserProfileRepository(database)

    @Provides
    @Singleton
    fun provideSeedRepository(database: com.plateau.app.data.local.PlateauDatabase): SeedRepository =
        SeedRepository(database)

    @Provides
    @Singleton
    fun provideGetActivePlanUseCase(planRepository: PlanRepository): GetActivePlanUseCase =
        GetActivePlanUseCase(planRepository)
}