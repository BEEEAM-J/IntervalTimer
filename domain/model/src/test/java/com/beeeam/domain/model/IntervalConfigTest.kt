package com.beeeam.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class IntervalConfigTest {

    // 3세트, 운동 30초, 휴식 10초 → 30 + 10 + 30 + 10 + 30 = 110초
    private val config = IntervalConfig(sets = 3, workDuration = 30.seconds, restDuration = 10.seconds)

    @Test
    fun `총 소요 시간은 마지막 세트 후 휴식을 제외한다`() {
        assertEquals(110.seconds, config.totalDuration)
    }

    @Test
    fun `1세트면 휴식 시간이 포함되지 않는다`() {
        val single = IntervalConfig(sets = 1, workDuration = 30.seconds, restDuration = 10.seconds)
        assertEquals(30.seconds, single.totalDuration)
    }

    @Test
    fun `시작 시점은 1세트 운동이다`() {
        assertEquals(
            TimerProgress(Phase.WORK, currentSet = 1, remainingSets = 3, remainingInPhase = 30.seconds, remainingTotal = 110.seconds),
            config.progressAt(Duration.ZERO),
        )
    }

    @Test
    fun `운동 도중에는 현재 세트 운동 남은 시간을 계산한다`() {
        assertEquals(
            TimerProgress(Phase.WORK, currentSet = 1, remainingSets = 3, remainingInPhase = 18.seconds, remainingTotal = 98.seconds),
            config.progressAt(12.seconds),
        )
    }

    @Test
    fun `운동 종료 시점은 휴식 시작이며 해당 세트는 완료로 센다`() {
        assertEquals(
            TimerProgress(Phase.REST, currentSet = 1, remainingSets = 2, remainingInPhase = 10.seconds, remainingTotal = 80.seconds),
            config.progressAt(30.seconds),
        )
    }

    @Test
    fun `휴식 종료 시점은 다음 세트 운동 시작이다`() {
        assertEquals(
            TimerProgress(Phase.WORK, currentSet = 2, remainingSets = 2, remainingInPhase = 30.seconds, remainingTotal = 70.seconds),
            config.progressAt(40.seconds),
        )
    }

    @Test
    fun `마지막 세트 운동 중에는 휴식 없이 종료까지 남은 시간이 같다`() {
        assertEquals(
            TimerProgress(Phase.WORK, currentSet = 3, remainingSets = 1, remainingInPhase = 5.seconds, remainingTotal = 5.seconds),
            config.progressAt(105.seconds),
        )
    }

    @Test
    fun `총 소요 시간에 도달하면 종료된다`() {
        val finished = TimerProgress(Phase.FINISHED, currentSet = 3, remainingSets = 0, remainingInPhase = Duration.ZERO, remainingTotal = Duration.ZERO)
        assertEquals(finished, config.progressAt(110.seconds))
        assertEquals(finished, config.progressAt(200.seconds))
    }

    @Test
    fun `밀리초 단위 경과 시간도 계산한다`() {
        assertEquals(
            TimerProgress(Phase.REST, currentSet = 1, remainingSets = 2, remainingInPhase = 500.milliseconds, remainingTotal = 70.5.seconds),
            config.progressAt(39.5.seconds),
        )
    }

    @Test
    fun `휴식 시간이 0이면 세트가 바로 이어진다`() {
        val noRest = IntervalConfig(sets = 2, workDuration = 20.seconds, restDuration = Duration.ZERO)
        assertEquals(40.seconds, noRest.totalDuration)
        assertEquals(
            TimerProgress(Phase.WORK, currentSet = 2, remainingSets = 1, remainingInPhase = 20.seconds, remainingTotal = 20.seconds),
            noRest.progressAt(20.seconds),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `세트 수는 1 이상이어야 한다`() {
        IntervalConfig(sets = 0, workDuration = 30.seconds, restDuration = 10.seconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `운동 시간은 0보다 커야 한다`() {
        IntervalConfig(sets = 1, workDuration = Duration.ZERO, restDuration = 10.seconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `휴식 시간은 음수일 수 없다`() {
        IntervalConfig(sets = 1, workDuration = 30.seconds, restDuration = (-1).seconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `경과 시간은 음수일 수 없다`() {
        config.progressAt((-1).seconds)
    }
}
