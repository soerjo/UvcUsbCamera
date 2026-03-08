# Native Libraries

This document provides a deep dive into the native libraries used by AUSBC: `libuvc` and `libnative`.

## Overview

AUSBC uses two native libraries:

| Library | Purpose | Build System |
|---------|---------|--------------|
| `libuvc` | USB camera communication | ndk-build |
| `libnative` | MP3 encoding, YUV utilities | CMake |

## libuvc (USB Camera Communication)

`libuvc` handles low-level USB UVC camera communication using native code.

### Architecture

```
libuvc/
├── android/
│   └── UVCCamera.cpp/h          # JNI bridge layer
├── uvccamera/
│   ├── UVCCamera.cpp/h          # Main camera implementation
│   ├── UVCPreview.cpp/h         # Preview handling
│   ├── Parameters.cpp/h         # Camera parameter controls
│   └── pipeline/                # Frame processing pipeline
│       ├── UVCPipeline.cpp/h    # Pipeline interface
│       └── UVCPreviewPipe.cpp/h # Preview pipeline
├── libusb/                      # USB library
│   ├── libusb/
│   └── android/
├── libjpeg-turbo-1.5.0/         # JPEG encoding/decoding
│   ├── simd/
│   ├── java/
│   └── ...
├── libuvc/                      # UVC library
│   ├── include/
│   └── src/
└── include/                     # Public headers
```

### JNI Bridge Layer

**Location:** `libuvc/src/main/jni/android/UVCCamera.cpp`

The JNI bridge provides the interface between Java/Kotlin and native code.

**JNI Method Pattern:**
```cpp
Java_com_jiangdg_usb_UVCCamera_methodName(JNIEnv *env, jobject thiz, ...)
```

**Key JNI Methods:**

| Java Method | Native Method | Purpose |
|-------------|---------------|---------|
| `UVCCamera.connect()` | `Java_com_jiangdg_usb_UVCCamera_connect` | Open USB connection |
| `UVCCamera.startPreview()` | `Java_com_jiangdg_usb_UVCCamera_startPreview` | Start preview |
| `UVCCamera.stopPreview()` | `Java_com_jiangdg_usb_UVCCamera_stopPreview` | Stop preview |
| `UVCCamera.setPreviewSize()` | `Java_com_jiangdg_usb_UVCCamera_setPreviewSize` | Set resolution |
| `UVCCamera.destroy()` | `Java_com_jiangdg_usb_UVCCamera_destroy` | Release resources |

### UVCCamera Class

**Location:** `libuvc/src/main/jni/uvccamera/UVCCamera.cpp`

The main camera class that manages USB UVC communication.

**Key Methods:**

```cpp
class UVCCamera {
public:
    // Camera lifecycle
    int connect();
    int startPreview();
    int stopPreview();
    int destroy();

    // Preview configuration
    int setPreviewSize(int width, int height, int mode, int fps);
    int setPreviewDisplay(ANativeWindow* window);

    // Frame callback
    void setFrameCallback(uvc_frame_callback_t callback);

    // Camera controls
    int setBrightness(int value);
    int getBrightness();
    int resetBrightness();

    // Format support
    int getSupportedSize();
};
```

### UVCPreview Class

**Location:** `libuvc/src/main/jni/uvccamera/UVCPreview.cpp`

Handles preview rendering and frame processing.

**Key Features:**
- Surface/SurfaceTexture binding
- Frame format conversion (YUYV to NV21, MJPEG decoding)
- FPS calculation

**Preview Flow:**
```
USB Frame → UVCPreview → Frame Conversion → Surface/SurfaceTexture
```

### Parameters Class

**Location:** `libuvc/src/main/jni/uvccamera/Parameters.cpp`

Handles UVC camera parameter controls.

**Supported Parameters:**
- Brightness
- Contrast
- Saturation
- Sharpness
- Gamma
- Hue
- Gain
- Zoom
- Focus (absolute/auto)
- White Balance (absolute/auto)
- Exposure (absolute/auto)

