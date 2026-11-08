package com.beeeam.domain.model

import kotlin.time.Duration

/**
 * 러닝 중 / 종료 후 화면에 보여줄 통계.
 *
 * 페이스는 1km 를 달리는 데 걸리는 시간(min/km)이다. 거리가 너무 짧으면 값이 비정상적으로 커지므로 null 로 둔다.
 *
 * @property distanceMeters 달린 거리(미터). 일시정지 구간 사이의 거리는 포함하지 않는다.
 * @property elapsed 운동 시간. 일시정지한 시간은 포함하지 않는다.
 * @property currentPace 최근 구간의 페이스. 지금 얼마나 빠르게 달리고 있는지를 나타낸다.
 * @property averagePace 전체 운동 시간 기준의 평균 페이스.
 */
data class RunningStats(
    val distanceMeters: Double,
    val elapsed: Duration,
    val currentPace: Duration?,
    val averagePace: Duration?,
)
