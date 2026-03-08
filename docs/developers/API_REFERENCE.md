# API Reference

This document provides detailed API documentation with examples for the most commonly used AUSBC APIs.

## Table of Contents

- [CameraFragment API](#camerafragment-api)
- [CameraRequest Configuration](#camerarequest-configuration)
- [Capture API](#capture-api)
- [Camera Parameters](#camera-parameters)
- [Render Effects](#render-effects)
- [Callbacks](#callbacks)
- [Multi-Camera API](#multi-camera-api)
- [Modern ICamera Interface](#modern-icamera-interface)

## CameraFragment API

`CameraFragment` is the base class for single camera implementations. It handles USB device detection, permission requests, and camera lifecycle.

### Required Methods

#### getCameraView()

Returns the view for camera preview (TextureView or SurfaceView).

```kotlin
override fun getCameraView(): IAspectRatio {
    // Option 1: TextureView
    return AspectRatioTextureView(requireContext())

    // Option 2: SurfaceView
    // return AspectRatioSurfaceView(requireContext())

    // Option 3: Null for offscreen rendering
    // return null
}
```

#### getCameraViewContainer()

Returns the ViewGroup container for the camera view.

```kotlin
override fun getCameraViewContainer(): ViewGroup {
    return binding.cameraViewContainer
}
```

#### onCameraState()

Handles camera state changes.

```kotlin
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
```

### Optional Methods

#### getCameraRequest()

Override to provide custom camera configuration.

```kotlin
override fun getCameraRequest(): CameraRequest {
    return CameraRequest.Builder()
        .setPreviewWidth(1920)
        .setPreviewHeight(1080)
        .setRenderMode(CameraRequest.RenderMode.OPENGL)
        .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
        .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
        .create()
}
```

#### getDefaultCamera()

Override to auto-open a specific USB device.

```kotlin
override fun getDefaultCamera(): UsbDevice? {
    // Return null to open first available camera
    // Or return specific UsbDevice with matching vid/pid
    return null
}
```

#### getGravity()

Override to set camera view gravity within container.

```kotlin
override fun getGravity(): Int {
    return Gravity.CENTER
    // Or: Gravity.TOP, Gravity.BOTTOM, Gravity.CENTER_HORIZONTAL
}
```

#### generateCamera()

Override to create custom camera implementation.

```kotlin
override fun generateCamera(ctx: Context, device: UsbDevice): MultiCameraClient.ICamera {
    // Default: CameraUVC
    // You can return custom implementation
    return CameraUVC(ctx, device)
}
```

## CameraRequest Configuration

`CameraRequest` configures camera behavior using the Builder pattern.

### Builder Methods

#### setPreviewWidth() / setPreviewHeight()

Set camera preview resolution.

```kotlin
CameraRequest.Builder()
    .setPreviewWidth(640)
    .setPreviewHeight(480)
    .create()
```

**Common resolutions:**
- 640x480 (VGA)
- 1280x720 (HD)
- 1920x1080 (Full HD)
- 3840x2160 (4K)

#### setRenderMode()

Set rendering mode.

```kotlin
CameraRequest.Builder()
    .setRenderMode(CameraRequest.RenderMode.OPENGL)  // GPU rendering with effects
    .setRenderMode(CameraRequest.RenderMode.NORMAL)  // Direct surface rendering
    .create()
```

#### setPreviewFormat()

Set preview format.

```kotlin
CameraRequest.Builder()
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)  // High FPS
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV)   // Lower FPS, raw data
    .create()
```

#### setAudioSource()

Set audio recording source.

```kotlin
CameraRequest.Builder()
    .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)       // Auto-detect
    .setAudioSource(CameraRequest.AudioSource.SOURCE_SYS_MIC)    // System mic
    .setAudioSource(CameraRequest.AudioSource.SOURCE_DEV_MIC)    // UAC device mic
    .setAudioSource(CameraRequest.AudioSource.NONE)              // No audio
    .create()
```

#### setAspectRatioShow()

Enable/disable aspect ratio preservation.

```kotlin
CameraRequest.Builder()
    .setAspectRatioShow(true)   // Preserve aspect ratio
    .setAspectRatioShow(false)  // Stretch to fill
    .create()
```

#### setRawPreviewData()

Enable raw preview data callbacks.

```kotlin
CameraRequest.Builder()
    .setRawPreviewData(true)    // Enable preview data callbacks
    .create()
```

#### setCaptureRawImage()

Capture raw JPEG image when using OpenGL rendering.

```kotlin
CameraRequest.Builder()
    .setCaptureRawImage(true)
    .setRawPreviewData(true)    // Must also be true
    .create()
```

#### setDefaultEffect()

Set default render effect.

```kotlin
val effect = EffectBlackWhite()

CameraRequest.Builder()
    .setDefaultEffect(effect)
    .create()
```

#### setDefaultRotateType()

Set default rotation.

```kotlin
CameraRequest.Builder()
    .setDefaultRotateType(RotateType.ANGLE_0)    // No rotation
    .setDefaultRotateType(RotateType.ANGLE_90)   // 90 degrees
    .setDefaultRotateType(RotateType.ANGLE_180)  // 180 degrees
    .setDefaultRotateType(RotateType.ANGLE_270)  // 270 degrees
    .create()
```

### Complete Example

```kotlin
val request = CameraRequest.Builder()
    .setPreviewWidth(1920)
    .setPreviewHeight(1080)
    .setRenderMode(CameraRequest.RenderMode.OPENGL)
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
    .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
    .setDefaultRotateType(RotateType.ANGLE_0)
    .setAspectRatioShow(true)
    .setCaptureRawImage(false)
    .setRawPreviewData(false)
    .create()
```

## Capture API

### Image Capture

#### captureImage()

Capture a still image.

```kotlin
captureImage(
    object : ICaptureCallBack {
        override fun onCaptureBegin() {
            // Show loading indicator
            showLoading("Capturing...")
        }

        override fun onCaptureComplete(path: String?, success: Boolean) {
            hideLoading()
            if (success) {
                // Image saved successfully
                showToast("Image saved: $path")
            } else {
                // Capture failed
                showToast("Capture failed")
            }
        }
    },
    "/custom/path/image.jpg"  // Optional custom path
)
```

### Video Recording

#### captureVideoStart()

Start video recording.

```kotlin
val savePath = getOutputDirectory().absolutePath + "/video_${System.currentTimeMillis()}.mp4"

captureVideoStart(
    object : ICaptureCallBack {
        override fun onCaptureBegin() {
            // Recording started
            binding.recordButton.text = "Stop"
            binding.recordButton.setBackgroundColor(Color.RED)
        }

        override fun onCaptureComplete(path: String?, success: Boolean) {
            // Recording stopped
            binding.recordButton.text = "Record"
            binding.recordButton.setBackgroundColor(Color.GRAY)
            if (success) {
                showToast("Video saved: $path")
            }
        }
    },
    savePath,
    60000L  // Optional: max duration in milliseconds (0 = no limit)
)
```

#### captureVideoStop()

Stop video recording.

```kotlin
captureVideoStop()
```

### Audio Recording

#### captureAudioStart()

Start audio recording.

```kotlin
val savePath = getOutputDirectory().absolutePath + "/audio_${System.currentTimeMillis()}.m4a"

captureAudioStart(
    object : ICaptureCallBack {
        override fun onCaptureBegin() {
            showToast("Audio recording started")
        }

        override fun onCaptureComplete(path: String?, success: Boolean) {
            if (success) {
                showToast("Audio saved: $path")
            }
        }
    },
    savePath
)
```

#### captureAudioStop()

Stop audio recording.

```kotlin
captureAudioStop()
```

### Stream Capture (H.264 & AAC)

#### captureStreamStart()

Start H.264 and AAC stream capture.

```kotlin
captureStreamStart()
setEncodeDataCallBack(object : IEncodeDataCallBack {
    override fun onEncodeData(data: ByteArray?, type: Int) {
        when (type) {
            IEncodeDataCallBack.TYPE_VIDEO -> {
                // H.264 video data
                handleVideoData(data)
            }
            IEncodeDataCallBack.TYPE_AUDIO -> {
                // AAC audio data
                handleAudioData(data)
            }
        }
    }
})
```

#### captureStreamStop()

Stop stream capture.

```kotlin
captureStreamStop()
```

## Camera Parameters

Camera parameters are available for `CameraUVC` type cameras only.

### Brightness

```kotlin
// Set brightness (typical range: 0-255)
setBrightness(128)

// Get current brightness
val brightness = getBrightness()

// Reset to default
resetBrightness()
```

### Contrast

```kotlin
// Set contrast
setContrast(128)

// Get current contrast
val contrast = getContrast()

// Reset to default
resetContrast()
```

### Saturation

```kotlin
// Set saturation
setSaturation(128)

// Get current saturation
val saturation = getSaturation()

// Reset to default
resetSaturation()
```

### Sharpness

```kotlin
// Set sharpness
setSharpness(128)

// Get current sharpness
val sharpness = getSharpness()

// Reset to default
resetSharpness()
```

### Gamma

```kotlin
// Set gamma
setGamma(128)

// Get current gamma
val gamma = getGamma()

// Reset to default
resetGamma()
```

### Hue

```kotlin
// Set hue
setHue(128)

// Get current hue
val hue = getHue()

// Reset to default
resetHue()
```

### Gain

```kotlin
// Set gain
setGain(128)

// Get current gain
val gain = getGain()

// Reset to default
resetGain()
```

### Zoom

```kotlin
// Set zoom
setZoom(100)

// Get current zoom
val zoom = getZoom()

// Reset to default
resetZoom()
```

### Auto Focus

```kotlin
// Enable/disable auto focus
setAutoFocus(true)
setAutoFocus(false)

// Get current auto focus state
val autoFocusEnabled = getAutoFocus()

// Reset to default
resetAutoFocus()
```

### Auto White Balance

```kotlin
// Enable/disable auto white balance
setAutoWhiteBalance(true)
setAutoWhiteBalance(false)

// Get current state
val awbEnabled = getAutoWhiteBalance()
```

### Custom Commands

```kotlin
// Send custom camera command (hex value)
sendCameraCommand(0x01)
```

## Render Effects

Effects are only available when using `RenderMode.OPENGL`.

### Built-in Effects

#### EffectBlackWhite

Black and white filter effect.

```kotlin
val effect = EffectBlackWhite()
addRenderEffect(effect)
```

#### EffectSoul

Soul/ghost effect.

```kotlin
val effect = EffectSoul()
addRenderEffect(effect)
```

#### EffectZoom

Zoom effect.

```kotlin
val effect = EffectZoom()
addRenderEffect(effect)
```

### Effect Management

#### addRenderEffect()

Add a render effect.

```kotlin
addRenderEffect(EffectBlackWhite())
```

#### removeRenderEffect()

Remove a render effect.

```kotlin
val effect = EffectBlackWhite()
addRenderEffect(effect)
// ... later
removeRenderEffect(effect)
```

#### updateRenderEffect()

Update effect by classification ID.

```kotlin
val newEffect = EffectBlackWhite()
updateRenderEffect(AbstractEffect.CLASSIFY_TYPE_FILTER, newEffect)
```

#### getDefaultEffect()

Get the default effect.

```kotlin
val defaultEffect = getDefaultEffect()
```

### Rotation

#### setRotateType()

Set preview rotation.

```kotlin
setRotateType(RotateType.ANGLE_0)
setRotateType(RotateType.ANGLE_90)
setRotateType(RotateType.ANGLE_180)
setRotateType(RotateType.ANGLE_270)
```

## Callbacks

### ICameraStateCallBack

Camera state changes.

```kotlin
interface ICameraStateCallBack {
    enum class State {
        OPENED,
        CLOSED,
        ERROR
    }

    fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: State,
        msg: String?
    )
}
```

### ICaptureCallBack

Capture status callbacks.

```kotlin
interface ICaptureCallBack {
    fun onCaptureBegin()
    fun onCaptureComplete(path: String?, success: Boolean)
}
```

### IPreviewDataCallBack

Raw preview data callbacks.

```kotlin
interface IPreviewDataCallBack {
    fun onPreviewData(data: ByteArray?, width: Int, height: Int, format: Int)
}

// Usage
addPreviewDataCallBack(object : IPreviewDataCallBack {
    override fun onPreviewData(data: ByteArray?, width: Int, height: Int, format: Int) {
        // Process raw preview data
        // format: IPreviewDataCallBack.FORMAT_NV21 or FORMAT_RGBA
    }
})
```

### IEncodeDataCallBack

Encoded data callbacks.

```kotlin
interface IEncodeDataCallBack {
    companion object {
        const val TYPE_VIDEO = 1
        const val TYPE_AUDIO = 2
    }

    fun onEncodeData(data: ByteArray?, type: Int)
}

// Usage
setEncodeDataCallBack(object : IEncodeDataCallBack {
    override fun onEncodeData(data: ByteArray?, type: Int) {
        when (type) {
            IEncodeDataCallBack.TYPE_VIDEO -> {
                // H.264 video data
            }
            IEncodeDataCallBack.TYPE_AUDIO -> {
                // AAC audio data
            }
        }
    }
})
```

### IPlayCallBack

Real-time microphone playback.

```kotlin
interface IPlayCallBack {
    fun onPlayBack(audioData: ByteArray?, length: Int)
}

// Usage
startPlayMic(object : IPlayCallBack {
    override fun onPlayBack(audioData: ByteArray?, length: Int) {
        // Process audio data in real-time
    }
})
```

### IDeviceConnectCallBack

USB device connection events.

```kotlin
interface IDeviceConnectCallBack {
    fun onAttachDev(device: UsbDevice?)
    fun onDetachDec(device: UsbDevice?)
    fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?)
    fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?)
    fun onCancelDev(device: UsbDevice?)
}
```

## Multi-Camera API

For multi-camera support, extend `MultiCameraFragment`.

### Required Methods

#### createCameraView()

Create a camera view for each device.

```kotlin
override fun createCameraView(deviceId: Int): IAspectRatio {
    return AspectRatioTextureView(requireContext())
}
```

#### getCameraViewContainer()

Return container for specific camera.

```kotlin
override fun getCameraViewContainer(deviceId: Int): ViewGroup? {
    return when (deviceId) {
        0 -> binding.camera1Container
        1 -> binding.camera2Container
        else -> null
    }
}
```

### Camera Management

#### getCameraList()

Get all connected USB cameras.

```kotlin
val cameraList = getCameraList()
```

#### switchCamera()

Switch to a different camera.

```kotlin
switchCamera(usbDevice)
```

## Modern ICamera Interface

The new architecture provides a modern coroutine-based API.

### State Management

```kotlin
// Observe camera state
lifecycleScope.launch {
    camera.cameraState.collect { state ->
        when (state) {
            is CameraState.Idle -> { /* Camera idle */ }
            is CameraState.Opening -> { /* Opening */ }
            is CameraState.Opened -> { /* Opened */ }
            is CameraState.Closed -> { /* Closed */ }
            is CameraState.Error -> { /* Error: state.error */ }
        }
    }
}
```

### Preview Frames

```kotlin
// Observe preview frames
lifecycleScope.launch {
    camera.previewFrames.collect { frame ->
        // Process frame: PreviewFrame(data, width, height, format, timestamp)
    }
}
```

### Camera Operations

```kotlin
// Open camera
val result = camera.open(request)
when (result) {
    is CameraResult.Success -> { /* Opened */ }
    is CameraResult.Error -> { /* Error: result.error */ }
}

// Close camera
camera.close()

// Start preview
camera.startPreview(surface)

// Capture image
val captureResult = camera.captureImage(CaptureRequest("/path/to/image.jpg"))
```

## Utility Methods

### Device Management

```kotlin
// Get device list
val devices = getDeviceList()

// Get current camera
val camera = getCurrentCamera()

// Check if camera is opened
val isOpened = isCameraOpened()
```

### Resolution Management

```kotlin
// Get current preview size
val size = getCurrentPreviewSize()

// Get all supported preview sizes
val sizes = getAllPreviewSizes()

// Get sizes for specific aspect ratio
val sizes16x9 = getAllPreviewSizes(16.0 / 9.0)

// Update resolution
updateResolution(1920, 1080)
```

### Audio Playback

```kotlin
// Start microphone playback
startPlayMic(object : IPlayCallBack {
    override fun onPlayBack(audioData: ByteArray?, length: Int) {
        // Process audio
    }
})

// Stop microphone playback
stopPlayMic()
```

## Complete Example

```kotlin
class MyCameraFragment : CameraFragment() {

    private lateinit var binding: FragmentMyCameraBinding
    private var isRecording = false

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = FragmentMyCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return binding.cameraViewContainer
    }

    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(1920)
            .setPreviewHeight(1080)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
            .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
            .create()
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                binding.statusText.text = "Camera opened"
                // Adjust brightness
                setBrightness(128)
            }
            ICameraStateCallBack.State.CLOSED -> {
                binding.statusText.text = "Camera closed"
            }
            ICameraStateCallBack.State.ERROR -> {
                binding.statusText.text = "Error: $msg"
            }
        }
    }

    // Capture button click
    fun onCaptureClick() {
        captureImage(object : ICaptureCallBack {
            override fun onCaptureBegin() {
                binding.statusText.text = "Capturing..."
            }

            override fun onCaptureComplete(path: String?, success: Boolean) {
                if (success) {
                    binding.statusText.text = "Saved: $path"
                } else {
                    binding.statusText.text = "Capture failed"
                }
            }
        })
    }

    // Record button click
    fun onRecordClick() {
        if (isRecording) {
            captureVideoStop()
            isRecording = false
            binding.recordButton.text = "Record"
        } else {
            val path = requireContext().getExternalFilesDir(null)?.absolutePath +
                       "/video_${System.currentTimeMillis()}.mp4"
            captureVideoStart(object : ICaptureCallBack {
                override fun onCaptureBegin() {
                    binding.statusText.text = "Recording..."
                }

                override fun onCaptureComplete(path: String?, success: Boolean) {
                    if (success) {
                        binding.statusText.text = "Saved: $path"
                    }
                }
            }, path)
            isRecording = true
            binding.recordButton.text = "Stop"
        }
    }
}
```

For more information, see:
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - Architecture details
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) - Integration examples
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues
