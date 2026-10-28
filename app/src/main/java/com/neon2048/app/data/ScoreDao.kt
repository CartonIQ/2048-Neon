package com.neon2048.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(entity: ScoreEntity): Long

    @Query("SELECT * FROM scores ORDER BY score DESC, timestamp ASC LIMIT 10")
    suspend fun getTop10(): List<ScoreEntity>

    @Query("DELETE FROM scores")
    suspend fun clear()

    @Query("DELETE FROM scores WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scores WHERE id NOT IN (SELECT id FROM scores ORDER BY score DESC, timestamp ASC LIMIT 10)")
    suspend fun trim()
}
