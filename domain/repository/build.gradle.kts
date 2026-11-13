plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    api(project(":domain:model"))
    // 인터페이스가 Flow 를 반환하므로, 이 모듈을 쓰는 쪽도 바로 쓸 수 있게 api 로 둔다.
    api(libs.kotlinx.coroutines.core)
}
