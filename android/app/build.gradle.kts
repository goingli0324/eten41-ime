import java.util.Properties

plugins {
    id("com.android.application")
}

// 上架用的 upload key 不放 repo（這是公開 repo）。預設讀 ~/keystores/eten41-keystore.properties，
// 也可用環境變數 ETEN41_KEYSTORE_PROPERTIES 指到別的檔；找不到就照常建置，只是 release 不簽章。
val keystorePropertiesFile = file(
    System.getenv("ETEN41_KEYSTORE_PROPERTIES")
        ?: "${System.getProperty("user.home")}/keystores/eten41-keystore.properties",
)
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.muzigoing.eten41ime"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.muzigoing.eten41"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // 目前只編 arm64：實機與 Apple Silicon 上的模擬器都是 arm64
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
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
