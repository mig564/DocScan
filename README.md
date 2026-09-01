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
        │   │   │   ├── ArchiveRules.kt          titles, search, folder rules
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
            ├── data/A4ComposerTest.kt        sheet geometry
            ├── data/TitleNumberingTest.kt    duplicate title numbering
            ├── data/SearchTest.kt            full-text search
            ├── data/FolderNameTest.kt        folder name uniqueness
            ├── data/FolderNameKeyTest.kt     migration of default folder names
            ├── data/DocumentRecordTest.kt    review badge, pages vs sheets
            ├── data/ImagesTest.kt            downsampling factor
            └── ocr/ParserTest.kt             tax code, MRZ, IBAN, PAN
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

**Command line:**

```
./gradlew assembleDebug     # debug APK in app/build/outputs/apk/debug/
./gradlew installDebug      # build and install on the connected device
```

Unit tests have their own section below.

## Tests

What goes in here: rules that are pure logic on values, where a mistake is
silent. A wrong checksum, a duplicate title, a search that quietly returns
nothing — none of these crash, so nobody notices until the archive is already
wrong. Those get a test.

What stays out: anything that needs a screen, a bitmap or a real file.
`A4Composer.build`, `PdfBuilder`, `SecureStore` and the Compose screens are all
covered by opening the app, and testing them would cost an emulator run for
each change. There is no `androidTest` folder on purpose.

The tests live in `app/src/test` and run on the JVM, so the whole suite finishes
in seconds without a device:

```
./gradlew test              # all of them
./gradlew test --tests '*ParserTest'
```

In Android Studio, right-click `app/src/test` and choose `Run Tests`, or use the
green arrow next to a class or a single method.

Each test follows the same three steps — build the input, call the one thing
under test, state what is expected — and the method name says the expected
behaviour in words, so a red test reads as a sentence about what broke.

One rule worth keeping: **a test calls the real function, it never re-implements
it.** Two of these tests used to hold their own copy of the repository logic,
and one copy had already fallen behind by two folders while still passing. That
is why the pure rules now live in `ArchiveRules.kt`, separate from the
repository that reads and writes files: the repository keeps the disk and the
resources, the rules can be called directly from a test.

What is covered today:

| Test | Guards against |
|---|---|
| `ParserTest` | tax code, MRZ and IBAN checksums; invented fields on plain prose; card numbers reaching storage |
| `A4ComposerTest` | cards printed off-sheet or off true scale |
| `TitleNumberingTest` | two identical rows in the same folder |
| `SearchTest` | documents that exist but cannot be found |
| `FolderNameTest` | two folders that look like the same folder |
| `FolderNameKeyTest` | default folders stuck in Italian after a language change |
| `DocumentRecordTest` | wrong review badge; a two-sided card counted as two pages |
| `ImagesTest` | out-of-memory on long documents; scans too blurry to read |

Still untested and worth doing next: the sort in `folderDocuments`, which decides
the order of every list the user scrolls. It needs the sorting to move out of the
ViewModel first, the same way the archive rules did.

## Notes

The UI is built with Jetpack Compose, so the screens are Kotlin functions that
declare what to draw. There is not a single XML layout in the project.

The underlined card style drops the container on purpose, so the scanned page
stays the only solid shape on screen.

Source comments are in Italian.
