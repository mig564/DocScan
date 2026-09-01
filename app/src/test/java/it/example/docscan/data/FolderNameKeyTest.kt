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

    /** Al posto delle risorse: i nomi italiani con cui l'archivio era nato. */
    private val originalName: (String) -> String = { key ->
        when (key) {
            "folder_documents" -> "Documenti e moduli"
            "folder_cards" -> "Carte"
            "folder_receipts" -> "Ricevute"
            "folder_invoices" -> "Fatture"
            "folder_contracts" -> "Contratti"
            else -> "Da ordinare"
        }
    }

    private fun migrate(stored: List<Folder>) =
        ArchiveRules.migrateFolders(stored, originalName)

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
        val once = migrate(listOf(Folder(DocumentRepository.FOLDER_UNSORTED, "Da ordinare", 0)))
        assertEquals(once, migrate(once))
    }

    @Test
    fun `ogni cartella predefinita e coperta dalla migrazione`() {
        // Regressione: le predefinite e le chiavi della migrazione erano due
        // elenchi scritti a mano, e il secondo era rimasto indietro di due
        // cartelle. Quelle due sarebbero restate in italiano per sempre, senza
        // che niente lo segnalasse. Ora le chiavi si ricavano dalle
        // predefinite; questo test è la rete che tiene ferma quella scelta.
        val defaults = ArchiveRules.defaultFolders()

        for (folder in defaults) {
            assertEquals(
                "la cartella ${folder.id} non è nella mappa della migrazione",
                folder.nameKey,
                ArchiveRules.defaultKeys[folder.id],
            )
        }

        // Un archivio vecchio: stessi id, nomi italiani, nessuna chiave.
        val vecchio = defaults.map {
            Folder(it.id, originalName(it.nameKey!!), it.order)
        }
        assertEquals(defaults, migrate(vecchio))
    }
}
