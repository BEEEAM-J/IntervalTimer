package com.beeeam.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * 러닝 기록 한 건. 경로는 [LocationPointEntity] 에 따로 저장한다.
 *
 * 도메인 모델은 거리를 경로로 계산하지만, 목록 화면에서 기록마다 지점 수천 개를 읽지 않도록
 * 저장할 때 계산한 거리를 [distanceMeters] 에 함께 둔다.
 */
@Entity(tableName = "running_session")
data class RunningSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at") val endedAtEpochMillis: Long,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "paused_duration_millis") val pausedDurationMillis: Long,
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double,
)

/**
 * 경로의 한 지점.
 *
 * 도메인 모델의 구간 목록(`List<List<LocationPoint>>`)을 펼쳐서 저장하므로, 몇 번째 구간의 몇 번째 지점인지를 함께 둔다.
 * 기록을 지우면 지점도 함께 지워진다.
 */
@Entity(
    tableName = "location_point",
    foreignKeys = [
        ForeignKey(
            entity = RunningSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id")],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "segment_index") val segmentIndex: Int,
    @ColumnInfo(name = "point_index") val pointIndex: Int,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "timestamp") val timestampEpochMillis: Long,
    val accuracy: Float,
)

/** 기록과 그 경로를 함께 읽기 위한 관계. */
data class RunningSessionWithPoints(
    @Embedded val session: RunningSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val points: List<LocationPointEntity>,
)
