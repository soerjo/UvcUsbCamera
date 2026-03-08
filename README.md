# AUSBC (Android USBCamera)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)

A powerful UVC (USB Video Class) camera engine for Android that allows you to open UVC cameras without system permissions.

## Features

- **No System Permission Required** - Works with UVC cameras directly via USB
- **Multi-Camera Support** - Open and manage multiple UVC cameras simultaneously
- **OpenGL ES Rendering** - GPU-based rendering with real-time effects support
- **Media Encoding** - H.264 video encoding, AAC audio encoding, and MP4 muxing
- **Audio Recording** - Support for system microphone and UAC (USB Audio Class) audio
- **Camera Controls** - Runtime parameter adjustments (brightness, contrast, zoom, focus, etc.)
- **Multiple Preview Modes** - TextureView and SurfaceView support
- **Rotation Support** - Camera view rotation (0°, 90°, 180°, 270°)
- **Capture Support** - Photo capture, video recording, and audio recording
- **Clean Architecture** - Modern modular architecture with separated concerns
- **Kotlin First** - Written entirely in Kotlin with coroutines support
- **Hilt DI** - Dependency injection with Hilt

## Screenshots

<img src="images/logo.png" alt="AUSBC Logo" width="120"/>

## Documentation

| Document | Description |
|----------|-------------|
| **[Quick Start Guide](docs/QUICKSTART.md)** | Get your UVC camera running in 5 minutes |
| **[Usage Guide](docs/USAGE.md)** | Complete integration guide with examples |
| **[FAQ](FAQ.md)** | Frequently asked questions |
| **[VERSION.md](VERSION.md)** | Version history and changelog |
| **[CLAUDE.md](CLAUDE.md)** | Project architecture and developer guide |

### Quick Links

- **[Installation](#installation)** - How to add the library
- **[Quick Start](#quick-start)** - Basic usage example
- **[Full Usage Guide](docs/USAGE.md)** - Comprehensive documentation with examples

## Requirements

| Requirement | Version |
|-------------|---------|
| Min SDK | 24 (Android 7.0+) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Java | 17 |
| Kotlin | 1.9.22 |
| NDK | 27.0.12077973 (for native builds) |

## Module Structure

### New Modular Architecture (Recommended)

The project is transitioning to a clean, modular architecture with separated concerns:

| Module | Description |
|--------|-------------|
| **libausbc-core** | Core abstractions, domain models, error handling, result types, and repository interfaces |
| **libausbc-camera** | Camera implementations (UvcCamera, UvcCameraV2), data sources, repositories, and lifecycle management |
| **libausbc-render** | OpenGL ES rendering engine with effects support |
| **libausbc-encode** | Media encoding (H.264, AAC) and MP4 muxing |
| **libausbc-utils** | Shared utilities (permissions, Flow extensions, logging) |

### Legacy Modules (Being Migrated)

| Module | Description |
|--------|-------------|
| **libausbc** | Main Kotlin library with camera abstractions and base classes |
| **libuvc** | Native JNI library for USB camera communication (ndk-build) |
| **libnative** | Native library with LAME MP3 encoder and YUV utilities (CMake) |

### Demo Application

| Module | Description |
|--------|-------------|
| **app** | Demo application showing library usage with examples |

## Installation

### Gradle

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.jiangdongguo:AndroidUSBCamera:3.3.0")
}
```

Add JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### From Source

```bash
# Clone the repository
git clone https://github.com/jiangdongguo/AndroidUSBCamera.git
cd AndroidUSBCamera

# Build the project
./gradlew build

# Install the library modules
./gradlew :libausbc:publishToMavenLocal
```

## Quick Start

### 1. Add USB Device Filter

Create `app/src/main/res/xml/default_device_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<usb>
    <!-- All UVC devices -->
    <usb-device class="239" subclass="2" />

    <!-- Your specific device -->
    <usb-device product-id="xxxx" vendor-id="xxxx" />
</usb>
```

### 2. Extend CameraFragment

```kotlin
class MyCameraFragment : CameraFragment() {

    override fun getCameraView(): IAspectRatio? {
        // Return TextureView or SurfaceView
        return binding.textureView
    }

    override fun getCameraViewContainer(): ViewGroup? {
        // Return the container for camera view
        return binding.cameraContainer
    }

    override fun onCameraState(state: CameraStatus) {
        when(state) {
            CameraStatus.OPENED -> {
                // Camera opened successfully
            }
            CameraStatus.CLOSED -> {
                // Camera closed
            }
            CameraStatus.ERROR -> {
                // Camera error occurred
            }
        }
    }
}
```

### 3. Configure Camera Request

```kotlin
override fun getCameraRequest(): CameraRequest {
    return CameraRequest.Builder()
        .setPreviewWidth(640)
        .setPreviewHeight(480)
        .setRenderMode(CameraRequest.RenderMode.OPENGL)
        .setDefaultRotateType(RotateType.ANGLE_0)
        .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)
        .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
        .setAspectRatioShow(true)
        .setCaptureRawImage(false)
        .setRawPreviewData(false)
        .create()
}
```

### 4. Capture Features

```kotlin
// Capture image
captureImage(object : ICaptureCallBack {
    override fun onCaptureBegin() {}
    override fun onCaptureComplete(path: String) {
        // Image saved at path
    }
    override fun onCaptureError(error: String) {}
}, savePath)

