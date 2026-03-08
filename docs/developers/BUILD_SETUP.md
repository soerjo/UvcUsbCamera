# Build Setup

This guide explains how to configure the build environment for AUSBC, including NDK setup, native build configuration, and common build issues.

## Requirements

### System Requirements

| Component | Version | Notes |
|-----------|---------|-------|
| Android Studio | Hedgehog | 2023.1.1 or later |
| JDK | 17 | Required by AGP 8.7.3 |
| Gradle | 8.7+ | Via Gradle Wrapper |
| NDK | 27.0.12077973 | Required for native builds |
| CMake | 3.22.1+ | Required for libnative |
| Android SDK | API 35 | Compile SDK |
| Android SDK | API 24+ | Min SDK |

### Platform-Specific Notes

**Windows:**
- Use double backslashes (`\\`) in paths
- NDK typically at: `C:\Users\YourUsername\AppData\Local\Android\Sdk\ndk\27.0.12077973`

**macOS/Linux:**
- Use forward slashes (`/`) in paths
- NDK typically at: `~/Android/Sdk/ndk/27.0.12077973`

## NDK Configuration

The project requires NDK 27.0.12077973 for building native libraries. There are three ways to configure it:

### Option 1: local.properties (Recommended)

Copy the template and configure the NDK path:

```bash
cp local.properties.template local.properties
```

Edit `local.properties`:

**Windows:**
```properties
sdk.dir=C\\:\\\\Users\\\\YourUsername\\\\AppData\\\\Local\\\\Android\\\\Sdk
ndk.dir=C\\:\\\\Users\\\\YourUsername\\\\AppData\\\\Local\\\\Android\\\\Sdk\\\\ndk\\\\27.0.12077973
```

**macOS/Linux:**
```properties
sdk.dir=/path/to/your/Android/Sdk
ndk.dir=/path/to/your/Android/Sdk/ndk/27.0.12077973
```

### Option 2: Environment Variable

Set the `NDK_HOME` environment variable:

**Windows (PowerShell):**
```powershell
$env:NDK_HOME="C:\Users\YourUsername\AppData\Local\Android\Sdk\ndk\27.0.12077973"
```

**Windows (System Environment):**
1. Search for "Environment Variables"
2. Add new system variable: `NDK_HOME`
3. Value: `C:\Users\YourUsername\AppData\Local\Android\Sdk\ndk\27.0.12077973`

**macOS/Linux (bash/zsh):**
```bash
export NDK_HOME=~/Android/Sdk/ndk/27.0.12077973
```

**macOS/Linux (.bashrc/.zshrc):**
```bash
echo 'export NDK_HOME=~/Android/Sdk/ndk/27.0.12077973' >> ~/.bashrc
source ~/.bashrc
```

### Option 3: SDK Location (Automatic)

If you have Android SDK configured in Android Studio, the build script will automatically detect the NDK from the SDK location. Add to `local.properties`:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

The build script will then use: `{sdk.dir}/ndk/27.0.12077973`

## Build Systems

The project uses different build systems for different native modules:

### libuvc - ndk-build

**Build Configuration:** `libuvc/build.gradle.kts`

The `libuvc` module uses the traditional ndk-build system with `Android.mk`:

```makefile
# libuvc/src/main/jni/Android.mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := uvccamera
LOCAL_SRC_FILES := ...
include $(BUILD_SHARED_LIBRARY)
```

**NDK Build Tasks:**
```bash
# Build native libraries
./gradlew :libuvc:ndkBuild

# Clean native libraries
./gradlew :libuvc:ndkClean
```

**ABI Filters:**
- `armeabi-v7a` - 32-bit ARM
- `arm64-v8a` - 64-bit ARM

**Note:** `x86` and `x86_64` are removed due to libjpeg-turbo compatibility issues with NDK 27.

### libnative - CMake

**Build Configuration:** `libnative/src/main/cpp/CMakeLists.txt`

```cmake
cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

set(CMAKE_CXX_STANDARD 17)

# LAME MP3 encoder
include_directories(${CMAKE_SOURCE_DIR}/module/mp3/lame)
aux_source_directory(${CMAKE_SOURCE_DIR}/module/mp3/lame SRC_LAME)

add_library(
    nativelib
    SHARED
    ${SRC_LAME}
    utils/logger.cpp
    module/yuv/yuv.cpp
    module/mp3/mp3.cpp
    proxy/proxy_yuv.cpp
    proxy/proxy_mp3.cpp
    nativelib.cpp
)

# Fix LAME MP3 encoder issues with NDK 27
target_compile_options(nativelib PRIVATE
    -Wno-error=implicit-function-declaration
    -Wno-error=missing-declarations
)

find_library(log-lib log)
target_link_libraries(nativelib ${log-lib})
```

**ABI Filters:**
```kotlin
ndk {
    abiFilters += listOf("armeabi-v7a", "arm64-v8a")
}
```

## Build Commands

### Full Project Build

```bash
# Clean build
./gradlew clean

# Full build (all modules)
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build specific module
./gradlew :libausbc:assembleRelease
./gradlew :libausbc-core:assembleRelease
./gradlew :libuvc:ndkBuild
```

### Native Build Commands

```bash
# Build libuvc native (ndk-build)
./gradlew :libuvc:ndkBuild

# Build libnative native (CMake)
./gradlew :libnative:externalNativeBuildDebug

# Clean native libraries
./gradlew :libuvc:ndkClean
./gradlew :libnative:clean
```

