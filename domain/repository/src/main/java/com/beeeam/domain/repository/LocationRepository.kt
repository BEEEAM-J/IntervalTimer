package com.beeeam.domain.repository

import com.beeeam.domain.model.GeoPoint

/** 기기의 위치를 조회한다. */
interface LocationRepository {

    /**
     * 현재 위치를 한 번 조회한다.
     *
     * 위치 권한 확인과 요청은 호출하는 쪽(UI)의 책임이다.
     * 권한이 없거나, 기기의 위치 설정이 꺼져 있는 등 위치를 알 수 없으면 null 을 반환한다.
     */
    suspend fun getCurrentLocation(): GeoPoint?
}
