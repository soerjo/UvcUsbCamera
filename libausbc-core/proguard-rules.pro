# Keep all public API classes and members
-keep public class com.jiangdg.ausbc.core.** { *; }

# Keep sealed classes and their subclasses
-keep class com.jiangdg.ausbc.core.common.error.CameraError { *; }
-keep class com.jiangdg.ausbc.core.common.error.CameraError$* { *; }
-keep class com.jiangdg.ausbc.core.common.result.* { *; }
-keep class com.jiangdg.ausbc.core.domain.model.* { *; }

# Keep data classes
-keep @kotlinx.serialization.Serializable class * {*;}

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep coroutine-related classes
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Flow-related classes
-keep class kotlinx.coroutines.flow.* { *; }
