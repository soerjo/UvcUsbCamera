# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AUSBC (Android USBCamera) is a UVC (USB Video Class) camera engine for Android that allows opening UVC cameras without system permissions. The project consists of multiple modules:

- **app/** - Demo application showing library usage
- **libausbc/** - Main Kotlin library with camera abstractions (API surface)
- **libuvc/** - Native JNI library for USB camera communication (ndk-build)
- **libnative/** - Native library with LAME MP3 encoder and YUV utilities (CMake)

## Build System

**Current versions (from build.gradle.kts):**
- **Gradle**: 8.11
- **Android Gradle Plugin**: 8.7.3
- **Kotlin**: 2.1.0
- **Compile SDK**: 35
- **Target SDK**: 35
- **Min SDK**: 24 (Android 7.0+)
- **Java Compatibility**: Java 17

**Build Configuration:**
- **Kotlin DSL**: All build files use `*.gradle.kts` format
- **Namespace**: Each module declares `namespace` in build file (AGP 8.x requirement)
- **Native**: libuvc uses ndk-build, libnative uses CMake 3.22.1

### Build Commands
```bash
# Build the entire project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Build specific module
./gradlew :libausbc:assembleRelease
./gradlew :app:assembleDebug
```

### Native Build Requirements
- **libuvc** uses ndk-build. Requires `ndk.dir` in `local.properties` or `NDK_HOME` environment variable.
- **libnative** uses CMake. Configure via `CMakeLists.txt` in `src/main/cpp/`.

### Native Build Scripts
- **libuvc**: Uses `Android.mk` build files
  - `libuvc/src/main/jni/Android.mk` - Main JNI build
  - `libuvc/src/main/jni/libuvc/android/jni/Android.mk` - libuvc
  - `libuvc/src/main/jni/libusb/android/jni/Android.mk` - libusb
  - `libuvc/src/main/jni/libjpeg-turbo-1.5.0/Android.mk` - JPEG library
- **libnative**: Uses CMake
  - `libnative/src/main/cpp/CMakeLists.txt` - Includes LAME MP3 encoder, YUV utilities

## Architecture

### Camera Strategy Pattern

The library uses a strategy pattern to support different camera types:

- **CameraUvcStrategy** - USB UVC cameras (main feature)
- **Camera1Strategy** - Legacy Camera API (Deprecated - see ICameraStrategy.kt)
- **Camera2Strategy** - Modern Camera2 API

**Key Interfaces:**
- `ICameraStrategy` (deprecated since v3.3.0) - Base camera interface for the old strategy pattern
- `MultiCameraClient.ICamera` - Modern runtime camera operations API (recommended)
- `CameraUVC` - Implements `ICamera` for UVC cameras

### Fragment/Activity API

Users extend base classes for camera functionality:

- **Single camera**: `CameraFragment` / `CameraActivity`
- **Multi-camera**: `MultiCameraFragment` / `MultiCameraActivity`

**Required overrides in CameraFragment:**
- `getCameraView()` - TextureView or SurfaceView for rendering (returns `IAspectRatio?`)
- `getCameraViewContainer()` - ViewGroup container for the camera view (returns `ViewGroup?`)
- `onCameraState()` - Camera state callbacks (OPENED, CLOSED, ERROR) - from `ICameraStateCallBack`

**Optional overrides:**
- `getCameraRequest()` - Custom camera configuration (preview size, format, render mode)
- `getDefaultCamera()` - Auto-open specific USB device on connect (returns `UsbDevice?`)
- `getGravity()` - Camera render view show gravity (defaults to `Gravity.CENTER`)
- `generateCamera()` - Override to create custom camera implementation

### Camera Request Configuration

```kotlin
CameraRequest.Builder()
    .setPreviewWidth(640)
    .setPreviewHeight(480)
    .setRenderMode(CameraRequest.RenderMode.OPENGL) // or MEDIAN
    .setDefaultRotateType(RotateType.ANGLE_0)
    .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG) // or FORMAT_YUYV
    .setAspectRatioShow(true)
    .setCaptureRawImage(false)
    .setRawPreviewData(false)
    .create()
```

### Render Architecture

- **RenderMode.OPENGL** - GPU rendering with effects support (uses OpenGL ES 2.0)
  - Supports custom effects via `AbstractEffect`
  - Supports rotation via `RotateType`
- **RenderMode.MEDIAN** - Direct surface rendering (no effects)

**Key render classes:**
- `RenderManager` - Manages OpenGL context and effects
- `CameraRender` - Camera preview rendering
- `CaptureRender` - Image capture rendering
- `EncodeRender` - Video encoding rendering
- `ScreenRender` - Offscreen rendering

**Built-in effects:**
- `EffectBlackWhite` - Black and white filter
- `EffectSoul` - Soul/ghost effect
- `EffectZoom` - Zoom effect

### Module Dependencies

```
app (Demo Application)
├── ViewBinding enabled
├── Dependencies: libausbc, Glide, Material dialogs, MMKV, Bugly
└── ABIs: armeabi-v7a, arm64-v8a

libausbc (Main Kotlin Library)
├── Camera abstractions (ICameraStrategy deprecated, MultiCameraClient.ICamera recommended)
├── Render: OpenGL ES 2.0 with effects
├── Encode: H.264 video, AAC audio, MP4 muxing
├── Audio: System mic and UAC support
└── Dependencies: libuvc, libnative

libuvc (Native JNI - ndk-build)
├── UVCCamera native implementation
├── libjpeg-turbo-1.5.0
├── libusb
└── Build: Android.mk, requires NDK in local.properties

libnative (Native JNI - CMake)
├── LAME MP3 encoder
├── YUV utilities
└── Build: CMakeLists.txt, ABIs: armeabi-v7a, arm64-v8a, x86, x86_64
```

## Important Notes

### CameraUVC Parameter Controls

The UVC camera supports runtime parameter adjustments (only works with CameraUVC type):
- Brightness, Contrast, Saturation, Hue, Sharpness, Gamma, Gain, Zoom
- Auto Focus, Auto White Balance
- Custom camera commands via `sendCameraCommand()`

Each has `set`, `get`, and `reset` methods available through `CameraFragment`.

### Audio Support

Audio sources configured via `CameraRequest.AudioSource`:
- `SOURCE_SYS_MIC` - System microphone
- `SOURCE_AUTO` - Auto-detect (prefers UAC audio if available)
- `SOURCE_UAC` - USB Audio Class (UAC) microphone

Audio encoding strategies:
- `AudioStrategySystem` - System microphone audio
- `AudioStrategyUAC` - USB Audio Class audio

### Data Callbacks

- `IPreviewDataCallBack` - Raw preview data (NV21/RGBA)
- `IEncodeDataCallBack` - Encoded data (H.264/AAC)
- `ICaptureCallBack` - Capture status for images/videos/audio
- `IPlayCallBack` - Real-time microphone playback

### Camera Device Management

Available through `CameraFragment`:
- `getDeviceList()` - Get all connected USB camera devices
- `switchCamera(UsbDevice)` - Switch between connected cameras
- `updateResolution(width, height)` - Change preview resolution
- `getAllPreviewSizes(aspectRatio)` - Get supported resolutions
- `getCurrentCamera()` - Get current opened camera (`MultiCameraClient.ICamera?`)
- `isCameraOpened()` - Check if camera is opened

### Encoding & Capture

- `captureImage(callBack, savePath)` - Capture image
- `captureVideoStart(callBack, path, durationInSec)` - Start video recording
- `captureVideoStop()` - Stop video recording
- `captureAudioStart(callBack, path)` - Start audio recording
- `captureAudioStop()` - Stop audio recording
- `captureStreamStart()` - Start H264 & AAC capture only
- `captureStreamStop()` - Stop H264 & AAC capture

### Render Effects

- `addRenderEffect(effect)` - Add OpenGL effect
- `removeRenderEffect(effect)` - Remove OpenGL effect
- `updateRenderEffect(classifyId, effect)` - Update effect by classification ID
- `getDefaultEffect()` - Get default effect

## File Structure Reference

### Core Camera Files (DO NOT MODIFY without understanding)

**Base Classes:**
- `libausbc/src/main/java/com/jiangdg/ausbc/base/CameraFragment.kt` - Base fragment for single camera
- `libausbc/src/main/java/com/jiangdg/ausbc/base/MultiCameraFragment.kt` - Base for multi-camera
- `libausbc/src/main/java/com/jiangdg/ausbc/base/CameraActivity.kt` - Base activity for single camera
- `libausbc/src/main/java/com/jiangdg/ausbc/base/MultiCameraActivity.kt` - Base activity for multi-camera
- `libausbc/src/main/java/com/jiangdg/ausbc/base/BaseFragment.kt` - Base fragment
- `libausbc/src/main/java/com/jiangdg/ausbc/base/BaseActivity.kt` - Base activity

**Camera Views:**
- `libausbc/src/main/java/com/jiangdg/ausbc/widget/IAspectRatio.kt` - Camera view interface
- `libausbc/src/main/java/com/jiangdg/ausbc/widget/AspectRatioTextureView.kt` - TextureView impl
- `libausbc/src/main/java/com/jiangdg/ausbc/widget/AspectRatioSurfaceView.kt` - SurfaceView impl

**Render:**
- `libausbc/src/main/java/com/jiangdg/ausbc/render/RenderManager.kt` - OpenGL render manager

### Camera Implementations

**Legacy Strategy Pattern (deprecated since v3.3.0):**
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/ICameraStrategy.kt` - Base strategy interface
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/CameraUvcStrategy.kt` - USB UVC camera
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/Camera1Strategy.kt` - Camera1 (legacy)
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/Camera2Strategy.kt` - Camera2 API

**Modern Runtime API (recommended):**
- `libausbc/src/main/java/com/jiangdg/ausbc/MultiCameraClient.kt` - Multi-camera client
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/CameraUVC.kt` - UVC camera implementing ICamera
- `libausbc/src/main/java/com/jiangdg/ausbc/CameraClient.kt` - Single camera client

### Demo Application

**Entry Points:**
- `app/src/main/java/com/jiangdg/demo/MainActivity.kt` - Entry point with permission handling
- `app/src/main/java/com/jiangdg/demo/SplashActivity.kt` - Splash screen

**Demo Fragments:**
- `app/src/main/java/com/jiangdg/demo/DemoFragment.kt` - Single camera demo
- `app/src/main/java/com/jiangdg/demo/DemoMultiCameraFragment.kt` - Multi-camera demo
- `app/src/main/java/com/jiangdg/demo/GlSurfaceFragment.kt` - GLSurface demo

**Dialogs:**
- `app/src/main/java/com/jiangdg/demo/EffectListDialog.kt` - Effects selection dialog
- `app/src/main/java/com/jiangdg/demo/MultiCameraDialog.kt` - Multi-camera selection dialog

### Callbacks

- `libausbc/src/main/java/com/jiangdg/ausbc/callback/ICameraStateCallBack.kt` - Camera state callbacks
- `libausbc/src/main/java/com/jiangdg/ausbc/callback/ICaptureCallBack.kt` - Capture callbacks
- `libausbc/src/main/java/com/jiangdg/ausbc/callback/IPreviewDataCallBack.kt` - Preview data callbacks
- `libausbc/src/main/java/com/jiangdg/ausbc/callback/IEncodeDataCallBack.kt` - Encode data callbacks
- `libausbc/src/main/java/com/jiangdg/ausbc/callback/IPlayCallBack.kt` - Playback callbacks
- `libausbc/src/main/java/com/jiangdg/ausbc/callback/IDeviceConnectCallBack.kt` - Device connect callbacks

### Camera Request & Data Classes

- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraRequest.kt` - Camera configuration
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/PreviewSize.kt` - Preview size data class
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraInfo.kt` - Camera info base class
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraUvcInfo.kt` - UVC camera info
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraV1Info.kt` - Camera1 info
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraV2Info.kt` - Camera2 info
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraStatus.kt` - Camera status enum

### Encoding

- `libausbc/src/main/java/com/jiangdg/ausbc/encode/AbstractProcessor.kt` - Base processor
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/H264EncodeProcessor.kt` - H.264 encoding
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/AACEncodeProcessor.kt` - AAC encoding
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/muxer/Mp4Muxer.kt` - MP4 muxer

### Audio

- `libausbc/src/main/java/com/jiangdg/ausbc/encode/audio/IAudioStrategy.kt` - Audio strategy interface
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/audio/AudioStrategySystem.kt` - System mic
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/audio/AudioStrategyUAC.kt` - UAC audio

### Render Effects

- `libausbc/src/main/java/com/jiangdg/ausbc/render/effect/AbstractEffect.kt` - Base effect class
- `libausbc/src/main/java/com/jiangdg/ausbc/render/effect/EffectBlackWhite.kt` - B&W effect
- `libausbc/src/main/java/com/jiangdg/ausbc/render/effect/EffectSoul.kt` - Soul effect
- `libausbc/src/main/java/com/jiangdg/ausbc/render/effect/EffectZoom.kt` - Zoom effect

### Native Build Files

**libuvc (ndk-build):**
- `libuvc/build.gradle.kts` - Module build config (Kotlin DSL)
- `libuvc/src/main/jni/Android.mk` - Main JNI build script
- `libuvc/src/main/jni/Application.mk` - NDK application config

**libnative (CMake):**
- `libnative/build.gradle.kts` - Module build config (Kotlin DSL)
- `libnative/src/main/cpp/CMakeLists.txt` - CMake 3.22.1 build script

## Known Issues

1. **ICameraStrategy deprecation**: The strategy pattern (`ICameraStrategy` and its subclasses) is deprecated since v3.3.0. Use `MultiCameraClient.ICamera` API instead.

2. **Native build setup**: First-time native builds require NDK configuration. Copy `local.properties.template` to `local.properties` and set:
   ```properties
   ndk.dir=/path/to/your/Android/Sdk/ndk/26.1.10909125
   ```
   Or set the `NDK_HOME` environment variable.

3. **AndroidManifest.xml**: Ensure USB device filters are configured for target cameras in the app module (`app/src/main/res/xml/default_device_filter.xml`).

4. **No Compose migration**: The project currently uses View-based UI. There is no Compose implementation yet.

## Third-Party Dependencies

### App Module
- **androidx.appcompat** - AppCompat support
- **androidx.core:core-ktx** - Kotlin extensions
- **com.google.android.material** - Material Design components
- **androidx.constraintlayout** - ConstraintLayout
- **org.jetbrains.kotlinx:kotlinx-coroutines-android** - Coroutines
- **androidx.lifecycle** - ViewModel, LiveData, Runtime
- **com.github.CymChad:BaseRecyclerViewAdapterHelper** - RecyclerView adapter helper
- **com.afollestad.material-dialogs** - Material dialogs
- **com.tencent.bugly** - Crash reporting
- **com.gyf.immersionbar:immersionbar** - Immersion bar (status bar)
- **com.github.bumptech.glide:glide** - Image loading
- **com.tencent.mmkv** - Key-value storage

### Library Module
- **androidx.appcompat** - AppCompat support
- **androidx.constraintlayout** - ConstraintLayout
- **com.google.android.material** - Material Design components
- **com.elvishew:xlog** - Logging
