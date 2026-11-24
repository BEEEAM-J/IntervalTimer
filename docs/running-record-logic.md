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
- DB에 저장할 때는 지점마다 구간 번호를 붙여 펼쳐서 저장하고, data 계층에서 구간 목록으로 변환합니다. (아래 "기록 저장" 참고)

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

이 규칙은 `RunningTrackerImpl`(아래)이 구현합니다.

## 기록 상태와 `RunningTracker`

### 구성

| 구성 요소 | 위치 | 역할 |
|---|---|---|
| `TrackingState` | `:domain:model` | 기록 상태. `Idle` / `Running(session)` / `Paused(session)` / `Finished(session)` |
| `RunningTracker` | `:domain:repository` | 화면(feature)이 아는 인터페이스. `state: StateFlow<TrackingState>`, `start() / pause() / resume() / stop()` |
| `RunningTrackerImpl` | `:app` (`tracking/`) | 위치를 받아 기록하는 구현체. `@SingleIn(AppScope::class)`이라 화면과 Service 가 같은 상태를 봅니다. |

### 상태 전환

```
Idle ─start─▶ Running ─pause─▶ Paused ─resume─▶ Running
                 │                 │
                 └──────stop───────┴──▶ Finished ─start─▶ Running (새 기록)
```

상태가 맞지 않는 호출(기록 중이 아닐 때 `pause()` 등)은 무시합니다.

| 동작 | 하는 일 |
|---|---|
| `start()` | 시간을 초기화하고 빈 구간 하나로 새 기록을 만들어 위치 수집을 시작. 필터도 초기화 |
| `pause()` | 위치 수집을 멈추고(위치 요청 해제, GPS 꺼짐) 일시정지 시각을 기록 |
| `resume()` | 일시정지한 시간을 더하고, **새 구간 추가 + 필터 `reset()`** 후 위치 수집 재개 |
| `stop()` | 위치 수집을 멈추고 종료 시각을 넣어 기록을 저장한 뒤 `Finished`로. 지점이 없는 빈 구간은 빼고, 일시정지 중에 마쳤으면 그 시간까지 반영 (아래 "기록 저장" 참고) |

### 시간 계산

- 운동 시간과 일시정지 시간은 **`TimeSource`(단조 시계)** 로 쟽니다. 기기 시간이 바뀌어도(시간대 변경, 자동 동기화) 운동 시간이 틀어지지 않게 하기 위해서입니다.
- 기록에 남길 시작·종료 시각(`startedAt`, `endedAt`)만 `Clock`(실제 시각)을 씁니다.
- 운동 시간 = 시작 후 흐른 시간 - 일시정지 시간(진행 중인 일시정지 포함)
- 위치가 들어오지 않아도 화면의 시간이 흐르도록, 기록 중에는 **1초마다** 운동 시간을 갱신합니다.

### 기준값

`RunningTrackerImpl`의 `companion object` 상수입니다.

| 상수 | 값 | 내용 |
|---|---|---|
| `LOCATION_INTERVAL` | 2초 | 위치를 받고 싶은 간격. 경로가 충분히 매끄럽으면서 배터리를 덜 씁니다. |
| `MIN_LOCATION_INTERVAL` | 1초 | 위치가 더 자주 들어와도 이보다 빠르게는 받지 않습니다. |
| `TIME_UPDATE_INTERVAL` | 1초 | 기록 중 운동 시간 갱신 간격 |

### 포그라운드 Service (`LocationTrackingService`)

화면이 꺼지거나 앱이 백그라운드로 가면 시스템이 위치 수신을 제한하거나 앱을 종료할 수 있습니다. 기록하는 동안 포그라운드 Service 로 앱을 유지합니다.

- 위치를 받고 기록하는 일은 `RunningTrackerImpl`이 하고, Service 는 **포그라운드 유지와 알림**만 맡습니다.
- `start()`는 위치 권한이 없으면 아무것도 하지 않습니다. Android 14 부터 권한 없이 위치 타입 포그라운드 Service 를 시작하면 앱이 종료되기 때문입니다. 권한 요청은 화면의 책임입니다.
- 권한이 있으면 `Running`으로 바꾼 뒤 Service 를 시작합니다. Service 는 `RunningTracker.state`를 보며 알림을 갱신하고, `Finished`(또는 `Idle`)가 되면 스스로 종료합니다.
- 기록 중에 Service 가 먼저 사라지면 위치를 계속 받을 수 없으므로 기록도 마칩니다(`stop()`).
- **복구 정책: 기록 폐기.** 프로세스가 강제 종료되면 기록이 사라지고, Service 는 `START_NOT_STICKY`라 다시 시작되지 않습니다. 기록은 마칠 때만 저장하기 때문이며, 기록 중 1분마다 저장하고 복구하는 방식은 이슈 #28 에서 추가합니다.

**알림** (`TrackingNotification`)
- 기록 중: "러닝 기록 중", 거리, 경과 시간. 경과 시간은 알림의 크로노미터가 스스로 세므로 매초 다시 그리지 않습니다.
- 일시정지: "일시정지됨", 거리와 멈춘 운동 시간
- 거리(10m 단위)나 상태가 바뀔 때만 다시 그립니다. 알림을 누르면 앱이 열립니다.
- 알림 권한(Android 13+)이 없어도 Service 는 돌지만 알림이 보이지 않습니다.

