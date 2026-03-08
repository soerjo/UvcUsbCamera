# AUSBC Library Usage Guide

Complete guide on how to integrate and use the AUSBC (Android USBCamera) library in your Android project.

## Table of Contents

1. [Installation](#1-installation)
2. [Project Setup](#2-project-setup)
3. [Basic Usage](#3-basic-usage)
4. [Modern Architecture Usage](#4-modern-architecture-usage)
5. [Advanced Features](#5-advanced-features)
6. [Multi-Camera Support](#6-multi-camera-support)
7. [Audio Recording](#7-audio-recording)
8. [Custom Effects](#8-custom-effects)
9. [Common Patterns](#9-common-patterns)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Installation

### 1.1 Add JitPack Repository

In your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 1.2 Add Dependency

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    // For the latest version, check the GitHub releases
    implementation("com.github.jiangdongguo:AndroidUSBCamera:3.3.0")
}
```

### 1.3 Sync Project

Click "Sync Now" in Android Studio or run:

```bash
./gradlew sync
```

---

## 2. Project Setup

### 2.1 Configure Application Class

Create or update your Application class:

```kotlin
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the library
    }
}
```

Register in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...>
    ...
</application>
```

### 2.2 Add Permissions

In `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.myapp">

    <!-- Required permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.USB_PERMISSION" />

    <!-- Hardware features -->
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.usb.host" android:required="false" />

    <application ...>
        ...
    </application>
</manifest>
```

### 2.3 Create USB Device Filter

Create `app/src/main/res/xml/device_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<usb>
    <!-- All UVC devices -->
    <usb-device class="239" subclass="2" />

    <!-- Specific devices (add your device vendor/product IDs) -->
    <usb-device vendor-id="1234" product-id="5678" />

    <!-- Android 9+ compatible filters -->
    <usb-device class="14" subclass="9" />
    <usb-device class="2" subclass="0" />
    <usb-device class="6" subclass="-1" />
</usb>
```

### 2.4 Update AndroidManifest for USB

In `AndroidManifest.xml`, add the USB device filter to your activity:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- USB device attach intent -->
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>

    <!-- USB device filter -->
    <meta-data
        android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
        android:resource="@xml/device_filter" />
</activity>
```

---

## 3. Basic Usage

### 3.1 Create Layout

Create your activity/fragment layout with a camera view:

```xml
<!-- activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Camera View Container -->
    <FrameLayout
        android:id="@+id/cameraContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/controlPanel"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Control Panel -->
    <LinearLayout
        android:id="@+id/controlPanel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <Button
            android:id="@+id/btnCapture"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Capture" />

        <Button
            android:id="@+id/btnRecord"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Record" />

    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 3.2 Create CameraFragment

```kotlin
package com.example.myapp

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.commit
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.render.env.RotateType

class MyCameraFragment : CameraFragment() {

    private var cameraView: AspectRatioTextureView? = null
    private var cameraContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle arguments here
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_camera, container, false)
        cameraContainer = root.findViewById(R.id.cameraContainer)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCameraView()
    }

    private fun setupCameraView() {
        cameraView = AspectRatioTextureView(requireContext())
        cameraContainer?.addView(cameraView)
    }

    // Required: Return the camera view
    override fun getCameraView() = cameraView

    // Required: Return the camera view container
    override fun getCameraViewContainer() = cameraContainer

    // Required: Handle camera state changes
    override fun onCameraState(state: CameraStatus) {
        when (state) {
            CameraStatus.OPENED -> {
                // Camera opened successfully
                println("Camera opened")
            }
            CameraStatus.CLOSING -> {
                println("Camera closing...")
            }
            CameraStatus.CLOSED -> {
                println("Camera closed")
            }
            CameraStatus.ERROR -> {
                println("Camera error occurred")
            }
        }
    }

    // Optional: Configure camera settings
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

    // Optional: Auto-open specific USB device
    override fun getDefaultCamera(): UsbDevice? {
        // Return null to auto-detect cameras
        return null
    }

    // Capture photo
    fun capturePhoto(savePath: String) {
        captureImage(object : ICaptureCallBack {
            override fun onCaptureBegin() {
                println("Starting capture...")
            }

            override fun onCaptureComplete(path: String) {
                println("Photo saved to: $path")
            }

            override fun onCaptureError(error: String) {
                println("Capture error: $error")
            }
        }, savePath)
    }

    // Start video recording
    fun startRecording(savePath: String, durationSec: Int = 0) {
        captureVideoStart(object : ICaptureCallBack {
            override fun onCaptureBegin() {
                println("Recording started")
            }

            override fun onCaptureComplete(path: String) {
                println("Video saved to: $path")
            }

            override fun onCaptureError(error: String) {
                println("Recording error: $error")
            }
        }, savePath, durationSec)
    }

    // Stop video recording
    fun stopRecording() {
        captureVideoStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraView = null
        cameraContainer = null
    }
}
```

### 3.3 Create Fragment Layout

Create `fragment_camera.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/cameraContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 3.4 Use in Activity

```kotlin
package com.example.myapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jiangdg.ausbc.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private var cameraFragment: MyCameraFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request permissions
        requestPermissions()
    }

    private fun requestPermissions() {
        PermissionUtils.requestPermissions(
            this,
            onGranted = {
                // Permissions granted, add camera fragment
                addCameraFragment()
            },
            onDenied = {
                // Permissions denied
                finish()
            }
        )
    }

    private fun addCameraFragment() {
        cameraFragment = MyCameraFragment()

        supportFragmentManager.commit {
            replace(R.id.cameraContainer, cameraFragment!!)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.handlePermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                addCameraFragment()
            },
            onDenied = {
                finish()
            }
        )
    }
}
```

---

## 4. Modern Architecture Usage

### 4.1 Add New Architecture Dependencies

```kotlin
dependencies {
    // Core module
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc-core:3.3.0")

    // Camera module
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc-camera:3.3.0")

    // Render module (optional)
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc-render:3.3.0")

    // Encode module (optional)
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc-encode:3.3.0")

    // Utils module
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc-utils:3.3.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
}
```

### 4.2 Create ViewModel

```kotlin
package com.example.myapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import com.jiangdg.ausbc.core.domain.model.CameraState
import com.jiangdg.ausbc.core.domain.repository.ICameraRepository
import com.jiangdg.ausbc.core.common.result.CameraResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraRepository: ICameraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Idle)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    init {
        observeCameraState()
    }

    private fun observeCameraState() {
        viewModelScope.launch {
            cameraRepository.cameraState.collect { state ->
                _cameraState.value = state
                when (state) {
                    is CameraState.Opened -> {
                        _uiState.value = CameraUiState.Ready
                    }
                    is CameraState.Error -> {
                        _uiState.value = CameraUiState.Error(state.error.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun openCamera(request: CameraRequest) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Loading
            when (val result = cameraRepository.openCamera(request)) {
                is CameraResult.Success -> {
                    _uiState.value = CameraUiState.Ready
                }
                is CameraResult.Error -> {
                    _uiState.value = CameraUiState.Error(result.error.message)
                }
            }
        }
    }

    fun closeCamera() {
        viewModelScope.launch {
            cameraRepository.closeCamera()
        }
    }

    fun startPreview() {
        viewModelScope.launch {
            // Get surface from view and start preview
            // cameraRepository.startPreview(surface)
        }
    }

    fun getPreviewFrameFlow() = cameraRepository.getPreviewFrameFlow()
}

sealed class CameraUiState {
    object Idle : CameraUiState()
    object Loading : CameraUiState()
    object Ready : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}
```

### 4.3 Create Hilt Module

```kotlin
package com.example.myapp.di

import com.jiangdg.ausbc.core.domain.repository.ICameraRepository
import com.jiangdg.ausbc.camera.data.repository.CameraRepository
import com.jiangdg.ausbc.camera.uvc.UvcCameraFactory
import com.jiangdg.ausbc.camera.datasource.IUvcCameraDataSource
import com.jiangdg.ausbc.camera.datasource.UvcCameraDataSource
import com.jiangdg.ausbc.camera.datasource.IUsbDeviceDataSource
import com.jiangdg.ausbc.camera.datasource.UsbDeviceDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideUvcCameraDataSource(): IUvcCameraDataSource {
        return UvcCameraDataSource()
    }

    @Provides
    @Singleton
    fun provideUsbDeviceDataSource(): IUsbDeviceDataSource {
        return UsbDeviceDataSource()
    }

    @Provides
    @Singleton
    fun provideCameraRepository(
        uvcDataSource: IUvcCameraDataSource,
        usbDeviceDataSource: IUsbDeviceDataSource
    ): ICameraRepository {
        return CameraRepository(uvcDataSource, usbDeviceDataSource)
    }

    @Provides
    fun provideUvcCameraFactory(): UvcCameraFactory {
        return UvcCameraFactory()
    }
}
```

### 4.4 Create Modern Fragment

```kotlin
package com.example.myapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.core.domain.model.CameraRequest
import com.jiangdg.ausbc.core.domain.model.PreviewSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModernCameraFragment : Fragment() {

    private val viewModel: CameraViewModel by viewModels()
    private var cameraView: AspectRatioTextureView? = null
    private var cameraContainer: FrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_camera, container, false)
        cameraContainer = root.findViewById(R.id.cameraContainer)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCameraView()
        observeUiState()
    }

    private fun setupCameraView() {
        cameraView = AspectRatioTextureView(requireContext())
        cameraContainer?.addView(cameraView)

        cameraView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                val surface = Surface(surface)
                lifecycleScope.launch {
                    // Start preview with surface
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is CameraUiState.Loading -> {
                        // Show loading
                    }
                    is CameraUiState.Ready -> {
                        // Camera ready
                    }
                    is CameraUiState.Error -> {
                        // Show error
                    }
                    else -> Unit
                }
            }
        }
    }

    fun openCamera() {
        val request = CameraRequest(
            width = 640,
            height = 480,
            format = PreviewSize.Format.MJPEG
        )
        viewModel.openCamera(request)
    }

    fun closeCamera() {
        viewModel.closeCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraView = null
        cameraContainer = null
    }
}
```

---

## 5. Advanced Features

### 5.1 Camera Parameter Controls

Access and control camera parameters:

```kotlin
class MyCameraFragment : CameraFragment() {

