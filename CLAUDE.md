# IntervalTimer

세트 수와 운동/휴식 시간을 설정해 인터벌 운동을 진행하는 Android 타이머 앱 (러닝 기록·히스토리 기능 추가 예정)

## 기술 스택
- 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 아키텍처: Circuit (Screen / Presenter / Ui, 단방향 데이터 흐름) + 멀티 모듈 클린 아키텍처
- 비동기: Kotlin Coroutines / Flow
- 의존성 주입: Metro (Circuit codegen과 KSP로 연동)
- 메모리 누수 감지: LeakCanary (debug 빌드 전용, Android Studio Profiler 연동 라이브러리 `com.android.tools.studio.leakcanary:leakcanary`로 추가)
- 지도·위치: Google Maps SDK + `maps-compose`, `play-services-location`(`FusedLocationProviderClient`)
- minSdk 24 / targetSdk·compileSdk 37 / Java 17 (`maps-compose`가 JVM 17 대상이라 11에서는 inline 함수를 쓸 수 없음)

## 빌드 및 테스트 명령어
- 디버그 빌드: `./gradlew :app:assembleDebug`
- 전체 단위 테스트: `./gradlew test`
- 모듈별 단위 테스트
  - 도메인(JVM 모듈): `./gradlew :domain:model:test`
  - 기능(Android 모듈): `./gradlew :feature:testDebugUnitTest`
- 린트: `./gradlew lint`

Windows(PowerShell)에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다.

- 파일을 삭제하거나 함수를 다른 파일로 옮긴 직후 증분 빌드에서 Metro 컴파일러 플러그인이 `An operation is not implemented: Unknown file`로 실패할 수 있습니다. 코드 문제가 아니라 증분 빌드 캐시 문제로, 해당 모듈을 clean한 뒤 다시 빌드하면 됩니다. (예: `./gradlew :feature:clean :app:assembleDebug`)

### Maps API 키
- 실제 키는 git에 올라가지 않는 `local.properties`에 `MAPS_API_KEY=...`로 둡니다. Secrets Gradle Plugin이 manifest의 `com.google.android.geo.API_KEY`에 넣습니다.
- 키가 없으면 커밋된 `local.defaults.properties`의 기본값이 쓰여 빌드는 되지만, 지도는 표시되지 않습니다.

### 메모리 누수 확인
- Android Studio Profiler에서 debug 앱 프로세스를 선택하고 **Find Memory Leaks with LeakCanary** task를 실행합니다.
  - 시작 시점은 **Now**(실행 중인 프로세스에 연결)를 사용합니다. 연동 라이브러리가 앱에 들어 있으므로 Process start가 필요 없습니다.
  - 단일 Activity 구조라 탭 전환만으로는 종료되는 Activity가 없으므로, **화면 회전**을 반드시 포함합니다. 회전할 때마다 `MainActivity`가 다시 생성되어 이전 Activity의 회수 여부를 확인할 수 있습니다.
- debug 앱은 평소에도 `leakcanary-android`가 누수를 감시하며, 누수가 있으면 기기 알림과 logcat(`LeakCanary` 태그)으로 알려줍니다.
- release 빌드에 포함되지 않았는지는 `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` 출력에 leakcanary가 없는 것으로 확인합니다.

## 프로젝트 구조
패키지 루트는 `com.beeeam` 입니다.

- `:app` (`com.beeeam.intervaltimer`): `Application`, `MainActivity`, Metro 의존성 그래프
  - `di/AppGraph.kt`: 플랫폼 무관 공통 그래프. `Circuit` 인스턴스를 제공 (KMP 전환 대비로 Android 타입에 의존하지 않음)
  - `di/AndroidAppGraph.kt`: 실제 `@DependencyGraph`. `Application`을 `Context`로 바인딩