### Gradle Tasks

```bash
# List all available tasks
./gradlew tasks

# List tasks for specific module
./gradlew :libuvc:tasks

# Build with debug info
./gradlew assembleDebug --info

# Build with stacktrace
./gradlew assembleDebug --stacktrace
```

## Build Configuration Files

### Root Build Configuration

**build.gradle.kts (root):**
```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}
```

### Module Build Configuration

**libausbc/build.gradle.kts:**
```kotlin
android {
    namespace = "com.jiangdg.ausbc"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```

### Settings Configuration

**settings.gradle.kts:**
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// Core modules (new architecture)
include(":libausbc-core")
include(":libausbc-camera")
include(":libausbc-render")
include(":libausbc-encode")
include(":libausbc-utils")

// Legacy modules (to be migrated)
include(":libausbc")
include(":libuvc")
include(":libnative")
include(":app")
```

## Gradle Properties

**gradle.properties:**
```properties
# Project properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# Android properties
android.useAndroidX=true
android.enableJetifier=true

# Build features
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true

# Kotlin
kotlin.code.style=official
```

## Native Build Issues and Solutions

### Issue: NDK Not Found

**Error:**
```
NDK not configured. Please set android.ndkVersion in build.gradle.kts.
```

**Solution:**
1. Ensure NDK 27.0.12077973 is installed via SDK Manager
2. Set `ndk.dir` in `local.properties` or `NDK_HOME` environment variable
3. Verify NDK path is correct

### Issue: ndk-build Not Found

**Error:**
```
NDK build executable not found at: /path/to/ndk/ndk-build
```

**Solution:**
1. Verify NDK installation path
2. On Windows, check for `ndk-build.cmd` instead of `ndk-build`
3. Ensure NDK version matches exactly: `27.0.12077973`

### Issue: LAME MP3 Compiler Errors

**Error:**
```
error: implicit declaration of function 'xyz' is invalid in C99
```

**Solution:**
This is handled in `CMakeLists.txt` with:
```cmake
target_compile_options(nativelib PRIVATE
    -Wno-error=implicit-function-declaration
    -Wno-error=missing-declarations
)
```

### Issue: libjpeg-turbo Compatibility

**Error:**
```
undefined reference to 'jpeg_*' functions
```

**Solution:**
x86/x86_64 ABIs are removed due to libjpeg-turbo compatibility with NDK 27. Only `armeabi-v7a` and `arm64-v8a` are supported.

### Issue: Multiple NDK Versions

**Error:**
```
Execution failed for task ':libuvc:ndkBuild'.
> Build Command failed.
```

**Solution:**
Ensure all modules use the same NDK version:
```kotlin
ndkVersion = "27.0.12077973"
```

### Issue: Out of Memory During Build

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

## Android Studio Configuration

### Install NDK via SDK Manager

1. **Tools → SDK Manager**
2. **SDK Tools tab**
3. Check **NDK (Side by side)**
4. Select **27.0.12077973**
5. Click **Apply**

### Install CMake via SDK Manager

1. **Tools → SDK Manager**
2. **SDK Tools tab**
3. Check **CMake**
4. Click **Apply**

### Gradle JVM Settings

1. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
2. **Gradle JVM**: Use Java 17
3. **Build and run using**: Gradle
4. **Run tests using**: Gradle

## IDE Configuration

### Build Variants

```
app/
├── debug/   # Debug build with native symbols
└── release/ # Release build (optimized)
```

### Native Code Debugging

To debug native code:

1. Ensure native build with debug symbols:
```kotlin
externalNativeBuild {
    cmake {
        cppFlags += "-O0 -g"
    }
}
```

2. Enable native debugging in Run Configuration:
- **Run → Edit Configurations**
- **Debugger**: Native or Dual

### Code Completion for Native Code

For better code completion in native files:
1. **File → Link C++ Project with Gradle**
2. Select build system (CMake or ndk-build)
3. Select project path

## Verifying Build Setup

### Check NDK Installation

```bash
# Check NDK version
ls ~/Android/Sdk/ndk/

# Verify ndk-build exists
ls ~/Android/Sdk/ndk/27.0.12077973/ndk-build

# Verify toolchains
ls ~/Android/Sdk/ndk/27.0.12077973/toolchains/
```

### Test Build

```bash
# Clean build
./gradlew clean

# Build all modules
./gradlew build

# Verify native libraries are built
ls -la app/build/intermediates/stripped_native_libs/
```

### Check Build Configuration

```bash
# Show project configuration
./gradlew projects

# Show module dependencies
./gradlew :app:dependencies

# Show native build configuration
./gradlew :libuvc:ndkBuild --info
./gradlew :libnative:externalNativeBuildDebug --info
```

## Performance Tips

### Gradle Build Performance

In `gradle.properties`:
```properties
# Enable parallel builds
org.gradle.parallel=true

# Enable configuration cache
org.gradle.configurationcache=true

# Enable build cache
org.gradle.caching=true

# Increase heap size
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### Native Build Optimization

For faster native builds during development:

**libuvc (ndk-build):**
```bash
# Use parallel jobs
ndk-build -j8
```

**libnative (CMake):**
```kotlin
externalNativeBuild {
    cmake {
        arguments += "-j8"
    }
}
```

## Additional Resources

- [Android NDK Guide](https://developer.android.com/ndk/guides)
- [CMake Documentation](https://cmake.org/documentation/)
- [Gradle Build Tool](https://docs.gradle.org/)

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for more build issues and solutions.