    private fun adjustCameraParameters() {
        val camera = getCurrentCamera() as? CameraUVC ?: return

        // Brightness (0-255)
        camera.setBrightness(128)
        val brightness = camera.getBrightness()

        // Contrast (0-255)
        camera.setContrast(128)

        // Saturation (0-255)
        camera.setSaturation(128)

        // Sharpness (0-255)
        camera.setSharpness(128)

        // White balance temperature
        camera.setWhiteBalance(4000)

        // Zoom (depends on camera support)
        camera.setZoom(100)

        // Focus
        camera.setFocus(0)

        // Auto focus
        camera.setAutoFocus(true)

        // Auto white balance
        camera.setAutoWhiteBalance(true)

        // Reset to default
        camera.resetBrightness()
        camera.resetContrast()
    }
}
```

### 5.2 Preview Data Callback

Get raw preview frame data:

```kotlin
class MyCameraFragment : CameraFragment() {

    private val previewDataCallback = object : IPreviewDataCallBack {
        override fun onPreviewData(data: ByteArray, width: Int, height: Int) {
            // Process raw preview data (NV21 or RGBA)
            processData(data, width, height)
        }
    }

    // Enable in CameraRequest
    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setRawPreviewData(true)
            .create()
    }

    private fun processData(data: ByteArray, width: Int, height: Int) {
        // Process the preview frame
    }
}
```

### 5.3 Encode Data Callback

Get encoded H.264/AAC data:

```kotlin
class MyCameraFragment : CameraFragment() {