- `:feature` (`com.beeeam.feature`): 화면 단위 Circuit Screen / Presenter / Ui
  - `main/`: 바텀바로 탭을 전환하는 메인 화면 (`MainScreen`, `MainTab`)
  - `timer/`: 인터벌 타이머 화면 (설정 → 준비 카운트다운 → 진행 → 완료)
  - `record/`: 러닝 측정 화면. 위치 권한을 확인·요청하고 지도에 현재 위치를 표시 (디자인 전 임시 화면)
    - `RecordMapContent`(권한 허용: 지도), `RecordPermissionContent`(거부·영구 거부 안내), `LocationPermissions.kt`(권한 목록과 권한 관련 유틸)
    - 권한 확인과 요청은 Activity가 필요하므로 Ui에서 처리하고, 결과만 `PermissionUpdated` 이벤트로 Presenter에 전달합니다.
  - `history/`: 러닝 기록 화면 (현재 `PlaceholderUi`, 구현 예정)
  - `component/`: 여러 화면에서 쓰는 공통 Composable (`WheelPicker`, `DurationPicker` 등)
  - `util/Const.kt`: 색상 상수와 피커 값 범위
- `:domain:model`: 순수 Kotlin 도메인 모델과 계산 로직 (`IntervalConfig`, `TimerProgress`, `Phase`, `GeoPoint`)
- `:domain:repository`: Repository 인터페이스 (`LocationRepository`)
- `:domain:usecase`: UseCase (현재 비어 있음)
- `:data:local` (`com.beeeam.data.local`): 기기 안의 데이터를 다루는 구현. 책임별로 패키지를 나눕니다.
  - `location/`: `LocationRepositoryImpl` (`FusedLocationProviderClient`로 현재 위치 조회). 위치 권한도 이 모듈의 manifest에 선언합니다.
  - `db/`: 러닝 기록 저장용 DB (구현 예정)
  - 패키지가 커지면 별도 모듈로 분리합니다. 구현체는 `@ContributesBinding`으로만 연결되므로 feature·domain 코드는 바뀌지 않습니다.
- `:data:remote`: 서버 통신 구현 (현재 비어 있음). GPS처럼 기기에서 얻는 데이터는 여기가 아니라 `:data:local`에 둡니다.

의존 방향: `app` → `feature`, `data:local` / `feature` → `domain:model`, `domain:repository` / `data` → `domain`. 도메인 모듈은 Android 의존성 없이 순수 JVM 모듈로 유지합니다.
- `app`이 data 모듈을 의존하는 것은 Metro 때문입니다. `@ContributesBinding` 구현체는 그래프(`AndroidAppGraph`)가 있는 app의 classpath에 있어야 그래프에 합쳐집니다.

## 코드 규칙
- Kotlin 공식 코딩 컨벤션을 따릅니다.
- 화면은 Circuit 패턴으로 작성합니다. ViewModel은 사용하지 않습니다.
  - `XxxScreen`: `@Parcelize data object` + `ParcelableScreen`. 안에 `State`(`CircuitUiState`, 마지막 파라미터로 `eventSink: (Event) -> Unit`)와 `sealed interface Event`(`CircuitUiEvent`)를 정의합니다.
  - `XxxPresenter`: `@CircuitInject(XxxScreen::class, AppScope::class)` + `@Inject`. `@Composable present()`에서 상태를 만들고 이벤트를 처리합니다.
  - `XxxUi`: `@CircuitInject(XxxScreen::class, AppScope::class)`가 붙은 `@Composable` 함수. `State`만 그리고, 사용자 입력은 `eventSink`로 전달합니다.
- Presenter/Ui는 `@CircuitInject`로 자동 등록되므로 `Circuit`에 수동 등록하지 않습니다.
- 탭 이동·설정 변경 후에도 유지돼야 하는 Presenter 상태는 `rememberRetained`를 사용합니다.
- 시간은 `kotlin.time.Duration` / `TimeSource`로 다루고, 테스트를 위해 `TimeSource`는 생성자로 주입받습니다.
- 순수 계산 로직은 `:domain:model`에 두고, Presenter에는 상태 관리만 둡니다.
- `:domain:repository`의 Repository 인터페이스 구현체는 data 계층에 두고 `XxxRepositoryImpl`로 이름을 짓습니다. (예: `LocationRepository` → `LocationRepositoryImpl`) 구현체도 `XxxRepository`라고 부르면 인터페이스와 헷갈리기 때문입니다.
- 문자열은 하드코딩하지 않고 각 모듈의 `res/values/strings.xml`에 둡니다.
- 색상 등 공통 상수는 `feature/util/Const.kt`에 `internal`로 둡니다.
- KDoc과 주석은 기존 코드처럼 한국어로, 의도(왜)를 중심으로 작성합니다.
- 테스트 코드는 사용자가 직접 작성합니다. 기능을 구현할 때 테스트 코드를 작성하지 않습니다. (도메인 로직은 JUnit, Presenter는 `circuit-test`를 사용)
  - 기존 테스트(`IntervalConfigTest`, `GeoPointTest`)는 참고용으로 남겨 둡니다.
