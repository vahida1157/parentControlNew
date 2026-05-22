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

# Keep Retrofit interfaces and their generic signatures
-keep interface com.vahak.mehrban.data.remote.** { *; }
-keep interface com.vahak.mehrban.domain.repository.** { *; }
-keep interface com.vahak.mehrban.**Api { *; }
-keep interface com.vahak.mehrban.**Service { *; }


# Keep generic type info (needed for ParameterizedType)
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep Retrofit core classes
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Keep OkHttp/Okio if used
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Kotlin metadata for suspend functions
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
