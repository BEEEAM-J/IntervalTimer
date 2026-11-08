# 러닝 기록 로직

러닝 중 위치를 기록하고 거리·페이스를 계산하는 도메인 로직을 정리한 문서입니다.
코드는 모두 `:domain:model`(`com.beeeam.domain.model`)에 있는 순수 Kotlin이며, 이슈 #13에서 추가했습니다.

로직을 수정하면 이 문서도 함께 갱신합니다.

## 전체 흐름

```
GPS 위치 수신
   │
   ▼
LocationPoint 로 변환 (위치 + 측정 시각 + 정확도)
   │
   ▼
LocationNoiseFilter.accept()  ── false → 버림
   │ true
   ▼
RunningSession.segments 의 현재 구간에 추가
   │
   ▼
RunningSession.stats() → RunningStats (거리, 운동 시간, 현재/평균 페이스) → 화면 표시
```

위치 수신과 연결(Service, `RunningTracker`)은 이슈 #14, 화면 표시는 #15에서 담당합니다.

## 파일 구성

| 파일 | 내용 |
|---|---|
| `GeoPoint.kt` | 위도·경도, 두 지점 사이 거리 `distanceTo()` |
| `LocationPoint.kt` | GPS가 기록한 한 지점, 구간 거리 `List<LocationPoint>.distanceMeters()` |
| `RunningSession.kt` | 한 번의 러닝 기록. 구간 목록, 거리 `distanceMeters`, 통계 `stats()` |
| `RunningStats.kt` | 화면에 보여 줄 통계 |
| `RunningPace.kt` | 페이스 계산 `paceOf()`, 최근 페이스 `recentPace()` |
| `LocationNoiseFilter.kt` | GPS 노이즈 필터 |

## 단위

