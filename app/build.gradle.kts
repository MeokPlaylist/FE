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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.android.material:material:1.12.0")
    // 최신 material-components
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    /* ✅ Google Sign-In 최신 */
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    //카카오 로그인
    implementation("com.kakao.sdk:v2-user:2.21.5")
    implementation("com.kakao.sdk:v2-auth:2.21.5")

    /* Retrofit */
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
