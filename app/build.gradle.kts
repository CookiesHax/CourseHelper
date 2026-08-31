import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.encoding.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun getSecret(envName: String, propertyName: String = envName): String {
    // 优先从系统环境变量读取
    val envValue = System.getenv(envName)
    if (!envValue.isNullOrEmpty()) return envValue

    // 其次从 local.properties 读取
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val localProperties = Properties()
        localPropertiesFile.inputStream().use { stream ->
            localProperties.load(stream)
        }
        val propValue = localProperties.getProperty(propertyName)
        if (!propValue.isNullOrEmpty()) return propValue
    }

    // 最后使用 project 属性或默认值兜底
    return (project.findProperty(propertyName) as? String) ?: ""
}

// OpenCV 自动化 Task 配置
val opencvVersion = providers.gradleProperty("OPENCV_VERSION").getOrElse("4.13.0")
val opencvSdkUrl =
    "https://github.com/opencv/opencv/releases/download/$opencvVersion/opencv-$opencvVersion-android-sdk.zip"
val opencvHome = layout.buildDirectory.dir("opencv-sdk/$opencvVersion").get().asFile
val opencvJniDir = file("${opencvHome.absolutePath}/OpenCV-android-sdk/sdk/native/jni")

// 注册解压 Task
val setupOpenCVTask = tasks.register("setupOpenCV") {
    group = "build setup"
    description = "Setup OpenCV SDK for CMake"

    // 声明输入输出 实现 UP-TO-DATE 跳过机制
    inputs.property("version", opencvVersion)
    inputs.property("sdkUrl", opencvSdkUrl)
    outputs.dir(opencvHome)

    doLast {
        if (!opencvJniDir.exists()) {
            logger.lifecycle("OpenCV SDK directory not found. Downloading $opencvVersion...")
            val tempZip = file("${layout.buildDirectory.get().asFile}/opencv-$opencvVersion.zip")
            tempZip.parentFile.mkdirs()

            URI.create(opencvSdkUrl).toURL().openStream().use { input ->
                Files.copy(input, tempZip.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            logger.lifecycle("Unzipping OpenCV SDK...")
            copy {
                from(zipTree(tempZip))
                into(opencvHome)
            }
            tempZip.delete()
            logger.lifecycle("OpenCV SDK setup complete: ${opencvHome.absolutePath}")
        }
    }
}

// CMake 配置任务明确依赖 setupOpenCV
tasks.configureEach {
    if (name.startsWith("configureCMake")) {
        dependsOn(setupOpenCVTask)
    }
}

// 挂载到 preBuild
tasks.named("preBuild") {
    dependsOn(setupOpenCVTask)
}


val baiduApiKey: String = getSecret("BAIDU_API_KEY")
val baiduApiKeyDebug: String =
    getSecret("BAIDU_API_KEY_DEBUG").let { it.ifEmpty { baiduApiKey } }

if (baiduApiKey.isEmpty()) {
    logger.lifecycle(
        "WARNING: Baidu Maps API key not found. Location features will not work. " +
                "Add your API key to local.properties:\n" +
                "  BAIDU_API_KEY=your_api_key\n" +
                "  BAIDU_API_KEY_DEBUG=your_debug_api_key (only used in debug mode)\n" +
                "Get one at: https://lbsyun.baidu.com/"
    )
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    // 获取路径配置 若未指定则默认 fallback 到 "keystore/release.jks"
    val configuredPath = getSecret("RELEASE_STORE_FILE_PATH")
        .ifBlank { "keystore/keystore.jks" }

    // 检查此路径对应的文件是否存在
    var releaseStoreFile = rootProject.file(configuredPath)

    // 如果路径文件不存在 尝试读取 Base64 密钥并生成文件
    if (!releaseStoreFile.exists()) {
        val base64Content = getSecret("RELEASE_STORE_FILE_BASE64")

        if (base64Content.isNotBlank()) {
            releaseStoreFile.parentFile?.mkdirs()
            releaseStoreFile.writeBytes(Base64.decode(base64Content.trim()))
            logger.lifecycle("Generated release store file at: ${releaseStoreFile.absolutePath}")
        }
    }

    // 校验最终文件是否存在
    val hasReleaseSigningConfig = releaseStoreFile.exists()
    if (!hasReleaseSigningConfig) {
        logger.lifecycle(
            "WARNING: Release signing config not found at '${releaseStoreFile.path}'. Using debug signing.\n" +
                    "To sign with your own key, place your keystore at '${configuredPath}' or set RELEASE_STORE_FILE_BASE64."
        )
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = getSecret("RELEASE_STORE_PASSWORD")
                keyAlias = getSecret("RELEASE_KEY_ALIAS")
                keyPassword = getSecret("RELEASE_KEY_ALIAS_PASSWORD")
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

        manifestPlaceholders["appName"] = "CourseHelper"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("")
                abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                // 使用静态库链接 路径指向自动下载的 SDK
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DOpenCV_DIR=${opencvJniDir.absolutePath}"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
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
            manifestPlaceholders["BAIDU_API_KEY"] = baiduApiKey
        }

        debug {
            applicationIdSuffix = ".debug"
            val baseAppName = defaultConfig.manifestPlaceholders["appName"]
            manifestPlaceholders["appName"] = "${baseAppName}-Debug"
            manifestPlaceholders["BAIDU_API_KEY"] = baiduApiKeyDebug
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 按 ABI 拆分 APK
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
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

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}

tasks.register("keystoreBase64Report") {
    group = "build"
    description = "Convert keystore to Base64 (Local execution only)"

    doLast {
        // 如果在 CI 环境运行 直接拒绝执行
        val isCiEnv = System.getenv("CI")?.toBoolean() == true ||
                System.getenv("GITHUB_ACTIONS")?.toBoolean() == true
        if (isCiEnv) {
            error("SECURITY WARNING: 'keystoreBase64Report' task is strictly disabled on CI environment to prevent secret leaks!")
        }

        val keystoreFilePath = getSecret("RELEASE_STORE_FILE_PATH")
            .ifBlank { "keystore/keystore.jks" }
        val keystoreFile = rootProject.file(keystoreFilePath)

        if (keystoreFile.exists()) {
            val base64Content = Base64.encode(keystoreFile.readBytes())
            logger.lifecycle("================ Keystore Base64 Content ================")
            logger.lifecycle(base64Content)
            logger.lifecycle("==========================================================")
        } else {
            logger.lifecycle("Keystore file does not exist at: ${keystoreFile.absolutePath}")
        }
    }
}
