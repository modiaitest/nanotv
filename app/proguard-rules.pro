-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class app.nanotv.tv.data.remote.** { *; }
-keep class app.nanotv.tv.server.** { *; }
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
