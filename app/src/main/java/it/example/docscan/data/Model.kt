package it.example.docscan.data

import kotlinx.serialization.Serializable

/** Visual category of a document. Decides which placeholder preview is drawn. */
enum class DocKind { FORM, RECEIPT }

/**
 * Un campo letto dal documento scansionato.
 *
 * [confidence] non è il punteggio grezzo di ML Kit: dice quanto il valore è
 * stato verificato (checksum del codice fiscale, cifra MRZ, IBAN mod-97). Un
 * valore basso accende il badge "da verificare".
 */
@Serializable
data class ExtractedField(
    val label: String,
    val value: String,
    val confidence: Float = 1f,
) {
    val needsReview: Boolean get() = confidence < 0.8f
    val confidencePercent: Int get() = (confidence * 100).toInt()
}

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val order: Int,
)

@Serializable
data class DocumentRecord(
    val id: String,
    val title: String,
    val folderId: String,
    val createdAtEpochMs: Long,
    val kind: DocKind = DocKind.FORM,
    val pageFiles: List<String> = emptyList(),
    val pdfFile: String? = null,
    val fields: List<ExtractedField> = emptyList(),
    /**
     * Testo OCR completo, serve alla ricerca dentro le scansioni.
     *
     * È cifrato come il resto, ma resta il dato più sensibile dell'archivio:
     * contiene tutto ciò che era stampato. I numeri di carta sono già mascherati.
     * Se un giorno si toglie la ricerca, va tolto anche questo campo.
     */
    val searchText: String = "",
    /** Serve a rigenerare il PDF con la stessa geometria dopo una modifica. */
    val scanMode: ScanMode = ScanMode.DOCUMENT,
    val fitMode: FitMode = FitMode.TRUE_SCALE,
) {
    val metaFile: String get() = "$id.meta.enc"
    val pageCount: Int get() = pageFiles.size.coerceAtLeast(1)

    /**
     * Una tessera composta su A4 è un foglio, non due pagine. Contare le due
     * immagini acquisite farebbe dire all'app "2 pagine", come se la
     * composizione non fosse avvenuta.
     */
    val isSheet: Boolean get() = scanMode.isTwoSided

    val pageLabel: String
        get() = when {
            isSheet -> "1 foglio A4 · fronte e retro"
            pageCount == 1 -> "1 pagina"
            else -> "$pageCount pagine"
        }

    val needsReviewCount: Int get() = fields.count { it.needsReview }
}
