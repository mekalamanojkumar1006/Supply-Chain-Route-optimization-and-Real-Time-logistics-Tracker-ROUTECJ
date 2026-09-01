# RouteCJ Driver App - ProGuard/R8 Rules

# Preserve Compose components
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Firebase & Firestore
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.routecj.driver.domain.model.** { *; }
-keep class com.routecj.driver.data.mapper.** { *; }

# OSMDroid (OpenStreetMap)
-keep class org.osmdroid.** { *; }
-keep interface org.osmdroid.** { *; }

# Kotlin Coroutines
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherLoader {
    <init>(...);
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherLoader

# Retrofit/OkHttp (if used in core for OSRM)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