**UVC Control Units:**
```cpp
// UVC processing unit controls
int getProcUnit(uvc_req_code code, int *value);
int setProcUnit(uvc_req_code code, int value);

// UVC camera unit controls
int getCameraUnit(uvc_req_code code, int *value);
int setCameraUnit(uvc_req_code code, int value);
```

### Pipeline Architecture

**Location:** `libuvc/src/main/jni/uvccamera/pipeline/`

The pipeline system processes camera frames.

**UVCPipeline Interface:**
```cpp
class UVCPipeline {
public:
    virtual void start() = 0;
    virtual void stop() = 0;
    virtual void onFrameReceived(uvc_frame_t *frame) = 0;
};
```

**UVCPreviewPipe Implementation:**
- Receives frames from USB camera
- Converts frame format if needed
- Renders to Surface/SurfaceTexture
- Calculates FPS

### libjpeg-turbo Integration

**Location:** `libuvc/src/main/jni/libjpeg-turbo-1.5.0/`

Used for MJPEG decoding and JPEG encoding.

**Key Components:**
- SIMD optimizations (ARM NEON)
- Memory management
- Color space conversion

**Build Configuration:**
```makefile
LOCAL_CFLAGS := -O3 -fstrict-aliasing \
    -DANDROID -DANDROID_ARM \
    -D__ARM_HAVE_NEON
```

### libusb Integration

**Location:** `libuvc/src/main/jni/libusb/`

USB library for device communication.

**Android-specific modifications:**
- `android/` directory contains Android adaptations
- USB device file descriptor handling
- Kernel ioctl calls

### Build Configuration

**Location:** `libuvc/src/main/jni/Android.mk`

```makefile
LOCAL_PATH := $(call my-dir)

# libuvccamera
include $(CLEAR_VARS)
LOCAL_MODULE    := uvccamera
LOCAL_SRC_FILES := \
    android/UVCCamera.cpp \
    uvccamera/UVCCamera.cpp \
    uvccamera/UVCPreview.cpp \
    uvccamera/Parameters.cpp \
    uvccamera/pipeline/UVCPipeline.cpp \
    uvccamera/pipeline/UVCPreviewPipe.cpp

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/libusb \
    $(LOCAL_PATH)/libuvc/include \
    $(LOCAL_PATH)/libjpeg-turbo-1.5.0

LOCAL_LDLIBS := -llog -landroid -lGLESv2
LOCAL_STATIC_LIBRARIES := libusb libuvc jpeg-turbo

include $(BUILD_SHARED_LIBRARY)
```

**Application.mk:**
```makefile
APP_ABI := armeabi-v7a arm64-v8a
APP_PLATFORM := android-24
APP_CFLAGS := -O2 -DANDROID
```

## libnative (Utilities)

`libnative` provides utility functions for MP3 encoding and YUV processing.

### Architecture

```
libnative/
├── module/
│   ├── mp3/
│   │   └── lame/                # LAME MP3 encoder
│   │       ├── libmp3lame/       # MP3 encoding
│   │       └── include/          # Public headers
│   └── yuv/
│       └── yuv.cpp              # YUV utilities
├── proxy/
│   ├── proxy_mp3.cpp            # MP3 JNI bridge
│   └── proxy_yuv.cpp            # YUV JNI bridge
├── utils/
│   └── logger.cpp               # Logging utilities
└── nativelib.cpp                # Main JNI entry
```

### LAME MP3 Encoder

**Location:** `libnative/src/main/cpp/module/mp3/lame/`

