package com.beeeam.intervaltimer.di

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/**
 * Android 앱의 최상위 그래프.
 *
 * `@ContributesTo(AppScope::class)` 로 기여된 인터페이스는 이 그래프에 자동으로 합쳐지므로,
 * 다른 모듈(예: ForegroundService 가 위치할 모듈)은 app 모듈을 몰라도 필요한 바인딩을 노출할 수 있다.
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {

    /**
     * 그래프에서 주입되는 [Context] 는 항상 Application Context 다.
     *
     * Activity 단위 그래프가 없으므로 싱글톤에 Activity Context 가 들어가 누수될 일이 없다.
     * 테마가 필요한 UI 작업이나 권한 요청처럼 Activity Context 가 필요한 경우에는
     * 주입받지 말고 `LocalContext.current` 나 함수 인자로 받는다.
     */
    @Binds val Application.bindContext: Context

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AndroidAppGraph
    }
}
