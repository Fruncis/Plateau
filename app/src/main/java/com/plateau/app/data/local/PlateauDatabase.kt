package com.plateau.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.plateau.app.data.local.entity.BodyMetric
import com.plateau.app.data.local.entity.BodyPhoto
import com.plateau.app.data.local.entity.Exercise
import com.plateau.app.data.local.entity.Goal
import com.plateau.app.data.local.entity.MuscleGroup
import com.plateau.app.data.local.entity.PhotoAnalysis
import com.plateau.app.data.local.entity.Plan
import com.plateau.app.data.local.entity.PlanDay
import com.plateau.app.data.local.entity.PlanExercise
import com.plateau.app.data.local.entity.SetLog
import com.plateau.app.data.local.entity.UserProfile
import com.plateau.app.data.local.entity.WorkoutSession
import com.plateau.app.data.local.converter.Converters

@Database(
    entities = [
        MuscleGroup::class,
        Exercise::class,
        Plan::class,
        PlanDay::class,
        PlanExercise::class,
        WorkoutSession::class,
        SetLog::class,
        BodyMetric::class,
        BodyPhoto::class,
        PhotoAnalysis::class,
        Goal::class,
        UserProfile::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlateauDatabase : RoomDatabase() {
    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun planDayDao(): PlanDayDao
    abstract fun planExerciseDao(): PlanExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun bodyPhotoDao(): BodyPhotoDao
    abstract fun photoAnalysisDao(): PhotoAnalysisDao
    abstract fun goalDao(): GoalDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile private var INSTANCE: PlateauDatabase? = null

        fun getInstance(context: Context): PlateauDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlateauDatabase::class.java,
                    "plateau.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}