package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.PhotoAnalysis
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: PhotoAnalysis)

    @Query("SELECT * FROM photo_analyses WHERE photoId = :photoId")
    suspend fun getByPhotoId(photoId: String): PhotoAnalysis?

    @Query("SELECT * FROM photo_analyses WHERE photoId = :photoId AND comparedToPhotoId = :comparedToPhotoId")
    suspend fun getComparison(photoId: String, comparedToPhotoId: String): PhotoAnalysis?
}