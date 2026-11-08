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

# kotlinx.serialization: DTO와 Navigation NavKey(sealed 목적지)가 직렬화할 때 쓰는 생성 serializer를 유지한다.
# 라이브러리 consumer 규칙과 겹치지만, release 축소 후 역직렬화 실패를 막기 위해 프로젝트 범위를 명시한다.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepclassmembers class com.nativelap.heartguard.** {
    *** Companion;
}
-keepclasseswithmembers class com.nativelap.heartguard.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nativelap.heartguard.**$$serializer { *; }