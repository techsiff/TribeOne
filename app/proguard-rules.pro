############################################
# PROGUARD / R8 RULES
############################################

############################################
# COMMON ATTRIBUTES
############################################
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

############################################
# KOTLIN
############################################
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

############################################
# FIREBASE
############################################
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

############################################
# FIRESTORE MODELS
############################################
-keepclassmembers class com.siffmember.info.ui.model.** {
    public <init>();
    *;

}
-keepclassmembers class com.siffmember.info.utils.** {
    public <init>();
    *;
}
-keepclassmembers class com.siffmember.info.data.remote.model.** {
    public <init>();
    *;
}

############################################
# GOOGLE PLAY SERVICES
############################################
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

############################################
# RETROFIT
############################################
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

############################################
# OKHTTP
############################################
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

############################################
# GSON
############################################
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

############################################
# ROOM DATABASE
############################################
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

############################################
# SQLCIPHER
############################################
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

-keepclasseswithmembernames class * {
    native <methods>;
}

############################################
# APACHE POI
############################################
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

-keep class org.apache.poi.xssf.** { *; }
-dontwarn org.apache.poi.xssf.**

-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**

-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.schemas.**

############################################
# GLIDE
############################################
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

############################################
# COIL
############################################
-keep class coil.** { *; }
-dontwarn coil.**

############################################
# EASY PERMISSIONS
############################################
-keep class pub.devrel.easypermissions.** { *; }
-dontwarn pub.devrel.easypermissions.**

############################################
# MULTIDEX
############################################
-keep class androidx.multidex.** { *; }
-dontwarn androidx.multidex.**

############################################
# XML PULL
############################################
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**

-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }

############################################
# YOUTUBE PLAYER
############################################
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }
-dontwarn com.pierfrancescosoffritti.androidyoutubeplayer.**

############################################
# ZOOM SDK
############################################
-keep class us.zoom.** { *; }
-keep class us.zipow.** { *; }
-keep class com.zipow.** { *; }

-dontwarn us.zoom.**
-dontwarn us.zipow.**
-dontwarn com.zipow.**

############################################
# WEBRTC
############################################
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

############################################
# GOOGLE TINK
############################################
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

############################################
# ANDROIDX SECURITY
############################################
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

############################################
# BROADCAST RECEIVERS
############################################
-keep public class * extends android.content.BroadcastReceiver {
    *;
}

############################################
# JNI
############################################
-keepclasseswithmembernames class * {
    native <methods>;
}

############################################
# PREVENT CAST REWRITING
############################################
-keepclassmembers class * {
    *** value;
}

############################################
# ZXING
############################################
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

############################################
# JAVA AWT / SWING (NOT AVAILABLE ON ANDROID)
############################################
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.swing.**

############################################
# KOTLIN COROUTINES SWING
############################################
-dontwarn kotlinx.coroutines.swing.**