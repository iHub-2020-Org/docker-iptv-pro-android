# ─── IPTV Pro Android – ProGuard Rules ───────────────────────────────────

# Keep app entry points (AGP 8.x handles Activity/Application automatically,
# but explicit rules prevent stripping of specific methods called via reflection)
-keep public class com.iptvpro.tv.MainActivity {
    public void onCreate(android.os.Bundle);
    public boolean onKeyDown(int, android.view.KeyEvent);
}
-keep public class com.iptvpro.tv.SafeApplication {
    public void onCreate();
}

# Keep all data models (JSON serialization / deserialization via reflection)
-keep class com.iptvpro.tv.data.model.** { *; }

# Keep Config, cache, api, player, safety (accessed dynamically)
-keep class com.iptvpro.tv.data.Config { *; }
-keep class com.iptvpro.tv.data.cache.PlayListCache { *; }
-keep class com.iptvpro.tv.data.api.** { *; }
-keep class com.iptvpro.tv.player.** { *; }
-keep class com.iptvpro.tv.safety.** { *; }

# Kotlin metadata (needed for reflection and coroutines-compatible libs)
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod

# Suppress expected warnings from Kotlin stdlib internals
-dontwarn kotlin.**
-dontwarn org.jetbrains.**
