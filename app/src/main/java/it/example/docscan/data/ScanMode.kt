package it.example.docscan.data

/**
 * Come viene composto il PDF.
 *
 * Tre modalità e non sei: carta d'identità, tessera sanitaria, patente e
 * bancomat hanno la stessa forma ID-1 e danno lo stesso foglio, quindi sarebbero
 * quattro pulsanti identici. Quale sia lo dice l'OCR: una MRZ è una CIE, un
 * codice fiscale è una tessera sanitaria.
 *
 * Il passaporto ha un preset suo perché è l'unico con forma diversa: ID-3.
 */
enum class ScanMode(
    val label: String,
    val description: String,
    val format: CardFormat?,
) {
    DOCUMENT(
        label = "Documento",
        description = "Una o più pagine in un unico PDF",
        format = null,
    ),
    CARD(
        label = "Tessera",
        description = "Identità, sanitaria, patente, bancomat — fronte e retro su A4 in scala reale",
        format = CardFormat.ID_1,
    ),
    PASSPORT(
        label = "Passaporto",
        description = "Pagina dati e pagina firma su A4 in scala reale",
        format = CardFormat.ID_3,
    );

    /** Le modalità a due facciate compongono un A4, il documento normale no. */
    val isTwoSided: Boolean get() = format != null
}

/**
 * Misure fisiche in millimetri, secondo ISO/IEC 7810.
 *
 * Si parte dai millimetri e non dai pixel: il PDF è scritto in punti PostScript
 * (1 mm = 72/25,4 pt) e la scala 1:1 sopravvive alla stampa. Un JPEG non ha
 * unità fisiche e la scala si perde alla prima stampa.
 */
enum class CardFormat(
    val label: String,
    val widthMm: Float,
    val heightMm: Float,
) {
    /** Carta d'identità elettronica, tessera sanitaria, patente, carte bancarie. */
    ID_1("ID-1", 85.60f, 53.98f),

    /** Pagina dati del passaporto, che è un libretto e non una tessera. */
    ID_3("ID-3", 125.00f, 88.00f),
}

/** Come sistemare l'immagine acquisita dentro il rettangolo della carta. */
enum class FitMode(val label: String, val description: String) {
    /**
     * L'immagine entra nel rettangolo esatto della carta mantenendo le
     * proporzioni. Un ritaglio impreciso lascia margini bianchi invece di
     * stirare la carta e falsare la misura stampata.
     */
    TRUE_SCALE("Scala reale", "Misurabile col righello, 1:1"),

    /** L'immagine riempie il rettangolo e l'eccesso viene ritagliato. Scala non garantita. */
    FILL("Adatta al foglio", "Riempie il riquadro, scala non garantita"),
}
