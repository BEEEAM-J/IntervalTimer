package com.beeeam.data.local.db

import com.beeeam.domain.model.IntervalConfig
import com.beeeam.domain.model.IntervalSession
import com.beeeam.domain.repository.IntervalRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Room 에 인터벌 운동 기록을 저장하는 [IntervalRepository] 구현체.
 *
 * @param clock 저장할 때마다 마지막 저장 시각(`updated_at`)을 남기는 시계.
 */
@ContributesBinding(AppScope::class)
@Inject
class IntervalRepositoryImpl(
    private val dao: IntervalSessionDao,
    private val clock: Clock = Clock.System,
) : IntervalRepository {

    override suspend fun create(session: IntervalSession): Long = dao.insert(session.toEntity())

    override suspend fun update(session: IntervalSession) {
        require(session.id != 0L) { "Only a created session can be updated" }
        dao.update(session.toEntity())
    }

    override fun sessions(): Flow<List<IntervalSession>> =
        dao.finishedSessions().map { list -> list.map { it.toDomain() } }

    override suspend fun delete(id: Long) = dao.delete(id)

    private fun IntervalSession.toEntity() = IntervalSessionEntity(
        id = id,
        startedAtEpochMillis = startedAt.toEpochMilliseconds(),
        endedAtEpochMillis = endedAt?.toEpochMilliseconds(),
        updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
        sets = config.sets,
        workMillis = config.workDuration.inWholeMilliseconds,
        restMillis = config.restDuration.inWholeMilliseconds,
        completedSets = completedSets,
        durationMillis = duration.inWholeMilliseconds,
    )

    private fun IntervalSessionEntity.toDomain() = IntervalSession(
        id = id,
        startedAt = Instant.fromEpochMilliseconds(startedAtEpochMillis),
        endedAt = endedAtEpochMillis?.let { Instant.fromEpochMilliseconds(it) },
        config = IntervalConfig(
            sets = sets,
            workDuration = workMillis.milliseconds,
            restDuration = restMillis.milliseconds,
        ),
        completedSets = completedSets,
        duration = durationMillis.milliseconds,
    )
}
