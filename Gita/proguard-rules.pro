# LiteRT-LM - keep all classes and members intact for JNI reflection
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
-keepnames class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# Keep data class getters specifically (Kotlin data classes)
-keepclassmembers class com.google.ai.edge.litertlm.SamplerConfig {
    public *;
}
-keepclassmembers class com.google.ai.edge.litertlm.ConversationConfig {
    public *;
}
-keepclassmembers class com.google.ai.edge.litertlm.EngineConfig {
    public *;
}
