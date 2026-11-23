package com.beeeam.data.local.db

import com.beeeam.domain.model.GeoPoint
import com.beeeam.domain.model.LocationPoint
import com.beeeam.domain.model.RunningSession
import com.beeeam.domain.repository.RunningRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Room 에 러닝 기록을 저장하는 [RunningRepository] 구현체.
 *
 * 도메인의 구간 목록은 지점마다 구간 번호와 순서를 붙여 펼쳐서 저장하고, 읽을 때 다시 구간 목록으로 묶는다.
 */
@ContributesBinding(AppScope::class)
@Inject
class RunningRepositoryImpl(
    private val dao: RunningSessionDao,
) : RunningRepository {

    override suspend fun save(session: RunningSession): Long {
        val endedAt = requireNotNull(session.endedAt) { "Only a finished session can be saved" }
        val entity = RunningSessionEntity(
            startedAtEpochMillis = session.startedAt.toEpochMilliseconds(),
            endedAtEpochMillis = endedAt.toEpochMilliseconds(),
            durationMillis = session.duration.inWholeMilliseconds,
            pausedDurationMillis = session.pausedDuration.inWholeMilliseconds,
            distanceMeters = session.distanceMeters,
        )
        return dao.insert(entity) { sessionId -> session.segments.toEntities(sessionId) }
    }

    override fun sessions(): Flow<List<RunningSession>> =
        dao.sessionsWithPoints().map { list -> list.map { it.toDomain() } }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun List<List<LocationPoint>>.toEntities(sessionId: Long): List<LocationPointEntity> =
        flatMapIndexed { segmentIndex, segment ->
            segment.mapIndexed { pointIndex, point ->
                LocationPointEntity(
                    sessionId = sessionId,
                    segmentIndex = segmentIndex,
                    pointIndex = pointIndex,
                    latitude = point.point.latitude,
                    longitude = point.point.longitude,
                    timestampEpochMillis = point.timestamp.toEpochMilliseconds(),
                    accuracy = point.accuracy,
                )
            }
        }

    private fun RunningSessionWithPoints.toDomain(): RunningSession = RunningSession(
        id = session.id,
        startedAt = Instant.fromEpochMilliseconds(session.startedAtEpochMillis),
        endedAt = Instant.fromEpochMilliseconds(session.endedAtEpochMillis),
        // Room 은 관계로 읽은 지점의 순서를 보장하지 않으므로 구간 번호와 순서로 다시 정렬해 묶는다.
        segments = points
            .groupBy { it.segmentIndex }
            .toSortedMap()
            .values
            .map { segment -> segment.sortedBy { it.pointIndex }.map { it.toDomain() } },
        duration = session.durationMillis.milliseconds,
        pausedDuration = session.pausedDurationMillis.milliseconds,
    )

    private fun LocationPointEntity.toDomain() = LocationPoint(
        point = GeoPoint(latitude = latitude, longitude = longitude),
        timestamp = Instant.fromEpochMilliseconds(timestampEpochMillis),
        accuracy = accuracy,
    )
}
