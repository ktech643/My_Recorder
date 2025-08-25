# ========================================================================================
# CheckMate Android ProGuard Configuration
# Optimized for performance, security, and APK size reduction
# ========================================================================================

# Basic ProGuard settings
-verbose
-dontskipnonpubliclibraryclasses
-dontpreverify
-optimizationpasses 5
-allowaccessmodification

# Keep debugging information for crash reports
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exception,InnerClasses

# ========================================================================================
# Android Framework Compatibility
# ========================================================================================

# Keep Android core classes
-keep class androidx.core.app.CoreComponentFactory { *; }
-keep class android.support.** { *; }
-keep class androidx.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all enums properly
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================================================================
# Application Specific Rules
# ========================================================================================

# Keep all CheckMate classes and their members
-keep class com.checkmate.android.** { *; }

# Keep permission manager classes for reflection
-keep class com.checkmate.android.util.SynchronizedPermissionManager { *; }
-keep class com.checkmate.android.util.DialogManager { *; }
-keep class com.checkmate.android.util.PermissionTestHelper { *; }

# Keep accessibility service classes
-keep class * extends android.accessibilityservice.AccessibilityService
-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
}

# Keep third-party app classes (for external integrations)
-keep class com.whatsapp.** { *; }
-keep class com.snapchat.** { *; }

# ========================================================================================
# UI and Fragment Rules
# ========================================================================================

# Keep dialog fragments
-keep class com.serenegiant.dialog.MessageDialogFragment { *; }
-keep class com.serenegiant.dialog.MessageDialogFragment$* { *; }

# Keep all Activities, Services, and Receivers
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends androidx.fragment.app.Fragment

# ========================================================================================
# Networking and Serialization
# ========================================================================================

# Retrofit and OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ========================================================================================
# Kotlin and Coroutines
# ========================================================================================

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# ========================================================================================
# Third-party Libraries
# ========================================================================================

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Camera and USB libraries
-keep class com.serenegiant.** { *; }
-dontwarn com.serenegiant.**

# QR Code Scanner
-keep class com.blikoon.qrcodescanner.** { *; }

# Permissions Dispatcher
-keep class permissions.dispatcher.** { *; }

# ACRA (Crash Reporting)
-keep class org.acra.** { *; }
-keep class * implements org.acra.** { *; }

# ========================================================================================
# Optimization and Obfuscation
# ========================================================================================

# Remove logging calls in release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Remove debug code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ========================================================================================
# Warning Suppressions
# ========================================================================================

# Suppress warnings for known issues
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn kotlin.jvm.internal.**
-dontwarn kotlinx.coroutines.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn com.serenegiant.**

# ========================================================================================
# End of ProGuard Configuration
# ========================================================================================