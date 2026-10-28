package com.beeeam.intervaltimer.di

import android.app.Application
import dev.zacsweers.metro.AppScope
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

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AndroidAppGraph
    }
}
