package it.example.docscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Unico punto di accesso all'archivio: tutto ciò che tocca il disco passa da
 * qui.
 *
 * Layout in filesDir/archivio, tutto cifrato AES-GCM:
 *   folders.enc     elenco delle cartelle
 *   {id}.meta.enc   metadati del documento
 *   {id}.p0.enc     JPEG pagina 1
 *   {id}.pdf.enc    PDF prodotto dallo scanner
 */
class DocumentRepository(private val context: Context) {

    private val store = SecureStore(context)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val META_SUFFIX = ".meta.enc"
        private const val FOLDERS_FILE = "folders.enc"
        const val FOLDER_UNSORTED = "da-ordinare"
        private const val SHARE_DIR = "condivisi"
    }

    // ------------------------------------------------------------- Cartelle

    /** Cartelle create al primo avvio. */
    private fun defaultFolders() = listOf(
        Folder("documenti", "Documenti e moduli", 0),
        Folder("carte", "Carte", 1),
        Folder("ricevute", "Ricevute", 2),
        Folder("fatture", "Fatture", 3),
        Folder("contratti", "Contratti", 4),
        Folder(FOLDER_UNSORTED, "Da ordinare", 5),
    )

    /**
     * Elenco delle cartelle, in ordine. Al primo avvio crea quelle predefinite.
     *
     * Se il file è illeggibile ricade sulle predefinite invece di propagare
     * l'errore: senza cartelle l'app non saprebbe dove salvare nulla.
     */
    suspend fun folders(): List<Folder> = withContext(Dispatchers.IO) {
        if (!store.exists(FOLDERS_FILE)) {
            val defaults = defaultFolders()
            saveFolders(defaults)
            return@withContext defaults
        }
        runCatching {
            json.decodeFromString<List<Folder>>(String(store.read(FOLDERS_FILE)))
        }.getOrElse { defaultFolders() }.sortedBy { it.order }
    }

    /** Salva l'elenco rinumerando le posizioni. */
    suspend fun saveFolders(folders: List<Folder>) = withContext(Dispatchers.IO) {
        val normalized = folders.mapIndexed { i, f -> f.copy(order = i) }
        store.write(FOLDERS_FILE, json.encodeToString(normalized).toByteArray())
    }

    /**
     * Confronto senza distinzione di maiuscole e con gli spazi tolti: "Fatture"
     * e "fatture " sono la stessa cartella per un utente, e permetterle entrambe
     * significa creare due posti indistinguibili dove cercare la stessa cosa.
     */
    suspend fun folderNameTaken(name: String, exceptId: String? = null): Boolean {
        val target = name.trim().lowercase()
        return folders().any { it.id != exceptId && it.name.trim().lowercase() == target }
    }

    /** Null se il nome è vuoto o già in uso. */
    suspend fun addFolder(name: String): List<Folder>? {
        val clean = name.trim()
        if (clean.isBlank() || folderNameTaken(clean)) return null
        val current = folders()
        val next = current + Folder(UUID.randomUUID().toString(), clean, current.size)
        saveFolders(next)
        return next
    }

    /**
     * Elimina la cartella e sposta i suoi documenti in "Da ordinare".
     * Cancellare in silenzio le scansioni contenute sarebbe la scelta peggiore:
     * l'utente sta riordinando, non buttando via.
     */
    suspend fun deleteFolder(folderId: String): List<Folder> {
        if (folderId == FOLDER_UNSORTED) return folders()
        records().filter { it.folderId == folderId }.forEach {
            updateRecord(it.copy(folderId = FOLDER_UNSORTED))
        }
        val next = folders().filterNot { it.id == folderId }
        saveFolders(next)
        return next
    }

    /** Rinomina conservando la posizione: l'ordine non deve cambiare da solo. */
    /** Null se il nome è vuoto o già usato da un'altra cartella. */
    suspend fun renameFolder(folderId: String, newName: String): List<Folder>? {
        val clean = newName.trim()
        if (clean.isBlank() || folderNameTaken(clean, exceptId = folderId)) return null
        val next = folders().map { if (it.id == folderId) it.copy(name = clean) else it }
        saveFolders(next)
        return next
    }

    /** Sposta una cartella di una posizione. Se è già in cima o in fondo, non cambia nulla. */
    suspend fun moveFolder(folderId: String, delta: Int): List<Folder> {
        val current = folders().toMutableList()
        val i = current.indexOfFirst { it.id == folderId }
        val target = i + delta
        if (i < 0 || target !in current.indices) return current
        current.add(target, current.removeAt(i))
        saveFolders(current)
        return current
    }

    // ------------------------------------------------------------ Documenti

    /**
     * Documenti leggibili e file che non si riesce a decifrare o interpretare.
     *
     * Scartare i falliti in silenzio fa apparire l'archivio vuoto senza dire
     * perché: "non c'è" e "non riesco a leggerlo" sono due cose diverse.
     */
    data class ArchiveContents(
        val records: List<DocumentRecord>,
        val unreadable: List<String>,
    )

    /** Legge tutto l'archivio, separando i documenti leggibili dai file rotti. */
    suspend fun loadArchive(): ArchiveContents = withContext(Dispatchers.IO) {
        val readable = mutableListOf<DocumentRecord>()
        val broken = mutableListOf<String>()

        store.list()
            .filter { it.endsWith(META_SUFFIX) }
            .forEach { name ->
                val record = runCatching {
                    json.decodeFromString<DocumentRecord>(String(store.read(name)))
                }.getOrNull()
                if (record != null) readable += record else broken += name
            }

        ArchiveContents(
            records = readable.sortedByDescending { it.createdAtEpochMs },
            unreadable = broken,
        )
    }

    /** Solo i documenti leggibili. */
    suspend fun records(): List<DocumentRecord> = loadArchive().records

    /**
     * Rimuove i file illeggibili e i blob collegati, dedotti dal prefisso: i
     * metadati che li elencavano sono proprio quelli che non si leggono.
     */
    suspend fun purgeUnreadable(metaFiles: List<String>): Int = withContext(Dispatchers.IO) {
        var removed = 0
        for (meta in metaFiles) {
            val id = meta.removeSuffix(META_SUFFIX)
            store.list().filter { it.startsWith("$id.") }.forEach {
                if (store.delete(it)) removed++
            }
        }
        removed
    }

    /**
     * Salva pagine, PDF e metadati come un'unica unità. Se fallisce a metà
     * ripulisce: meglio nessun documento che una scansione orfana senza
     * metadati, invisibile nella UI ma presente su disco.
     */
    suspend fun save(
        title: String,
        folderId: String,
        pageUris: List<Uri>,
        fields: List<ExtractedField>,
        searchText: String,
        kind: DocKind,
        scanMode: ScanMode = ScanMode.DOCUMENT,
        fitMode: FitMode = FitMode.TRUE_SCALE,
    ): DocumentRecord = withContext(Dispatchers.IO) {
        // Il PDF viene generato ora dalle pagine effettive, non ereditato dalla
        // sessione di scansione: così resta allineato anche dopo che l'utente ha
        // eliminato o aggiunto pagine in revisione.
        // Le modalità a due facciate compongono un A4 in scala fisica reale;
        // il documento normale segue l'orientamento delle pagine.
        val format = scanMode.format
        val pdfBytes = if (format != null) {
            // Nessun ripiego su PdfBuilder: se la composizione A4 non riesce
            // meglio nessun PDF che un PDF a due pagine indistinguibile da un
            // documento normale, che è esattamente il bug che nasconderebbe.
            A4Composer.build(context, pageUris, format, fitMode)
        } else {
            PdfBuilder.build(context, pageUris)
        }
        val id = UUID.randomUUID().toString()
        val written = mutableListOf<String>()
        try {
            val pageFiles = pageUris.mapIndexedNotNull { index, uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@mapIndexedNotNull null
                val name = "$id.p$index.enc"
                store.write(name, bytes)
                written += name
                name
            }

            val pdfFile = pdfBytes?.let { bytes ->
                val name = "$id.pdf.enc"
                store.write(name, bytes)
                written += name
                name
            }

            val record = DocumentRecord(
                id = id,
                title = title,
                folderId = folderId,
                createdAtEpochMs = System.currentTimeMillis(),
                kind = kind,
                pageFiles = pageFiles,
                pdfFile = pdfFile,
                fields = fields,
                searchText = searchText,
                scanMode = scanMode,
                fitMode = fitMode,
            )
            store.write(record.metaFile, json.encodeToString(record).toByteArray())
            record
        } catch (e: Exception) {
            written.forEach { runCatching { store.delete(it) } }
            throw e
        }
    }

    /** Riscrive i metadati. Le pagine e il PDF restano quelli, non vengono rigenerati. */
    suspend fun updateRecord(record: DocumentRecord) = withContext(Dispatchers.IO) {
        store.write(record.metaFile, json.encodeToString(record).toByteArray())
        Unit
    }

    /** Sposta un documento in un'altra cartella dopo il salvataggio. */
    suspend fun moveRecord(record: DocumentRecord, folderId: String): DocumentRecord {
        val updated = record.copy(folderId = folderId)
        updateRecord(updated)
        return updated
    }

    /** Cancella metadati, pagine e PDF di un documento. Gli errori sui singoli file sono ignorati. */
    suspend fun deleteRecord(record: DocumentRecord) = withContext(Dispatchers.IO) {
        (record.pageFiles + listOfNotNull(record.pdfFile) + record.metaFile)
            .forEach { runCatching { store.delete(it) } }
    }

    /**
     * Decifra una pagina in un Bitmap in memoria, senza file temporanei in
     * chiaro. È il motivo per cui si salvano anche i JPEG: PdfRenderer pretende
     * un file seekable e costringerebbe a scrivere il PDF decifrato.
     */
    suspend fun pageBitmap(record: DocumentRecord, index: Int = 0): Bitmap? =
        withContext(Dispatchers.IO) {
            val name = record.pageFiles.getOrNull(index) ?: return@withContext null
            runCatching {
                val bytes = store.read(name)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }

    /** Ricerca full-text sul testo OCR conservato, più titolo e campi. */
    fun search(records: List<DocumentRecord>, query: String): List<DocumentRecord> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return records
        return records.filter { r ->
            r.title.lowercase().contains(q) ||
                r.searchText.lowercase().contains(q) ||
                r.fields.any { it.value.lowercase().contains(q) || it.label.lowercase().contains(q) }
        }
    }

    /**
     * Scrive il PDF decifrato su una Uri scelta dall'utente via SAF: la
     * destinazione la sceglie l'utente e non serve alcun permesso runtime.
     */
    suspend fun exportPdfTo(record: DocumentRecord, destination: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val pdfName = record.pdfFile ?: return@withContext false
            runCatching {
                val bytes = store.read(pdfName)
                context.contentResolver.openOutputStream(destination)?.use { it.write(bytes) }
                true
            }.getOrDefault(false)
        }

    /**
     * Scrive il PDF nella cartella predefinita.
     *
     * Usa DocumentsContract invece della libreria documentfile: serve una sola
     * operazione. Restituisce null se il permesso persistente è stato revocato,
     * così il chiamante ricade sul selettore manuale.
     */
    suspend fun exportPdfToTree(
        record: DocumentRecord,
        treeUri: Uri,
        fileName: String,
    ): Uri? = withContext(Dispatchers.IO) {
        val pdfName = record.pdfFile ?: return@withContext null
        runCatching {
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
            val target = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "application/pdf",
                fileName,
            ) ?: return@runCatching null
            context.contentResolver.openOutputStream(target)?.use { it.write(store.read(pdfName)) }
            target
        }.getOrNull()
    }

    /**
     * Prepara il PDF per la condivisione.
     *
     * La copia in chiaro vive in cacheDir/condivisi e viene cancellata al
     * rientro nell'app. Una volta inviata a un'altra app non è più controllabile.
     */
    suspend fun pdfForSharing(record: DocumentRecord): Uri? = withContext(Dispatchers.IO) {
        val pdfName = record.pdfFile ?: return@withContext null
        runCatching {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            // Il nome del file è quello che vedrà il destinatario, quindi usiamo
            // il titolo del documento e non l'UUID interno.
            val safeTitle = record.title.replace(Regex("""[/\\:*?"<>|]"""), "_").ifBlank { "Scansione" }
            val out = File(dir, "$safeTitle.pdf")
            out.writeBytes(store.read(pdfName))
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)
        }.getOrNull()
    }

    /** Da chiamare a ogni rientro nell'app: niente PDF in chiaro dimenticati. */
    fun clearShareCache() {
        File(context.cacheDir, SHARE_DIR).listFiles()?.forEach { it.delete() }
    }
}
