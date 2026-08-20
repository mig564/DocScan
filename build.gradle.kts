// Build file di root: dichiara i plugin senza applicarli.
// Le tre versioni Kotlin DEVONO coincidere: il plugin Compose e quello di
// serialization fanno parte della toolchain Kotlin 2.x. Un disallineamento qui
// è la causa più frequente di sync fallito.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
}
