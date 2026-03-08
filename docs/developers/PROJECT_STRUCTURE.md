# Project Structure

This document explains the architecture and module organization of the AUSBC project.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Demo Application                          │
│                              (app)                                │
└─────────────────────────────────────────────────────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
            ┌───────▼────────┐          ┌────────▼────────┐
            │ New Modules    │          │  Legacy Modules │
            │ (Recommended)  │          │  (Deprecated)   │
            └───────┬────────┘          └────────┬────────┘
                    │                             │
    ┌───────────────┼───────────────┐             │
    │               │               │             │
┌───▼────┐   ┌─────▼───┐   ┌──────▼──┐   ┌──────▼──────┐
│  core  │   │ camera  │   │ render  │   │   libausbc  │
└───┬────┘   └────┬────┘   └─────┬───┘   └──────┬──────┘
    │             │              │               │
    └─────────────┼──────────────┘               │
                  │                              │
         ┌────────▼─────────┐          ┌────────▼────────┐
         │  encode          │          │  libuvc (native)│
         └──────────────────┘          │  libnative      │
                                       └─────────────────┘
```

## Module Descriptions

### New Modular Architecture (Recommended)

#### libausbc-core
**Core abstractions and domain models**

```
libausbc-core/
├── src/main/java/com/jiangdg/ausbc/core/
│   ├── contract/           # Core interfaces
│   │   ├── ICamera.kt      # Modern camera operations
│   │   ├── IRenderEngine.kt
│   │   ├── IEncodeEngine.kt
│   │   └── IAudioStrategy.kt
│   ├── domain/
│   │   ├── model/          # Domain models
│   │   │   ├── CameraState.kt
│   │   │   ├── CameraRequest.kt
│   │   │   ├── PreviewSize.kt
│   │   │   ├── RenderConfig.kt
│   │   │   └── EncodeConfig.kt
│   │   └── repository/     # Repository interfaces
│   │       ├── ICameraRepository.kt
│   │       └── IDeviceRepository.kt
│   └── common/
│       ├── error/          # Error handling
│       │   ├── CameraError.kt
│       │   └── ErrorHandler.kt
│       └── result/         # Result types
│           ├── CameraResult.kt
│           ├── CaptureResult.kt
│           ├── AudioResult.kt
│           └── EncodeFileResult.kt
```

**Key Features:**
- Sealed class hierarchy for errors (`CameraError`)
- Result types for type-safe operations (`CameraResult<T>`)
- StateFlow for reactive state management
- Coroutines-based async operations

#### libausbc-camera
**Camera implementations and data sources**

```
libausbc-camera/
├── src/main/java/com/jiangdg/ausbc/camera/
│   ├── uvc/                # UVC camera implementations
│   │   ├── UvcCamera.kt
│   │   ├── UvcCameraV2.kt
│   │   └── UvcCameraFactory.kt
│   ├── datasource/         # Data sources
│   │   ├── IUvcCameraDataSource.kt
│   │   ├── IUsbDeviceDataSource.kt
│   │   ├── ICameraParameterDataSource.kt
│   │   ├── UvcCameraDataSource.kt
│   │   ├── UsbDeviceDataSource.kt
│   │   └── CameraParameterDataSource.kt
│   ├── data/
│   │   └── repository/     # Repository implementations
│   │       ├── CameraRepository.kt
│   │       └── DeviceRepository.kt
│   ├── lifecycle/          # Lifecycle management
│   │   ├── CameraLifecycleManager.kt
│   │   └── CameraStateManager.kt
│   ├── platform/           # Platform services
│   │   └── UsbDeviceManager.kt
│   └── di/                 # Dependency injection
│       └── CameraModule.kt
```

**Dependencies:**
- `api project(:libausbc-core)` - Exposes core API
- `implementation project(:libuvc)` - Native UVC
- `implementation project(:libausbc)` - Legacy library

#### libausbc-render
**OpenGL ES rendering with effects**

```
libausbc-render/
├── src/main/java/com/jiangdg/ausbc/render/
│   ├── engine/            # Render engines
│   ├── effect/            # Effect system
│   │   ├── AbstractEffect.kt
│   │   ├── EffectBlackWhite.kt
│   │   ├── EffectSoul.kt
│   │   └── EffectZoom.kt
│   └── env/               # Render environment
│       └── RotateType.kt
```

#### libausbc-encode
**Media encoding (H.264, AAC, MP4)**

```
libausbc-encode/
├── src/main/java/com/jiangdg/ausbc/encode/
│   ├── video/             # H.264 encoding
│   ├── audio/             # AAC encoding
│   └── muxer/             # MP4 muxing
```

#### libausbc-utils
**Shared utilities**

```
libausbc-utils/
├── src/main/java/com/jiangdg/ausbc/utils/
│   ├── permission/        # Permission utilities
│   ├── flow/              # Flow extensions
│   ├── logging/           # Logging utilities
│   └── extensions/        # Kotlin extensions
```

### Legacy Modules (Being Migrated)

#### libausbc
**Main Kotlin library (Legacy - being migrated)**

```
libausbc/
├── src/main/java/com/jiangdg/ausbc/
│   ├── base/              # Base classes
│   │   ├── BaseFragment.kt
│   │   ├── BaseActivity.kt
│   │   ├── CameraFragment.kt
│   │   ├── CameraActivity.kt
│   │   ├── MultiCameraFragment.kt
│   │   └── MultiCameraActivity.kt
│   ├── camera/            # Camera implementations
│   │   ├── ICameraStrategy.kt       # Deprecated
│   │   ├── CameraUvcStrategy.kt     # Deprecated
│   │   ├── Camera1Strategy.kt       # Deprecated
│   │   ├── Camera2Strategy.kt       # Deprecated
│   │   ├── CameraUVC.kt             # Runtime API
│   │   └── bean/
│   │       ├── CameraRequest.kt
│   │       ├── PreviewSize.kt
│   │       └── CameraInfo.kt
│   ├── render/            # Rendering
│   │   ├── RenderManager.kt
│   │   ├── CameraRender.kt
│   │   └── effect/
│   ├── encode/            # Encoding
│   │   ├── H264EncodeProcessor.kt
│   │   ├── AACEncodeProcessor.kt
│   │   ├── muxer/Mp4Muxer.kt
│   │   └── audio/
│   │       ├── IAudioStrategy.kt
│   │       ├── AudioStrategySystem.kt
│   │       └── AudioStrategyUAC.kt
│   ├── callback/          # Callbacks
│   │   ├── ICameraStateCallBack.kt
│   │   ├── ICaptureCallBack.kt
│   │   ├── IPreviewDataCallBack.kt
│   │   ├── IEncodeDataCallBack.kt
│   │   ├── IPlayCallBack.kt
│   │   └── IDeviceConnectCallBack.kt
│   ├── widget/            # Custom views
│   │   ├── IAspectRatio.kt
│   │   ├── AspectRatioTextureView.kt
│   │   └── AspectRatioSurfaceView.kt
│   ├── MultiCameraClient.kt
│   └── CameraClient.kt
```

#### libuvc
**Native JNI library for USB camera communication (ndk-build)**

```
libuvc/
├── src/main/jni/
│   ├── android/
│   │   └── UVCCamera.cpp/h       # JNI bridge
│   ├── uvccamera/
│   │   ├── UVCCamera.cpp/h       # Main camera class
│   │   ├── UVCPreview.cpp/h      # Preview handling
│   │   ├── Parameters.cpp/h      # Camera parameters
│   │   └── pipeline/             # Processing pipeline
│   ├── libusb/                   # USB library
│   ├── libjpeg-turbo-1.5.0/      # JPEG encoding
│   ├── Android.mk                # Build script
│   └── Application.mk            # NDK config
├── build.gradle.kts
```

**JNI Method Pattern:**
```cpp
Java_com_jiangdg_usb_UVCCamera_*  // JNI bridge methods
```

#### libnative
**Native library with LAME MP3 encoder and YUV utilities (CMake)**

```
libnative/
├── src/main/cpp/
│   ├── module/
│   │   ├── mp3/
│   │   │   └── lame/             # LAME MP3 encoder
│   │   └── yuv/
│   │       └── yuv.cpp           # YUV utilities
│   ├── proxy/
│   │   ├── proxy_mp3.cpp         # MP3 JNI bridge
│   │   └── proxy_yuv.cpp         # YUV JNI bridge
│   ├── utils/
│   │   └── logger.cpp
│   ├── nativelib.cpp             # Main JNI entry
│   └── CMakeLists.txt            # CMake build
├── build.gradle.kts
```

#### app
**Demo application showing library usage**

```
app/
├── src/main/
│   ├── java/com/jiangdg/demo/
│   │   ├── MainActivity.kt           # Entry point
│   │   ├── DemoFragment.kt           # Single camera demo
│   │   ├── DemoMultiCameraFragment.kt # Multi-camera demo
│   │   ├── GlSurfaceFragment.kt      # GLSurface demo
│   │   ├── DemoApplication.kt        # Application class
│   │   └── dialog/
│   │       ├── EffectListDialog.kt
│   │       └── MultiCameraDialog.kt
│   ├── res/
│   │   ├── xml/default_device_filter.xml  # USB device filter
│   │   └── layout/
│   └── AndroidManifest.xml
```

## Architecture Comparison

### Legacy Architecture (Deprecated)

```kotlin
// Strategy pattern with callbacks
interface ICameraStrategy {
    fun openCamera()
    fun closeCamera()
    fun setCameraStateCallBack(callback: ICameraStateCallBack)
}

