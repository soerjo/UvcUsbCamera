# Troubleshooting

This document provides solutions to common issues encountered when working with AUSBC.

## Table of Contents

- [Build Errors](#build-errors)
- [Runtime Errors](#runtime-errors)
- [Camera Issues](#camera-issues)
- [Native Code Issues](#native-code-issues)
- [Performance Issues](#performance-issues)
- [Device-Specific Issues](#device-specific-issues)

## Build Errors

### NDK Not Found

**Error:**
```
NDK not configured. Please set android.ndkVersion in build.gradle.kts.
```

**Solution:**
1. Install NDK 27.0.12077973 via SDK Manager
2. Set `ndk.dir` in `local.properties`:
```properties
ndk.dir=/path/to/your/Android/Sdk/ndk/27.0.12077973
```
3. Or set `NDK_HOME` environment variable
4. Sync project with Gradle files

### ndk-build Not Found

**Error:**
```
NDK build executable not found at: /path/to/ndk/ndk-build
```

**Solution:**
1. Verify NDK is installed at the specified path
2. On Windows, ensure `ndk-build.cmd` exists (not just `ndk-build`)
3. Check path separators in `local.properties`:
   - Windows: Use double backslashes `\\`
   - macOS/Linux: Use forward slashes `/`

### Native Build Failures

**Error:**
```
Execution failed for task ':libuvc:ndkBuild'.
> Build Command failed.
```

**Solutions:**
1. **Check NDK version matches exactly:**
```kotlin
ndkVersion = "27.0.12077973"
```

2. **Clean and rebuild:**
```bash
./gradlew clean
./gradlew :libuvc:ndkBuild --info
```

3. **Check for compiler errors in build output**

4. **Verify all source files exist:**
```bash
ls -la libuvc/src/main/jni/
```

### LAME MP3 Compiler Errors

**Error:**
```
error: implicit declaration of function 'xyz' is invalid in C99
```

**Solution:**
This is already handled in `CMakeLists.txt`. If you see this error:
1. Ensure you're using the correct `CMakeLists.txt`
2. Verify NDK version is 27.0.12077973
3. Clean rebuild:
```bash
./gradlew :libnative:clean
./gradlew :libnative:externalNativeBuildDebug
```

### ABI Mismatch

**Error:**
```
Failed to load native library: dlopen failed: library "libuvccamera.so" not found
```

**Solution:**
1. Check device architecture:
```bash
adb shell getprop ro.product.cpu.abi
```

2. Verify ABI filters in `build.gradle.kts`:
```kotlin
ndk {
    abiFilters += listOf("armeabi-v7a", "arm64-v8a")
}
```

3. Rebuild native libraries for correct ABI:
```bash
./gradlew :libuvc:ndkBuild
./gradlew :libnative:externalNativeBuildDebug
```

### Out of Memory During Build

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### Gradle Sync Failures

**Error:**
`` Gradle sync failed: Could not resolve ...
```

**Solutions:**
1. **Check internet connection**
2. **Clear Gradle cache:**
```bash
./gradlew cleanBuildCache
```

3. **Invalidate caches:** File → Invalidate Caches → Invalidate and Restart

4. **Check repository configuration in `settings.gradle.kts`**

## Runtime Errors

### Permission Denied

**Error:**
```
Permission Denial: opening provider requires android.permission.CAMERA
```

**Solution:**
1. Add permission to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA"/>
```

2. Request runtime permission for API 23+:
```kotlin
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        PERMISSION_REQUEST_CODE
    )
}
```

### USB Permission Not Granted

**Symptoms:** Permission dialog doesn't appear, camera doesn't open

**Solution:**
1. Check `device_filter.xml` in `res/xml/`:
```xml
<usb-device vendor-id="0x0000" product-id="0x0000" class="0x0E" subclass="0x01"/>
```

2. Verify device is connected before app launch

3. Check if your device requires explicit USB permission:
```kotlin
// In your Activity/Fragment
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Ensure USB device is connected
}
```

### Camera Not Opening

**Symptoms:** `onCameraState(State.ERROR, ...)` is called

**Solutions:**
1. **Check camera permissions**
2. **Verify USB device is supported:**
```bash
# List USB devices
adb shell ls -la /dev/bus/usb/
```

3. **Check device filter matches your camera:**
```kotlin
// Find your camera's VID/PID
// Then update device_filter.xml
```

4. **Check logcat for specific error:**
```bash
adb logcat | grep -E "UVC|Camera"
```

### Black Screen on Preview

**Symptoms:** Camera opens but screen is black

**Solutions:**
1. **Check if SurfaceView/TextureView is properly initialized:**
```kotlin
override fun getCameraView(): IAspectRatio {
    return AspectRatioTextureView(requireContext())
}
```

2. **Try different render mode:**
```kotlin
CameraRequest.Builder()
    .setRenderMode(CameraRequest.RenderMode.NORMAL)  // Try this first
    .create()
```

3. **Check camera state:**
```kotlin
override fun onCameraState(
    self: MultiCameraClient.ICamera,
    code: ICameraStateCallBack.State,
    msg: String?
) {
    Log.d(TAG, "Camera state: $code, msg: $msg")
}
```

4. **Verify preview format is supported:**
```kotlin
// Try MJPEG first
CameraRequest.Builder()
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
    .create()
```

5. **Check if SurfaceView/SurfaceTexture is ready:**
```kotlin
// For TextureView
override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    // Surface is ready, camera will start preview
}
```

### ANR on Hot Plug

**Symptoms:** App freezes when plugging/unplugging camera

**Solution:**
Update to version 3.2.7+ which includes hot plug fixes. Ensure you're not blocking main thread:

```kotlin
// Don't do this on main thread
Thread {
    // Heavy operations
}.start()
```

## Camera Issues

### Camera Disconnects Frequently

**Symptoms:** Camera closes randomly during use

**Solutions:**
1. **Check USB cable quality** - Use shielded cable
2. **Check USB power** - Some cameras need external power
3. **Avoid USB hub** - Connect directly to device
4. **Check device sleep settings:**
```kotlin
// Keep screen on
window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
```

### Camera Resolution Not Supported

**Error:**
```
Resolution 1920x1080 not supported
```

**Solution:**
1. **Get supported resolutions:**
```kotlin
val sizes = getAllPreviewSizes()
sizes?.forEach { size ->
    Log.d(TAG, "Supported: ${size.width}x${size.height}")
}
```

2. **Use a supported resolution:**
```kotlin
CameraRequest.Builder()
    .setPreviewWidth(640)
    .setPreviewHeight(480)
    .create()
```

### Camera Parameter Changes Not Working

**Symptoms:** `setBrightness()` etc. have no effect

**Solutions:**
1. **Check if camera is opened:**
```kotlin
if (isCameraOpened()) {
    setBrightness(128)
}
```

2. **Verify camera supports the parameter:**
```kotlin
// Only CameraUVC supports parameters
if (getCurrentCamera() is CameraUVC) {
    (getCurrentCamera() as CameraUVC).setBrightness(128)
}
```

3. **Check if value is in valid range:**
```kotlin
// Get min/max first
val current = getBrightness()
// Then set new value
setBrightness(128)
```

### Capture Fails

**Error:**
```
Capture failed: null path
```

**Solutions:**
1. **Check storage permissions:**
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

2. **Provide valid save path:**
```kotlin
val path = context.getExternalFilesDir(null)?.absolutePath + "/image.jpg"
captureImage(callback, path)
```

3. **Ensure directory exists:**
```kotlin
val dir = File(path).parentFile
if (!dir.exists()) {
    dir.mkdirs()
}
```

## Native Code Issues

### Native Library Not Loaded

**Error:**
```
java.lang.UnsatisfiedLinkError: dlopen failed: cannot locate symbol "xyz"
```

**Solutions:**
1. **Clean and rebuild native libraries:**
```bash
./gradlew :libuvc:ndkClean
./gradlew :libuvc:ndkBuild
```

2. **Check ABI compatibility:**
```bash
# Check device ABI
adb shell getprop ro.product.cpu.abi

# Check built ABIs
ls -la app/build/intermediates/stripped_native_libs/
```

3. **Rebuild all modules:**
```bash
./gradlew clean
./gradlew build
```

### JNI Method Not Found

**Error:**
```
java.lang.NoSuchMethodError: no static method with name='nativeMethod'
```

**Solutions:**
1. **Check JNI method signature matches Java method**
2. **Rebuild native library**
3. **Verify package name in JNI function:**
```cpp
// Must match: com.jiangdg.usb.UVCCamera
Java_com_jiangdg_usb_UVCCamera_connect(...)
```

### Native Crash

**Symptoms:** App crashes with native stack trace

**Solutions:**
1. **Get tombstone:**
```bash
adb shell ls -la /data/tombstones/
adb shell cat /data/tombstones/tombstone_XX
```

2. **Enable native debugging:**
```kotlin
externalNativeBuild {
    cmake {
        cppFlags += "-O0 -g"  // Debug symbols
    }
}
```

3. **Use Address Sanitizer:**
```kotlin
externalNativeBuild {
    cmake {
        arguments += "-fsanitize=address"
    }
}
```

## Performance Issues

### Low Frame Rate

**Symptoms:** Preview is choppy or laggy

**Solutions:**
1. **Use MJPEG format:**
```kotlin
CameraRequest.Builder()
    .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG)
    .create()
```

2. **Lower resolution:**
```kotlin
CameraRequest.Builder()
    .setPreviewWidth(640)
    .setPreviewHeight(480)
    .create()
```

3. **Disable raw preview data:**
```kotlin
CameraRequest.Builder()
    .setRawPreviewData(false)
    .create()
```

4. **Use NORMAL render mode instead of OpenGL:**
```kotlin
CameraRequest.Builder()
    .setRenderMode(CameraRequest.RenderMode.NORMAL)
    .create()
```

### High CPU Usage

**Symptoms:** Device gets warm, battery drains

**Solutions:**
1. **Use MJPEG instead of YUYV**
2. **Lower preview resolution**
3. **Reduce frame rate** (if camera supports it)
4. **Disable unnecessary callbacks:**
```kotlin
// Remove when not needed
removePreviewDataCallBack(callback)
```

### Memory Leaks

**Symptoms:** App gets slower over time, crashes

**Solutions:**
1. **Release camera in fragment lifecycle:**
```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    closeCamera()
}
```

2. **Remove callbacks when not needed:**
```kotlin
override fun onDestroy() {
    super.onDestroy()
    removePreviewDataCallBack(callback)
}
```

3. **Use memory profiler:** Android Studio → Profiler → Memory

## Device-Specific Issues

### Samsung Devices

**Issue:** Camera doesn't open on Samsung devices

**Solution:**
Some Samsung devices require explicit USB permission:
```xml
<intent-filter>
    <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"/>
</intent-filter>
<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
    android:resource="@xml/device_filter"/>
```

### Huawei Devices

**Issue:** Camera permission denied on Huawei

**Solution:**
Check device-specific permission settings:
- Settings → Apps → Your App → Permissions
- Enable camera and storage permissions

### Low-End Devices

**Issue:** Poor performance on older devices

**Solutions:**
1. Use lower resolution (640x480)
2. Use MJPEG format
3. Use NORMAL render mode
4. Disable raw preview data

### USB 3.0 Cameras

**Issue:** USB 3.0 cameras not working

**Solution:**
Some USB 3.0 cameras require USB 3.0 ports. If using USB 2.0:
- Lower resolution
- Lower frame rate
- Check camera specs

## Debugging Tips

### Enable Detailed Logging

```kotlin
// In your Application class
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Enable debug logging
            Log.d("AUSBC", "Debug mode enabled")
        }
    }
}
```

### Useful Logcat Commands

```bash
# Filter for AUSBC logs
adb logcat | grep -E "ausbc|UVC|Camera"

# Filter for native logs
adb logcat | grep -E "Native|libuvccamera|libnativelib"

# Filter for errors only
adb logcat *:E

# Save logs to file
adb logcat > debug_log.txt
```

### Check Camera Capabilities

```kotlin
// After camera opens
override fun onCameraState(
    self: MultiCameraClient.ICamera,
    code: ICameraStateCallBack.State,
    msg: String?
) {
    if (code == ICameraStateCallBack.State.OPENED) {
        // Get supported resolutions
        val sizes = getAllPreviewSizes()
        sizes?.forEach { size ->
            Log.d(TAG, "Supported: ${size.width}x${size.height}")
        }

        // Get current preview size
        val current = getCurrentPreviewSize()
        Log.d(TAG, "Current: ${current?.width}x${current?.height}")
    }
}
```

### Monitor Frame Rate

```kotlin
// Use EventBus provided by library
EventBus.with<Int>(BusKey.KEY_FRAME_RATE).observe(this) { fps ->
    Log.d(TAG, "Frame rate: $fps fps")
}
```

## Getting Help

If you can't resolve your issue:

1. **Search existing issues:** https://github.com/jiangdg/AUSBC/issues
2. **Check device compatibility:** Your device may not be supported
3. **Provide detailed information:**
   - Device model and Android version
   - Camera model and specifications
   - Full error message
   - Relevant logcat output
4. **Create minimal reproducible example**

## Additional Resources

- [BUILD_SETUP.md](BUILD_SETUP.md) - Build configuration issues
- [API_REFERENCE.md](API_REFERENCE.md) - API usage questions
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - Architecture understanding
- [GitHub Issues](https://github.com/jiangdg/AUSBC/issues) - Community support