- 기존 코드 스타일과 네이밍을 우선 따릅니다.

## 작업 방식
- 코드를 수정하기 전에 관련 파일을 먼저 읽고, 변경 계획을 짧게 설명합니다.
- 한 번에 큰 변경을 하지 않고, 작은 단위로 나눠서 수정합니다.
- 변경 후에는 빌드가 통과하는지 확인합니다.
- 요청하지 않은 파일은 수정하지 않습니다. 단, 아래 CLAUDE.md 갱신은 예외입니다.
- 모듈·패키지 구조, 기술 스택, 코드 규칙에 영향을 주는 변경을 하면 CLAUDE.md의 관련 항목도 같은 작업 안에서 함께 수정합니다.
  - 특히 `구현 예정`, `추가 예정`, `현재 비어 있음`으로 표시된 항목을 구현하면 해당 표시를 지우고 실제 구조와 역할로 바꿉니다.
  - CLAUDE.md 변경은 코드 변경과 별도의 커밋으로 분리합니다.

## 하지 말아야 할 것
- `build/`, `.gradle/` 폴더와 `local.properties`는 수정하지 않습니다.
- API 키나 비밀번호를 코드에 직접 넣지 않습니다.
- 라이브러리 버전은 `gradle/libs.versions.toml`에서만 관리하고, 임의로 올리지 않습니다.
- 사용자가 요청하기 전에는 `git push`를 하지 않습니다.

## 커밋 및 PR 규칙

### 작성 원칙
- 커밋 메시지와 PR은 **영어**로 작성합니다.
- 작성하기 전에 `git log`와 이전 PR을 확인하고, **같은 형태와 톤**으로 작성합니다.
- 커밋은 **하나의 목적 단위로 묶어서** 진행합니다. 하나의 커밋에는 하나의 목적만 담되, 파일마다 따로 쪼개지 않습니다.
  - 같은 목적의 설정·의존성 변경은 한 커밋으로 묶습니다. (예: 플러그인 적용 + `libs.versions.toml` 항목 + manifest 설정 → "지도 API 사용을 위한 의존성 설정")
  - 함께 쓰이는 변경은 한 커밋으로 묶습니다. (예: 도메인 모델 + 그 모델을 쓰는 Repository 인터페이스, Repository 구현체 + 그것을 그래프에 연결하는 모듈 의존성)
- 커밋하기 전에 `git status`와 `git diff`로 변경 내용을 확인하고, 관련 없는 파일은 포함하지 않습니다.

### 형식
커밋과 PR 모두 아래 형식을 따릅니다.

```
Title

Detailed description of the changes

Test
- Gradle build [ ]
- APK runs normally [ ]
- Tested the changed behavior on a device [ ]

Related issues
```

### 작성 방법
- 제목은 한 줄로 간결하게 쓰고, 무엇을 했는지가 드러나게 합니다.
- 상세 작업 내용에는 **무엇을, 왜** 변경했는지 씁니다.
- 테스트 항목은 **실제로 확인한 항목만** `[x]`로 체크하고, 확인하지 못한 항목은 `[ ]`로 남깁니다. 확인하지 않은 항목을 체크하지 않습니다.
- 관련 이슈가 없으면 `None`으로 적습니다.


## 응답 언어
- 설명과 대화는 한국어로 답합니다.
- 코드 주석은 한국어로 작성합니다.
- 커밋 메시지와 PR은 영어로 작성합니다.