// Start video recording
captureVideoStart(callBack, path, durationInSec)

// Stop video recording
captureVideoStop()

// Start audio recording
captureAudioStart(callBack, path)

// Stop audio recording
captureAudioStop()
```

### 5. Camera Parameter Controls

```kotlin
// Get current camera
val camera = getCurrentCamera() as? CameraUVC

// Adjust parameters
camera?.setBrightness(128)
camera?.setContrast(128)
camera?.setZoom(100)

// Auto focus
camera?.setAutoFocus(true)

// Custom camera command
camera?.sendCameraCommand(UVCCamera.CAMERA_COMMAND, value)
```

## Modern API (New Architecture)

The new architecture provides a coroutine-based API with Flow support:

### Using ICamera Interface

```kotlin
class MyViewModel @Inject constructor(
    private val cameraRepository: ICameraRepository
) : ViewModel() {

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    suspend fun openCamera(request: CameraRequest) {
        when (val result = cameraRepository.openCamera(request)) {
            is CameraResult.Success -> {
                _cameraState.value = CameraState.Opened
            }
            is CameraResult.Error -> {
                _cameraState.value = CameraState.Error(result.error)
            }
        }
    }

    fun getPreviewFrames(): Flow<PreviewFrame> {
        return cameraRepository.getPreviewFrameFlow()
    }
}
```

### Dependency Injection with Hilt

```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraRepository(
        dataSource: IUvcCameraDataSource
    ): ICameraRepository {
        return CameraRepository(dataSource)
    }
}
```

## Camera Request Configuration

| Parameter | Type | Description |
|-----------|------|-------------|
| `previewWidth` | Int | Preview width (default: 640) |
| `previewHeight` | Int | Preview height (default: 480) |
| `renderMode` | RenderMode | OPENGL (GPU effects) or MEDIAN (direct) |
| `rotateType` | RotateType | ANGLE_0, ANGLE_90, ANGLE_180, ANGLE_270 |
| `audioSource` | AudioSource | SOURCE_SYS_MIC, SOURCE_AUTO, SOURCE_UAC |
| `previewFormat` | PreviewFormat | FORMAT_MJPEG or FORMAT_YUYV |
| `aspectRatioShow` | Boolean | Maintain aspect ratio (default: true) |
| `captureRawImage` | Boolean | Capture raw image data (default: false) |
| `rawPreviewData` | Boolean | Get raw preview data callback (default: false) |

## Render Effects

The library supports custom OpenGL ES effects:

### Built-in Effects

| Effect | Description |
|--------|-------------|
| `EffectBlackWhite` | Black and white filter |
| `EffectSoul` | Soul/ghost effect |
| `EffectZoom` | Zoom effect |

### Custom Effects

```kotlin
abstract class AbstractEffect {
    abstract fun getId(): Int
    abstract fun getName(): String
    abstract fun getShader(): String
    abstract fun draw(textureId: Int, texMatrix: Float[])
}

// Add effect
addRenderEffect(myEffect)

// Remove effect
removeRenderEffect(myEffect)

// Update effect
updateRenderEffect(classifyId, newEffect)
```

## Multi-Camera Support

```kotlin
class MultiCameraFragment : MultiCameraFragment() {

    override fun getCameraViewContainer(): ViewGroup? {
        return binding.cameraContainer
    }

    override fun getCameraView(): IAspectRatio? {
        // Return null for multi-camera (will create multiple views)
        return null
    }

    // Get list of connected cameras
    val cameras = getDeviceList()

    // Switch to specific camera
    switchCamera(usbDevice)
}
```

## Audio Support

### Audio Sources

| Source | Description |
|--------|-------------|
| `SOURCE_SYS_MIC` | System microphone |
| `SOURCE_AUTO` | Auto-detect (prefers UAC if available) |
| `SOURCE_UAC` | USB Audio Class microphone |

### Audio Recording

```kotlin
// Start audio recording
captureAudioStart(object : ICaptureCallBack {
    override fun onCaptureBegin() {}
    override fun onCaptureComplete(path: String) {
        // Audio file saved
    }
    override fun onCaptureError(error: String) {}
}, savePath)
```

## Build Configuration

### Native Build Setup

For native builds, configure NDK in `local.properties`:

```properties
ndk.dir=/path/to/your/Android/Sdk/ndk/27.0.12077973
```

Or set the `NDK_HOME` environment variable.

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
./gradlew :libausbc-core:assembleRelease
```

