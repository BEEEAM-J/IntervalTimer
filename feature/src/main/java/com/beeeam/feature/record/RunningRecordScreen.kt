package com.beeeam.feature.record

import com.beeeam.domain.model.GeoPoint
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import kotlinx.parcelize.Parcelize

/** 지도에 현재 위치를 보여주는 러닝 기록 측정 화면. */
@Parcelize
data object RunningRecordScreen : ParcelableScreen {

    /**
     * @property permission 위치 권한 상태. Activity 가 있어야 확인할 수 있으므로 Ui 가 확인해서 [Event.PermissionUpdated] 로 알려준다.
     * @property location 현재 위치 조회 상태.
     */
    data class State(
        val permission: LocationPermission,
        val location: CurrentLocation,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        /** Ui 가 권한을 확인하거나 권한 요청 결과를 받았을 때. */
        data class PermissionUpdated(val permission: LocationPermission) : Event

        /** 현재 위치를 다시 조회한다. (위치를 찾지 못했을 때 재시도 등) */
        data object RefreshLocation : Event
    }
}

enum class LocationPermission {
    /** 아직 확인하지 않음. */
    UNKNOWN,

    /** 정확한 위치 또는 대략적 위치 중 하나 이상 허용. */
    GRANTED,

    /** 거부됨. 다시 요청할 수 있다. */
    DENIED,

    /** "다시 묻지 않음" 등으로 거부되어 앱에서 다시 요청할 수 없다. 설정 화면에서만 허용할 수 있다. */
    PERMANENTLY_DENIED,
}

sealed interface CurrentLocation {
    /** 권한이 없어 조회하지 않음. */
    data object Idle : CurrentLocation

    data object Loading : CurrentLocation

    data class Found(val point: GeoPoint) : CurrentLocation

    /** 권한은 있지만 위치를 알 수 없음. (기기의 위치 설정이 꺼져 있는 등) */
    data object Unavailable : CurrentLocation
}
