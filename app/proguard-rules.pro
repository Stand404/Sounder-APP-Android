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

# ===== 泛型与反射支持（防止 Class cannot be cast to ParameterizedType） =====
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations, AnnotationDefault
-keepattributes SourceFile,LineNumberTable

# ===== Retrofit API 接口 =====
# 禁止 obfuscation/shrinking — Retrofit 需要通过反射读取 suspend 函数的
# Continuation<? super T> 参数签名（ParameterizedType），allowshrinking 会导致
# R8 剥离 Signature 属性，引发 Class cannot be cast to ParameterizedType
-keep interface com.stand.sounder_app.data.api.** { *; }
-keep class com.stand.sounder_app.data.api.** { *; }

# ===== Retrofit 内部类（2.9.0 的 consumer rules 在 R8 full mode 下不完整） =====
-keep,allowobfuscation class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ===== 数据模型 =====
-keep class com.stand.sounder_app.data.model.** { *; }
-keep class com.stand.sounder_app.data.download.** { *; }

# ===== Gson =====
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ===== Kotlin Metadata =====
-dontnote kotlinx.serialization.AnnotationsKt
