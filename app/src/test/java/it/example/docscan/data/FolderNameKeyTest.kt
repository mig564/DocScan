package it.example.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Migrazione dei nomi delle cartelle predefinite verso le chiavi.
 *
 * Gli archivi creati prima della traduzione hanno il nome scritto in italiano.
 * Il punto delicato è distinguere una cartella mai toccata, che può prendere la
 * chiave e diventare traducibile, da una rinominata dall'utente, che deve
 * restare esattamente com'è.
 */
class FolderNameKeyTest {

    private val defaultKeys = mapOf(
        "documenti" to "folder_documents",
        "carte" to "folder_cards",
        "fatture" to "folder_invoices",
        "da-ordinare" to "unsorted",
    )

    private val originalNames = mapOf(
        "folder_documents" to "Documenti e moduli",
        "folder_cards" to "Carte",
        "folder_invoices" to "Fatture",
        "unsorted" to "Da ordinare",
    )

    /** Stessa logica di DocumentRepository.folders(). */
    private fun migrate(stored: List<Folder>): List<Folder> = stored.map { folder ->
        val key = defaultKeys[folder.id]
        when {
            folder.nameKey != null || key == null -> folder
            folder.name.isBlank() || folder.name == originalNames[key] ->
                folder.copy(name = "", nameKey = key)
            else -> folder
        }
    }

    @Test
    fun `una cartella predefinita mai rinominata prende la chiave`() {
        val migrated = migrate(listOf(Folder("fatture", "Fatture", 0)))
        assertEquals("folder_invoices", migrated[0].nameKey)
        assertEquals("", migrated[0].name)
    }

    @Test
    fun `una predefinita rinominata resta col nome dell'utente`() {
        val migrated = migrate(listOf(Folder("carte", "Le mie carte", 0)))
        assertNull(migrated[0].nameKey)
        assertEquals("Le mie carte", migrated[0].name)
    }

    @Test
    fun `una cartella creata dall'utente non viene toccata`() {
        val custom = Folder("abc123", "Bollette", 0)
        assertEquals(listOf(custom), migrate(listOf(custom)))
    }

    @Test
    fun `la migrazione si puo ripetere senza effetti`() {
        val once = migrate(listOf(Folder("da-ordinare", "Da ordinare", 0)))
        assertEquals(once, migrate(once))
    }
}
