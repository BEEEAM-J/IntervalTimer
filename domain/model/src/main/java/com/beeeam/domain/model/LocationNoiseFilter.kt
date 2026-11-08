package com.beeeam.domain.model

/**
 * GPS 노이즈를 걸러 경로에 넣을 지점만 고른다.
 *
 * GPS 위치는 항상 조금씩 틀리므로, 받은 지점을 모두 경로에 넣으면 거리가 실제보다 길게 기록된다.
 * 지점이 들어올 때마다 [accept] 로 판단하고, true 인 지점만 구간에 추가한다.
 *
 * 마지막으로 받아들인 지점을 기준으로 판단하므로 한 구간 안에서만 이어서 쓴다.
 * 일시정지 후 재개해 새 구간을 시작할 때는 [reset] 해서, 재개 후 첫 지점을 일시정지 전 지점과 비교하지 않게 한다.
 *
 * 기준값은 러닝을 기준으로 한 초기값이다. 야외에서 실제로 기록해 보며 조정한다.
 */
class LocationNoiseFilter(
    private val maxAccuracyMeters: Float = MAX_ACCURACY_METERS,
    private val maxSpeedMetersPerSecond: Double = MAX_SPEED_METERS_PER_SECOND,
    private val maxConsecutiveJumps: Int = MAX_CONSECUTIVE_JUMPS,
    private val minMoveMeters: Double = MIN_MOVE_METERS,
) {
    private var lastAccepted: LocationPoint? = null
    private var consecutiveJumps = 0

    /** [candidate] 를 경로에 넣어야 하면 true. true 를 반환하면 다음 판단의 기준 지점이 된다. */
    fun accept(candidate: LocationPoint): Boolean {
        // ① 정확도가 낮은 지점 (실내, Wi-Fi·기지국으로만 잡힌 위치 등)
        if (candidate.accuracy > maxAccuracyMeters) return false

        val last = lastAccepted ?: return acceptAsReference(candidate)
        val elapsed = candidate.timestamp - last.timestamp
        // 시각이 같거나 앞선 지점은 순서가 뒤바뀌었거나 중복된 위치다.
        if (!elapsed.isPositive()) return false

        val distance = last.point.distanceTo(candidate.point)

        // ② 사람이 달릴 수 없는 속도로 이동한 지점 (GPS 가 순간적으로 튐)
        val speed = distance / (elapsed.inWholeMilliseconds / 1000.0)
        if (speed > maxSpeedMetersPerSecond) {
            consecutiveJumps++
            // 터널 등에서 GPS 가 끊겼다가 멀리서 다시 잡히면 실제 이동인데도 계속 튄 것으로 보인다.
            // 연속으로 제외되면 새 위치를 기준으로 다시 시작해, 이후 지점이 모두 버려지는 것을 막는다.
            return if (consecutiveJumps >= maxConsecutiveJumps) acceptAsReference(candidate) else false
        }
        consecutiveJumps = 0

        // ③ 멈춰 있을 때의 흔들림. 정확도 반경 안의 이동은 실제 이동인지 알 수 없다.
        // 버린 지점의 이동은 사라지지 않는다. 다음 지점도 마지막으로 받아들인 지점과 비교하므로,
        // 천천히 움직여도 기준을 넘는 순간 그동안의 거리가 한 번에 더해진다.
        if (distance < maxOf(candidate.accuracy.toDouble(), minMoveMeters)) return false

        return acceptAsReference(candidate)
    }

    /** 새 구간을 시작할 때 호출한다. 이전 구간의 지점을 기준으로 판단하지 않게 한다. */
    fun reset() {
        lastAccepted = null
        consecutiveJumps = 0
    }

    private fun acceptAsReference(point: LocationPoint): Boolean {
        lastAccepted = point
        consecutiveJumps = 0
        return true
    }

    companion object {
        /** 이보다 정확도 반경이 크면(부정확하면) 버린다. 야외 GPS 는 보통 3~10m, 건물 사이는 10~30m. */
        const val MAX_ACCURACY_METERS = 20f

        /** 이보다 빠르면 GPS 가 튄 것으로 본다. 약 시속 43km 로, 단거리 세계 기록의 최고 속도(약 12.4m/s) 수준이다. */
        const val MAX_SPEED_METERS_PER_SECOND = 12.0

        /** 튄 것으로 보고 연속으로 이만큼 버리면 새 위치를 기준으로 다시 시작한다. */
        const val MAX_CONSECUTIVE_JUMPS = 5

        /** 정확도가 아주 좋아도 이보다 적게 움직이면 흔들림으로 본다. */
        const val MIN_MOVE_METERS = 3.0
    }
}
