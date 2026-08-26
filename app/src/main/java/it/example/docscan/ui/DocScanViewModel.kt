package it.example.docscan.ui

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.net.Uri as AndroidUri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.example.docscan.R
import it.example.docscan.data.AppLanguage
import it.example.docscan.data.AppSettings
import it.example.docscan.data.DocKind
import it.example.docscan.data.DocumentRecord
import it.example.docscan.data.DocumentRepository
import it.example.docscan.data.ExtractedField
import it.example.docscan.data.FitMode
import it.example.docscan.data.Folder
import it.example.docscan.data.ScanMode
import it.example.docscan.data.SettingsStore
import it.example.docscan.data.ThemeMode
import it.example.docscan.ocr.FieldExtractor
import it.example.docscan.ocr.Ocr
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { LIBRARY, FOLDER, REVIEW, DETAIL }

/** Criterio di ordinamento nella schermata cartella. */
enum class SortField { NAME, MODIFIED }

/** Id della cartella virtuale "Scansioni recenti": non esiste su disco. */
const val FOLDER_RECENT = "__recenti"

/** Mensola virtuale con gli esiti della ricerca. */
const val FOLDER_SEARCH = "__risultati"
private const val RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

/** Stato del foglio di salvataggio, che nel design ha tre fasi. */
enum class ExportStage { CLOSED, DESTINATIONS, FOLDERS, BUSY }

/** Scansione appena acquisita, non ancora salvata. */
data class PendingScan(
    val scanMode: ScanMode = ScanMode.DOCUMENT,
    val fitMode: FitMode = FitMode.TRUE_SCALE,
    val pageUris: List<Uri> = emptyList(),
    val fields: List<ExtractedField> = emptyList(),
    val searchText: String = "",
    val kind: DocKind = DocKind.FORM,
    val fileName: String = "",
    /** Pagina attualmente in anteprima nella schermata di revisione. */
    val selectedPage: Int = 0,
) {
    val pageCount: Int get() = pageUris.size.coerceAtLeast(1)
    val selectedUri: Uri? get() = pageUris.getOrNull(selectedPage)

    /** Stima grossolana, coerente con quella mostrata nel prototipo. */
    /** Stima grossolana, coerente con quella mostrata nel prototipo. */
    val fileSizeMb: Float get() = 0.34f + pageCount * 0.29f
}

data class UiState(
    val screen: Screen = Screen.LIBRARY,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val records: List<DocumentRecord> = emptyList(),
    val query: String = "",
    val filter: String = FILTER_ALL,
    val editing: Boolean = false,
    val pending: PendingScan? = null,
    val exportStage: ExportStage = ExportStage.CLOSED,
    val openDoc: DocumentRecord? = null,
    val toast: String? = null,
    val settings: AppSettings = AppSettings(),
    val showSettings: Boolean = false,
    /** Foglio di scelta della modalita, aperto da "Scansiona". */
    val showScanModes: Boolean = false,
    val scanMode: ScanMode = ScanMode.DOCUMENT,
    /** Uri pronta da passare all'intent di condivisione; l'Activity la consuma. */
    val pendingShareUri: String? = null,
    /** Cartella aperta a schermo intero, con i documenti in righe. */
    val openFolder: Folder? = null,
    val sortField: SortField = SortField.MODIFIED,
    /** Predefinito: piu recenti in cima, come nella libreria. */
    val sortAscending: Boolean = false,
    /** Documento su cui e stata fatta una pressione lunga. */
    val actionsFor: DocumentRecord? = null,
    /** File dell'archivio che non si riesce a decifrare o interpretare. */
    val unreadable: List<String> = emptyList(),
    /** Cartella in fase di rinomina. */
    val renamingFolder: Folder? = null,
    /** Vero mentre è aperto il dialogo di creazione cartella. */
    val creatingFolder: Boolean = false,
    /** Documento per cui è aperto il selettore di cartella. */
    val movingDoc: DocumentRecord? = null,
    /** Documento in fase di rinomina, con la bozza del nuovo titolo. */
    val renaming: DocumentRecord? = null,
) {
    companion object {
        /**
         * Sentinella del filtro "tutte le cartelle". Resta una costante fissa
         * perché viene confrontata con i nomi delle cartelle; l'etichetta
         * mostrata la traduce la libreria.
         */
        const val FILTER_ALL = "__tutte__"
    }
}

private const val FILTER_ALL = UiState.FILTER_ALL

class DocScanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DocumentRepository(app)
    private val settingsStore = SettingsStore(app)
    private val ocr = Ocr()

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        // Lettura sincrona: se il tema arrivasse in modo asincrono, l'app
        // lampeggerebbe in chiaro per un frame prima di passare allo scuro.
        _state.update { it.copy(settings = settingsStore.load()) }
        refresh()
    }

    /**
     * Unico punto di caricamento dei documenti. Registra anche i file
     * illeggibili, così nessun percorso può dimenticarsene.
     */
    /**
     * Testo localizzato per i messaggi del ViewModel.
     *
     * Qui `stringResource` non esiste: non siamo in una composizione. Si
     * costruisce un Context con la lingua scelta, così i messaggi seguono
     * l'impostazione come il resto dell'interfaccia.
     */
    /** Resources con la lingua scelta, per le etichette costruite qui. */
    private fun res(): android.content.res.Resources {
        val base = getApplication<Application>()
        val tag = _state.value.settings.language.tag ?: return base.resources
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config).resources
    }

    private fun str(@StringRes id: Int, vararg args: Any): String {
        val base = getApplication<Application>()
        val tag = _state.value.settings.language.tag
        val ctx = if (tag == null) base else {
            val locale = Locale.forLanguageTag(tag)
            val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
            base.createConfigurationContext(config)
        }
        return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
    }

    private suspend fun loadRecords(): List<DocumentRecord> {
        val archive = runCatching { repo.loadArchive() }
            .getOrDefault(DocumentRepository.ArchiveContents(emptyList(), emptyList()))
        _state.update { it.copy(unreadable = archive.unreadable) }
        return archive.records
    }

    /** Ricarica cartelle e documenti. Chiamata all'avvio e dopo ogni modifica all'archivio. */
    private fun refresh() {
        viewModelScope.launch {
            val folders = repo.folders()
            val records = loadRecords()
            _state.update {
                it.copy(folders = folders, records = records, loading = false)
            }
        }
    }

    // -------------------------------------------------------- Navigazione

    /** Apre un documento nel dettaglio. */
    fun openDocument(record: DocumentRecord) =
        _state.update { it.copy(openDoc = record, screen = Screen.DETAIL) }

    /**
     * Back gerarchico: chiude prima l'overlay piu interno, e da un documento
     * aperto dentro una cartella torna alla cartella, non alla libreria.
     */
    fun goBack() {
        val s = _state.value
        when {
            s.renaming != null -> cancelRename()
            s.renamingFolder != null -> cancelFolderRename()
            s.creatingFolder -> cancelFolderCreate()
            s.movingDoc != null -> cancelMove()
            s.actionsFor != null -> hideActions()
            // Il pannello di salvataggio si chiude un passo alla volta: dalla
            // scelta della cartella si torna alle destinazioni, e da lì
            // all'anteprima. Durante il salvataggio il back non fa nulla,
            // perché interromperlo lascerebbe l'archivio a metà.
            s.exportStage == ExportStage.BUSY -> Unit
            s.exportStage == ExportStage.FOLDERS -> backToDestinations()
            s.exportStage != ExportStage.CLOSED -> closeExport()
            s.showScanModes -> closeScanModes()
            // Uscire dalla ricerca prima di lasciare la schermata: il back
            // annulla l'ultima cosa fatta, e l'ultima cosa era cercare.
            s.screen == Screen.LIBRARY && s.query.isNotBlank() -> setQuery("")
            s.showSettings -> closeSettings()
            s.screen == Screen.DETAIL && s.openFolder != null ->
                _state.update { it.copy(screen = Screen.FOLDER, openDoc = null) }
            s.screen == Screen.FOLDER -> closeFolder()
            else -> backToLibrary()
        }
    }

    /**
     * Torna alla libreria azzerando cartella aperta e scansione in revisione.
     *
     * Per il tasto indietro usare [goBack], che chiude un livello alla volta.
     */
    fun backToLibrary() = _state.update {
        it.copy(screen = Screen.LIBRARY, openDoc = null, openFolder = null, pending = null)
    }

    // ----------------------------------------------------------- Archivio

    /** Aggiorna il testo di ricerca. */
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    /** Cambia il filtro per cartella. */
    fun setFilter(f: String) = _state.update { it.copy(filter = f) }
    /** Entra o esce dalla modalità modifica delle cartelle. */
    fun toggleEditing() = _state.update { it.copy(editing = !it.editing) }

    /**
     * Le mensole visibili: filtro per cartella, poi ricerca full-text.
     * "Scansioni recenti" è virtuale — mostra gli ultimi documenti a
     * prescindere dalla cartella, come nel prototipo.
     */
    fun shelves(s: UiState): List<Pair<Folder, List<DocumentRecord>>> {
        val matching = repo.search(s.records, s.query)

        // Durante la ricerca si mostra un solo elenco di esiti. Con le mensole
        // normali lo stesso documento comparirebbe due volte, in "Scansioni
        // recenti" e nella sua cartella, e tutte le cartelle senza risultati
        // resterebbero visibili e vuote: sembrava che la ricerca non partisse.
        if (s.query.isNotBlank()) {
            val inScope = if (s.filter == FILTER_ALL) matching else {
                matching.filter { it.folderId == s.filter }
            }
            // Nessun esito: si restituisce una lista vuota, così compare il
            // messaggio "Nessun risultato" invece di una mensola che invita a
            // riempire una cartella che non esiste.
            if (inScope.isEmpty()) return emptyList()
            return listOf(Folder(FOLDER_SEARCH, str(R.string.shelf_results), -1) to inScope)
        }

        val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
        val recents = Folder(FOLDER_RECENT, str(R.string.shelf_recent), -1) to
                matching.filter { it.createdAtEpochMs >= cutoff }
        val real = s.folders.map { folder ->
            folder to matching.filter { it.folderId == folder.id }
        }
        val filtered = if (s.filter == FILTER_ALL) listOf(recents) + real
        else real.filter { it.first.id == s.filter }

        // In modalità modifica si vedono anche le cartelle vuote, per poterle
        // riordinare o eliminare.
        return if (s.editing) filtered
        else filtered.filter { it.second.isNotEmpty() || s.filter != FILTER_ALL }
    }

    /** Apre una cartella a schermo intero. */
    fun openFolder(folder: Folder) =
        _state.update { it.copy(openFolder = folder, screen = Screen.FOLDER) }

    /** Chiude la cartella e torna alla libreria. */
    fun closeFolder() =
        _state.update { it.copy(openFolder = null, screen = Screen.LIBRARY) }

    /** Cambia il criterio di ordinamento. */
    fun setSortField(field: SortField) = _state.update { it.copy(sortField = field) }

    /** Inverte crescente e decrescente. */
    fun toggleSortDirection() = _state.update { it.copy(sortAscending = !it.sortAscending) }

    /**
     * Documenti della cartella aperta, filtrati dalla ricerca e ordinati.
     * L'ordinamento per nome usa un confronto case-insensitive: "fattura" e
     * "Fattura" devono stare vicini, non in due blocchi separati.
     */
    fun folderDocuments(s: UiState): List<DocumentRecord> {
        val folder = s.openFolder ?: return emptyList()
        val matching = repo.search(s.records, s.query)
        // "Scansioni recenti" e' una vista, non una cartella: nessun documento
        // ha quel folderId, quindi filtrarci sopra darebbe sempre zero risultati.
        val docs = if (folder.id == FOLDER_SEARCH) {
            matching
        } else if (folder.id == FOLDER_RECENT) {
            val cutoff = System.currentTimeMillis() - RECENT_WINDOW_MS
            matching.filter { it.createdAtEpochMs >= cutoff }
        } else {
            matching.filter { it.folderId == folder.id }
        }
        val sorted = when (s.sortField) {
            SortField.NAME -> docs.sortedBy { it.title.lowercase() }
            SortField.MODIFIED -> docs.sortedBy { it.createdAtEpochMs }
        }
        return if (s.sortAscending) sorted else sorted.reversed()
    }

    // ------------------------------------------------- Azioni sul documento

    /** Apre il menu di un documento. */
    fun showActions(record: DocumentRecord) = _state.update { it.copy(actionsFor = record) }
    /** Chiude il menu. */
    fun hideActions() = _state.update { it.copy(actionsFor = null) }

    /** Apre il dialogo di rinomina di un documento. */
    fun startRename(record: DocumentRecord) =
        _state.update { it.copy(actionsFor = null, renaming = record) }

    /** Chiude il dialogo senza salvare. */
    fun cancelRename() = _state.update { it.copy(renaming = null) }

    /** Rinomina un documento, ripulendo i caratteri non ammessi nei nomi file. */
    fun renameDocument(record: DocumentRecord, newTitle: String) {
        val clean = newTitle.replace(Regex("""[/\\:*?"<>|]"""), "_").trim()
        if (clean.isBlank()) {
            _state.update { it.copy(renaming = null) }
            toast(str(R.string.msg_name_empty))
            return
        }
        viewModelScope.launch {
            // Stessa regola del salvataggio: nella cartella non possono restare
            // due documenti con lo stesso nome. `exceptId` evita che il
            // documento vada in conflitto con se stesso quando lo si rinomina
            // lasciando il nome com'era.
            val unique = repo.uniqueTitle(clean, record.folderId, exceptId = record.id)
            val updated = record.copy(title = unique)
            repo.updateRecord(updated)
            val records = loadRecords()
            _state.update {
                it.copy(
                    renaming = null,
                    records = records,
                    openDoc = if (it.openDoc?.id == record.id) updated else it.openDoc,
                    toast = str(R.string.msg_renamed_to, unique),
                )
            }
        }
    }

    /** Apre il dialogo di creazione cartella. */
    fun startFolderCreate() = _state.update { it.copy(creatingFolder = true) }
    /** Chiude il dialogo senza creare nulla. */
    fun cancelFolderCreate() = _state.update { it.copy(creatingFolder = false) }

    /**
     * Errore da mostrare nel dialogo, null se il nome va bene. Sta qui perché è
     * il ViewModel a conoscere le cartelle, e la regola serve identica a
     * creazione e rinomina.
     */
    fun folderNameError(name: String, exceptId: String? = null): String? {
        val clean = name.trim()
        return when {
            clean.isBlank() -> str(R.string.msg_name_empty)
            _state.value.folders.any {
                it.id != exceptId && it.name.trim().equals(clean, ignoreCase = true)
            } -> str(R.string.msg_name_taken)
            else -> null
        }
    }

    /** Crea una cartella. Non fa nulla se il nome è vuoto o già in uso: vedi [folderNameError]. */
    fun createFolder(name: String) {
        viewModelScope.launch {
            val folders = repo.addFolder(name)
            if (folders == null) {
                toast(str(R.string.msg_name_invalid))
                return@launch
            }
            _state.update {
                it.copy(
                    folders = folders,
                    creatingFolder = false,
                    toast = str(R.string.msg_folder_created, name.trim()),
                )
            }
        }
    }

    /**
     * Elimina una cartella e sposta i suoi documenti in "Da ordinare".
     *
     * I documenti non si cancellano mai insieme alla cartella: chi riordina
     * l'archivio non si aspetta di perdere delle scansioni.
     */
    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            val folders = repo.deleteFolder(folder.id)
            val records = loadRecords()
            _state.update { it.copy(folders = folders, records = records) }
            toast(str(R.string.msg_folder_deleted))
        }
    }

    /** Sposta una cartella di una posizione nell'elenco. */
    fun moveFolder(folder: Folder, delta: Int) {
        viewModelScope.launch {
            val folders = repo.moveFolder(folder.id, delta)
            _state.update { it.copy(folders = folders) }
        }
    }

    // ------------------------------------------------------------ Scansione

    /**
     * Chiamata quando ML Kit restituisce le pagine acquisite.
     *
     * Ogni avvio dello scanner è indipendente: per aggiungere pagine si accoda
     * il nuovo risultato a quello in revisione e si rifà l'OCR sull'insieme. La
     * MRZ o un totale possono stare sulla pagina appena aggiunta.
     */
    fun onScanned(newPages: List<Uri>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, screen = Screen.REVIEW, showScanModes = false) }

            val existing = _state.value.pending
            val mode = _state.value.scanMode
            val allPages = (existing?.pageUris ?: emptyList()) + newPages

            // Righe, non testo appiattito: l'estrattore si ancora alle etichette
            // e ha bisogno di sapere cosa sta sulla stessa riga di cosa.
            val lines = withContext(Dispatchers.Default) {
                ocr.readAllLines(getApplication(), allPages)
            }
            val result = FieldExtractor.extract(lines)

            _state.update {
                it.copy(
                    busy = false,
                    pending = PendingScan(
                        scanMode = mode,
                        // L'adattamento scelto non va perso quando si acquisisce il retro.
                        fitMode = existing?.fitMode ?: FitMode.TRUE_SCALE,
                        pageUris = allPages,
                        fields = result.fields,
                        // Testo con i numeri di carta mascherati: il PAN completo
                        // non deve entrare nell'archivio nemmeno via ricerca.
                        searchText = result.searchableText,
                        kind = result.kind,
                        // Il nome scelto a mano non va sovrascritto da una seconda scansione.
                        fileName = existing?.fileName ?: sanitizeFileName(result.suggestedTitle),
                        selectedPage = if (existing == null) 0 else allPages.lastIndex,
                    ),
                )
            }
        }
    }

    /** Pagina mostrata nell'anteprima grande. */
    fun selectPage(index: Int) = _state.update { s ->
        val pending = s.pending
        if (pending == null || index !in pending.pageUris.indices) s
        else s.copy(pending = pending.copy(selectedPage = index))
    }

    /**
     * Elimina una pagina e riestrae i campi dalle rimanenti: altrimenti
     * resterebbero dati letti da una pagina che non c'è più. Se non resta
     * nulla, la scansione viene scartata.
     */
    fun removePage(index: Int) {
        val pending = _state.value.pending ?: return
        if (index !in pending.pageUris.indices) return

        val remaining = pending.pageUris.toMutableList().apply { removeAt(index) }
        if (remaining.isEmpty()) {
            discardScan()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val lines = withContext(Dispatchers.Default) {
                ocr.readAllLines(getApplication(), remaining)
            }
            val result = FieldExtractor.extract(lines)
            _state.update {
                it.copy(
                    busy = false,
                    pending = pending.copy(
                        pageUris = remaining,
                        selectedPage = index.coerceAtMost(remaining.lastIndex),
                        fields = result.fields,
                        searchText = result.searchableText,
                        kind = result.kind,
                    ),
                    toast = str(R.string.msg_page_deleted),
                )
            }
        }
    }

    /** Cambia il nome della scansione in revisione. */
    fun setFileName(name: String) = _state.update { s ->
        s.copy(pending = s.pending?.copy(fileName = name))
    }

    /** Butta via la scansione in revisione. */
    fun discardScan() = _state.update {
        it.copy(pending = null, screen = Screen.LIBRARY, exportStage = ExportStage.CLOSED)
    }

    // -------------------------------------------------------- Salvataggio

    /** Apre il pannello di salvataggio. */
    fun openExport() = _state.update { it.copy(exportStage = ExportStage.DESTINATIONS) }
    /** Chiude il pannello di salvataggio. */
    fun closeExport() = _state.update { it.copy(exportStage = ExportStage.CLOSED) }
    /** Passa alla scelta della cartella. */
    fun showFolderPicker() = _state.update { it.copy(exportStage = ExportStage.FOLDERS) }
    /** Torna all'elenco delle destinazioni. */
    fun backToDestinations() = _state.update { it.copy(exportStage = ExportStage.DESTINATIONS) }

    /** Salvataggio nell'archivio interno cifrato. */
    fun saveToFolder(folder: Folder) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(exportStage = ExportStage.BUSY) }
            try {
                val record = repo.save(
                    title = pending.fileName,
                    folderId = folder.id,
                    pageUris = pending.pageUris,
                    fields = pending.fields,
                    searchText = pending.searchText,
                    kind = pending.kind,
                    scanMode = pending.scanMode,
                    fitMode = pending.fitMode,
                )
                delay(200.milliseconds)
                val records = loadRecords()
                _state.update {
                    it.copy(
                        exportStage = ExportStage.CLOSED,
                        pending = null,
                        openDoc = record,
                        screen = Screen.DETAIL,
                        records = records,
                        toast = str(R.string.msg_saved_in, folderName(res(), folder), record.pageLabel(res())),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(exportStage = ExportStage.CLOSED) }
                toast(str(R.string.msg_save_failed, e.message ?: ""))
            }
        }
    }

    /** Nome proposto al selettore SAF per l'esportazione fuori dalla sandbox. */
    fun exportFileName(): String = "${_state.value.pending?.fileName ?: str(R.string.scan_default_name)}.pdf"

    /**
     * Esportazione verso una destinazione esterna. Salva comunque in archivio:
     * altrimenti dopo "Salva" la libreria resterebbe vuota.
     */
    fun onExternalDestinationChosen(destination: Uri) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(exportStage = ExportStage.BUSY) }
            try {
                val unsorted = _state.value.folders
                    .firstOrNull { it.id == DocumentRepository.FOLDER_UNSORTED }
                    ?: _state.value.folders.first()
                val record = repo.save(
                    title = pending.fileName,
                    folderId = unsorted.id,
                    pageUris = pending.pageUris,
                    fields = pending.fields,
                    searchText = pending.searchText,
                    kind = pending.kind,
                    scanMode = pending.scanMode,
                    fitMode = pending.fitMode,
                )
                val ok = repo.exportPdfTo(record, destination)
                val records = loadRecords()
                _state.update {
                    it.copy(
                        exportStage = ExportStage.CLOSED,
                        pending = null,
                        openDoc = record,
                        screen = Screen.DETAIL,
                        records = records,
                        toast = if (ok) str(R.string.msg_exported, record.pageLabel(res()))
                        else str(R.string.msg_export_failed),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(exportStage = ExportStage.CLOSED) }
                toast(str(R.string.msg_share_failed, e.message ?: ""))
            }
        }
    }

    /**
     * Esportazione nella cartella fissa scelta nelle impostazioni, senza
     * riaprire il selettore. Se il permesso non è più valido lo dice e tiene
     * comunque il documento in archivio, invece di perdere la scansione.
     */
    fun exportToDefaultFolder(treeUri: AndroidUri) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(exportStage = ExportStage.BUSY) }
            try {
                val unsorted = _state.value.folders
                    .firstOrNull { it.id == DocumentRepository.FOLDER_UNSORTED }
                    ?: _state.value.folders.first()
                val record = repo.save(
                    title = pending.fileName,
                    folderId = unsorted.id,
                    pageUris = pending.pageUris,
                    fields = pending.fields,
                    searchText = pending.searchText,
                    kind = pending.kind,
                    scanMode = pending.scanMode,
                    fitMode = pending.fitMode,
                )
                // Il nome del PDF è quello del record, non quello digitato: se
                // l'archivio ha aggiunto un "(2)" per non avere due omonimi, il
                // file sul telefono deve chiamarsi allo stesso modo.
                val written = repo.exportPdfToTree(record, treeUri, "${record.title}.pdf")
                val label = _state.value.settings.defaultFolderLabel ?: str(R.string.export_folder)
                val records = loadRecords()
                _state.update {
                    it.copy(
                        exportStage = ExportStage.CLOSED,
                        pending = null,
                        openDoc = record,
                        screen = Screen.DETAIL,
                        records = records,
                        toast = if (written != null) str(R.string.msg_exported_in, label)
                        else str(R.string.msg_permission_lost),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(exportStage = ExportStage.CLOSED) }
                toast(str(R.string.msg_share_failed, e.message ?: ""))
            }
        }
    }

    // -------------------------------------------------------------- Dettaglio

    /** Sostituisce il valore di un campo. */
    fun updateField(record: DocumentRecord, index: Int, newValue: String) {
        viewModelScope.launch {
            val fields = record.fields.toMutableList()
            val old = fields.getOrNull(index) ?: return@launch
            // Correzione manuale: la confidenza torna piena.
            fields[index] = old.copy(value = newValue, confidence = 1f)
            val updated = record.copy(fields = fields)
            repo.updateRecord(updated)
            val records = loadRecords()
            _state.update { it.copy(openDoc = updated, records = records) }
        }
    }

    /**
     * Campo aggiunto a mano. Confidenza piena: chi sta guardando il documento è
     * più affidabile dell'OCR con qualunque checksum.
     */
    fun addField(record: DocumentRecord, label: String, value: String) {
        val l = label.trim()
        val v = value.trim()
        if (l.isBlank() || v.isBlank()) {
            toast(str(R.string.msg_name_empty))
            return
        }
        viewModelScope.launch {
            val updated = record.copy(fields = record.fields + ExtractedField(l, v, 1f))
            repo.updateRecord(updated)
            val records = loadRecords()
            _state.update {
                it.copy(openDoc = updated, records = records, toast = str(R.string.msg_field_added, l))
            }
        }
    }

    /** Toglie un campo dal documento. */
    fun removeField(record: DocumentRecord, index: Int) {
        viewModelScope.launch {
            val fields = record.fields.toMutableList()
            if (index !in fields.indices) return@launch
            val removed = fields.removeAt(index)
            val updated = record.copy(fields = fields)
            repo.updateRecord(updated)
            val records = loadRecords()
            _state.update {
                it.copy(openDoc = updated, records = records, toast = str(R.string.msg_field_removed, removed.label))
            }
        }
    }

    /**
     * Marca come verificati tutti i campi rimasti a bassa confidenza.
     *
     * Non ricontrolla nulla: è l'operatore che si assume la responsabilità di
     * quello che ha letto sullo schermo.
     */
    fun confirmAllFields(record: DocumentRecord) {
        viewModelScope.launch {
            val updated = record.copy(
                fields = record.fields.map { if (it.needsReview) it.copy(confidence = 1f) else it },
            )
            repo.updateRecord(updated)
            val records = loadRecords()
            _state.update { it.copy(openDoc = updated, records = records) }
            toast(str(R.string.msg_fields_confirmed))
        }
    }

    /** Elimina un documento e i suoi file. */
    fun deleteDocument(record: DocumentRecord) {
        viewModelScope.launch {
            repo.deleteRecord(record)
            val records = loadRecords()
            _state.update {
                it.copy(
                    records = records,
                    openDoc = null,
                    actionsFor = null,
                    screen = if (it.openFolder != null) Screen.FOLDER else Screen.LIBRARY,
                    toast = str(R.string.msg_document_deleted),
                )
            }
        }
    }

    /** Decifra una pagina per mostrarla. Restituisce null se il file manca o è illeggibile. */
    suspend fun pageBitmap(record: DocumentRecord, index: Int = 0): Bitmap? =
        repo.pageBitmap(record, index)

    /** Tutti i campi come testo, una riga per campo nel formato "etichetta: valore". */
    fun copyAllText(record: DocumentRecord): String =
        record.fields.joinToString("\n") { "${it.label}: ${it.value}" }

    // ------------------------------------------------- Archivio danneggiato

    /**
     * Elimina i file illeggibili. Mai in automatico: potrebbero essere
     * recuperabili da un backup del telefono.
     */
    fun purgeUnreadable() {
        val files = _state.value.unreadable
        if (files.isEmpty()) return
        viewModelScope.launch {
            val removed = repo.purgeUnreadable(files)
            val records = loadRecords()
            _state.update {
                it.copy(records = records, toast = str(R.string.msg_purged, removed))
            }
        }
    }

    // ------------------------------------------------------ Rinomina cartella

    /** Apre il dialogo di rinomina di una cartella. */
    fun startFolderRename(folder: Folder) = _state.update { it.copy(renamingFolder = folder) }
    /** Chiude il dialogo senza rinominare. */
    fun cancelFolderRename() = _state.update { it.copy(renamingFolder = null) }

    /**
     * Rinomina una cartella.
     *
     * Aggiorna anche il filtro attivo e la cartella eventualmente aperta, che
     * puntano al nome vecchio: senza, dopo la rinomina la libreria apparirebbe
     * vuota. Non fa nulla se il nome è vuoto o già usato.
     */
    fun renameFolder(folder: Folder, newName: String) {
        viewModelScope.launch {
            val folders = repo.renameFolder(folder.id, newName)
            if (folders == null) {
                toast(str(R.string.msg_name_invalid))
                return@launch
            }
            val clean = newName.trim()
            _state.update {
                it.copy(
                    folders = folders,
                    renamingFolder = null,
                    // Il filtro attivo puntava al vecchio nome: senza questo
                    // l'utente si ritrova una libreria vuota dopo la rinomina.
                    // Il filtro lavora sugli id: rinominare non lo invalida più.
                    openFolder = if (it.openFolder?.id == folder.id)
                        it.openFolder.copy(name = clean) else it.openFolder,
                    toast = str(R.string.msg_folder_renamed),
                )
            }
        }
    }

    // ------------------------------------------------- Spostamento documento

    /** Apre il selettore per spostare un documento. */
    fun startMove(record: DocumentRecord) =
        _state.update { it.copy(actionsFor = null, movingDoc = record) }

    /** Chiude il selettore. */
    fun cancelMove() = _state.update { it.copy(movingDoc = null) }

    /** Sposta un documento in un'altra cartella. Cambia solo i metadati, i file restano dove sono. */
    fun moveDocument(record: DocumentRecord, folder: Folder) {
        viewModelScope.launch {
            val updated = repo.moveRecord(record, folder.id)
            val records = loadRecords()
            _state.update {
                it.copy(
                    movingDoc = null,
                    records = records,
                    openDoc = if (it.openDoc?.id == record.id) updated else it.openDoc,
                    toast = str(R.string.msg_moved_to, folderName(res(), folder)),
                )
            }
        }
    }

    // -------------------------------------------------- Modalita scansione

    /** Apre il pannello con le modalità di scansione. */
    fun openScanModes() = _state.update { it.copy(showScanModes = true) }
    /** Chiude il pannello. */
    fun closeScanModes() = _state.update { it.copy(showScanModes = false) }
    /** Cambia modalità: documento, tessera o passaporto. */
    fun setScanMode(mode: ScanMode) = _state.update { it.copy(scanMode = mode) }

    /** Testo del pulsante: le modalita a due facciate dichiarano il passo. */
    fun scanButtonLabel(s: UiState): String = when {
        !s.scanMode.isTwoSided -> str(R.string.scan)
        s.pending == null || s.pending.pageUris.isEmpty() -> str(R.string.scan_front)
        else -> str(R.string.scan_back)
    }

    /**
     * Didascalia sempre presente: quando compariva solo nelle modalita a due
     * facciate, il pannello cambiava altezza passando da una all'altra.
     */
    fun scanStepLabel(s: UiState): String = when {
        !s.scanMode.isTwoSided -> str(R.string.step_document)
        s.pending == null || s.pending.pageUris.isEmpty() -> str(R.string.step_one_of_two)
        else -> str(R.string.step_two_of_two)
    }

    /**
     * Nelle modalita a due facciate il numero di pagine e' fissato a due: una
     * terza non avrebbe posto sul foglio.
     */
    fun pageLimitForMode(): Int = if (_state.value.scanMode.isTwoSided) 1 else 10

    /**
     * Cambia l'adattamento dell'immagine sul foglio A4.
     *
     * Ha effetto solo sulle modalità a due facciate. Il PDF viene composto al
     * salvataggio, quindi la scelta può cambiare quante volte si vuole prima.
     */
    fun setFitMode(mode: FitMode) = _state.update { s ->
        s.copy(pending = s.pending?.copy(fitMode = mode))
    }

    /** Vero quando entrambe le facciate sono state acquisite. */
    fun bothSidesCaptured(s: UiState): Boolean =
        s.scanMode.isTwoSided && (s.pending?.pageUris?.size ?: 0) >= 2

    // ------------------------------------------------------- Condivisione

    /**
     * Condivide un documento gia in archivio (dalla schermata di dettaglio).
     * La Uri finisce nello stato: l'Activity la consuma e lancia l'intent, cosi
     * il ViewModel non ha bisogno di conoscere il Context dell'Activity.
     */
    fun shareDocument(record: DocumentRecord) {
        viewModelScope.launch {
            val uri = repo.pdfForSharing(record)
            if (uri == null) {
                toast(str(R.string.msg_pdf_unavailable))
                return@launch
            }
            _state.update { it.copy(pendingShareUri = uri.toString(), actionsFor = null) }
        }
    }

    /**
     * Condivide una scansione appena acquisita. Salva prima in archivio: se
     * l'utente annulla la condivisione, la scansione non deve andare persa.
     */
    /**
     * Condivide la scansione senza salvarla in archivio.
     *
     * Prima salvava, cifrava e apriva il dettaglio: un'attesa per un lavoro che
     * l'utente non aveva chiesto. Ora genera solo il PDF e resta in revisione,
     * così dopo aver condiviso puoi comunque premere Salva se vuoi tenerlo.
     */
    fun shareScan() {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val uri = repo.pdfForSharing(
                pageUris = pending.pageUris,
                scanMode = pending.scanMode,
                fitMode = pending.fitMode,
                fileName = pending.fileName,
            )
            _state.update {
                it.copy(
                    busy = false,
                    pendingShareUri = uri?.toString(),
                    toast = if (uri == null) str(R.string.msg_pdf_failed) else null,
                )
            }
        }
    }

    /** Azzera la Uri di condivisione dopo che l'Activity ha lanciato l'intent. */
    fun consumeShare() = _state.update { it.copy(pendingShareUri = null) }

    /** Svuota la cartella dei PDF decifrati per la condivisione. Da chiamare a ogni rientro nell'app. */
    fun clearShareCache() = repo.clearShareCache()

    // ------------------------------------------------------ Impostazioni

    /** Apre le impostazioni. */
    fun openSettings() = _state.update { it.copy(showSettings = true) }
    /** Chiude le impostazioni. */
    fun closeSettings() = _state.update { it.copy(showSettings = false) }

    /** Cambia lingua e la salva subito. */
    fun setLanguage(language: AppLanguage) {
        val next = _state.value.settings.copy(language = language)
        settingsStore.save(next)
        _state.update { it.copy(settings = next) }
    }

    /** Cambia tema e lo salva subito: deve sopravvivere alla chiusura dell'app. */
    fun setThemeMode(mode: ThemeMode) {
        val next = _state.value.settings.copy(themeMode = mode)
        settingsStore.save(next)
        _state.update { it.copy(settings = next) }
    }

    /** [label] arriva gia leggibile dall'Activity, che sa interrogare SAF. */
    fun setDefaultFolder(uri: AndroidUri, label: String) {
        val next = _state.value.settings.copy(
            defaultFolderUri = uri.toString(),
            defaultFolderLabel = label,
        )
        settingsStore.save(next)
        _state.update { it.copy(settings = next, toast = str(R.string.msg_default_folder, label)) }
    }

    /**
     * Dimentica la cartella predefinita.
     *
     * Il permesso SAF concesso a suo tempo resta valido a livello di sistema,
     * ma l'app smette di usarlo e torna a chiedere la destinazione ogni volta.
     */
    fun clearDefaultFolder() {
        val next = _state.value.settings.copy(defaultFolderUri = null, defaultFolderLabel = null)
        settingsStore.save(next)
        _state.update { it.copy(settings = next) }
    }

    /** Null se non c'e una cartella fissa: il chiamante apre il selettore. */
    fun defaultFolderUri(): String? = _state.value.settings.defaultFolderUri

    // ------------------------------------------------------------------ Varie

    /** Mostra un messaggio in fondo allo schermo. */
    private fun toast(msg: String) = _state.update { it.copy(toast = msg) }
    /** Nasconde il messaggio. */
    fun consumeToast() = _state.update { it.copy(toast = null) }

    /** Toglie dal nome i caratteri vietati nei nomi file e lo accorcia a 60 caratteri. */
    private fun sanitizeFileName(raw: String): String =
        raw.replace(Regex("""[/\\:*?"<>|]"""), "_").take(60).trim().ifBlank { str(R.string.scan_default_name) }

    /** Rilascia il riconoscitore OCR quando il ViewModel viene distrutto. */
    override fun onCleared() {
        ocr.close()
        super.onCleared()
    }
}