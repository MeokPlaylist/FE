plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

/* Kotlin 2.0+ 사용 시: android{} 블록 바깥! */
kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.example.meokpli"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.meokpli"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    /* ☑️ Java 21 지정 */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    /* ☑️ Kotlin 컴파일 타깃도 21 */
    kotlinOptions { jvmTarget = "21" }


}

dependencies {
    val nav_version = "2.7.7"  // 최신 확인 가능

    implementation("androidx.navigation:navigation-fragment-ktx:${nav_version}")
    implementation("androidx.navigation:navigation-ui-ktx:${nav_version}")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // --- AndroidX 기본 ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")   // ← 하나만
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("io.coil-kt:coil:2.6.0")

    // --- Auth / Kakao / Google ---
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.kakao.sdk:v2-user:2.21.5")
    implementation("com.kakao.sdk:v2-auth:2.21.5")

    // --- 네트워크 ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")                 // 권장 추가
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)    // (선택) 로그

    // --- 테스트 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}