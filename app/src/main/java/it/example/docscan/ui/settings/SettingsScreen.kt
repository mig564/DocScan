package it.example.docscan.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.example.docscan.R
import it.example.docscan.data.AccentColor
import it.example.docscan.data.AppLanguage
import it.example.docscan.data.CardStyle
import it.example.docscan.data.AppSettings
import it.example.docscan.data.ThemeMode
import it.example.docscan.ui.BackButton
import it.example.docscan.ui.rememberOverlayPhase
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.AccentContainer
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.CornerRound
import it.example.docscan.ui.theme.CornerSmall
import it.example.docscan.ui.theme.OnAccentContainer
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineSoft
import it.example.docscan.ui.theme.PaperSheen
import it.example.docscan.ui.theme.Surface
import it.example.docscan.ui.theme.SurfaceHigh
import it.example.docscan.ui.theme.TextBody
import it.example.docscan.ui.theme.TextLabel
import it.example.docscan.ui.theme.TextMeta
import it.example.docscan.ui.theme.TextSubtitle
import it.example.docscan.ui.theme.accentPreview
import it.example.docscan.ui.theme.SurfaceContainer

/**
 * Schermata impostazioni.
 *
 * Era un pannello a scomparsa, quando le voci erano due. Cresciuta fino a
 * coprire quasi tutto lo schermo, un pannello smette di essere tale: resta con
 * i vincoli del pannello — niente posto per un titolo, scorrimento che litiga
 * con il trascinamento della maniglia — senza più il vantaggio di lasciar
 * vedere cosa c'è sotto.
 *
 * Copre la schermata sottostante con un fondo pieno, quindi non è trasparente e
 * non lascia passare i tocchi. Entra e esce scorrendo da destra, e il tasto
 * indietro di sistema lo intercetta lei per poter animare l'uscita prima di
 * avvisare il ViewModel.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onCardStyleChange: (CardStyle) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onBack: () -> Unit,
) {
    // Stessa meccanica dei pannelli: la chiusura è in due tempi, perché anche
    // qui il chiamante ci mostra con un `if` e ci toglierebbe dalla
    // composizione prima che l'uscita possa vedersi.
    val phase = rememberOverlayPhase(onClosed = onBack)

    // Il back di sistema lo intercettiamo noi, per lo stesso motivo per cui lo
    // intercetta il pannello: passando dal ViewModel salterebbe l'animazione.
    // Questo BackHandler si registra dopo quello dell'Activity e ha quindi la
    // precedenza finché le impostazioni sono aperte.
    BackHandler { phase.close() }

    AnimatedVisibility(
        visibleState = phase.state,
        // Entra da destra e se ne va da destra, come una schermata spinta sopra
        // la libreria: è la direzione che corrisponde alla freccia in alto a
        // sinistra con cui si torna indietro. Breve, come chiedevi: 200 per
        // entrare e 160 per uscire, in linea con il resto dell'app dove
        // chiudere è sempre più rapido di aprire.
        enter = slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { it },
        exit = slideOutHorizontally(tween(160, easing = FastOutLinearInEasing)) { it },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Surface)
                // Le altre schermate stanno dentro il Box di MainActivity che
                // applica il padding delle barre di sistema; questa no, perché è un
                // sovrapposto e i sovrapposti se lo gestiscono da soli — il
                // pannello si tiene la barra di navigazione, il riquadro di
                // rinomina usa safeDrawing. Qui mancava del tutto: la barra in alto
                // finiva sotto l'orologio e il contenuto in fondo sotto la barra di
                // navigazione. Con questo si comporta come Revisione e Cartella.
                .windowInsetsPadding(WindowInsets.systemBars)
                // Consuma i tocchi: sotto c'è ancora la libreria, e senza questo un
                // tap su una zona vuota arriverebbe a lei.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            // Stesse misure della barra di Revisione e di Cartella: altezza fissa
            // 60dp, rientro 8dp, titolo a 17sp e filetto sotto. Averle scritte a
            // occhio la faceva sedere più in basso delle altre, e passando da una
            // schermata all'altra la freccia si spostava.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BackButton(onClick = { phase.close() })
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = TextSubtitle,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
            ) {

                // ------------------------------------------------------- Tema
                SectionLabel(stringResource(R.string.appearance))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeOption(
                        icon = Icons.Default.LightMode,
                        label = stringResource(R.string.theme_light),
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChange(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        icon = Icons.Default.DarkMode,
                        label = stringResource(R.string.theme_dark),
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { onThemeChange(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeOption(
                        icon = Icons.Default.PhoneAndroid,
                        label = stringResource(R.string.theme_system),
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChange(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(22.dp))

                // ------------------------------------------------------- Colore
                SectionLabel(stringResource(R.string.accent_color))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AccentColor.entries.forEach { option ->
                        AccentSwatch(
                            accent = option,
                            selected = settings.accent == option,
                            onClick = { onAccentChange(option) },
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // -------------------------------------------------------- Schede
                SectionLabel(stringResource(R.string.card_style))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CardStyleOption(
                        label = stringResource(R.string.card_style_rounded),
                        style = CardStyle.ROUNDED,
                        selected = settings.cardStyle == CardStyle.ROUNDED,
                        onClick = { onCardStyleChange(CardStyle.ROUNDED) },
                        modifier = Modifier.weight(1f),
                    )
                    CardStyleOption(
                        label = stringResource(R.string.card_style_underlined),
                        style = CardStyle.UNDERLINED,
                        selected = settings.cardStyle == CardStyle.UNDERLINED,
                        onClick = { onCardStyleChange(CardStyle.UNDERLINED) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(22.dp))

                // ------------------------------------------------------- Lingua
                SectionLabel(stringResource(R.string.language))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LanguageOption(stringResource(R.string.theme_system), settings.language == AppLanguage.SYSTEM,
                        { onLanguageChange(AppLanguage.SYSTEM) }, Modifier.weight(1f))
                    LanguageOption(stringResource(R.string.language_italian), settings.language == AppLanguage.ITALIAN,
                        { onLanguageChange(AppLanguage.ITALIAN) }, Modifier.weight(1f))
                    LanguageOption(stringResource(R.string.language_english), settings.language == AppLanguage.ENGLISH,
                        { onLanguageChange(AppLanguage.ENGLISH) }, Modifier.weight(1f))
                }

                Spacer(Modifier.height(22.dp))

                // -------------------------------------------- Cartella predefinita
                SectionLabel(stringResource(R.string.export_folder))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(62.dp)
                        .clip(CornerMedium)
                        .background(SurfaceContainer)
                        .clickable(onClick = onPickFolder)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Default.Folder, null, Modifier.size(22.dp), Accent)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = settings.defaultFolderLabel ?: stringResource(R.string.ask_every_time),
                            fontSize = TextBody,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (settings.defaultFolderUri == null)
                                stringResource(R.string.pick_folder_hint)
                            else stringResource(R.string.change_folder_hint),
                            fontSize = TextLabel,
                            color = OnSurfaceVariant,
                        )
                    }
                    if (settings.defaultFolderUri != null) {
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(CornerMedium)
                                .border(1.dp, Outline, CornerMedium)
                                .clickable(onClick = onClearFolder)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.reset), fontSize = TextLabel, color = OnSurfaceStrong)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Parti

/** Titoletto di una sezione. */
@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = TextMeta, color = OnSurfaceVariant)
}