    private val encodeDataCallback = object : IEncodeDataCallBack {
        override fun onEncodeData(data: ByteArray, type: Int) {
            when (type) {
                IEncodeDataCallBack.TYPE_VIDEO -> {
                    // H.264 encoded data
                }
                IEncodeDataCallBack.TYPE_AUDIO -> {
                    // AAC encoded data
                }
            }
        }
    }

    fun startStreamCapture() {
        captureStreamStart()
    }

    fun stopStreamCapture() {
        captureStreamStop()
    }
}
```

### 5.4 Camera Resolution Switching

```kotlin
class MyCameraFragment : CameraFragment() {

    fun switchResolution(width: Int, height: Int) {
        updateResolution(width, height)
    }

    fun getSupportedSizes() {
        val camera = getCurrentCamera() as? CameraUVC ?: return
        val sizes = camera.supportedPreviewSizes
        // Display or filter sizes
    }

    fun getSizesByAspectRatio(ratio: Float) {
        val sizes = getAllPreviewSizes(ratio)
        // Filter by aspect ratio (e.g., 16:9 = 1.777f)
    }
}
```

### 5.5 Camera Rotation

```kotlin
class MyCameraFragment : CameraFragment() {

    fun setRotation(angle: RotateType) {
        val camera = getCurrentCamera() as? CameraUVC ?: return
        camera.setRotateType(angle)
    }

