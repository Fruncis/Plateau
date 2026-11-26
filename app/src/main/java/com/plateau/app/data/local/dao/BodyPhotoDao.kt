package com.plateau.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.plateau.app.data.local.entity.BodyPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: BodyPhoto)

    @Query("SELECT * FROM body_photos WHERE id = :id")
    suspend fun getById(id: String): BodyPhoto?

    @Query("SELECT * FROM body_photos WHERE pose = :pose ORDER BY date DESC LIMIT 1")
    fun getLatestByPose(pose: String): Flow<BodyPhoto?>

    @Query("SELECT * FROM body_photos ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<BodyPhoto>>

    @Query("SELECT * FROM body_photos WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<BodyPhoto>>
}