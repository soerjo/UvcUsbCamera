# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AUSBC (Android USBCamera) is a UVC (USB Video Class) camera engine for Android that allows opening UVC cameras without system permissions.

**Note:** The project is undergoing a major architectural restructuring. Both legacy and new modular architectures coexist during the migration period.

### Module Structure

**New Modular Architecture (Recommended):**
- **libausbc-core/** - Core abstractions and domain models
  - Error handling: `CameraError` (sealed class hierarchy)
  - Result types: `CameraResult<T>`, `CaptureResult`, `AudioResult`
  - State management: `CameraState` (sealed class) with StateFlow
  - Core contracts: `ICamera`, `IRenderEngine`, `IEncodeEngine`, `IAudioStrategy`
  - Repository interfaces: `ICameraRepository`, `IDeviceRepository`

- **libausbc-camera/** - Camera implementations
  - `UvcCamera` / `UvcCameraV2` - Modern UVC camera implementations
  - Data sources: `IUvcCameraDataSource`, `IUsbDeviceDataSource`, `ICameraParameterDataSource`
  - Lifecycle management: `CameraLifecycleManager`, `CameraStateManager`
  - Repository implementations: `CameraRepository`, `DeviceRepository`
  - Platform services: `UsbDeviceManager`

- **libausbc-render/** - OpenGL ES rendering
  - GPU-based rendering with effects support
  - Effect system via `AbstractEffect`

- **libausbc-encode/** - Media encoding
  - H.264 video encoding
  - AAC audio encoding
  - MP4 muxing

- **libausbc-utils/** - Shared utilities
  - Permission utilities: `PermissionUtils`
  - Flow extensions: `UiState`, `retry`
  - Logging: `Logger`

**Legacy Modules (Being Migrated):**
- **app/** - Demo application showing library usage (uses both legacy and new modules)
- **libausbc/** - Main Kotlin library (Legacy - being migrated to new modules)
- **libuvc/** - Native JNI library for USB camera communication (ndk-build)
- **libnative/** - Native library with LAME MP3 encoder and YUV utilities (CMake)

## Build System

**Current versions (from root build.gradle.kts):**
- **Android Gradle Plugin**: 8.7.3
- **Kotlin**: 1.9.22
- **Hilt**: 2.48 (root and all modules)
- **Compile SDK**: 35
- **Target SDK**: 35
- **Min SDK**: 24 (Android 7.0+)
- **Java Compatibility**: Java 17
- **NDK Version**: 27.0.12077973 (app module)

**Build Configuration:**
- **Kotlin DSL**: All build files use `*.gradle.kts` format
- **Namespace**: Each module declares `namespace` in build file (AGP 8.x requirement)
- **Native**: libuvc uses ndk-build, libnative uses CMake 3.22.1
- **Hilt**: Dependency injection via `@HiltAndroidApp` in application
- **Gradle Features**: Configuration cache enabled, parallel builds enabled

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

# Build specific module (legacy)
./gradlew :libausbc:assembleRelease

# Build specific module (new architecture)
./gradlew :libausbc-core:assembleRelease
./gradlew :libausbc-camera:assembleRelease
```

### Native Build Requirements
- **libuvc** uses ndk-build. Requires `ndk.dir` in `local.properties` or `NDK_HOME` environment variable.
- **libnative** uses CMake. Configure via `CMakeLists.txt` in `src/main/cpp/`.
- Use `local.properties.template` as a reference for setting up your local environment.

## Architecture

### Legacy Camera Strategy Pattern

The legacy library uses a strategy pattern to support different camera types:

- **CameraUvcStrategy** - USB UVC cameras (main feature)
- **Camera1Strategy** - Legacy Camera API (Deprecated)
- **Camera2Strategy** - Modern Camera2 API

**Key Interfaces (Legacy):**
- `ICameraStrategy` (deprecated since v3.3.0) - Base camera interface for the old strategy pattern
- `MultiCameraClient.ICamera` - Runtime camera operations API (in libausbc module)
- `CameraUVC` - Implements `ICamera` for UVC cameras

### New Modular Architecture (Recommended)

The new architecture follows clean architecture principles with separated concerns:

**Modern ICamera Interface (libausbc-core):**

Located at `libausbc-core/src/main/java/com/jiangdg/ausbc/core/contract/ICamera.kt`

Key features:
- Coroutine-based async operations (suspend functions)
- StateFlow for reactive state management
- Type-safe error handling with sealed classes (`CameraResult<T>`)
- Flow-based preview frame streaming

```kotlin
interface ICamera {
    val cameraState: StateFlow<CameraState>

    suspend fun open(request: CameraRequest): CameraResult<Unit>
    suspend fun close(): CameraResult<Unit>
    suspend fun startPreview(surface: Surface): CameraResult<Unit>
    suspend fun stopPreview(): CameraResult<Unit>
    // ... more methods
}
```

**New Architecture Components:**

- **libausbc-core**: Core domain layer
  - Contracts: `ICamera`, `IRenderEngine`, `IEncodeEngine`, `IAudioStrategy`
  - Domain models: `CameraRequest`, `CameraState`, `PreviewSize`, `RenderConfig`, `EncodeConfig`
  - Error handling: `CameraError` (sealed class), `ErrorHandler`
  - Result types: `CameraResult<T>`, `CaptureResult`, `AudioResult`, `EncodeFileResult`
  - Repository interfaces: `ICameraRepository`, `IDeviceRepository`

- **libausbc-camera**: Camera implementations
  - `UvcCamera` / `UvcCameraV2` - Modern UVC camera implementation
  - Data sources: `IUvcCameraDataSource`, `IUsbDeviceDataSource`, `ICameraParameterDataSource`
  - Repositories: `CameraRepository`, `DeviceRepository`
  - Lifecycle: `CameraLifecycleManager`, `CameraStateManager`
  - Platform: `UsbDeviceManager`

- **libausbc-render**: OpenGL ES rendering
  - GPU-based rendering with effects support
  - Effect system via `AbstractEffect`

- **libausbc-encode**: Media encoding
  - H.264 video encoding
  - AAC audio encoding
  - MP4 muxing

- **libausbc-utils**: Shared utilities
  - Permission utilities: `PermissionUtils`
  - Flow extensions: `FlowUtils` (UiState, retry)
  - Logging: `Logger`

### Fragment/Activity API

Users extend base classes for camera functionality:

- **Single camera**: `CameraFragment` / `CameraActivity` (libausbc module - legacy)
- **Multi-camera**: `MultiCameraFragment` / `MultiCameraActivity` (libausbc module - legacy)

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
├── Hilt enabled
├── Dependencies: libausbc (legacy), libausbc-core, libausbc-camera
└── ABIs: armeabi-v7a, arm64-v8a

libausbc-camera
├── api libausbc-core
├── implementation libuvc (legacy)
└── implementation libausbc (legacy)

libausbc-core
├── Hilt 2.48
├── Coroutines 1.9.0
└── Lifecycle 2.8.7

libausbc (Main Kotlin Library - Legacy)
├── Camera abstractions (ICameraStrategy deprecated, MultiCameraClient.ICamera)
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

## Architecture Migration Status

The project is transitioning from a monolithic `libausbc` module to a clean architecture with separated concerns.

**Migration Status:**
- ✅ Phase 1: Foundation (completed)
- ✅ Phase 2: Core Module (completed)
- ✅ Phase 3: Data Layer (completed)
- ✅ Phase 4: Render Module (completed)
- 🔄 Phase 5: Encode Module (in progress)

**During this transition:**
- Legacy modules (libausbc, libuvc, libnative) remain fully functional
- New modules (libausbc-*) provide modern alternatives
- The `ICameraStrategy` pattern is deprecated in favor of `ICamera` (from libausbc-core)
- Both APIs can be used during the migration period

**Migration path for new code:**
1. Use `ICamera` interface from `libausbc-core` instead of `ICameraStrategy`
2. Use coroutines/Flow instead of callbacks
3. Use `CameraResult<T>` for error handling
4. Inject dependencies via Hilt
5. Use repository pattern for data access

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

### New Architecture Files (libausbc-core)

**Contracts:**
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/contract/ICamera.kt` - Modern camera operations interface
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/contract/IRenderEngine.kt` - Render engine interface
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/contract/IEncodeEngine.kt` - Encode engine interface
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/contract/IAudioStrategy.kt` - Audio strategy interface

**Domain Models:**
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/model/CameraState.kt` - Sealed state class
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/model/CameraRequest.kt` - Camera configuration
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/model/PreviewSize.kt` - Preview size data
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/model/RenderConfig.kt` - Render configuration
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/model/EncodeConfig.kt` - Encode configuration

**Error Handling:**
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/common/error/CameraError.kt` - Sealed error hierarchy
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/common/error/ErrorHandler.kt` - Error utilities

**Result Types:**
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/common/result/CameraResult.kt` - Result wrapper
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/common/result/CaptureResult.kt` - Capture results
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/common/result/AudioResult.kt` - Audio results
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/common/result/EncodeFileResult.kt` - Encode results

**Repositories:**
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/repository/ICameraRepository.kt` - Camera repository interface
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/domain/repository/IDeviceRepository.kt` - Device repository interface

**DI:**
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/di/CoreModule.kt` - Hilt core module
- `libausbc-core/src/main/java/com/jiangdg/ausbc/core/di/CameraQualifiers.kt` - DI qualifiers

### New Architecture Files (libausbc-camera)

**Camera Implementations:**
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/uvc/UvcCamera.kt` - UVC camera implementation
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/uvc/UvcCameraV2.kt` - V2 implementation
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/uvc/UvcCameraFactory.kt` - Factory for UVC cameras

**Data Sources:**
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/datasource/IUvcCameraDataSource.kt` - UVC camera data source interface
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/datasource/IUsbDeviceDataSource.kt` - USB device data source interface
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/datasource/ICameraParameterDataSource.kt` - Camera parameters interface
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/datasource/UvcCameraDataSource.kt` - Implementation
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/datasource/UsbDeviceDataSource.kt` - Implementation
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/datasource/CameraParameterDataSource.kt` - Implementation

**Repositories:**
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/data/repository/CameraRepository.kt` - Camera repository
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/data/repository/DeviceRepository.kt` - Device repository

**Lifecycle:**
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/lifecycle/CameraLifecycleManager.kt` - Lifecycle management
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/lifecycle/CameraStateManager.kt` - State management

**Platform:**
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/platform/UsbDeviceManager.kt` - USB device management

**DI:**
- `libausbc-camera/src/main/java/com/jiangdg/ausbc/camera/di/CameraModule.kt` - Hilt camera module

### Core Camera Files (DO NOT MODIFY without understanding)

**Base Classes (Legacy):**
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

### Camera Implementations (Legacy)

**Legacy Strategy Pattern (deprecated since v3.3.0):**
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/ICameraStrategy.kt` - Base strategy interface
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/CameraUvcStrategy.kt` - USB UVC camera
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/Camera1Strategy.kt` - Camera1 (legacy)
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/Camera2Strategy.kt` - Camera2 API

**Modern Runtime API (libausbc module):**
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

### Camera Request & Data Classes (Legacy)

- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraRequest.kt` - Camera configuration
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/PreviewSize.kt` - Preview size data class
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraInfo.kt` - Camera info base class
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraUvcInfo.kt` - UVC camera info
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraV1Info.kt` - Camera1 info
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraV2Info.kt` - Camera2 info
- `libausbc/src/main/java/com/jiangdg/ausbc/camera/bean/CameraStatus.kt` - Camera status enum

### Encoding (Legacy)

- `libausbc/src/main/java/com/jiangdg/ausbc/encode/AbstractProcessor.kt` - Base processor
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/H264EncodeProcessor.kt` - H.264 encoding
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/AACEncodeProcessor.kt` - AAC encoding
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/muxer/Mp4Muxer.kt` - MP4 muxer

### Audio (Legacy)

- `libausbc/src/main/java/com/jiangdg/ausbc/encode/audio/IAudioStrategy.kt` - Audio strategy interface
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/audio/AudioStrategySystem.kt` - System mic
- `libausbc/src/main/java/com/jiangdg/ausbc/encode/audio/AudioStrategyUAC.kt` - UAC audio

### Render Effects (Legacy)

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

1. **ICameraStrategy deprecation**: The strategy pattern (`ICameraStrategy` and its subclasses) is deprecated since v3.3.0. Use `ICamera` from `libausbc-core` or `MultiCameraClient.ICamera` (in libausbc module) instead.

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
- **org.jetbrains.kotlinx:kotlinx-coroutines-android** - Coroutines 1.9.0
- **androidx.lifecycle** - ViewModel, LiveData, Runtime 2.8.7
- **com.google.dagger:hilt-android** - Hilt 2.48

### Library Module (Legacy - libausbc)
- **androidx.appcompat** - AppCompat support
- **androidx.constraintlayout** - ConstraintLayout
- **com.google.android.material** - Material Design components
- **com.elvishew:xlog** - Logging

### New Architecture Modules

**libausbc-core:**
- **androidx.core:core-ktx** - Kotlin extensions
- **org.jetbrains.kotlinx:kotlinx-coroutines** - Coroutines 1.9.0
- **androidx.lifecycle:lifecycle-runtime-ktx** - Lifecycle 2.8.7
- **com.google.dagger:hilt-android** - Hilt 2.48
- **com.elvishew:xlog** - Logging

**libausbc-camera:**
- **api project(:libausbc-core)** - Core module
- **implementation project(:libuvc)** - Legacy native
- **implementation project(:libausbc)** - Legacy library
- **org.jetbrains.kotlinx:kotlinx-coroutines** - Coroutines 1.9.0
- **androidx.lifecycle:lifecycle-runtime-ktx** - Lifecycle 2.8.7
- **com.google.dagger:hilt-android** - Hilt 2.48
