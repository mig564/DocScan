# kotlinx.serialization: senza queste regole i @Serializable si rompono SOLO
# nelle build release con minify attivo, e il problema non si vede in debug.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class it.example.docscan.** {
    *** Companion;
}
-keepclasseswithmembers class it.example.docscan.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class it.example.docscan.**$$serializer { *; }

# ML Kit carica alcune classi per riflessione. Si tengono solo i punti di
# ingresso invece dell'intero package: una regola generica su com.google.mlkit.**
# blocca oltre cento classi e vanifica buona parte della riduzione.
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.vision.documentscanner.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
