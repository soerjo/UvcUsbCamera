# AUSBC Developer Guide

Welcome to the AUSBC (Android USBCamera) developer documentation. This guide helps you use AUSBC as a reference for building UVC (USB Video Class) camera applications on Android.

## Overview

AUSBC is a complete UVC camera engine that allows opening USB cameras without system permissions. It provides:

- **Native USB Communication**: Direct USB UVC camera support via libuvc
- **GPU Rendering**: OpenGL ES 2.0 rendering with effects support
- **Media Encoding**: H.264 video, AAC audio, MP4 muxing
- **Audio Support**: System mic and UAC (USB Audio Class) audio
- **Camera Controls**: Brightness, contrast, zoom, focus, and more

## Project Status

The project is undergoing a major architectural restructuring:

- **Legacy modules** (`libausbc`, `libuvc`, `libnative`) - Fully functional, production-ready
- **New modules** (`libausbc-core`, `libausbc-camera`, `libausbc-render`, `libausbc-encode`) - Modern clean architecture with coroutines

## Quick Start

### Prerequisites

- **Android Studio**: Hedgehog | 2023.1.1 or later
- **JDK**: 17
- **NDK**: 27.0.12077973 (for native builds)
- **CMake**: 3.22.1+ (for libnative)
- **Android SDK**: API 35 (compile), API 24+ (min)

### Clone and Run

```bash
# Clone the repository
git clone https://github.com/jiangdg/AUSBC.git
cd UvcUsbCamera

# Set up NDK (see BUILD_SETUP.md)
cp local.properties.template local.properties
# Edit local.properties to set ndk.dir

# Open in Android Studio
# Run the demo app on a device with a UVC camera connected
```

### Minimal "Hello World" Example

```kotlin
class MyCameraFragment : CameraFragment() {

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return binding.cameraViewContainer
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                // Camera opened successfully
            }
            ICameraStateCallBack.State.CLOSED -> {
                // Camera closed
            }
            ICameraStateCallBack.State.ERROR -> {
                // Handle error: $msg
            }
        }
    }
}
```

## Learning Path

Choose your starting point based on your needs:

### New to UVC Cameras?
Start with [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) to understand the architecture and module organization.

### Want to Integrate into Your Project?
See [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) for step-by-step integration instructions.

### Build Issues?
Check [BUILD_SETUP.md](BUILD_SETUP.md) for NDK configuration and build troubleshooting.

### Need API Reference?
See [API_REFERENCE.md](API_REFERENCE.md) for detailed API documentation with examples.

### Working with Native Code?
Read [NATIVE_LIBRARIES.md](NATIVE_LIBRARIES.md) for libuvc and libnative details.

### Common Problems?
Visit [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for solutions to common issues.

## Key Concepts

### Camera Fragment API

The library uses a `CameraFragment` base class that handles USB device detection, permission requests, and camera lifecycle. Simply extend it and implement the required methods.

### Camera Request Configuration

Configure camera behavior using `CameraRequest.Builder()`:

```kotlin
CameraRequest.Builder()
    .setPreviewWidth(640)
    .setPreviewHeight(480)
    .setRenderMode(CameraRequest.RenderMode.OPENGL)
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
    .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
    .create()
```

### Camera Types

- **CameraUVC**: USB UVC cameras (main feature)
- **Camera1Strategy**: Legacy Camera API (deprecated)
- **Camera2Strategy**: Camera2 API (for built-in cameras)

### Render Modes

- **OpenGL**: GPU rendering with effects support
- **Normal**: Direct surface rendering (no effects)

## Architecture Evolution

### Legacy Architecture (Deprecated)

- `ICameraStrategy` interface with strategy pattern
- Callback-based API
- Monolithic `libausbc` module

### New Architecture (Recommended)

- `ICamera` interface from `libausbc-core`
- Coroutine-based async operations
- StateFlow for reactive state management
- Modular architecture with separated concerns

## Module Overview

| Module | Purpose | Build System |
|--------|---------|--------------|
| `libausbc-core` | Core abstractions and domain models | - |
| `libausbc-camera` | Camera implementations | - |
| `libausbc-render` | OpenGL ES rendering | - |
| `libausbc-encode` | Media encoding | - |
| `libausbc-utils` | Shared utilities | - |
| `libausbc` | Legacy library (deprecated) | - |
| `libuvc` | Native UVC implementation | ndk-build |
| `libnative` | Native utilities (MP3, YUV) | CMake |
| `app` | Demo application | - |

## Next Steps

1. **Explore the demo**: Run the app and connect a UVC camera
2. **Read the architecture**: Understand the project structure
3. **Try integration**: Follow the integration guide for your project
4. **Check API reference**: Learn about available APIs

## Additional Resources

- [GitHub Repository](https://github.com/jiangdg/AUSBC)
- [USB Video Class Specification](https://www.usb.org/documents)
- [Android USB Documentation](https://developer.android.com/guide/topics/connectivity/usb)

## License

```
Copyright 2017-2023 Jiangdg

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