The LAME (LAME Ain't an MP3 Encoder) library for MP3 encoding.

**Features:**
- MPEG-1 Layer III encoding
- Constant bitrate (CBR)
- Variable bitrate (VBR)
- Joint stereo encoding

**JNI Interface (`proxy/proxy_mp3.cpp`):**
```cpp
// Initialize encoder
extern "C" JNIEXPORT jlong JNICALL
Java_com_jiangdg_natives_MP3Encoder_nativeCreate(
    JNIEnv *env, jobject thiz,
    jint sample_rate, jint channels, jint bitrate);

// Encode PCM data
extern "C" JNIEXPORT jint JNICALL
Java_com_jiangdg_natives_MP3Encoder_nativeEncode(
    JNIEnv *env, jobject thiz,
    jbyteArray pcm_data, jint length,
    jbyteArray mp3_data);

// Close encoder
extern "C" JNIEXPORT void JNICALL
Java_com_jiangdg_natives_MP3Encoder_nativeClose(
    JNIEnv *env, jobject thiz, jlong handle);
```

**Build Configuration:**
```cmake
# Add compiler flags to fix LAME MP3 encoder issues with NDK 27
target_compile_options(nativelib PRIVATE
    -Wno-error=implicit-function-declaration
    -Wno-error=missing-declarations
)
```

### YUV Utilities

**Location:** `libnative/src/main/cpp/module/yuv/yuv.cpp`

YUV color space conversion utilities.

**Supported Conversions:**
- YUYV to NV21
- YUYV to RGBA
- NV21 to RGBA
- YV12 to NV21

**JNI Interface (`proxy/proxy_yuv.cpp`):**
```cpp
// YUYV to NV21 conversion
extern "C" JNIEXPORT void JNICALL
Java_com_jiangdg_natives_YUVConverter_yuyvToNv21(
    JNIEnv *env, jclass clazz,
    jbyteArray src, jbyteArray dst,
    jint width, jint height);

// YUYV to RGBA conversion
extern "C" JNIEXPORT void JNICALL
Java_com_jiangdg_natives_YUVConverter_yuyvToRgba(
    JNIEnv *env, jclass clazz,
    jbyteArray src, jbyteArray dst,
    jint width, jint height);
```

### Logging Utilities

**Location:** `libnative/src/main/cpp/utils/logger.cpp`

Native logging utilities that integrate with Android logcat.

```cpp
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
```

### CMake Configuration

**Location:** `libnative/src/main/cpp/CMakeLists.txt`

```cmake
cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# LAME encoder
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

## JNI Architecture

### Data Flow

```
Java/Kotlin Layer
       │
       ▼ JNI Bridge
Native Layer
       │
       ▼
Native Libraries
       │
       ▼
Hardware/OS
```

### JNI Method Signatures

**Basic JNI Method Pattern:**
```cpp
extern "C" JNIEXPORT ReturnType JNICALL
Java_com_package_ClassName_methodName(
    JNIEnv *env,     // JNI environment
    jobject thiz,    // Object instance (or jclass for static)
    ...parameters... // Method parameters
);
```

**Example with Parameters:**
```cpp
// Java: boolean setBrightness(int value)
extern "C" JNIEXPORT jboolean JNICALL
Java_com_jiangdg_usb_UVCCamera_setBrightness(
    JNIEnv *env,        // JNI environment
    jobject thiz,       // UVCCamera instance
    jint value)         // brightness value
{
    // Implementation
    return JNI_TRUE;
}
```

### JNI Type Mapping

| Java Type | JNI Type | Native Type |
|-----------|----------|-------------|
| boolean | jboolean | unsigned char |
| byte | jbyte | signed char |
| char | jchar | unsigned short |
| short | jshort | short |
| int | jint | int |
| long | jlong | long |
| float | jfloat | float |
| double | jdouble | double |
| Object | jobject | - |
| Class | jclass | - |
| String | jstring | - |
| byte[] | jbyteArray | - |

### JNI Array Handling

**Reading Java Arrays:**
```cpp
jbyte* data = env->GetByteArrayElements(javaArray, NULL);
// Use data
env->ReleaseByteArrayElements(javaArray, data, 0);
```

**Writing Java Arrays:**
```cpp
env->SetByteArrayRegion(javaArray, 0, length, nativeData);
```

## Build System Integration

### libuvc Gradle Integration

**Location:** `libuvc/build.gradle.kts`

The Gradle build script integrates ndk-build:

```kotlin
val ndkDir = System.getenv("NDK_HOME") ?: // Get NDK path
    project.rootProject.file("local.properties").let { file ->
        val props = Properties()
        if (file.exists()) {
            props.load(file.inputStream())
            props.getProperty("ndk.dir")
        } else null
    }

val ndkBuildExecutable = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    "$ndkDir/ndk-build.cmd"
} else {
    "$ndkDir/ndk-build"
}

