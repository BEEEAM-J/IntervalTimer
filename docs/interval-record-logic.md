# 인터벌 기록 로직

인터벌 운동 기록을 저장하는 로직을 정리한 문서입니다. 이슈 #27 에서 추가했습니다.

로직을 수정하면 이 문서도 함께 갱신합니다.

## 모델

### `IntervalSession` (`:domain:model`)

```kotlin
data class IntervalSession(
    val id: Long,                 // DB 가 정함. 저장 전에는 0
    val startedAt: Instant,       // 준비 카운트다운이 끝나 운동을 시작한 시각
    val endedAt: Instant?,        // 진행 중이면 null
    val config: IntervalConfig,   // 세트 수, 운동·휴식 시간
    val completedSets: Int,       // 마친 세트 수
    val duration: Duration,       // 운동한 시간 (일시정지 제외)
)
```

- `isCompleted`: 모든 세트를 마쳤는지(`completedSets == config.sets`). `false`이고 `endedAt`이 있으면 중간에 그만둔 기록입니다.

### 마친 세트 수: `IntervalConfig.completedSetsAt(elapsed)`

**세트의 운동 구간이 끝나면 그 세트를 마친 것으로 봅니다.** 휴식은 따지지 않습니다.

| 시점 | 마친 세트 수 |
|---|---|
| n 세트 운동 중 | n - 1 |
| n 세트 휴식 중 | n |
| 모든 세트 완료 | 전체 세트 수 |

- 마지막 세트 뒤에는 휴식이 없습니다.
- 운동을 끝내고 쉬는 중에 그만둬도 그 세트는 한 것으로 보는 것이 자연스럽기 때문입니다.
- 휴식 시간이 0 이면 운동 구간이 끝나는 순간 다음 세트 운동이 시작되므로, 그때 마친 세트 수가 늘어납니다.

## 저장 시점 (`IntervalTimerPresenter`)

타이머는 Presenter 안에서 돌기 때문에, 기록도 Presenter 가 저장합니다. 사용자가 저장 버튼을 누르지 않아도 자동으로 저장합니다.

| 시점 | 저장 |
|---|---|
| 준비 카운트다운이 끝나 운동을 시작할 때 (건너뛰기 포함) | 진행 중인 기록 생성 (`endedAt` 없음, 마친 세트 0) |
| 마친 세트 수가 늘었을 때 | 진행 상황 갱신 (**세트마다 저장**) |
| 일시정지할 때 | 진행 상황 갱신 |
| 모든 세트를 마쳤을 때 | 종료 (`endedAt` 기록, 완료) |
| 중간에 "종료"를 눌렀을 때 | 종료 (`endedAt` 기록, 마친 세트까지) |

- 준비 카운트다운 중에 그만두면 운동을 시작하지 않았으므로 기록을 만들지 않습니다.
- 종료까지 저장한 기록은 다시 바꾸지 않습니다. 완료 화면의 "종료" 버튼도 같은 `Stop` 이벤트이지만, 이미 저장한 완료 기록을 덮어쓰지 않습니다.
- 시작·종료 시각은 `Clock`(실제 시각), 운동 시간은 타이머의 경과 시간(`TimeSource`)입니다. 둘 다 테스트를 위해 생성자로 주입받습니다.
- **저장은 호출한 순서대로 하나씩 처리합니다(`Mutex`).** 시작 직후 바로 일시정지해도 기록을 만든 뒤에 갱신합니다.
- **저장 중에 탭을 옮겨도 끝까지 저장합니다(`NonCancellable`).** 화면이 사라지면 Presenter 의 코루틴이 취소되지만, 저장은 짧은 DB 쓰기 한 번이라 끝까지 실행합니다.
- 운동 시간은 전체 시간을 넘지 않게 저장합니다. 타이머가 0.1초마다 경과 시간을 갱신해서, 끝난 직후에는 조금 넘을 수 있기 때문입니다.