    // Available rotations
    // RotateType.ANGLE_0
    // RotateType.ANGLE_90
    // RotateType.ANGLE_180
    // RotateType.ANGLE_270
}
```

---

## 6. Multi-Camera Support

### 6.1 Create Multi-Camera Fragment

```kotlin
package com.example.myapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.jiangdg.ausbc.base.MultiCameraFragment
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.camera.bean.CameraStatus
import com.jiangdg.ausbc.camera.bean.CameraUvcInfo
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import android.hardware.usb.UsbDevice

class MultiCameraFragment : MultiCameraFragment() {

    private var cameraContainer: FrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_multi_camera, container, false)
        cameraContainer = root.findViewById(R.id.cameraContainer)
        return root
    }

    override fun getCameraViewContainer() = cameraContainer

    override fun getCameraView(): AspectRatioSurfaceView? {
        // Return null to let the fragment create multiple views automatically
        return null
    }

    override fun onCameraState(state: CameraStatus, camera: ICamera) {
        when (state) {
            CameraStatus.OPENED -> {
                println("Camera opened: ${camera.cameraInfo?.deviceName}")
            }
            CameraStatus.CLOSED -> {
                println("Camera closed")
            }
            CameraStatus.ERROR -> {
                println("Camera error")
            }
        }
    }

    // Device connect callback
    private val deviceConnectCallback = object : IDeviceConnectCallBack {
        override fun onAttach(device: UsbDevice) {
            println("Device attached: ${device.deviceName}")
            showCameraSelectionDialog()
        }

        override fun onDetach(device: UsbDevice) {
            println("Device detached: ${device.deviceName}")
        }
    }

    fun getConnectedCameras(): List<CameraUvcInfo> {
        return getDeviceList()
    }

    fun switchToCamera(device: UsbDevice) {
        switchCamera(device)
    }

    private fun showCameraSelectionDialog() {
        val cameras = getDeviceList()
        // Show dialog to select camera
    }
}
```

### 6.2 Multi-Camera Layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/cameraContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

---

## 7. Audio Recording

### 7.1 Record Audio Only

```kotlin
class MyCameraFragment : CameraFragment() {

    fun startAudioRecording(savePath: String) {
        captureAudioStart(object : ICaptureCallBack {
            override fun onCaptureBegin() {
                println("Audio recording started")
            }

            override fun onCaptureComplete(path: String) {
                println("Audio saved to: $path")
            }

            override fun onCaptureError(error: String) {
                println("Audio recording error: $error")
            }
        }, savePath)
    }

