plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jiangdg.demo"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.jiangdg.ausbc"
        minSdk = 24
        targetSdk = 35
        versionCode = 126
        versionName = "3.3.3"

        multiDexEnabled = true

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // libuvc module should need set local.properties
    // eg: ndk.dir=D\:\\Developer\\Environment\\AndroidSdks\\ndk\\21.0.6113669
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:2.9.50")
    implementation("com.afollestad.material-dialogs:core:3.3.0")

    implementation("com.tencent.bugly:crashreport:4.1.9.3")
    implementation("com.tencent.bugly:nativecrashreport:3.9.2")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    // webpdecoder not available, using Glide's built-in support
    implementation("com.tencent:mmkv:1.3.5")

    implementation("androidx.multidex:multidex:2.0.1")
    // For debug online
    implementation(project(":libausbc"))

    // demo
//    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc:3.3.3")
}
