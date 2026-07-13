import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun getLocalProperty(key: String): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return ""
    val props = Properties()
    file.inputStream().use { props.load(it) }
    return props.getProperty(key) ?: ""
}

val baiduApiKey: String = getLocalProperty("BAIDU_API_KEY")

if (baiduApiKey.isEmpty()) {
    logger.lifecycle(
        "WARNING: Baidu Maps API key not found. Location features will not work. " +
                "Add your API key to local.properties:\n" +
                "  BAIDU_API_KEY=your_api_key\n" +
                "Get one at: https://lbsyun.baidu.com/"
    )
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    // 有签名文件时使用自定义签名 否则 fallback 到 debug 签名
    val releaseStoreFilePath = getLocalProperty("RELEASE_STORE_FILE")
    val hasReleaseSigningConfig = releaseStoreFilePath.isNotEmpty()
            && rootProject.file(releaseStoreFilePath).exists()

    if (!hasReleaseSigningConfig) {
        logger.lifecycle(
            "WARNING: Release signing config not found in local.properties. " +
                    "Using debug signing. To sign with your own key, add these properties to local.properties:\n" +
                    "  RELEASE_STORE_FILE=keystore/release.jks\n" +
                    "  RELEASE_STORE_PASSWORD=...\n" +
                    "  RELEASE_KEY_ALIAS=...\n" +
                    "  RELEASE_KEY_PASSWORD=..."
        )
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath)
                storePassword = getLocalProperty("RELEASE_STORE_PASSWORD")
                keyAlias = getLocalProperty("RELEASE_KEY_ALIAS")
                keyPassword = getLocalProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    namespace = "com.cookieshax.coursehelper"
    compileSdk = 37

    packaging {
        jniLibs {
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/liblocSDK8b.so"
            keepDebugSymbols += "**/libBaiduMapSDK*.so"
            keepDebugSymbols += "**/liblocSDK*.so"
        }
    }

    androidResources {
        // 仅保留中文资源 剔除依赖库中的其他语言
        localeFilters += listOf("zh")
    }

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "com.cookieshax.coursehelper"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BAIDU_API_KEY", "\"$baiduApiKey\"")
        manifestPlaceholders["BAIDU_API_KEY"] = baiduApiKey

        // 按 ABI 拆分 APK
        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                isUniversalApk = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseSigningConfig) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        /*debug {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }*/
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 百度地图 SDK
    implementation(libs.baidumapsdk.location.all)
    implementation(libs.baidumapsdk.map)
    implementation(libs.baidumapsdk.search)

    // Material Icons Extended
    implementation(libs.androidx.material.icons.extended)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // CameraX
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.camera2)

    // ML Kit
    implementation(libs.barcode.scanning)

    // ZXing
    implementation(libs.android)

    // LiveData
    implementation(libs.androidx.runtime.livedata)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Retrofit
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // WebKit
    implementation(libs.androidx.webkit)

    // HCT Color
    implementation(libs.material.color.utilities)
    implementation(libs.material.kolor)

    // OpenCV
    implementation(libs.opencv)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}

tasks.register("buildReleaseApk") {
    group = "build"
    description = "build release APK"
    dependsOn("assembleRelease")
}
