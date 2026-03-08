# Keep all public API classes and members
-keep public class com.jiangdg.ausbc.camera.** { *; }

# Keep native methods
-keep class com.jiangdg.uvc.** { *; }
-keep class com.jiangdg.usb.** { *; }

# Keep USB-related classes
-keep class android.hardware.usb.** { *; }
-keep class com.jiangdg.ausbc.camera.uvc.** { *; }
