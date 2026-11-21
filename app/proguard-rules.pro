# Keep Kotlinx Serialization models and generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bhora.app.domain.model.**$$serializer { *; }
-keepclassmembers class com.bhora.app.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.bhora.app.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bhora.app.data.cache.**$$serializer { *; }
-keepclassmembers class com.bhora.app.data.cache.** {
    *** Companion;
}
-keepclasseswithmembers class com.bhora.app.data.cache.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bhora.app.data.remote.**$$serializer { *; }
-keepclassmembers class com.bhora.app.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.bhora.app.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jsoup safe rules
-keep public class org.jsoup.** { public *; }
-dontwarn org.jsoup.nodes.EntitiesData

# OkHttp optional platform integrations
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
