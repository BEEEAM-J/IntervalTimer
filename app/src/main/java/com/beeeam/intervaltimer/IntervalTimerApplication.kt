package com.beeeam.intervaltimer

import android.app.Application
import com.beeeam.intervaltimer.di.AndroidAppGraph
import dev.zacsweers.metro.createGraphFactory

class IntervalTimerApplication : Application() {

    /**
     * 앱 전역 그래프. Activity 뿐 아니라 Service 등 다른 Android 컴포넌트에서도
     * 생성 시점과 무관하게 접근할 수 있도록 lazy 로 생성한다.
     */
    val appGraph: AndroidAppGraph by lazy {
        createGraphFactory<AndroidAppGraph.Factory>().create(this)
    }
}
