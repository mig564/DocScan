package it.example.docscan.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unicità dei nomi delle cartelle.
 *
 * Il confronto ignora maiuscole e spazi ai bordi: "Fatture" e "fatture " sono la
 * stessa cartella per un utente, e permetterle entrambe crea due posti
 * indistinguibili dove cercare la stessa cosa.
 */
class FolderNameTest {

    private val folders = listOf(
        Folder("a", "Fatture", 0),
        Folder("b", "Ricevute", 1),
    )

    /** Stessa regola applicata da DocumentRepository e dal ViewModel. */
    private fun taken(name: String, exceptId: String? = null): Boolean {
        val target = name.trim().lowercase()
        return folders.any { it.id != exceptId && it.name.trim().lowercase() == target }
    }

    @Test
    fun `un nome nuovo e accettato`() {
        assertFalse(taken("Contratti"))
    }

    @Test
    fun `i duplicati sono respinti a prescindere da maiuscole e spazi`() {
        assertTrue(taken("Fatture"))
        assertTrue(taken("fatture"))
        assertTrue(taken("  FATTURE  "))
    }

    @Test
    fun `rinominare una cartella con il proprio nome e permesso`() {
        // Altrimenti correggere solo le maiuscole sarebbe impossibile.
        assertFalse(taken("Fatture", exceptId = "a"))
        assertFalse(taken("FATTURE", exceptId = "a"))
    }

    @Test
    fun `rinominare verso il nome di un'altra cartella e respinto`() {
        assertTrue(taken("Ricevute", exceptId = "a"))
    }
}
