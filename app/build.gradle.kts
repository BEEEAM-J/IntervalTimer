plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.beeeam.intervaltimer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.beeeam.intervaltimer"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

secrets {
    // 실제 키는 git 에 올라가지 않는 local.properties 에 둔다.
    propertiesFileName = "local.properties"
    // 키가 없는 환경(새로 clone, CI)에서도 빌드되도록 커밋된 기본값을 쓴다.
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(project(":feature"))
    // @ContributesBinding 으로 기여된 구현체가 AndroidAppGraph 에 합쳐지려면 app 의 classpath 에 있어야 한다.
    implementation(project(":data:local"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.circuit.foundation)
    debugImplementation(libs.leakcanary.studio)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
