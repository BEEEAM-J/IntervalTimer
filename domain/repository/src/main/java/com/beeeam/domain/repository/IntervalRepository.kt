package com.beeeam.domain.repository

import com.beeeam.domain.model.IntervalSession
import kotlinx.coroutines.flow.Flow

/**
 * 인터벌 운동 기록을 기기에 저장하고 불러온다.
 *
 * 운동을 시작할 때 [create] 로 진행 중인 기록을 만들고, 세트를 마치거나 종료할 때 [update] 로 갱신한다.
 * 앱이 강제 종료돼 진행 중으로 남은 기록은 다음 실행 때 마지막으로 저장한 시각에 종료한 것으로 마무리된다.
 */
interface IntervalRepository {

    /** 기록을 새로 저장하고, 저장소가 정한 id 를 돌려준다. */
    suspend fun create(session: IntervalSession): Long

    /** [create] 로 저장한 기록(같은 id)을 [session] 의 내용으로 바꾼다. */
    suspend fun update(session: IntervalSession)

    /** 마친 기록을 최신순으로. 진행 중인 기록은 빠진다. 저장하거나 지우면 새 목록이 다시 온다. */
    fun sessions(): Flow<List<IntervalSession>>

    suspend fun delete(id: Long)
}