    fun stopAudioRecording() {
        captureAudioStop()
    }
}
```

### 7.2 Audio Source Selection

Configure audio source in CameraRequest:

```kotlin
override fun getCameraRequest(): CameraRequest {
    return CameraRequest.Builder()
        // System microphone
        .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)

        // Auto-detect (prefers UAC if available)
        // .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)

        // USB Audio Class only
        // .setAudioSource(CameraRequest.AudioSource.SOURCE_UAC)

        .create()
}
```

### 7.3 Audio Playback

```kotlin
class MyCameraFragment : CameraFragment() {

    private val playCallback = object : IPlayCallBack {
        override fun onPlayStart() {
            println("Audio playback started")
        }

        override fun onPlayStop() {
            println("Audio playback stopped")
        }

        override fun onPlayError(error: String) {
            println("Audio playback error: $error")
        }
    }

    fun startAudioPlayback() {
        val camera = getCurrentCamera() as? CameraUVC ?: return
        camera.startAudioMonitor(playCallback)
    }

    fun stopAudioPlayback() {
        val camera = getCurrentCamera() as? CameraUVC ?: return
        camera.stopAudioMonitor()
    }
}
```

---

## 8. Custom Effects

### 8.1 Add Built-in Effects

```kotlin
class MyCameraFragment : CameraFragment() {

    fun addBuiltInEffects() {
        // Black and white effect
        val bwEffect = EffectBlackWhite()
        addRenderEffect(bwEffect)

        // Soul effect
        val soulEffect = EffectSoul()
        addRenderEffect(soulEffect)

        // Zoom effect
        val zoomEffect = EffectZoom()
        addRenderEffect(zoomEffect)
    }

    fun removeEffects() {
        val effects = listOf(
            EffectBlackWhite(),
            EffectSoul(),
            EffectZoom()
        )
        effects.forEach { removeRenderEffect(it) }
    }
}
```

### 8.2 Create Custom Effect

```kotlin
package com.example.myapp.effect

import com.jiangdg.ausbc.render.effect.AbstractEffect
import android.opengl.GLES20

class MyCustomEffect : AbstractEffect() {

    override fun getId(): Int {
        return EFFECT_ID_MY_CUSTOM
    }

    override fun getName(): String {
        return "My Custom Effect"
    }

