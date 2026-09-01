package it.example.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Numerazione dei titoli ripetuti.
 *
 * È la regola che l'utente vede più spesso senza saperlo: salva due volte una
 * bolletta e si aspetta "Bolletta" e "Bolletta (2)". Se si rompe non crasha
 * niente, ci si ritrova solo due righe identiche nell'elenco e nessun modo di
 * sapere quale aprire — un bug silenzioso, quindi proprio quelli che i test
 * servono a prendere.
 */
class TitleNumberingTest {

    /** Comodità: i titoli occupati si confrontano già minuscoli. */
    private fun taken(vararg titles: String) = titles.map { it.lowercase() }.toSet()

    @Test
    fun `un titolo libero resta com'e`() {
        assertEquals("Bolletta", ArchiveRules.uniqueTitle("Bolletta", taken("Contratto")))
    }

    @Test
    fun `il secondo documento con lo stesso nome diventa (2)`() {
        assertEquals("Bolletta (2)", ArchiveRules.uniqueTitle("Bolletta", taken("Bolletta")))
    }

    @Test
    fun `il numero sale finche non trova un posto libero`() {
        val occupati = taken("Bolletta", "Bolletta (2)", "Bolletta (3)")
        assertEquals("Bolletta (4)", ArchiveRules.uniqueTitle("Bolletta", occupati))
    }

    @Test
    fun `il buco in mezzo viene riempito`() {
        // "(2)" è stato eliminato: il posto è libero e va riusato, non saltato.
        val occupati = taken("Bolletta", "Bolletta (3)")
        assertEquals("Bolletta (2)", ArchiveRules.uniqueTitle("Bolletta", occupati))
    }

    @Test
    fun `il confronto ignora le maiuscole`() {
        // "bolletta" e "Bolletta" sono lo stesso nome per chiunque tranne che
        // per un equals.
        assertEquals("BOLLETTA (2)", ArchiveRules.uniqueTitle("BOLLETTA", taken("bolletta")))
    }

    @Test
    fun `duplicare un nome gia numerato non impila i suffissi`() {
        // Da "Bolletta (3)" deve nascere "Bolletta (4)", non "Bolletta (3) (2)":
        // altrimenti dopo qualche copia il nome diventa illeggibile.
        val occupati = taken("Bolletta", "Bolletta (2)", "Bolletta (3)")
        assertEquals("Bolletta (4)", ArchiveRules.uniqueTitle("Bolletta (3)", occupati))
    }

    @Test
    fun `un titolo fatto solo di suffisso non sparisce`() {
        // Togliendo "(2)" da "(2)" resterebbe la stringa vuota: il documento
        // finirebbe senza nome. In quel caso si tiene il titolo intero.
        assertEquals("(2) (2)", ArchiveRules.uniqueTitle("(2)", taken("(2)")))
    }

    @Test
    fun `i titoli di altre cartelle non entrano nel conto`() {
        val records = listOf(
            record(id = "1", title = "Bolletta", folderId = "casa"),
            record(id = "2", title = "Bolletta", folderId = "ufficio"),
        )

        // Una cartella non vede i nomi dell'altra: lì un nome ripetuto è
        // normale, come in due directory qualsiasi.
        val occupati = ArchiveRules.takenTitles(records, folderId = "casa")
        assertEquals(setOf("bolletta"), occupati)
    }

    @Test
    fun `rinominare un documento non lo mette in conflitto con se stesso`() {
        val records = listOf(record(id = "1", title = "Bolletta", folderId = "casa"))

        // Senza exceptId, correggere solo le maiuscole di "Bolletta"
        // produrrebbe "Bolletta (2)".
        val occupati = ArchiveRules.takenTitles(records, "casa", exceptId = "1")
        assertEquals("Bolletta", ArchiveRules.uniqueTitle("Bolletta", occupati))
    }

    @Test
    fun `gli spazi ai bordi non creano un titolo diverso`() {
        val records = listOf(record(id = "1", title = "  Bolletta  ", folderId = "casa"))
        assertEquals(setOf("bolletta"), ArchiveRules.takenTitles(records, "casa"))
    }

    private fun record(id: String, title: String, folderId: String) = DocumentRecord(
        id = id,
        title = title,
        folderId = folderId,
        createdAtEpochMs = 0L,
    )
}
