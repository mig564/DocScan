package it.example.docscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.remember
import it.example.docscan.data.AccentColor
import it.example.docscan.data.CardStyle
import it.example.docscan.data.ThemeMode

/**
 * Gli schemi Material si costruiscono ora a partire dall'accento scelto.
 *
 * Erano due costanti perché il colore era uno solo. Con quattro accenti
 * diventerebbero otto costanti, quindi li si costruisce al volo e si ricordano:
 * l'utente cambia colore una volta ogni tanto, non a ogni fotogramma.
 */
private fun schemeFor(accent: AccentColor, dark: Boolean): ColorScheme {
    val t = tonesFor(accent, dark)
    return if (dark) {
        darkColorScheme(
            primary = t.base,
            onPrimary = t.onBase,
            primaryContainer = t.container,
            onPrimaryContainer = t.onContainer,
            background = DarkRaw.surface,
            onBackground = DarkRaw.onSurface,
            surface = DarkRaw.surface,
            onSurface = DarkRaw.onSurface,
            surfaceVariant = DarkRaw.surfaceContainer,
            onSurfaceVariant = DarkRaw.onSurfaceVariant,
            outline = DarkRaw.outline,
            outlineVariant = DarkRaw.outlineSoft,
            error = DarkRaw.dangerText,
            onError = Color(0xFF2B0C05),
            errorContainer = DarkRaw.dangerContainer,
            onErrorContainer = DarkRaw.dangerText,
        )
    } else {
        lightColorScheme(
            primary = t.base,
            onPrimary = t.onBase,
            primaryContainer = t.container,
            onPrimaryContainer = t.onContainer,
            background = LightRaw.surface,
            onBackground = LightRaw.onSurface,
            surface = LightRaw.surface,
            onSurface = LightRaw.onSurface,
            surfaceVariant = LightRaw.surfaceContainer,
            onSurfaceVariant = LightRaw.onSurfaceVariant,
            outline = LightRaw.outline,
            outlineVariant = LightRaw.outlineSoft,
            error = LightRaw.dangerText,
            onError = Color.White,
            errorContainer = LightRaw.dangerContainer,
            onErrorContainer = LightRaw.dangerText,
        )
    }
}

// Roboto è il font di sistema Android, non serve impacchettarlo. Per i valori
// monospace uso FontFamily.Monospace invece di Roboto Mono: evita di dipendere
// da un .ttf negli asset, che è una fonte classica di build rotte.
private val DocScanTypography = Typography(
    headlineMedium = TextStyle(fontSize = TextDisplay, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = TextTitle, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontSize = TextSubtitle, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = TextBody, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = TextBody, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = TextBody, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = TextLabel, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(
        fontSize = TextMeta,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
    ),
)

/**
 * [ThemeMode.SYSTEM] delega a [isSystemInDarkTheme], quindi l'app segue anche i
 * cambi automatici del telefono (pianificazione notturna, risparmio energetico)
 * senza bisogno di riavvio.
 */
@Composable
fun DocScanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.BLUE,
    cardStyle: CardStyle = CardStyle.ROUNDED,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Senza questo, in tema scuro le icone di sistema restano scure su fondo
    // scuro (e viceversa): l'ora e la batteria spariscono. Va impostato in modo
    // dinamico, perché il tema può cambiare senza riavviare l'Activity.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val scheme = remember(accent, dark) { schemeFor(accent, dark) }

    CompositionLocalProvider(
        LocalDarkTheme provides dark,
        LocalAccent provides accent,
        LocalCardStyle provides cardStyle,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DocScanTypography,
            content = content,
        )
    }
}
