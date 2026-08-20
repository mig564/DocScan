plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "it.example.docscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.example.docscan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Obbligatorio insieme a isMinifyEnabled: senza, R8 riduce il codice
            // ma lascia dentro tutte le risorse, comprese quelle irraggiungibili.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Serve per le icone fuori dal set base (Folder, ContentCopy, Download...).
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Acquisizione: UI propria, rilevamento bordi, deskew, output PDF.
    // Gira via Google Play services; il modulo si scarica al primo uso, ma le
    // immagini non lasciano il dispositivo.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // OCR con modello bundled nell'APK: nessuna rete.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}
