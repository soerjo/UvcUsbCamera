# Keep all public API classes and members
-keep public class com.jiangdg.ausbc.utils.** { *; }

# Keep EventBus-related classes
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe *;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
