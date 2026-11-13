package com.beeeam.domain.repository

import com.beeeam.domain.model.GeoPoint
import com.beeeam.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * 기기의 위치를 조회한다.
 *
 * 위치 권한 확인과 요청은 호출하는 쪽(UI)의 책임이다.
 */
interface LocationRepository {

    /**
     * 현재 위치를 한 번 조회한다.
     *
     * 권한이 없거나, 기기의 위치 설정이 꺼져 있는 등 위치를 알 수 없으면 null 을 반환한다.
     */
    suspend fun getCurrentLocation(): GeoPoint?

    /**
     * 위치를 계속 받는다. 러닝 경로 기록에 쓰므로 측정 시각과 정확도가 담긴 [LocationPoint] 를 내보낸다.
     *
     * 수집을 멈추면(코루틴 취소, 화면·Service 종료 등) 위치 요청도 함께 해제된다.
     * 권한이 없으면 아무것도 내보내지 않고 바로 끝난다.
     *
     * @param interval 원하는 업데이트 간격. 기기 상황에 따라 더 늦게 올 수 있다.
     * @param minInterval 다른 앱의 요청 등으로 위치가 더 자주 들어와도 이보다 빠르게는 받지 않는다.
     */
    fun locationUpdates(interval: Duration, minInterval: Duration): Flow<LocationPoint>
}
