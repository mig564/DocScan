package it.example.docscan.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import it.example.docscan.data.AppLanguage
import java.util.Locale

/**
 * Applica la lingua scelta a tutto ciò che sta dentro.
 *
 * Costruisce un Context con la lingua richiesta e lo mette al posto di quello
 * corrente: `stringResource` legge da lì, quindi ogni testo cambia senza che
 * nessuna schermata debba saperlo.
 *
 * Con [AppLanguage.SYSTEM] fornisce il Context di partenza, cioè non cambia
 * niente, ma lo fa passando dallo stesso identico punto del codice.
 *
 * Questo dettaglio conta più di quanto sembri. Prima c'era un ritorno
 * anticipato per il caso "Sistema", con una seconda chiamata a `content()`
 * fuori dal provider. Per Compose quelle sono due posizioni diverse nell'albero:
 * passando da una lingua fissa a "Sistema" o viceversa, tutto il sottoalbero
 * veniva buttato e ricostruito da zero, perdendo ogni `remember`. Si vedeva
 * come uno sfarfallio — la schermata impostazioni spariva e rientrava rifacendo
 * la sua animazione — ma la stessa cosa azzerava anche scorrimento, testo nella
 * barra di ricerca e qualunque altro stato locale. Con una sola chiamata a
 * `content()` cambia solo il valore fornito, e lo stato resta dov'è.
 */
@Composable
fun WithLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val tag = language.tag

    val localized = remember(tag, configuration, context) {
        if (tag == null) {
            null
        } else {
            val locale = Locale.forLanguageTag(tag)
            val config = Configuration(configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            context.createConfigurationContext(config)
        }
    }

    CompositionLocalProvider(
        LocalContext provides (localized ?: context),
        LocalConfiguration provides (localized?.resources?.configuration ?: configuration),
    ) {
        content()
    }
}
