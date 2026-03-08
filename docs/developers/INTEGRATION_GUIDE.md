# Integration Guide

This guide explains how to integrate AUSBC libraries into your own Android project.

## Integration Options

There are two main approaches to integrating AUSBC:

| Option | Description | Best For |
|--------|-------------|----------|
| **Option 1: Module Copy** | Copy source modules directly | Learning, customization |
| **Option 2: AAR Dependency** | Build and include AAR | Production apps |

## Option 1: Module Copy (Recommended for Learning)

This approach copies the library source code into your project, allowing you to explore and modify the code.

### Step 1: Copy Library Modules

Copy these directories to your project:

```
YourProject/
├── app/
├── libausbc/          # Copy from AUSBC
├── libuvc/            # Copy from AUSBC
├── libnative/         # Copy from AUSBC
└── (optional) libausbc-core/     # New architecture
    └── (optional) libausbc-camera/
```

### Step 2: Update settings.gradle.kts

Add the copied modules to your settings file:

```kotlin
// Your existing modules
include(":app")

// Add AUSBC modules
include(":libausbc")
include(":libuvc")
include(":libnative")

// Optional: New architecture modules
include(":libausbc-core")
include(":libausbc-camera")
include(":libausbc-render")
include(":libausbc-encode")
include(":libausbc-utils")
```

### Step 3: Add Dependencies

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    // AUSBC library
    implementation(project(":libausbc"))

    // Native libraries
    implementation(project(":libuvc"))
    implementation(project(":libnative"))

    // Optional: New architecture
    implementation(project(":libausbc-core"))
    implementation(project(":libausbc-camera"))

    // Required dependencies
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

### Step 4: Configure NDK

Create `local.properties` in your project root:

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

Or set the `NDK_HOME` environment variable.

### Step 5: Configure ProGuard (Release Builds)

In `proguard-rules.pro`:

```proguard
# AUSBC library
-keep class com.jiangdg.ausbc.** { *; }
-keep interface com.jiangdg.ausbc.** { *; }

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# USB camera
-keep class com.jiangdg.uvccamera.** { *; }

# Native library
-keep class com.jiangdg.natives.** { *; }
```

## Option 2: AAR Dependency

This approach builds the library as an AAR file and includes it as a dependency.

### Step 1: Build AAR

From the AUSBC project:

```bash
# Build release AAR
./gradlew :libausbc:assembleRelease

# Output location:
# libausbc/build/outputs/aar/libausbc-release.aar
```

### Step 2: Copy AAR to Your Project

```bash
# Create libs directory if not exists
mkdir YourProject/app/libs

# Copy AAR
cp libausbc/build/outputs/aar/libausbc-release.aar YourProject/app/libs/
```

**Important:** You also need to include the native libraries (`libuvc`, `libnative`) either as modules or pre-built `.so` files.

### Step 3: Add AAR Dependency

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
```

Or explicitly:

```kotlin
dependencies {
    implementation(files("libs/libausbc-release.aar"))
}
```

### Step 4: Handle Native Libraries

You have two options for native libraries:

**Option A: Include as modules**
- Copy `libuvc` and `libnative` modules to your project
- Follow Option 1 steps for native modules

**Option B: Use pre-built `.so` files**
- Copy `.so` files to `src/main/jniLibs/`
```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── libuvccamera.so
└── armeabi-v7a/
    └── libuvccamera.so
```

**Note:** Option A is recommended as it ensures native libraries are properly built.

## AndroidManifest.xml Configuration

### Permissions

Add required permissions to `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required permissions -->
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>

    <!-- For Android 12+ (API 31+) -->
    <uses-permission android:name="android.permission.USB_PERMISSION"/>

    <!-- Hardware features -->
    <uses-feature
        android:name="android.hardware.camera"
        android:required="false"/>
    <uses-feature
        android:name="android.hardware.camera.autofocus"
        android:required="false"/>

    <application ...>
        ...
    </application>
</manifest>
```

### USB Device Filter

Create `res/xml/device_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Generic UVC camera filter -->
    <usb-device vendor-id="0x0000" product-id="0x0000" class="0x0E" subclass="0x01" protocol="0x00"/>

    <!-- Or add specific cameras -->
    <!-- Example: Logitech C920 -->
    <!-- <usb-device vendor-id="0x046D" product-id="0x082D"/> -->
</resources>
```

**To find your camera's Vendor ID and Product ID:**

On Windows:
```powershell
# Open Device Manager, find your camera
# Properties → Details → Hardware Ids
# Format: USB\VID_xxxx&PID_xxxx
```

On macOS/Linux:
```bash
# List USB devices
system_profiler SPUSBDataType  # macOS
lsusb                          # Linux
```

## Application Class Setup

### With Hilt (Recommended)

Create an Application class:

```kotlin
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DemoApplication : Application()
```

Add to `AndroidManifest.xml`:
```xml
<application
    android:name=".DemoApplication"
    ...>
```

Add Hilt plugin to `build.gradle.kts`:
```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
}
```

### Without Hilt

```kotlin
import android.app.Application

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any components here
    }
}
```

## Activity/Fragment Setup

### Create Camera Fragment

Extend `CameraFragment`:

```kotlin
class MyCameraFragment : CameraFragment() {

    private lateinit var binding: FragmentMyCameraBinding

    override fun getRootView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        binding = FragmentMyCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    // REQUIRED: Return camera view (TextureView or SurfaceView)
    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    // REQUIRED: Return container for camera view
    override fun getCameraViewContainer(): ViewGroup {
        return binding.cameraViewContainer
    }