tasks.register("ndkBuild", Exec::class) {
    commandLine(ndkBuildExecutable, "-j8", "-C", file("src/main").absolutePath)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn("ndkBuild")
}
```

### libnative Gradle Integration

**Location:** `libnative/build.gradle.kts`

The Gradle build script integrates CMake:

```kotlin
android {
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
    }
}
```

## Native Debugging

### Logging

Use `__android_log_print` for native logging:

```cpp
#include <android/log.h>

#define TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

void someFunction() {
    LOGI("Function called");
    LOGE("Error occurred: %s", error_message);
}
```

View logs in Logcat:
```bash
adb logcat | grep NativeLib
```

### GDB Debugging

Configure in `build.gradle.kts`:
```kotlin
externalNativeBuild {
    cmake {
        cppFlags += "-O0 -g"  // Debug symbols
    }
}
```

### Address Sanitizer

Enable in `build.gradle.kts`:
```kotlin
externalNativeBuild {
    cmake {
        arguments += "-fsanitize=address"
        cppFlags += "-fsanitize=address -fno-omit-frame-pointer"
    }
}
```

## Native Performance Considerations

### Memory Management

- Use `jbyte*` with `GetByteArrayElements` for direct array access
- Always release with `ReleaseByteArrayElements`
- Consider `GetPrimitiveArrayCritical` for short-term access
- Avoid unnecessary JNI boundary crossings

### CPU Optimization

- Use SIMD instructions (ARM NEON) where available
- Enable compiler optimizations: `-O3`
- Profile with `simpleperf` for bottlenecks

### Thread Safety

- Use `pthread_mutex_t` for critical sections
- Be careful with shared state between threads
- Use `JavaVM->AttachCurrentThread` for callbacks

## When to Modify Native Code

### Modify When:

1. **Adding new camera features** - UVC controls not supported
2. **Fixing device-specific issues** - Hardware quirks
3. **Performance optimization** - CPU bottlenecks
4. **New format support** - Additional video formats
5. **Bug fixes** - Native code issues

### Avoid Modifying When:

1. **UI-related changes** - Do in Java/Kotlin
2. **Application logic** - Keep in Java/Kotlin
3. **Simple format conversions** - Use existing APIs

## Native Code Style

### Naming Conventions

- **Functions**: `snake_case` (e.g., `set_preview_size`)
- **Classes**: `PascalCase` (e.g., `UVCCamera`)
- **Constants**: `UPPER_SNAKE_CASE` (e.g., `MAX_WIDTH`)
- **JNI functions**: Java package mapping (e.g., `Java_com_jiangdg_usb_UVCCamera_connect`)

### Code Organization

```cpp
// Header guard
#ifndef PROJECT_FILE_H
#define PROJECT_FILE_H

// Includes
#include <jni.h>
#include <android/log.h>

// Constants
#define TAG "MyTag"
#define MAX_SIZE 1024

// Class declaration
class MyClass {
public:
    // Public methods
    int doSomething();

private:
    // Private members
    int value;
};

// Implementation
int MyClass::doSomething() {
    return value;
}

#endif // PROJECT_FILE_H
```

## Resources

### Documentation

- [Android NDK Guide](https://developer.android.com/ndk/guides)
- [JNI Specification](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/)
- [libusb Documentation](https://libusb.info/)
- [LAME Documentation](http://lame.sourceforge.net/)

### Tools

- **ndk-build**: Native build system for libuvc
- **CMake**: Native build system for libnative
- **adb**: Debugging and logging
- **simpleperf**: Native performance profiling

See [BUILD_SETUP.md](BUILD_SETUP.md) for build configuration details.
