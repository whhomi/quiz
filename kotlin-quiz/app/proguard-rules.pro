# QuizHelper ProGuard Rules

# Keep Room entities and DAOs
-keep class com.quizhelper.app.data.model.** { *; }
-keep class com.quizhelper.app.data.db.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-dontwarn com.google.gson.**

# Keep all public API classes
-keep class com.quizhelper.app.** { *; }
