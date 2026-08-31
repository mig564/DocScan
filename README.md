# DocScan

An Android document scanner. It scans documents and ID cards, pulls the data out
with on-device OCR, and keeps everything encrypted on the phone.

<p align="center">
  <img src="docs/photo_1.jpg" width="260" alt="Document library">
  <img src="docs/photo_2.jpg" width="260" alt="Scanning a card">
</p>

## What it does

- Multi-page document scanning to PDF.
- Front and back of an ID card, health card, driving licence or bank card on a
  single A4 sheet, printed at true 1:1 scale.
- On-device OCR that extracts Italian tax codes, MRZ lines, IBANs, VAT numbers,
  totals and dates.
- AES-256-GCM encrypted archive, organised into folders.
- Full-text search across scans.
- PDF export and sharing.
- Interface in Italian or English, or following the phone's language.
- Light, dark or system theme, with four accent colours: rust, blue, plum, green.
- Two card styles: rounded cards on a filled background, or underlined rows with
  no container.

## Requirements

- Android 7.0 (API 24) or later.
- Google Play Services: the capture module comes from there.

## Layout

```
DocScan/
├── README.md
├── docs/                               screenshots and icon drafts
├── build.gradle.kts                    plugin versions
├── settings.gradle.kts
├── gradle.properties
├── gradlew  gradlew.bat                Gradle wrapper
├── gradle/wrapper/
└── app/
    ├── build.gradle.kts                module dependencies
    ├── proguard-rules.pro              kotlinx.serialization rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml     no INTERNET permission, FileProvider
        │   ├── java/it/example/docscan/
        │   │   ├── MainActivity.kt         startup, navigation, system intents
        │   │   ├── data/
        │   │   │   ├── SecureStore.kt          AES-GCM on a Keystore key
        │   │   │   ├── DocumentRepository.kt   archive, folders, export
        │   │   │   ├── Model.kt                documents, folders, fields
        │   │   │   ├── ScanMode.kt             modes and ISO 7810 card sizes
        │   │   │   ├── A4Composer.kt           both sides on A4 at true scale
        │   │   │   ├── PdfBuilder.kt           multi-page PDF
        │   │   │   ├── AppSettings.kt          theme, accent, card style, language
        │   │   │   └── Images.kt               downsampled image decoding
        │   │   ├── ocr/
        │   │   │   ├── Ocr.kt                  ML Kit, on-device recognition
        │   │   │   ├── ItalianDocumentParser.kt tax code and MRZ
        │   │   │   └── FieldExtractor.kt       labelled fields, checks, PAN masking
        │   │   └── ui/
        │   │       ├── DocScanViewModel.kt     app state and operations
        │   │       ├── WithLanguage.kt         applies the chosen language
        │   │       ├── Labels.kt               field and folder key translation
        │   │       ├── BottomSheet.kt          shared bottom sheet
        │   │       ├── AnimatedDialog.kt       shared dialog
        │   │       ├── OverlayPhase.kt         enter and exit of overlays
        │   │       ├── Components.kt           card thumbnails, filter chips
        │   │       ├── DocumentActionsSheet.kt long-press menu, name dialog
        │   │       ├── FolderPickerSheet.kt    folder picker
        │   │       ├── library/LibraryScreen.kt   main screen
        │   │       ├── folder/FolderScreen.kt     folder contents
        │   │       ├── review/ReviewScreen.kt     post-scan review
        │   │       ├── review/A4Preview.kt        A4 sheet preview
        │   │       ├── detail/DetailScreen.kt     open document
        │   │       ├── scan/ScanModeSheet.kt      mode selection
        │   │       ├── settings/SettingsScreen.kt settings
        │   │       └── theme/
        │   │           ├── Color.kt               palettes and accent tones
        │   │           ├── Scale.kt               spacing, radii, type sizes
        │   │           └── Theme.kt               theme assembly
        │   └── res/
        │       ├── values/              strings.xml (it), colors.xml, themes.xml
        │       ├── values-en/           strings.xml (en)
        │       ├── values-night/        themes.xml
        │       ├── drawable/            launcher vector icons
        │       ├── mipmap-anydpi-v26/   adaptive icons, API 26+
        │       ├── mipmap-*dpi/         PNG icons for API 24-25
        │       └── xml/                 file_paths.xml, data_extraction_rules.xml
        └── test/java/it/example/docscan/
            ├── data/A4ComposerTest.kt      sheet geometry
            ├── data/FolderNameTest.kt      folder name uniqueness
            ├── data/FolderNameKeyTest.kt   migration of default folder names
            └── ocr/ParserTest.kt           tax code, MRZ, IBAN, PAN
```

## Build and run

Toolchain: JDK 17, Gradle 8.13 (via the wrapper), Android Gradle Plugin 8.13.2,
Kotlin 2.0.20, compileSdk 35.

**Android Studio** (Ladybug or later):

1. `File > Open` and pick the project folder, not a file inside it.
2. Wait for the Gradle sync. If it fails, `File > Sync Project with Gradle Files`
   after checking that the embedded JDK is 17 in
   `Settings > Build Tools > Gradle`.
3. Plug in a device with USB debugging on, select it in the toolbar and press
   Run. The scanner needs Play Store, so an emulator without Google APIs will
   install the app but fail at capture.
4. Unit tests: right-click `app/src/test` and choose `Run Tests`. No device
   needed, they run on the JVM.

**Command line:**

```
./gradlew assembleDebug     # debug APK in app/build/outputs/apk/debug/
./gradlew installDebug      # build and install on the connected device
./gradlew test              # unit tests
```

## Notes

The UI is built with Jetpack Compose, so the screens are Kotlin functions that
declare what to draw. There is not a single XML layout in the project.

The underlined card style drops the container on purpose, so the scanned page
stays the only solid shape on screen.

Source comments are in Italian.
