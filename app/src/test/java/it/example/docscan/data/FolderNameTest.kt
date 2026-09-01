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
 *
 * Il nome mostrato arriva da fuori perché le cartelle predefinite si traducono e
 * la traduzione richiede il Context di Android, che qui non c'è. Il test passa
 * una funzione finta: quello che verifica è la regola di confronto, non le
 * stringhe tradotte.
 */
class FolderNameTest {

    private val folders = listOf(
        Folder("a", "Fatture", 0),
        Folder("b", "Ricevute", 1),
        Folder("carte", "", 2, nameKey = "folder_cards"),
    )

    /** Al posto delle risorse: le predefinite hanno il nome della loro chiave. */
    private val displayName: (Folder) -> String = { folder ->
        when (folder.nameKey) {
            "folder_cards" -> "Carte"
            else -> folder.name
        }
    }

    private fun taken(name: String, exceptId: String? = null) =
        ArchiveRules.folderNameTaken(name, folders, exceptId, displayName)

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
    fun `il confronto usa il nome tradotto delle predefinite`() {
        // La cartella "carte" ha il campo name vuoto: senza passare dal nome
        // mostrato, "Carte" sembrerebbe libero e nascerebbe un doppione.
        assertTrue(taken("Carte"))
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