### 동시성

위치 수신, 1초 시간 갱신, 시작·일시정지 같은 조작이 동시에 일어날 수 있습니다. 상태가 꼬이지 않도록 **모든 상태 변경을 한 스레드에서 순서대로** 처리합니다(`Dispatchers.Default.limitedParallelism(1)`). `start()` 등의 호출은 이 스레드에 작업을 넣기만 하고 바로 돌아갑니다.

## 기록 저장 (`RunningRepository`)

마친 기록은 Room DB(`:data:local`의 `db/` 패키지)에 저장합니다. Room을 고른 이유는 이슈 #16 코멘트에 있습니다.

### 자동 저장

사용자가 저장 버튼을 누르지 않아도, `RunningTrackerImpl.stop()`이 기록을 마치면서 저장합니다.

- 화면에서 종료한 경우와 Service 가 사라져 종료한 경우 모두 `stop()`을 지나므로, 화면이 없어도 저장됩니다.
- **저장을 마친 뒤 `Finished`로 바꿉니다.** Service 는 `Finished`가 되면 종료되므로, 먼저 바꾸면 백그라운드에서 저장을 마치기 전에 프로세스가 종료될 수 있습니다. `Finished`의 기록에는 저장소가 정한 id 가 들어 있습니다.
- **지점이 하나도 없는 기록은 저장하지 않습니다.** 시작하자마자 종료했거나 GPS 를 한 번도 잡지 못한 경우입니다. 이때 `Finished`의 id 는 0 입니다.
- 저장에 실패해도(저장 공간 부족 등) 기록 종료는 그대로 진행하고, 실패는 logcat(`RunningTracker` 태그)에 남깁니다.
- 기록 중에는 저장하지 않습니다. 진행 중 저장과 복구는 이슈 #28 에서 다룹니다.

| 구성 요소 | 위치 | 역할 |
|---|---|---|
| `RunningRepository` | `:domain:repository` | `save(session)`(id 반환), `summaries(): Flow`(목록용, 최신순), `session(id)`(상세용), `delete(id)` |
| `RunningSummary` | `:domain:model` | 목록용 요약. 시각, 운동·일시정지 시간, 거리, 평균 페이스. 경로는 없습니다. |
| `RunningRepositoryImpl` | `:data:local` | 도메인 모델 ↔ 엔티티 변환 |
| `RunningDatabase`, `RunningSessionDao` | `:data:local` | Room DB(`running.db`)와 DAO. DB 는 `@SingleIn(AppScope::class)`로 하나만 만듭니다. |

DB 는 **운동 종류별로 따로** 둡니다. 러닝 기록은 `RunningDatabase`, 인터벌 기록은 `IntervalDatabase`(이슈 #27)에 저장합니다. 종류마다 필요한 값(러닝은 경로와 거리, 인터벌은 세트 설정)과 버전 관리가 서로 영향을 주지 않게 하기 위해서입니다. 여러 종류를 함께 보여 줄 때는 각 Repository 에서 읽어 합칩니다.

### 테이블

| 테이블 | 열 |
|---|---|
| `running_session` | `id`(자동), `started_at`, `ended_at`, `duration_millis`, `paused_duration_millis`, `distance_meters` |
| `location_point` | `id`(자동), `session_id`(외래 키), `segment_index`, `point_index`, `latitude`, `longitude`, `timestamp`, `accuracy` |

- 시각은 1970년 기준 밀리초, 시간은 밀리초로 저장합니다.
- **구간 목록을 펼쳐서 저장합니다.** 지점마다 구간 번호(`segment_index`)와 구간 안 순서(`point_index`)를 붙이고, 읽을 때 이 두 값으로 정렬해 다시 구간 목록으로 묶습니다. Room 은 관계로 읽은 지점의 순서를 보장하지 않기 때문입니다.
- **거리는 목록 표시용 사본으로 저장합니다.** 도메인에서는 거리를 경로로 계산하지만, 목록 화면에서 기록마다 지점 수천 개를 읽지 않도록 저장할 때 계산한 값을 함께 둡니다. 저장할 때 `distanceMeters`로 계산하므로 경로와 어긋나지 않습니다.
- 기록과 경로는 **하나의 트랜잭션**으로 저장합니다. 중간에 실패하면 둘 다 저장되지 않습니다.
- 기록을 지우면 외래 키(`CASCADE`)로 경로도 함께 지워집니다.
- **목록과 상세를 따로 읽습니다.** 목록(`summaries()`)은 `running_session`만 읽고, 경로는 상세(`session(id)`)에서 기록 하나만 읽습니다. Room 의 `Flow` 쿼리는 테이블이 바뀔 때마다 처음부터 다시 실행되므로, 목록에서 경로까지 읽으면 저장할 때마다 모든 기록의 지점을 다시 읽게 됩니다.
- 진행 중인 기록(`endedAt == null`)은 저장할 수 없습니다. 저장하면 DB 가 id 를 정합니다. (저장 전 기록의 id 는 0)
- **테이블을 바꾸면** `RunningDatabase`의 `version`을 올리고 마이그레이션을 추가합니다. 버전별 구조는 `data/local/schemas/`에 JSON 으로 남기고 커밋합니다.
