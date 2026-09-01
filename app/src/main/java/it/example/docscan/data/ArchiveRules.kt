package it.example.docscan.data

/**
 * Le decisioni sull'archivio che non toccano il disco né le risorse.
 *
 * [DocumentRepository] resta il guscio che legge e scrive file cifrati; qui
 * dentro stanno le regole — quale titolo è libero, cosa risponde una ricerca,
 * quale cartella può prendere una chiave — che sono pura logica su liste.
 *
 * La separazione non è estetica. Senza Context e senza filesystem queste
 * funzioni si eseguono davvero in un test sulla JVM, mentre finché stavano
 * dentro il repository l'unico modo di provarle era ricopiarle nel test e
 * verificare la fotocopia. Una fotocopia non fallisce quando l'originale
 * cambia: era già successo, e il test continuava a passare.
 */
object ArchiveRules {

    /** Il "(2)" finale aggiunto per distinguere due nomi uguali. */
    private val COPY_SUFFIX = Regex("""\s*\(\d+\)$""")

    // ------------------------------------------------------------- Cartelle

    /** Cartelle create al primo avvio. */
    fun defaultFolders(): List<Folder> = listOf(
        Folder("documenti", "", 0, "folder_documents"),
        Folder("carte", "", 1, "folder_cards"),
        Folder("ricevute", "", 2, "folder_receipts"),
        Folder("fatture", "", 3, "folder_invoices"),
        Folder("contratti", "", 4, "folder_contracts"),
        Folder(DocumentRepository.FOLDER_UNSORTED, "", 5, "unsorted"),
    )

    /**
     * Chiave attesa per ogni cartella predefinita, usata dalla migrazione.
     *
     * Derivata da [defaultFolders] invece di essere riscritta a mano: erano due
     * elenchi della stessa cosa, e aggiungere una cartella predefinita
     * dimenticando la seconda lista lasciava quella cartella fuori dalla
     * migrazione, quindi col nome italiano congelato per sempre.
     */
    val defaultKeys: Map<String, String> =
        defaultFolders().mapNotNull { folder -> folder.nameKey?.let { folder.id to it } }.toMap()

    /**
     * Assegna la chiave alle cartelle predefinite che non ce l'hanno ancora.
     *
     * Gli archivi creati prima della traduzione hanno il nome scritto in
     * italiano e nessuna chiave. Se il nome coincide ancora con quello
     * predefinito la cartella non è mai stata rinominata, quindi riceve la
     * chiave e da lì in poi si traduce; se è stato cambiato è dell'utente e
     * resta esattamente com'è.
     *
     * @param originalName da chiave a nome predefinito nella lingua corrente
     */
    fun migrateFolders(
        stored: List<Folder>,
        originalName: (key: String) -> String,
    ): List<Folder> = stored.map { folder ->
        val key = defaultKeys[folder.id]
        when {
            folder.nameKey != null || key == null -> folder
            folder.name.isBlank() || folder.name == originalName(key) ->
                folder.copy(name = "", nameKey = key)
            else -> folder
        }
    }

    /**
     * Confronto senza distinzione di maiuscole e con gli spazi tolti: "Fatture"
     * e "fatture " sono la stessa cartella per un utente, e permetterle
     * entrambe significa creare due posti indistinguibili dove cercare la
     * stessa cosa.
     *
     * @param exceptId cartella da ignorare, per la rinomina: altrimenti una
     *   cartella risulterebbe in conflitto con se stessa e correggere solo le
     *   maiuscole sarebbe impossibile.
     * @param displayName nome mostrato, tradotto per le predefinite
     */
    fun folderNameTaken(
        name: String,
        folders: List<Folder>,
        exceptId: String? = null,
        displayName: (Folder) -> String,
    ): Boolean {
        val target = name.trim()
        return folders.any {
            it.id != exceptId && displayName(it).trim().equals(target, ignoreCase = true)
        }
    }

    // ------------------------------------------------------------ Documenti

    /**
     * Titoli già usati nella cartella, normalizzati per il confronto.
     *
     * @param exceptId documento da non considerare, per la rinomina
     */
    fun takenTitles(
        records: List<DocumentRecord>,
        folderId: String,
        exceptId: String? = null,
    ): Set<String> = records
        .filter { it.folderId == folderId && it.id != exceptId }
        .map { it.title.trim().lowercase() }
        .toSet()

    /**
     * Titolo libero, numerato se serve.
     *
     * Due documenti con lo stesso nome nella stessa cartella non sono un
     * conflitto tecnico — sul disco ognuno è un UUID — ma per chi guarda
     * l'elenco sì: due righe identiche, e nessun modo di sapere quale aprire.
     * Quindi al secondo si aggiunge un numero, come fa qualunque gestore di
     * file: "Bolletta", "Bolletta (2)", "Bolletta (3)".
     *
     * @param base titolo desiderato, già ripulito e non vuoto
     * @param taken titoli occupati, da [takenTitles]
     */
    fun uniqueTitle(base: String, taken: Set<String>): String {
        if (base.lowercase() !in taken) return base

        // Se il nome finisce già per "(3)" si riparte dalla radice, così da
        // "Bolletta (3)" nasce "Bolletta (4)" e non "Bolletta (3) (2)".
        val root = base.replace(COPY_SUFFIX, "").trim().ifBlank { base }
        var n = 2
        while ("$root ($n)".lowercase() in taken) n++
        return "$root ($n)"
    }

    /** Ricerca full-text sul testo OCR conservato, più titolo e campi. */
    fun search(records: List<DocumentRecord>, query: String): List<DocumentRecord> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return records
        return records.filter { r ->
            r.title.lowercase().contains(q) ||
                    r.searchText.lowercase().contains(q) ||
                    r.fields.any {
                        it.value.lowercase().contains(q) || it.label.lowercase().contains(q)
                    }
        }
    }
}