    override fun getShader(): String {
        // Vertex shader
        val vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        // Fragment shader (custom effect)
        val fragmentShader = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                // Apply your custom effect here
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                gl_FragColor = vec4(vec3(gray), color.a);
            }
        """.trimIndent()

        return "$vertexShader|$fragmentShader"
    }

    override fun draw(textureId: Int, texMatrix: FloatArray?) {
        // Custom draw logic
    }

    companion object {
        const val EFFECT_ID_MY_CUSTOM = 1001
    }
}
```

### 8.3 Use Custom Effect

```kotlin
class MyCameraFragment : CameraFragment() {

    fun addMyCustomEffect() {
        val myEffect = MyCustomEffect()
        addRenderEffect(myEffect)
    }

    fun updateMyCustomEffect() {
        val newEffect = MyCustomEffect()
        updateRenderEffect(MyCustomEffect.EFFECT_ID_MY_CUSTOM, newEffect)
    }
}
```

---

## 9. Common Patterns

### 9.1 Check If Camera Is Open

```kotlin
fun isCameraReady(): Boolean {
    return isCameraOpened() && getCurrentCamera() != null
}
```

### 9.2 Get Current Resolution

```kotlin
fun getCurrentResolution(): Pair<Int, Int>? {
    val camera = getCurrentCamera() as? CameraUVC ?: return null
    val size = camera.currentPreviewSize
    return Pair(size.width, size.height)
}
```

### 9.3 Save Files with Proper Naming

```kotlin
fun getOutputPath(type: String): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DCIM
    )
    return File(storageDir, "AUSBC_${type}_$timeStamp.mp4").absolutePath
}

// Usage
val imagePath = getOutputPath("IMG")
val videoPath = getOutputPath("VIDEO")
val audioPath = getOutputPath("AUDIO")
```

### 9.4 Handle USB Device Events

```kotlin
class MyApplication : Application(), UsbDeviceManager.UsbDeviceListener {

    override fun onCreate() {
        super.onCreate()
        UsbDeviceManager.registerListener(this)
    }

    override fun onDeviceAttached(device: UsbDevice) {
        // Handle device attached
        notifyCameraFragment(device)
    }

    override fun onDeviceDetached(device: UsbDevice) {
        // Handle device detached
    }

    private fun notifyCameraFragment(device: UsbDevice) {
        // Send broadcast or use LiveData to notify fragment
    }
}
```

### 9.5 Full Lifecycle Implementation

```kotlin
class MainActivity : AppCompatActivity() {

    private var cameraFragment: MyCameraFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupCamera()
    }

    override fun onPause() {
        super.onPause()
        // Close camera when activity is paused
        cameraFragment?.let {
            if (it.isCameraOpened()) {
                it.closeCamera()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-open camera when activity resumes
        cameraFragment?.openCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraFragment = null
    }

    private fun setupCamera() {
        // After permissions granted
        cameraFragment = MyCameraFragment()
        supportFragmentManager.commit {
            replace(R.id.cameraContainer, cameraFragment!!)
        }
    }
}
```

---

## 10. Troubleshooting

### 10.1 Camera Preview Black Screen

**Symptoms:** Camera opens but preview is black.

**Solutions:**
1. Check permissions are granted:
```kotlin
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    // Request permission
}
```

2. Verify USB device filter configuration in `device_filter.xml`

3. Try different preview format:
```kotlin
CameraRequest.Builder()
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV)
    .create()
```

4. Check if device is actually UVC compatible

### 10.2 Native Library Not Found

**Symptoms:** `java.lang.UnsatisfiedLinkError`

**Solutions:**
1. Check ABIs in build.gradle:
```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }
}
```

2. Ensure device architecture is supported

3. Clean and rebuild:
```bash
./gradlew clean
./gradlew build
```

### 10.3 Hot Plug ANR

**Symptoms:** ANR when connecting/disconnecting camera

**Solutions:**
1. Ensure you're using version 3.2.7 or later
2. Handle USB events in a background thread
3. Don't block the main thread during device operations

### 10.4 Recording Issues

**Symptoms:** Video recording fails or produces corrupt files

**Solutions:**
1. Check storage permissions
2. Ensure storage is available:
```kotlin
fun isStorageAvailable(): Boolean {
    val state = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == state
}
```

3. Use proper file path:
```kotlin
val path = getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath
val file = File(path, "video_${System.currentTimeMillis()}.mp4")
```

### 10.5 Get Device Information

For debugging, get device information:

```kotlin
fun printDeviceInfo() {
    val devices = getDeviceList()
    devices.forEach { camera ->
        println("Device: ${camera.deviceName}")
        println("Vendor ID: ${camera.vendorId}")
        println("Product ID: ${camera.productId}")
        println("Bus: ${camera.busNum}")
        println("Supported sizes: ${camera.supportedPreviewSizes.joinToString()}")
    }
}
```

### 10.6 Enable Logging

Enable detailed logging for debugging:

```kotlin
// In Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable debug logging
        Logger.setDebug(true)
    }
}
```

---

## Additional Resources

- [GitHub Repository](https://github.com/jiangdongguo/AndroidUSBCamera)
- [FAQ](../FAQ.md)
- [Version History](../VERSION.md)
- [Architecture Documentation](../CLAUDE.md)

---

## Support

For issues and questions:
1. Check the [FAQ](../FAQ.md)
2. Search existing [GitHub Issues](https://github.com/jiangdongguo/AndroidUSBCamera/issues)
3. Create a new issue with logs and device information

---

**Last Updated:** 2025-01-09
