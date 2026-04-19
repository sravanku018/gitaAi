# Add project specific ProGuard rules here.

# Keep line numbers for crash analysis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Moshi (JSON parsing)
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.JsonSerializable
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room Database
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# DataStore
-keep class androidx.datastore.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# LiteRT / TensorFlow Lite
-keep class com.google.ai.edge.litertlm.** { *; }
-keep interface com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# App specific models and entities
-keep class com.aipoweredgita.app.data.** { *; }
-keep class com.aipoweredgita.app.database.** { *; }
-keep class com.aipoweredgita.app.ml.** { *; }
-keep class com.aipoweredgita.app.network.** { *; }
-keep class com.aipoweredgita.app.repository.** { *; }
-keep class com.aipoweredgita.app.util.TextUtils { *; }

# Keep ViewModels
-keep class com.aipoweredgita.app.viewmodel.** extends androidx.lifecycle.ViewModel { *; }

# Preserve exceptions during stack trace processing
-keepnames class * extends java.lang.Exception

# AndroidX
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# MediaPipe / AutoValue internals
-dontwarn com.google.auto.value.**