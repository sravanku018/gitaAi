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
<<<<<<< HEAD
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class **_Impl { *; }
-keep class **_DefaultImpl { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
=======
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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
<<<<<<< HEAD
-keep class org.tensorflow.** { *; }
-keepclassmembers class org.tensorflow.** { *; }
-keep class com.google.flatbuffers.** { *; }
-dontwarn org.tensorflow.**
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

# App specific models and entities
-keep class com.aipoweredgita.app.data.** { *; }
-keep class com.aipoweredgita.app.database.** { *; }
-keep class com.aipoweredgita.app.ml.** { *; }
-keep class com.aipoweredgita.app.network.** { *; }
-keep class com.aipoweredgita.app.repository.** { *; }
<<<<<<< HEAD
-keep class com.aipoweredgita.app.coin.** { *; }
-keep class com.aipoweredgita.app.prompt.** { *; }
-keep class com.aipoweredgita.app.notifications.** { *; }
-keep class com.aipoweredgita.app.services.** { *; }
-keep class com.aipoweredgita.app.navigation.** { *; }
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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