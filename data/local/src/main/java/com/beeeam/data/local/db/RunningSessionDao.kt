package com.beeeam.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RunningSessionDao {

    /**
     * 기록과 경로를 함께 저장한다. 하나의 트랜잭션이라 중간에 실패하면 둘 다 저장되지 않는다.
     *
     * @param points 저장할 경로. 기록의 id 는 저장한 뒤에 정해지므로, 그 id 를 받아 지점 목록을 만든다.
     */
    @Transaction
    open suspend fun insert(session: RunningSessionEntity, points: (sessionId: Long) -> List<LocationPointEntity>): Long {
        val sessionId = insertSession(session)
        insertPoints(points(sessionId))
        return sessionId
    }

    /** 목록용. 지점은 읽지 않으므로 지점 수와 상관없이 가볍다. */
    @Query("SELECT * FROM running_session ORDER BY started_at DESC")
    abstract fun sessions(): Flow<List<RunningSessionEntity>>

    /** 상세용. 기록 하나와 그 경로를 함께 읽는다. */
    @Transaction
    @Query("SELECT * FROM running_session WHERE id = :id")
    abstract suspend fun sessionWithPoints(id: Long): RunningSessionWithPoints?

    /** 지점은 외래 키(CASCADE)로 함께 지워진다. */
    @Query("DELETE FROM running_session WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Insert
    protected abstract suspend fun insertSession(session: RunningSessionEntity): Long

    @Insert
    protected abstract suspend fun insertPoints(points: List<LocationPointEntity>)
}
