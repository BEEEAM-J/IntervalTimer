package com.beeeam.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IntervalSessionDao {

    @Insert
    suspend fun insert(session: IntervalSessionEntity): Long

    @Update
    suspend fun update(session: IntervalSessionEntity)

    /** 마친 기록만 최신순으로. 진행 중인 기록(`ended_at` 이 비어 있음)은 뺀다. */
    @Query("SELECT * FROM interval_session WHERE ended_at IS NOT NULL ORDER BY started_at DESC")
    fun finishedSessions(): Flow<List<IntervalSessionEntity>>

    @Query("DELETE FROM interval_session WHERE id = :id")
    suspend fun delete(id: Long)
}