| 값 | 단위 / 타입 |
|---|---|
| 거리 | 미터, `Double` (km 변환은 화면에서) |
| 정확도 | 미터, `Float` |
| 시각 | `kotlin.time.Instant` |
| 시간 | `kotlin.time.Duration` |
| 페이스 | 1km 를 달리는 데 걸리는 시간, `Duration` (예: 5분 30초 = 5'30"/km) |

## 모델

### `LocationPoint`

```kotlin
data class LocationPoint(
    val point: GeoPoint,       // 위도·경도 (기존 GeoPoint 재사용)
    val timestamp: Instant,    // 측정 시각
    val accuracy: Float,       // 정확도 반경(m). 작을수록 정확
)
```

- 위도·경도는 따로 두지 않고 `GeoPoint`를 포함합니다. 범위 검증(-90~90, -180~180)과 거리 계산을 그대로 재사용하고, 지도에 그릴 때도 `point`를 바로 꺼내 씁니다.
- `accuracy`는 "실제 위치가 이 반경 안에 있을 가능성이 높다"는 뜻입니다. 음수면 예외를 냅니다.

### `RunningSession`: 구간(segment) 목록

```kotlin
data class RunningSession(
    val id: Long,
    val startedAt: Instant,
    val endedAt: Instant?,                       // 진행 중이면 null
    val segments: List<List<LocationPoint>>,     // 구간 목록
    val duration: Duration,                      // 운동 시간 (일시정지 제외)
    val pausedDuration: Duration,                // 일시정지 시간 합계
)
```

**일시정지했다가 재개할 때마다 새 구간(리스트)을 시작합니다.**

```
A ──달림──▶ B   [일시정지 후 300m 걷기]   C ──달림──▶ D

segments = [[A, …, B], [C, …, D]]
```

- 거리는 **각 구간 안에서만** 더합니다(A→B + C→D). 구간 사이(B→C, 쉬면서 걸은 300m)는 러닝으로 치지 않습니다.
- 지도에서도 구간마다 선을 따로 그리면 일시정지한 사이가 끊긴 채로 보입니다.
- 한 줄 목록 `[A, B, C, D]`로 두면 B→C가 이어져서 걸은 거리가 더해지기 때문에 이 방식을 택했습니다.
- DB에 저장할 때는 지점마다 구간 번호를 붙여 펼쳐서 저장하고, data 계층에서 구간 목록으로 변환합니다.

**거리는 저장하지 않고 계산합니다.**

```kotlin
val distanceMeters: Double
    get() = segments.sumOf { it.distanceMeters() }
```

경로와 거리가 서로 어긋나지 않게 하기 위해서입니다.

**운동 시간과 일시정지 시간은 저장합니다.** 일시정지·재개 시각은 지점 목록만으로 알 수 없으므로, 위치를 기록하는 쪽(#14)이 시간을 재서 넣습니다.

- 운동 시간 = (지금 또는 종료 시각 - 시작 시각) - 일시정지 시간
- 일시정지한 시간은 평균 페이스에 들어가지 않습니다.

### `RunningStats`

```kotlin
data class RunningStats(
    val distanceMeters: Double,    // 달린 거리 (일시정지 사이 거리 제외)
    val elapsed: Duration,         // 운동 시간 (일시정지 시간 제외)
    val currentPace: Duration?,    // 최근 30초의 페이스
    val averagePace: Duration?,    // 전체 평균 페이스
)
```

`RunningSession.stats()`로 만듭니다. 현재 페이스는 **마지막 구간만** 보므로, 재개 직후에는 새 구간에 지점이 모일 때까지 `null`입니다.

## 계산

### 거리: `GeoPoint.distanceTo()`

- 지구를 반지름 6,371km 의 구로 보고, 구 위의 최단 거리(대원 거리)를 구하는 **Haversine 공식**을 씁니다.
- 지구가 완전한 구가 아니어서 생기는 오차(0.5% 이내)는 GPS 자체의 오차(수 m)보다 훨씬 작습니다.
- 도메인을 순수 Kotlin으로 유지하기 위해 `java.lang.Math` 대신 `kotlin.math`를 씁니다.

한 구간의 거리는 이웃한 지점 사이의 거리를 순서대로 더합니다.

```kotlin
fun List<LocationPoint>.distanceMeters(): Double =
    zipWithNext { from, to -> from.point.distanceTo(to.point) }.sum()
```

### 페이스: `paceOf()`

```kotlin
fun paceOf(distanceMeters: Double, duration: Duration): Duration?
// = duration × (1000 / distanceMeters)
```

- 예: 2,500m 를 12분 30초에 달림 → 12분 30초 × (1000 / 2500) = 5분 / km
- **거리가 20m(`MIN_PACE_DISTANCE_METERS`) 미만이면 `null`** 입니다. 출발 직후 3m 를 5초에 이동하면 27분 / km 같은 의미 없는 값이 나오기 때문입니다.

### 현재 페이스: `recentPace()`

- 구간의 **마지막 지점 시각 기준으로 30초(`CURRENT_PACE_WINDOW`) 안의 지점들**만 뒤에서부터 모아 페이스를 계산합니다.
- 평균 페이스는 전체 값이라 천천히 바뀌고, 현재 페이스는 속도를 바꾸면 바로 반영됩니다.
- 지점이 2개 미만이거나 그 사이 거리가 20m 미만이면 `null`입니다.

## GPS 노이즈 필터: `LocationNoiseFilter`

GPS 위치는 항상 조금씩 틀리므로, 받은 지점을 모두 경로에 넣으면 거리가 실제보다 길게 기록됩니다.
새 지점마다 `accept()`로 판단하고, `true`인 지점만 구간에 추가합니다.
**마지막으로 받아들인 지점**을 기준으로 판단하므로 상태가 있는 클래스입니다.

### 판단 순서

| 순서 | 검사 | 결과 |
|---|---|---|
| ① | 정확도 > 20m | 버림 |
| - | 기준 지점이 없음 (첫 지점) | 받아들이고 기준으로 삼음 |
| - | 시각이 기준 지점과 같거나 이전 | 버림 (순서가 뒤바뀌었거나 중복) |
| ② | 기준 지점에서의 속도 > 12m/s | 버림. **5번 연속**이면 새 위치를 기준으로 받아들임 |
| ③ | 이동 거리 < max(정확도, 3m) | 버림 (멈춰 있을 때의 흔들림) |
| - | 모두 통과 | 받아들이고 새 기준으로 삼음 |

### 기준값

`LocationNoiseFilter`의 `companion object` 상수에 모여 있습니다. 러닝을 기준으로 한 **초기값**이며, 야외 테스트 후 조정합니다. 생성자로 다른 값을 넘길 수도 있습니다.

| 상수 | 값 | 근거 |
|---|---|---|
| `MAX_ACCURACY_METERS` | 20m | 야외 GPS 는 보통 3~10m, 건물 사이 10~30m, 실내·Wi-Fi·기지국 위치는 30~100m 이상 |
| `MAX_SPEED_METERS_PER_SECOND` | 12m/s (약 43km/h) | 일반 러너 2.5~5m/s, 단거리 세계 기록의 최고 속도 약 12.4m/s |
| `MAX_CONSECUTIVE_JUMPS` | 5회 | 터널 등에서 GPS 가 끊겼다 멀리서 다시 잡혀도 기록이 멈추지 않게 함 |
| `MIN_MOVE_METERS` | 3m | 정확도가 아주 좋아도 이보다 적은 이동은 흔들림으로 봄 |

조정할 때 참고할 점:
- `MAX_ACCURACY_METERS`를 낮추면 경로가 정확해지지만, 도심에서는 지점이 많이 버려져 경로가 듬성듬성해집니다.
- `MIN_MOVE_METERS`나 정확도 기준이 크면 지점이 덜 자주 들어가 지도 경로가 각져 보이지만, 총 거리에는 거의 영향이 없습니다.

### 예시

```
P1 (정확도 8m)                      → 첫 지점: 받아들임 (기준 = P1)
P2 (1초 뒤, 1m 이동, 정확도 8m)       → ③ 8m 안의 이동: 흔들림으로 버림 (기준은 여전히 P1)
P3 (2초 뒤, P1 에서 6m, 정확도 8m)    → ③ 여전히 8m 안: 버림
P4 (3초 뒤, P1 에서 10m, 정확도 8m)   → 통과: 받아들임 → P1→P4 거리 10m 가 한 번에 더해짐
P5 (1초 뒤, P4 에서 80m)              → ② 80m/s: GPS 가 튐, 버림 (연속 1회)
P6 (정확도 45m)                       → ① 부정확: 버림
```

**버린 지점의 이동은 사라지지 않습니다.** 다음 지점도 마지막으로 받아들인 지점과 비교하므로, 천천히 움직여도 기준을 넘는 순간 그동안의 거리가 한 번에 더해집니다.

### `reset()`

**새 구간을 시작할 때(재개할 때) 반드시 호출합니다.** 호출하지 않으면 재개 후 첫 지점(C)을 일시정지 전 마지막 지점(B)과 비교해서, 쉬면서 이동한 거리 때문에 속도·흔들림 판단이 틀어집니다.

## 위치 기록 쪽(#14)에서 지켜야 할 것

- 지점은 `LocationNoiseFilter.accept()`가 `true`일 때만 현재 구간에 추가합니다.
- 재개할 때는 새 구간(빈 리스트)을 추가하고 `LocationNoiseFilter.reset()`을 호출합니다.
- `duration`과 `pausedDuration`은 일시정지·재개 시각으로 직접 계산해 넣습니다. 일시정지 중에는 `duration`이 늘지 않아야 합니다.
- 화면에 보여 줄 값은 `RunningSession.stats()`로 만듭니다.
