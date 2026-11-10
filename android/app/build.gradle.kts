plugins {
    id("com.android.application")
}

android {
    namespace = "com.muzigoing.eten41ime"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.muzigoing.eten41"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // 目前只編 arm64：實機與 Apple Silicon 上的模擬器都是 arm64
            abiFilters += "arm64-v8a"
        }
    }

    sourceSets.getByName("main") {
        // scripts/build-libchewing-android.sh 的輸出（libeten41jni.so、詞庫、授權檔）
        jniLibs.srcDir("../../build/android/jniLibs")
        assets.srcDir("../../build/android/assets")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.1")
}
