# QuizHelper ProGuard Rules

# Keep Room entities and DAOs
-keep class com.quizhelper.app.data.model.** { *; }
-keep class com.quizhelper.app.data.db.** { *; }

# Apache POI - keep all its classes
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class com.graphbuilder.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn com.microsoft.schemas.**

# XML Beans
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-dontwarn net.sf.saxon.**
-dontwarn aQute.bnd.**
-dontwarn com.graphbuilder.**

# Stax / XML
-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**
-dontwarn org.codehaus.stax2.**

# Log4j
-dontwarn org.apache.logging.log4j.**
-dontwarn org.osgi.framework.**
-dontwarn org.slf4j.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-dontwarn com.google.gson.**

# Kotlin serialization
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Generic: ignore all missing class warnings from libraries
-dontwarn java.awt.**
-dontwarn org.bouncycastle.**
-dontwarn com.ctc.wstx.**
-dontwarn org.w3c.dom.**

# Keep all public API classes
-keep class com.quizhelper.app.** { *; }
