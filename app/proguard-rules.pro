# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Additional flags to pass to Proguard when processing a binary that uses
# MediaPipe.

# Keep public members of our public interfaces. This also prevents the
# obfuscation of the corresponding methods in classes implementing them,
# such as implementations of PacketCallback#process.

# Gson
-keepattributes Signature
-keepattributes *Annotation*

# Keep all Registra API models (data classes in registra package)
-keep class app.excoda.features.registra.*Response { <fields>; }
-keep class app.excoda.features.registra.*Result { <fields>; }
-keep class app.excoda.features.registra.*Info { <fields>; }

# Generic Gson rules
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Official MediaPipe rules
-keep public interface com.google.mediapipe.framework.* {
  public *;
}

-keep public class com.google.mediapipe.framework.Packet {
  public static *** create(***);
  public long getNativeHandle();
  public void release();
}

-keep public class com.google.mediapipe.framework.PacketCreator {
  *** releaseWithSyncToken(...);
}

-keep public class com.google.mediapipe.framework.MediaPipeException {
  <init>(int, byte[]);
}

-keep class com.google.mediapipe.framework.ProtoUtil$SerializedMessage { *; }

# Keep MediaPipe Graph class (uses Flogger stack inspection)
-keep class com.google.mediapipe.framework.Graph { *; }

# Flogger (logging library that inspects stack frames)
-keep class com.google.common.flogger.** { *; }
-keepattributes SourceFile,LineNumberTable

# Protobuf - keep all fields
-keep class com.google.protobuf.** { *; }
-keep class com.google.mediapipe.proto.** { *; }
-keep class com.google.mediapipe.tasks.core.proto.** { *; }

# Keep all private fields in protobuf classes
-keepclassmembers class * extends com.google.protobuf.** {
    <fields>;
}

# Suppress warnings
-dontwarn com.google.protobuf.**
-dontwarn com.google.mediapipe.**
-dontwarn com.google.common.flogger.**