## Architecture

### Legacy Camera Strategy Pattern

```
ICameraStrategy (deprecated)
├── CameraUvcStrategy - USB UVC cameras
├── Camera1Strategy - Legacy Camera API
└── Camera2Strategy - Camera2 API
```

### Modern Runtime API

```
MultiCameraClient.ICamera
└── CameraUVC - UVC camera implementation
```

### New Architecture

```
libausbc-core (Domain Layer)
├── Contracts: ICamera, IRenderEngine, IEncodeEngine, IAudioStrategy
├── Domain Models: CameraRequest, CameraState, PreviewSize, etc.
├── Error Handling: CameraError (sealed class)
└── Repositories: ICameraRepository, IDeviceRepository

libausbc-camera (Data Layer)
├── Camera: UvcCamera, UvcCameraV2
├── Data Sources: IUvcCameraDataSource, IUsbDeviceDataSource
├── Repositories: CameraRepository, DeviceRepository
└── Lifecycle: CameraLifecycleManager, CameraStateManager

libausbc-render (Presentation Layer)
├── Engines: OpenGLRenderEngine, SurfaceRenderEngine
└── Effects: EffectManager, RenderEffect

libausbc-encode (Encoding Layer)
├── Engines: H264EncodeEngine, AACEncodeEngine
└── Muxer: Mp4MuxerV2

libausbc-utils (Utilities)
├── PermissionUtils
├── FlowUtils
└── Logger
```

## API Reference

### CameraFragment

| Method | Description |
|--------|-------------|
| `getCameraView()` | Returns the camera view (TextureView/SurfaceView) |
| `getCameraViewContainer()` | Returns the container ViewGroup |
| `getCameraRequest()` | Returns camera configuration |
| `onCameraState()` | Camera state callbacks |
| `getDefaultCamera()` | Auto-open specific USB device |
| `captureImage()` | Capture photo |
| `captureVideoStart()` | Start video recording |
| `captureVideoStop()` | Stop video recording |
| `captureAudioStart()` | Start audio recording |
| `captureAudioStop()` | Stop audio recording |

### Callbacks

| Interface | Description |
|-----------|-------------|
| `ICameraStateCallBack` | Camera state (OPENED, CLOSED, ERROR) |
| `ICaptureCallBack` | Capture status (photo/video/audio) |
| `IPreviewDataCallBack` | Raw preview data (NV21/RGBA) |
| `IEncodeDataCallBack` | Encoded data (H.264/AAC) |
| `IPlayCallBack` | Real-time microphone playback |
| `IDeviceConnectCallBack` | Device connect/disconnect events |

## Version History

See [VERSION.md](VERSION.md) for detailed version history.

| Version | Date | Highlights |
|---------|------|-------------|
| 3.3.0 | 2022+ | Modern ICamera API, refactor |
| 3.2.7 | 2022.08 | Multi-camera, camera parameters, Android S+ fix |
| 3.2.0 | 2022.07 | Multi-road camera support |
| 3.1.x | 2022.07 | Native memory leak fix |
| 3.0.0 | 2022.07 | Kotlin refactor, OpenGL effects, capture support |
| 2.x | 2020-2021 | Java version, basic UVC support |

## FAQ

See [FAQ.md](FAQ.md) for frequently asked questions.

**Common Issues:**

1. **Camera preview black screen?**
   - Grant `android.permission.CAMERA` permission for targetSdk>=28
   - Check device filter configuration

2. **How to filter specific devices?**
   - Add device to `default_device_filter.xml`

3. **ANR on hot plug?**
   - Update to version 3.2.7+

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Dependencies

### App Module
- androidx.appcompat
- androidx.core:core-ktx
- com.google.android.material
- org.jetbrains.kotlinx:kotlinx-coroutines-android 1.9.0
- androidx.lifecycle 2.8.7
- com.google.dagger:hilt-android 2.48

### Library Modules
- com.elvishew:xlog - Logging
- libuvc - Native UVC implementation
- libnative - Native utilities (LAME, YUV)

## Third-Party Libraries

- **libuvc** - USB video class library
- **libjpeg-turbo** - JPEG codec
- **libusb** - USB device access
- **rapidjson** - JSON parsing
- **LAME** - MP3 encoder

## License

```
Copyright 2017-2022 jiangdongguo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Author

**jiangdongguo**

- GitHub: [@jiangdongguo](https://github.com/jiangdongguo)
- Project: [AndroidUSBCamera](https://github.com/jiangdongguo/AndroidUSBCamera)

## Related Projects

- [UVCCameraLib](https://github.com/jiangdongguo/UVCCameraLib) - Native UVC NDK Library

---

**Note:** The project is undergoing a major architectural restructuring. Both legacy and new modular architectures coexist during the migration period. See [CLAUDE.md](CLAUDE.md) for detailed project documentation and migration status.
