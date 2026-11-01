package com.beeeam.intervaltimer.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * 플랫폼과 무관한 앱 공통 그래프.
 *
 * KMP 전환 시 commonMain 으로 옮길 수 있도록 Android 타입에 의존하지 않는다.
 * 실제 `@DependencyGraph` 는 플랫폼별 그래프(예: [AndroidAppGraph])가 이 인터페이스를 상속해 선언한다.
 * commonMain 에 선언된 그래프는 플랫폼 소스셋의 contribution 을 볼 수 없기 때문이다.
 */
interface AppGraph {

    val circuit: Circuit

    /** `@CircuitInject` 로 생성되어 [AppScope] 에 기여된 Presenter/Ui 팩토리로 [Circuit] 을 만든다. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideCircuit(
        presenterFactories: Set<Presenter.Factory>,
        uiFactories: Set<Ui.Factory>,
    ): Circuit = Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
        .build()
}
