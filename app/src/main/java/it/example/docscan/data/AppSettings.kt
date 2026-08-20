package it.example.docscan.data

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * Impostazioni dell'app.
 *
 * [defaultFolderUri] non è un percorso: da Android 10 lo storage è isolato e un
 * percorso non sarebbe scrivibile. È un URI SAF su cui l'utente ha concesso un
 * permesso persistente scegliendo la cartella una volta.
 * [defaultFolderLabel] è la versione leggibile da mostrare.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultFolderUri: String? = null,
    val defaultFolderLabel: String? = null,
)

/**
 * Le impostazioni stanno in SharedPreferences, non nell'archivio cifrato: non
 * sono contenuto dei documenti, e il tema va letto in modo sincrono prima della
 * prima composizione, altrimenti l'app lampeggia in chiaro per un frame.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("docscan_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_FOLDER_URI = "default_folder_uri"
        private const val KEY_FOLDER_LABEL = "default_folder_label"
    }

    /**
     * Legge le impostazioni salvate, con i valori predefiniti dove mancano.
     *
     * Sincrona di proposito: il tema serve prima del primo frame. Se un valore
     * salvato non corrisponde più a nessuna costante — è successo un
     * aggiornamento che ne ha rinominata una — si torna al predefinito invece di
     * far crashare l'avvio.
     */
    fun load(): AppSettings {
        val themeName = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeName ?: "") }
                .getOrDefault(ThemeMode.SYSTEM),
            defaultFolderUri = prefs.getString(KEY_FOLDER_URI, null),
            defaultFolderLabel = prefs.getString(KEY_FOLDER_LABEL, null),
        )
    }

    /** Scrive le impostazioni su disco. */
    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.themeMode.name)
            putString(KEY_FOLDER_URI, settings.defaultFolderUri)
            putString(KEY_FOLDER_LABEL, settings.defaultFolderLabel)
        }
    }

    /** Da `primary:Download/Scansioni` a `Download > Scansioni`. */
    fun prettyLabel(treeDocumentId: String?): String {
        if (treeDocumentId.isNullOrBlank()) return "Nessuna cartella scelta"
        val path = treeDocumentId.substringAfter(':', treeDocumentId)
        if (path.isBlank()) return "Memoria interna"
        return path.split('/').filter { it.isNotBlank() }.joinToString(" › ")
    }
}
