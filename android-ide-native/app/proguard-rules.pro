-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NativeBridge
-keep class com.androidide.jni.NativeBridge { *; }

# Keep all Kotlin data classes
-keep class com.androidide.project.** { *; }
-keep class com.androidide.engine.** { *; }
-keep class com.androidide.toolchain.** { *; }

# Compose
-dontwarn androidx.compose.**
