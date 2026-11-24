package com.beeeam.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 인터벌 운동 기록 한 건. 설정값(세트 수, 운동·휴식 시간)은 열로 펼쳐서 저장한다.
 *
 * [updatedAtEpochMillis] 는 마지막으로 저장한 시각이다. 앱이 강제 종료돼 [endedAtEpochMillis] 가 비어 있는 기록을
 * 다음 실행 때 이 시각에 종료한 것으로 마무리하는 데 쓴다. ([IntervalDatabase] 참고)
 */
@Entity(tableName = "interval_session")
data class IntervalSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at") val endedAtEpochMillis: Long?,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    val sets: Int,
    @ColumnInfo(name = "work_millis") val workMillis: Long,
    @ColumnInfo(name = "rest_millis") val restMillis: Long,
    @ColumnInfo(name = "completed_sets") val completedSets: Int,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
)