class CameraUvcStrategy : ICameraStrategy {
    // Implementation with callback-based API
}
```

### New Architecture (Recommended)

```kotlin
// Modern interface with coroutines and StateFlow
interface ICamera {
    val cameraState: StateFlow<CameraState>
    val previewFrames: Flow<PreviewFrame>

    suspend fun open(request: CameraRequest): CameraResult<Unit>
    suspend fun close(): CameraResult<Unit>
    suspend fun startPreview(surface: Surface): CameraResult<Unit>
}
```

## Data Flow

### Camera Open Flow

```
User Action
    │
    ▼
CameraFragment.initView()
    │
    ▼
MultiCameraClient.register()
    │
    ▼
USBDevice.attach()
    │
    ▼
requestPermission()
    │
    ▼
USBDevice.connect()
    │
    ▼
Camera.openCamera()
    │
    ▼
onCameraState(OPENED)
```

### Preview Frame Flow

```
USB Camera (libuvc)
    │
    ▼ Native callback
UVCCamera.cpp
    │
    ▼ JNI bridge
UVCCamera.java
    │
    ▼
CameraRender
    │
    ▼
OpenGL ES
    │
    ▼
SurfaceView/TextureView
```

## Key Classes and Their Roles

### Base Classes

| Class | Purpose | Module |
|-------|---------|--------|
| `CameraFragment` | Base fragment for single camera | libausbc |
| `MultiCameraFragment` | Base for multi-camera | libausbc |
| `ICamera` | Modern camera operations interface | libausbc-core |
| `ICameraStrategy` | Deprecated strategy interface | libausbc |

### Camera Implementations

| Class | Purpose | Module |
|-------|---------|--------|
| `CameraUVC` | UVC camera runtime API | libausbc |
| `CameraUvcStrategy` | Deprecated UVC strategy | libausbc |
| `UvcCamera` | Modern UVC implementation | libausbc-camera |

### Configuration

| Class | Purpose | Module |
|-------|---------|--------|
| `CameraRequest` | Camera configuration builder | libausbc |
| `CameraRequest.Builder` | Fluent configuration API | libausbc |
| `PreviewSize` | Preview size data class | libausbc |

### Rendering

| Class | Purpose | Module |
|-------|---------|--------|
| `RenderManager` | OpenGL context management | libausbc |
| `CameraRender` | Camera preview rendering | libausbc |
| `AbstractEffect` | Base effect class | libausbc |

## Migration Status

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1: Foundation | ✅ Complete | Core infrastructure |
| Phase 2: Core Module | ✅ Complete | libausbc-core |
| Phase 3: Data Layer | ✅ Complete | Repository pattern |
| Phase 4: Render Module | ✅ Complete | libausbc-render |
| Phase 5: Encode Module | 🔄 In Progress | libausbc-encode |

**Note:** During migration, both legacy and new APIs are fully functional. Use the new `ICamera` interface for new code.

## Dependencies Graph

```
app
├── libausbc-core (api)
├── libausbc-camera (api)
├── libausbc-render (implementation)
├── libausbc-encode (implementation)
├── libausbc-utils (implementation)
├── libausbc (implementation - legacy)
└── Hilt (dependency injection)

