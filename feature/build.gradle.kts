plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.metro)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.beeeam.feature"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

ksp {
    // @CircuitInject 로 생성되는 Presenter/Ui 팩토리를 Metro 멀티바인딩(@ContributesIntoSet)으로 만든다.
    arg("circuit.codegen.mode", "metro")
}

dependencies {
    implementation(project(":domain:model"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.retained)
    implementation(libs.circuit.overlay)
    implementation(libs.circuit.codegen.annotations)
    ksp(libs.circuit.codegen)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.circuit.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