### 알려진 한계

- 타이머가 Presenter 안에서 돌기 때문에, **다른 탭에 있는 동안 끝난 세트는 타이머 탭으로 돌아와 경과 시간을 따라잡을 때 저장됩니다.** 그래서 다른 탭에 있는 동안 앱이 강제 종료되면 탭을 떠나기 전까지의 진행만 남습니다.
- 같은 이유로, 다른 탭에 있는 동안 모든 세트가 끝나면 종료 시각은 타이머 탭으로 돌아온 시각으로 남습니다.
- 이 한계를 없애려면 러닝처럼 타이머를 화면 밖(Tracker + Service)으로 옮겨야 합니다.

## 저장 (`IntervalRepository`)

| 구성 요소 | 위치 | 역할 |
|---|---|---|
| `IntervalRepository` | `:domain:repository` | `create(session)`(id 반환), `update(session)`, `sessions(): Flow`(마친 기록, 최신순), `delete(id)` |
| `IntervalRepositoryImpl` | `:data:local` | 도메인 모델 ↔ 엔티티 변환. 저장할 때마다 `updated_at`을 지금 시각으로 남김 |
| `IntervalDatabase`, `IntervalSessionDao` | `:data:local` | Room DB(`interval.db`)와 DAO. DB 는 `@SingleIn(AppScope::class)`로 하나만 만듭니다. |

러닝 기록(`RunningDatabase`)과 DB 를 따로 둡니다. 운동 종류마다 필요한 값과 버전 관리가 서로 영향을 주지 않게 하기 위해서입니다.

### 테이블

| 테이블 | 열 |
|---|---|
| `interval_session` | `id`(자동), `started_at`, `ended_at`(진행 중이면 비어 있음), `updated_at`(마지막 저장 시각), `sets`, `work_millis`, `rest_millis`, `completed_sets`, `duration_millis` |

- 시각은 1970년 기준 밀리초, 시간은 밀리초로 저장합니다.
- 설정값(`IntervalConfig`)은 열로 펼쳐서 저장합니다.
- 경로처럼 무거운 데이터가 없으므로, 러닝 기록과 달리 목록용과 상세용 조회를 나누지 않습니다.
- `sessions()`는 마친 기록(`ended_at`이 있음)만 돌려줍니다.
- **테이블을 바꾸면** `IntervalDatabase`의 `version`을 올리고 마이그레이션을 추가합니다. 버전별 구조는 `data/local/schemas/`에 JSON 으로 남기고 커밋합니다.

### 강제 종료로 남은 기록 정리

앱이 운동 중에 강제 종료되면 종료를 저장할 기회가 없어서 `ended_at`이 빈 채로 남습니다.

```
19:00:00  운동 시작        → 생성 (ended_at 없음, completed_sets 0)
19:01:00  1세트 완료       → 갱신 (completed_sets 1, updated_at 19:01:00)
19:02:00  2세트 완료       → 갱신 (completed_sets 2, updated_at 19:02:00)
19:02:30  앱 강제 종료
다음 실행 → DB 를 열 때 ended_at = 19:02:00 으로 마무리 → "2세트까지 하고 중단"
```

- **DB 를 열 때(Room `onOpen` 콜백)** `ended_at`이 비어 있는 기록을 `ended_at = updated_at`으로 마무리합니다.
- DB 는 앱 프로세스마다 한 번 열리고, 새 프로세스가 시작된 시점에는 진행 중인 운동이 있을 수 없습니다. 그래서 이때 남아 있는 진행 중 기록은 모두 이전 실행에서 끝나지 못한 것입니다.
- 모든 조회·저장은 DB 를 연 뒤에 실행되므로, 새 기록을 만들거나 목록을 읽기 **전에 항상 먼저 정리됩니다.**
- 인터벌 타이머는 이어서 할 수 없으므로 러닝 기록(#28)과 달리 복구하지 않고 "중단"으로만 남깁니다.
