# AUSBC Quick Start Guide

Get your UVC camera running in 5 minutes!

## Step 1: Add Dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.jiangdongguo:AndroidUSBCamera:3.3.0")
}
```

## Step 2: Add Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## Step 3: Create USB Device Filter

Create `app/src/main/res/xml/device_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<usb>
    <usb-device class="239" subclass="2" />
</usb>
```

## Step 4: Create Camera Fragment

```kotlin
class MyCameraFragment : CameraFragment() {

    private lateinit var cameraView: AspectRatioTextureView
    private lateinit var cameraContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        cameraContainer = view.findViewById(R.id.cameraContainer)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraView = AspectRatioTextureView(requireContext())
        cameraContainer.addView(cameraView)
    }

    override fun getCameraView() = cameraView
    override fun getCameraViewContainer() = cameraContainer

    override fun onCameraState(state: CameraStatus) {
        when (state) {
            CameraStatus.OPENED -> println("Camera opened!")
            CameraStatus.ERROR -> println("Camera error!")
            else -> {}
        }
    }
}
```

## Step 5: Create Layout

`fragment_camera.xml`:

```xml
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/cameraContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

## Step 6: Use in Activity

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Add camera fragment
            supportFragmentManager.commit {
                replace(R.id.cameraContainer, MyCameraFragment())
            }
        }
    }
}
```

That's it! Connect your UVC camera and you should see the preview.

---

## Need More?

- [Full Usage Guide](USAGE.md)
- [FAQ](../FAQ.md)
- [Examples](../app/src/main/java/com/jiangdg/demo/)
