# ProGuard rules for IPTV Pro Android

# Keep entry points
-keep public class com.iptvpro.tv.MainActivity {
    public void onCreate(android.os.Bundle);
    public boolean onKeyDown(int, android.view.KeyEvent);
}

-keep public class com.iptvpro.tv.SafeApplication {
    public void onCreate();
}

# Keep data models
-keep class com.iptvpro.tv.data.model.** { *; }
-keep class com.iptvpro.tv.data.Config { *; }
-keep class com.iptvpro.tv.data.cache.PlayListCache { *; }
-keep class com.iptvpro.tv.data.api.** { *; }
-keep class com.iptvpro.tv.player.** { *; }
-keep class com.iptvpro.tv.safety.** { *; }

# Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Android
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# No warnings
-dontwarn kotlin.**
-dontwarn org.jetbrains.**

# Optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
