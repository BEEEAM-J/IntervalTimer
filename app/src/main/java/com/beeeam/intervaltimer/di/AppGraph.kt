package com.beeeam.intervaltimer.di

/**
 * 플랫폼과 무관한 앱 공통 그래프.
 *
 * KMP 전환 시 commonMain 으로 옮길 수 있도록 Android 타입에 의존하지 않는다.
 * 실제 `@DependencyGraph` 는 플랫폼별 그래프(예: [AndroidAppGraph])가 이 인터페이스를 상속해 선언한다.
 * commonMain 에 선언된 그래프는 플랫폼 소스셋의 contribution 을 볼 수 없기 때문이다.
 */
interface AppGraph