    // OPTIONAL: Override camera configuration
    override fun getCameraRequest(): CameraRequest {
        return CameraRequest.Builder()
            .setPreviewWidth(640)
            .setPreviewHeight(480)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
            .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO)
            .create()
    }

    // REQUIRED: Handle camera state changes
    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                // Camera opened successfully
                binding.statusText.text = "Camera opened"
            }
            ICameraStateCallBack.State.CLOSED -> {
                // Camera closed
                binding.statusText.text = "Camera closed"
            }
            ICameraStateCallBack.State.ERROR -> {
                // Camera error occurred
                binding.statusText.text = "Error: $msg"
            }
        }
    }
}
```

### Activity Integration

Option 1: Extend `CameraActivity`:

```kotlin
class MyCameraActivity : CameraActivity() {

    override fun createCameraFragment(): CameraFragment {
        return MyCameraFragment()
    }
}
```

Option 2: Use `CameraFragment` directly:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add camera fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MyCameraFragment())
            .commit()
    }
}
```

### Layout XML

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/cameraViewContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/statusText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Waiting for camera..." />

</FrameLayout>
```

## Permission Handling

For Android 6.0+ (API 23+), request runtime permissions:

```kotlin
class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Camera permission (required for API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(
                permissions.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // Permissions granted, navigate to camera
            navigateToCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                navigateToCamera()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToCamera() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MyCameraFragment())
            .commit()
    }
}
```

## Usage Examples

### Capture Image

```kotlin
class MyCameraFragment : CameraFragment() {

    private fun capturePhoto() {
        captureImage(object : ICaptureCallBack {
            override fun onCaptureBegin() {
                // Show loading indicator
            }

            override fun onCaptureComplete(path: String?, success: Boolean) {
                if (success) {
                    Toast.makeText(context, "Saved: $path", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
```

### Record Video

```kotlin
class MyCameraFragment : CameraFragment() {

    private fun startRecording() {
        val savePath = getOutputDirectory().absolutePath + "/video_${System.currentTimeMillis()}.mp4"

        captureVideoStart(
            object : ICaptureCallBack {
                override fun onCaptureBegin() {
                    binding.recordButton.text = "Stop"
                }

                override fun onCaptureComplete(path: String?, success: Boolean) {
                    binding.recordButton.text = "Record"
                    if (success) {
                        Toast.makeText(context, "Video saved: $path", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            savePath,
            0L // 0 = no time limit
        )
    }

    private fun stopRecording() {
        captureVideoStop()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = context?.externalCacheDir?.let {
            File(it, "videos").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context?.filesDir!!
    }
}
```

### Adjust Camera Parameters

```kotlin
class MyCameraFragment : CameraFragment() {

    private fun adjustBrightness() {
        // Get current brightness
        val currentBrightness = getBrightness()

        // Set new brightness (typical range: 0-255)
        setBrightness(128)
    }

    private fun enableAutoFocus() {
        setAutoFocus(true)
    }

    private fun zoomIn() {
        val currentZoom = getZoom() ?: 0
        setZoom(currentZoom + 10)
    }
}
```

### Add Render Effects

```kotlin
class MyCameraFragment : CameraFragment() {

    private fun addBlackAndWhiteEffect() {
        val effect = EffectBlackWhite()
        addRenderEffect(effect)
    }

    private fun removeEffect(effect: AbstractEffect) {
        removeRenderEffect(effect)
    }

    private fun rotatePreview() {
        setRotateType(RotateType.ANGLE_90)
    }
}
```

## Multi-Camera Support

For multi-camera scenarios, extend `MultiCameraFragment`:

```kotlin
class MultiCameraFragment : MultiCameraFragment() {

    private lateinit var binding: FragmentMultiCameraBinding
    private val cameraViews = mutableMapOf<Int, IAspectRatio>()

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = FragmentMultiCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun createCameraView(deviceId: Int): IAspectRatio {
        val textureView = AspectRatioTextureView(requireContext())
        cameraViews[deviceId] = textureView
        return textureView
    }

    override fun getCameraViewContainer(deviceId: Int): ViewGroup? {
        // Return appropriate container for each camera
        return when (deviceId) {
            0 -> binding.camera1Container
            1 -> binding.camera2Container
            else -> null
        }
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        // Handle camera state for each camera
    }
}
```

## Troubleshooting Integration

### Issue: Camera Not Opening

**Symptoms:** Permission dialog doesn't appear, camera doesn't open

**Solutions:**
1. Check `device_filter.xml` matches your camera
2. Verify USB device is connected before launching app
3. Check Logcat for USB permission errors

### Issue: Native Libraries Not Found

**Symptoms:** `java.lang.UnsatisfiedLinkError`

**Solutions:**
1. Ensure native modules are included in build
2. Check `ndk.dir` is correctly set
3. Verify ABIs match device architecture
4. Clean and rebuild project

### Issue: Black Screen

**Symptoms:** Camera opens but screen is black

**Solutions:**
1. Check render mode (try NORMAL if OPENGL fails)
2. Verify SurfaceView/TextureView is properly initialized
3. Check camera permissions
4. Try different preview format (MJPEG vs YUYV)

For more issues, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Next Steps

1. **Build your project**: Ensure all modules compile
2. **Connect a UVC camera**: Test with actual hardware
3. **Explore the demo**: Check `app/src/main/java/com/jiangdg/demo/` for examples
4. **Customize**: Modify camera settings for your needs
5. **Read API docs**: See [API_REFERENCE.md](API_REFERENCE.md) for detailed API documentation
