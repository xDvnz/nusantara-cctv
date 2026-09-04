# Media3, OkHttp, osmdroid: keep core entry points
-keep class androidx.media3.** { *; }
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class id.nusantara.cctv.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
