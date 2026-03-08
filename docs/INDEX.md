# AUSBC Documentation Index

Welcome to the AUSBC (Android USBCamera) documentation hub. This library allows you to use UVC (USB Video Class) cameras in your Android applications without requiring system camera permissions.

## 📚 Getting Started

| Document | For Developers Who... | Time to Read |
|----------|----------------------|--------------|
| **[Quick Start Guide](QUICKSTART.md)** | Want to get a camera running in 5 minutes | ~5 min |
| **[Usage Guide](USAGE.md)** | Need comprehensive integration instructions | ~30 min |
| **[FAQ](../FAQ.md)** | Have specific questions or issues | ~5 min |

## 🏗️ Architecture

| Document | Description |
|----------|-------------|
| **[Project Guide (CLAUDE.md)](../CLAUDE.md)** | Project structure, architecture patterns, and file reference |

## 📦 Module Documentation

### Core Modules (New Architecture)

| Module | Description | API Reference |
|--------|-------------|---------------|
| **libausbc-core** | Core abstractions, domain models, error handling, result types | See [USAGE.md](USAGE.md#4-modern-architecture-usage) |
| **libausbc-camera** | Camera implementations, data sources, repositories | See [USAGE.md](USAGE.md#4-modern-architecture-usage) |
| **libausbc-render** | OpenGL ES rendering engine with effects support | See [USAGE.md](USAGE.md#8-custom-effects) |
| **libausbc-encode** | Media encoding (H.264, AAC) and MP4 muxing | See [USAGE.md](USAGE.md#5-advanced-features) |
| **libausbc-utils** | Shared utilities (permissions, Flow extensions, logging) | See [USAGE.md](USAGE.md#9-common-patterns) |

### Legacy Modules

| Module | Description | Migration Status |
|--------|-------------|------------------|
| **libausbc** | Main Kotlin library with camera abstractions | Migrating to new modules |
| **libuvc** | Native JNI library for USB camera communication | Will be migrated to libausbc-native |
| **libnative** | Native library with LAME MP3 encoder and YUV utilities | Will be migrated to libausbc-native |

## 🚀 Features by Category

### Camera Operations
- [Opening a Camera](USAGE.md#31-create-camerafragment)
- [Multi-Camera Support](USAGE.md#6-multi-camera-support)
- [Camera Resolution Switching](USAGE.md#54-camera-resolution-switching)
- [Camera Parameter Controls](USAGE.md#51-camera-parameter-controls)

### Capture Features
- [Photo Capture](USAGE.md#35-create-camerafragment)
- [Video Recording](USAGE.md#35-create-camerafragment)
- [Audio Recording](USAGE.md#7-audio-recording)
- [Raw Preview Data](USAGE.md#52-preview-data-callback)
- [Encoded Stream Data](USAGE.md#53-encode-data-callback)

### Rendering & Effects
- [Built-in Effects](USAGE.md#81-add-built-in-effects)
- [Custom Effects](USAGE.md#82-create-custom-effect)
- [Camera Rotation](USAGE.md#55-camera-rotation)

### Audio Features
- [Audio Source Selection](USAGE.md#72-audio-source-selection)
- [Audio Recording](USAGE.md#71-record-audio-only)
- [Audio Playback](USAGE.md#73-audio-playback)

## 📖 Code Examples

The [Usage Guide](USAGE.md) contains complete examples for:

- Basic camera setup with `CameraFragment`
- Modern architecture with `ViewModel` and Hilt DI
- Multi-camera support with `MultiCameraFragment`
- Custom OpenGL effects
- Camera parameter adjustments
- Audio recording and playback
- Capture and recording callbacks

## 🔧 Configuration Reference

### Camera Request Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `previewWidth` | Int | 640 | Preview width in pixels |
| `previewHeight` | Int | 480 | Preview height in pixels |
| `renderMode` | RenderMode | OPENGL | OPENGL or MEDIAN |
| `rotateType` | RotateType | ANGLE_0 | 0°, 90°, 180°, 270° |
| `audioSource` | AudioSource | SOURCE_SYS_MIC | System mic, UAC, or auto |
| `previewFormat` | PreviewFormat | FORMAT_MJPEG | MJPEG or YUYV |
| `aspectRatioShow` | Boolean | true | Maintain aspect ratio |
| `captureRawImage` | Boolean | false | Capture raw image data |
| `rawPreviewData` | Boolean | false | Get raw preview callbacks |

See [Usage Guide - Camera Request Configuration](USAGE.md#43-create-hilt-module) for details.

### Audio Sources

| Source | Description |
|--------|-------------|
| `SOURCE_SYS_MIC` | System microphone |
| `SOURCE_AUTO` | Auto-detect (prefers UAC) |
| `SOURCE_UAC` | USB Audio Class microphone |

### Render Modes

| Mode | Description |
|------|-------------|
| `OPENGL` | GPU rendering with effects support |
| `MEDIAN` | Direct surface rendering (no effects) |

### Rotation Types

| Type | Description |
|------|-------------|
| `ANGLE_0` | No rotation |
| `ANGLE_90` | 90° clockwise |
| `ANGLE_180` | 180° rotation |
| `ANGLE_270` | 270° clockwise |

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Black screen preview | Check [Troubleshooting - Camera Preview Black Screen](USAGE.md#101-camera-preview-black-screen) |
| Native library error | Check [Troubleshooting - Native Library Not Found](USAGE.md#102-native-library-not-found) |
| Hot plug ANR | Check [Troubleshooting - Hot Plug ANR](USAGE.md#103-hot-plug-anr) |
| Recording issues | Check [Troubleshooting - Recording Issues](USAGE.md#104-recording-issues) |

## 📋 Migration Guide

### From ICameraStrategy to ICamera

The `ICameraStrategy` interface is deprecated since v3.3.0. Migrate to the new `ICamera` interface:

```kotlin
// Old (deprecated)
class MyFragment : CameraFragment() {
    // Uses ICameraStrategy internally
}

// New (recommended)
class MyFragment : CameraFragment() {
    // Uses MultiCameraClient.ICamera internally
}
```

### To New Modular Architecture

See [Usage Guide - Modern Architecture Usage](USAGE.md#4-modern-architecture-usage) for the new coroutine-based API with Flow support.

## 📄 Additional Resources

- **[Version History](../VERSION.md)** - What's new in each version
- **[FAQ](../FAQ.md)** - Common questions and solutions
- **[Demo App](../app/src/main/java/com/jiangdg/demo/)** - Example implementations
- **[GitHub Issues](https://github.com/jiangdongguo/AndroidUSBCamera/issues)** - Bug reports and feature requests

## 🔗 Quick Links

- [Main README](../README.md)
- [Quick Start Guide](QUICKSTART.md)
- [Full Usage Guide](USAGE.md)
- [FAQ](../FAQ.md)
- [GitHub Repository](https://github.com/jiangdongguo/AndroidUSBCamera)

---

**Need Help?**

1. Check the [FAQ](../FAQ.md)
2. Search existing [GitHub Issues](https://github.com/jiangdongguo/AndroidUSBCamera/issues)
3. Create a new issue with logs and device information

---

**Last Updated:** 2025-01-09
