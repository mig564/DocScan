# kotlinx.serialization: senza queste regole i @Serializable si rompono SOLO
# nelle build release con minify attivo, e il problema non si vede in debug.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Solo data/: è l'unico package con classi @Serializable. Una regola su tutto
# it.example.docscan.** trattiene oltre cento classi senza motivo.
-keepclassmembers class it.example.docscan.data.** {
    *** Companion;
}
-keepclasseswithmembers class it.example.docscan.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class it.example.docscan.data.**$$serializer { *; }

# ML Kit e Play services portano le proprie regole ProGuard dentro l'AAR
# (consumer-rules.pro), applicate in automatico. Ripeterle qui è ridondante e
# una regola generica su com.google.mlkit.** trattiene oltre cento classi.
# Se una build di release dovesse rompere l'OCR, il posto da guardare è questo.
