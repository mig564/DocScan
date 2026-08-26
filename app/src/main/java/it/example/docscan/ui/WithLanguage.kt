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
 * Con [AppLanguage.SYSTEM] non tocca nulla e resta la lingua del telefono, che
 * è quello che serve al primo avvio.
 */
@Composable
fun WithLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val tag = language.tag
    if (tag == null) {
        content()
        return
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localized = remember(tag, configuration) {
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
    ) {
        content()
    }
}
