package it.example.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ricerca nell'archivio.
 *
 * La ricerca guarda in tre posti diversi — titolo, testo OCR e campi estratti —
 * e basta che uno smetta di funzionare perché un documento diventi
 * irraggiungibile senza che nulla segnali l'errore. È il motivo per cui il testo
 * OCR viene conservato: se la ricerca dentro le scansioni non serve più, va
 * tolto anche quello.
 */
class SearchTest {

    private val bolletta = record(
        id = "1",
        title = "Bolletta luce",
        searchText = "ENERGIA SPA importo dovuto entro il 30/09",
        fields = listOf(ExtractedField("field_total", "€ 84,20")),
    )

    private val tessera = record(
        id = "2",
        title = "Tessera sanitaria",
        searchText = "SERVIZIO SANITARIO NAZIONALE",
        fields = listOf(ExtractedField("field_tax_code", "RSSMRA85T10A562S")),
    )

    private val archivio = listOf(bolletta, tessera)

    @Test
    fun `trova per titolo`() {
        assertEquals(listOf(bolletta), ArchiveRules.search(archivio, "bolletta"))
    }

    @Test
    fun `trova dentro il testo scansionato`() {
        // Il senso della ricerca è proprio questo: "energia" non compare da
        // nessuna parte nell'interfaccia, solo nella carta fotografata.
        assertEquals(listOf(bolletta), ArchiveRules.search(archivio, "energia"))
    }

    @Test
    fun `trova per valore di un campo estratto`() {
        assertEquals(listOf(tessera), ArchiveRules.search(archivio, "RSSMRA85"))
    }

    @Test
    fun `la ricerca ignora maiuscole e spazi ai bordi`() {
        assertEquals(listOf(bolletta), ArchiveRules.search(archivio, "  BOLLETTA  "))
    }

    @Test
    fun `una query vuota restituisce tutto`() {
        // Non zero risultati: a schermo significherebbe archivio vuoto appena si
        // cancella il testo cercato.
        assertEquals(archivio, ArchiveRules.search(archivio, ""))
        assertEquals(archivio, ArchiveRules.search(archivio, "   "))
    }

    @Test
    fun `una query senza riscontri non restituisce niente`() {
        assertTrue(ArchiveRules.search(archivio, "passaporto").isEmpty())
    }

    @Test
    fun `l'ordine dell'archivio viene mantenuto`() {
        // I documenti arrivano già ordinati per data: la ricerca filtra e basta,
        // non rimescola.
        assertEquals(archivio, ArchiveRules.search(archivio, "a"))
    }

    private fun record(
        id: String,
        title: String,
        searchText: String,
        fields: List<ExtractedField>,
    ) = DocumentRecord(
        id = id,
        title = title,
        folderId = "documenti",
        createdAtEpochMs = 0L,
        searchText = searchText,
        fields = fields,
    )
}