/** Riquadro di scelta del tema. */
@Composable
private fun ThemeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(CornerMedium)
            .then(
                if (selected) Modifier.background(AccentContainer)
                else Modifier.border(1.dp, Outline, CornerMedium),
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) OnAccentContainer else OnSurfaceStrong,
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    Modifier.size(13.dp),
                    OnAccentContainer,
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = label,
                fontSize = TextLabel,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) OnAccentContainer else OnSurfaceStrong,
            )
        }
    }
}

/** Pillola di scelta della lingua: come le opzioni del tema, senza icona. */
@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(CornerMedium)
            .then(
                if (selected) Modifier.background(AccentContainer)
                else Modifier.border(1.dp, Outline, CornerMedium),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = TextLabel,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) OnAccentContainer else OnSurfaceStrong,
        )
    }
}

/**
 * Pastiglia di scelta del colore.
 *
 * Mostra la tinta del tema attivo, non quella chiara: se scegli il blu di notte
 * devi vedere il blu che avrai di notte. L'anello di selezione è staccato dal
 * cerchio da un alone del colore di sfondo, altrimenti su un accento scuro non
 * si distinguerebbe dal cerchio stesso.
 */
@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val tone = accentPreview(accent)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CornerRound)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CornerMedium)
                .then(if (selected) Modifier.border(2.dp, tone, CornerMedium) else Modifier)
                .padding(if (selected) 4.dp else 0.dp)
                .clip(CornerMedium)
                .background(tone),
        )
    }
}

/** Riquadro di scelta dello stile delle carte, con un fac-simile dentro. */
@Composable
private fun CardStyleOption(
    label: String,
    style: CardStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rounded = style == CardStyle.ROUNDED
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(CornerMedium)
            .then(
                if (selected) Modifier.background(AccentContainer)
                else Modifier.border(1.dp, Outline, CornerMedium),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Fac-simile della carta: un foglio e, nello stile sottolineato, il
        // filetto che lo chiude. Vedere la forma vale più di leggerne il nome.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (rounded) {
                Modifier.clip(CornerSmall).background(SurfaceHigh).padding(4.dp)
            } else {
                Modifier
            },
        ) {
            Box(
                Modifier
                    .size(width = 24.dp, height = 30.dp)
                    .clip(if (rounded) CornerSmall else RectangleShape)
                    .background(PaperSheen),
            )
            if (!rounded) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(width = 24.dp, height = 1.dp)
                        .background(if (selected) OnAccentContainer else Outline),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = label,
            fontSize = TextLabel,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) OnAccentContainer else OnSurfaceStrong,
        )
    }
}
