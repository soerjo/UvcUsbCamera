# Keep all public API classes and members
-keep public class com.jiangdg.ausbc.encode.** { *; }

# Keep media codec-related classes
-keep class android.media.MediaCodec { *; }
-keep class android.media.MediaFormat { *; }
-keep class android.media.MediaMuxer { *; }

# Keep audio-related classes
-keep class com.jiangdg.ausbc.encode.audio.** { *; }

# Keep native encoder methods
-keep class com.jiangdg.native.** { *; }
