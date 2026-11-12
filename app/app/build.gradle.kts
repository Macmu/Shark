import java.io.FileInputStream
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.android.snake"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildToolsVersion = "37.0.0"
    defaultConfig {
        applicationId = "com.android.snake"
        minSdk = 33
        targetSdk = 37
        versionCode = 103
        versionName = "1.1"
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/libk61821.so")
            keepDebugSymbols.add("**/libksud_universal.so")
            keepDebugSymbols.add("**/libk61269.so")
            keepDebugSymbols.add("**/libk61223.so")
            keepDebugSymbols.add("**/libk61258.so")
            keepDebugSymbols.add("**/libk61238.so")
            keepDebugSymbols.add("**/libk66118.so")
            keepDebugSymbols.add("**/libk66127.so")
            keepDebugSymbols.add("**/libsusfs_guard_lkm.so")
            keepDebugSymbols.add("**/libsusfs_guard_lkm_612.so")
            keepDebugSymbols.add("**/libhybridmount.so")
        }
    }
}



dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("io.github.itoseo:coui:0.0.1-alpha7")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:aidl:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("com.github.roro2239:Stellar-API:1.0.3")

    // 内置授权：App 内直连本机 adbd，无需额外安装 Shizuku / Stellar。
    // libadb-android: GPL-3.0-or-later OR Apache-2.0（传递 spake2-android、bcprov）。
    implementation("com.github.MuntashirAkon:libadb-android:3.1.1")
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.81")
}