libausbc-camera
├── libausbc-core (api)
├── libuvc (implementation)
└── libausbc (implementation - legacy)

libausbc-render
├── libausbc-core (api)
└── OpenGL ES 2.0

libausbc-encode
├── libausbc-core (api)
└── MediaCodec API

libausbc
├── libuvc (native)
└── libnative (native)
```

## File Organization Conventions

### Package Structure

```
com.jiangdg.ausbc
├── core/           # New architecture core
│   ├── contract/   # Interfaces
│   ├── domain/     # Domain models
│   └── common/     # Common utilities
├── camera/         # Camera implementations
├── render/         # Rendering
├── encode/         # Encoding
├── utils/          # Utilities
├── widget/         # Custom views
├── callback/       # Callbacks
└── base/           # Base classes
```

### Naming Conventions

- **Interfaces**: Prefix with `I` (e.g., `ICamera`, `IAspectRatio`)
- **Callbacks**: Prefix with `I` and suffix with `CallBack` (e.g., `ICaptureCallBack`)
- **Strategies**: Suffix with `Strategy` (deprecated)
- **Managers**: Suffix with `Manager` (e.g., `RenderManager`)
- **Data classes**: Plain names (e.g., `CameraRequest`, `PreviewSize`)

## Build System Organization

| Module | Build System | Build File |
|--------|--------------|------------|
| app | Gradle | `build.gradle.kts` |
| libausbc-core | Gradle + Hilt | `build.gradle.kts` |
| libausbc-camera | Gradle + Hilt | `build.gradle.kts` |
| libausbc-render | Gradle | `build.gradle.kts` |
| libausbc-encode | Gradle | `build.gradle.kts` |
| libausbc-utils | Gradle | `build.gradle.kts` |
| libausbc | Gradle | `build.gradle.kts` |
| libuvc | Gradle + ndk-build | `build.gradle.kts`, `Android.mk` |
| libnative | Gradle + CMake | `build.gradle.kts`, `CMakeLists.txt` |

See [BUILD_SETUP.md](BUILD_SETUP.md) for detailed build configuration.
