package com.beeeam.domain.repository

import com.beeeam.domain.model.RunningSession
import kotlinx.coroutines.flow.Flow

/** 마친 러닝 기록을 기기에 저장하고 불러온다. */
interface RunningRepository {

    /**
     * 마친 기록을 저장하고, 저장소가 정한 id 를 돌려준다.
     *
     * 진행 중인 기록(`endedAt == null`)은 저장할 수 없다.
     */
    suspend fun save(session: RunningSession): Long

    /** 저장된 기록을 최신순으로. 저장하거나 지우면 새 목록이 다시 온다. */
    fun sessions(): Flow<List<RunningSession>>

    suspend fun delete(id: Long)
}
