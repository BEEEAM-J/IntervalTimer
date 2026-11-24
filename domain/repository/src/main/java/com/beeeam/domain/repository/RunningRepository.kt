package com.beeeam.domain.repository

import com.beeeam.domain.model.RunningSession
import com.beeeam.domain.model.RunningSummary
import kotlinx.coroutines.flow.Flow

/** 마친 러닝 기록을 기기에 저장하고 불러온다. */
interface RunningRepository {

    /**
     * 마친 기록을 저장하고, 저장소가 정한 id 를 돌려준다.
     *
     * 진행 중인 기록(`endedAt == null`)은 저장할 수 없다.
     */
    suspend fun save(session: RunningSession): Long

    /** 목록용 요약(경로 제외)을 최신순으로. 저장하거나 지우면 새 목록이 다시 온다. */
    fun summaries(): Flow<List<RunningSummary>>

    /** 경로를 포함한 기록 하나. 없으면 null. */
    suspend fun session(id: Long): RunningSession?

    suspend fun delete(id: Long)
}
