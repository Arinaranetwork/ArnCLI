# Add project specific ProGuard rules here.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-dontwarn androidx.camera.**

# Kotlinx Serialization
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class com.arinara.arntcli.core.** { *; }
-keep class com.arinara.arntcli.ui.model.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
