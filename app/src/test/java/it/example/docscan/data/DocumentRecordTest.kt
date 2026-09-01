package it.example.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le proprietà calcolate del modello.
 *
 * Sono poche righe, ma decidono due cose che l'utente vede subito: se accanto a
 * un campo compare il badge "da verificare", e se un documento si conta in
 * pagine o in fogli. Nessuna delle due fallisce rumorosamente — semplicemente
 * l'app dice la cosa sbagliata con la faccia di chi ha ragione.
 */
class DocumentRecordTest {

    // ------------------------------------------------------- Campi estratti

    @Test
    fun `un valore verificato non chiede conferma`() {
        // Codice fiscale con checksum giusto, IBAN che passa il mod-97: 1f.
        assertFalse(ExtractedField("field_iban", "IT60X0542811101000000123456").needsReview)
    }

    @Test
    fun `sotto la soglia il campo chiede conferma`() {
        assertTrue(ExtractedField("field_total", "€ 84,20", confidence = 0.5f).needsReview)
    }

    @Test
    fun `la soglia e inclusiva a 0,8`() {
        // Il confine è dove si sbaglia: 0.8 esatto è considerato affidabile,
        // appena sotto no. Spostarlo cambia quanti badge vede l'utente.
        assertFalse(ExtractedField("x", "y", confidence = 0.8f).needsReview)
        assertTrue(ExtractedField("x", "y", confidence = 0.79f).needsReview)
    }

    @Test
    fun `il conteggio dei campi da verificare guarda solo quelli incerti`() {
        val record = record(
            fields = listOf(
                ExtractedField("field_tax_code", "RSSMRA85T10A562S"),
                ExtractedField("field_total", "€ 84,20", confidence = 0.5f),
                ExtractedField("field_issue_date", "18/07/2026", confidence = 0.6f),
            ),
        )
        assertEquals(2, record.needsReviewCount)
    }

    // ------------------------------------------------------------ Documento

    @Test
    fun `un documento normale si conta in pagine`() {
        val record = record(
            scanMode = ScanMode.DOCUMENT,
            pageFiles = listOf("a.p0.enc", "a.p1.enc", "a.p2.enc"),
        )
        assertFalse(record.isSheet)
        assertEquals(3, record.pageCount)
    }

    @Test
    fun `una tessera a due facciate e un foglio solo`() {
        // Fronte e retro sono due immagini ma un A4 stampato: chiamarle
        // "2 pagine" descriverebbe la scansione invece del risultato.
        val tessera = record(
            scanMode = ScanMode.CARD,
            pageFiles = listOf("a.p0.enc", "a.p1.enc"),
        )
        assertTrue(tessera.isSheet)
        assertTrue(record(scanMode = ScanMode.PASSPORT).isSheet)
    }

    @Test
    fun `un documento senza pagine si conta comunque come una`() {
        // Un elenco vuoto non deve diventare "0 pagine" a schermo.
        assertEquals(1, record(pageFiles = emptyList()).pageCount)
    }

    @Test
    fun `il nome del file dei metadati deriva dall'id`() {
        // Serve a purgeUnreadable, che ricava i blob collegati dal prefisso:
        // cambiare questo schema lascerebbe file orfani non cancellabili.
        assertEquals("abc-123.meta.enc", record(id = "abc-123").metaFile)
    }

    @Test
    fun `solo le modalita a due facciate compongono un A4`() {
        // isSheet si appoggia qui: se una modalità prendesse un formato per
        // sbaglio, i suoi PDF cambierebbero geometria senza altri sintomi.
        assertNull(ScanMode.DOCUMENT.format)
        assertEquals(CardFormat.ID_1, ScanMode.CARD.format)
        assertEquals(CardFormat.ID_3, ScanMode.PASSPORT.format)
        assertFalse(ScanMode.DOCUMENT.isTwoSided)
    }

    private fun record(
        id: String = "1",
        scanMode: ScanMode = ScanMode.DOCUMENT,
        pageFiles: List<String> = listOf("1.p0.enc"),
        fields: List<ExtractedField> = emptyList(),
    ) = DocumentRecord(
        id = id,
        title = "Documento",
        folderId = "documenti",
        createdAtEpochMs = 0L,
        scanMode = scanMode,
        pageFiles = pageFiles,
        fields = fields,
    )
}
