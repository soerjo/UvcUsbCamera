# Keep all public API classes and members
-keep public class com.jiangdg.ausbc.render.** { *; }

# Keep OpenGL-related classes
-keep class android.opengl.** { *; }

# Keep effect classes
-keep class com.jiangdg.ausbc.render.effect.** { *; }
-keep class com.jiangdg.ausbc.render.internal.** { *; }

# Keep EGL-related classes
-keep class javax.microedition.khronos.egl.** { *; }
