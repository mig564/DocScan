# DocScan

Scanner di documenti per Android. Acquisisce documenti e tessere, ne estrae i
dati con OCR e li conserva cifrati sul telefono. Non usa la rete.

<p align="center">
  <img src="docs/photo_1.jpg" width="260" alt="Libreria dei documenti">
  <img src="docs/photo_2.jpg" width="260" alt="Scansione di una tessera">
</p>

## Cosa fa

- Scansione di documenti multipagina in PDF.
- Fronte e retro di carta d'identità, tessera sanitaria, patente o bancomat su
  un unico foglio A4, in scala reale 1:1.
- OCR sul dispositivo, con estrazione di codice fiscale, MRZ, IBAN, partita IVA,
  totali e date.
- Archivio cifrato AES-256-GCM, organizzato in cartelle.
- Ricerca nel testo delle scansioni.
- Esportazione e condivisione in PDF.
- Tema chiaro, scuro o di sistema.

## Requisiti

- Android 7.0 (API 24) o superiore.
- Google Play Services: il modulo di acquisizione arriva da lì.
- Un dispositivo fisico. Lo scanner non funziona sugli emulatori senza Play Store.

## Struttura

```
DocScan/
├── README.md
├── docs/                               schermate usate qui sopra
├── build.gradle.kts                    versioni dei plugin
├── settings.gradle.kts
├── gradle.properties
├── gradlew  gradlew.bat                wrapper Gradle
├── gradle/wrapper/
└── app/
    ├── build.gradle.kts                dipendenze del modulo
    ├── proguard-rules.pro              regole per kotlinx.serialization e ML Kit
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml     nessun permesso INTERNET, FileProvider
        │   ├── java/it/example/docscan/
        │   │   ├── MainActivity.kt         avvio, navigazione, intent di sistema
        │   │   ├── data/
        │   │   │   ├── SecureStore.kt          cifratura AES-GCM su chiave Keystore
        │   │   │   ├── DocumentRepository.kt   archivio, cartelle, esportazione
        │   │   │   ├── Model.kt                documenti, cartelle, campi estratti
        │   │   │   ├── ScanMode.kt             modalità e formati carta ISO 7810
        │   │   │   ├── A4Composer.kt           fronte e retro su A4 in scala reale
        │   │   │   ├── PdfBuilder.kt           PDF multipagina
        │   │   │   ├── AppSettings.kt          tema e cartella predefinita
        │   │   │   └── Images.kt               decodifica ridotta delle immagini
        │   │   ├── ocr/
        │   │   │   ├── Ocr.kt                  ML Kit, riconoscimento sul dispositivo
        │   │   │   ├── ItalianDocumentParser.kt codice fiscale e MRZ
        │   │   │   └── FieldExtractor.kt       campi etichettati, verifiche, PAN mascherati
        │   │   └── ui/
        │   │       ├── DocScanViewModel.kt     stato dell'app e operazioni
        │   │       ├── BottomSheet.kt          pannello a scomparsa condiviso
        │   │       ├── Components.kt           anteprime carta, chip filtro
        │   │       ├── DocumentActionsSheet.kt menu a pressione lunga, dialogo nome
        │   │       ├── FolderPickerSheet.kt    selettore di cartella
        │   │       ├── library/LibraryScreen.kt   schermata principale
        │   │       ├── folder/FolderScreen.kt     contenuto di una cartella
        │   │       ├── review/ReviewScreen.kt     revisione dopo la scansione
        │   │       ├── review/A4Preview.kt        anteprima del foglio A4
        │   │       ├── detail/DetailScreen.kt     documento aperto
        │   │       ├── scan/ScanModeSheet.kt      scelta della modalità
        │   │       ├── settings/SettingsSheet.kt  impostazioni
        │   │       └── theme/                     Color.kt, Theme.kt
        │   └── res/
        │       ├── values/          strings.xml, colors.xml, themes.xml
        │       ├── values-night/    themes.xml
        │       ├── drawable/        ic_launcher_foreground.xml
        │       ├── mipmap*/         icone vettoriali
        │       └── xml/             file_paths.xml, data_extraction_rules.xml
        └── test/java/it/example/docscan/
            ├── data/A4ComposerTest.kt      geometria del foglio
            ├── data/FolderNameTest.kt      unicità dei nomi cartella
            └── ocr/ParserTest.kt           codice fiscale, MRZ, IBAN, PAN
```

L'interfaccia è interamente in Jetpack Compose: non ci sono layout XML.

## Scelte tecniche

**Nessun permesso INTERNET.** Il manifest non lo dichiara, quindi il processo
dell'app è tecnicamente incapace di trasmettere dati, e la cosa si verifica
aprendo il manifest. Il modello OCR è impacchettato nell'APK e funziona in
modalità aereo.

L'acquisizione passa dal Document Scanner di ML Kit, che gira nel processo di
Google Play Services. Google dichiara che le immagini restano sul dispositivo,
ma riceve metriche d'uso delle API: chi distribuisce l'app deve dirlo agli
utenti.

**Cifratura.** Ogni file dell'archivio è cifrato AES-256-GCM con una chiave non
esportabile custodita nell'Android Keystore. Le anteprime vengono decifrate
direttamente in memoria: nessun file in chiaro tocca il disco, tranne la copia
temporanea creata per condividere o esportare, cancellata al rientro nell'app.

**Scala reale.** La composizione A4 parte dai millimetri e scrive il PDF in
punti PostScript (1 mm = 72/25,4 pt). Una carta ID-1 stampata misura davvero
85,6 × 54 mm. Un'immagine non ha unità fisiche e perderebbe la scala alla prima
stampa.

**Estrazione dati.** L'OCR restituisce le righe della pagina, non un testo
appiattito: su una fattura etichetta e valore stanno sulla stessa riga, ed è il
segnale più affidabile disponibile. Codice fiscale, MRZ, IBAN e partita IVA
vengono verificati con le rispettive cifre di controllo. Se una pagina non è né
un documento d'identità né un documento commerciale, non viene estratto nulla.

**Dati di pagamento.** Ogni sequenza di 13-19 cifre passa per la verifica di
Luhn. Se la supera è un numero di carta: non diventa un campo e viene mascherato
nel testo salvato, lasciando le ultime quattro cifre.

## Cosa manca

In ordine di utilità.

- **Evidenziazione sulla pagina.** ML Kit restituisce il rettangolo di ogni riga
  riconosciuta. Conservandolo, toccando un campo si potrebbe vedere da dove è
  stato letto, invece di doversi fidare di una percentuale.
- **Blocco biometrico.** Oggi chi ha in mano il telefono sbloccato vede tutti i
  documenti. Basterebbe `setUserAuthenticationRequired(true)` sulla chiave
  Keystore, più il prompt.
- **Retention.** L'archivio conserva i documenti per sempre. Servirebbe almeno
  una scadenza per cartella e un avviso sui documenti più vecchi di N mesi.
- **Selezione multipla.** Eliminare o spostare venti scansioni una per una è
  scomodo.
- **Password sul PDF condiviso.** Una copia inviata via messaggio resta in chiaro
  nel telefono di chi la riceve.
- **Preset passaporto da rivedere.** Assume due facciate come le tessere, ma la
  pagina dati del passaporto è una sola: probabilmente serve un riquadro unico
  più grande.
- **Riordino delle cartelle con trascinamento.** Al momento ci sono i pulsanti
  su e giù.
- **Anteprime `@Preview`** per lavorare sulle schermate senza ricompilare.
