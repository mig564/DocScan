package it.example.docscan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import it.example.docscan.data.AccentColor
import it.example.docscan.data.CardStyle

/**
 * Palette del progetto: superfici neutre più un accento scelto dall'utente.
 *
 * I nomi pubblici in fondo al file sono proprietà composable: leggono
 * [LocalDarkTheme] e [LocalAccent] e danno la tinta giusta per il tema e il
 * colore attivi. Le schermate scrivono OnSurface o Accent e si adeguano da sole,
 * senza sapere né quale tema né quale accento sia in uso.
 *
 * Le superfici sono volutamente grigie anche in chiaro, non bianche: il
 * contenuto dell'app è carta bianca, e su un fondo bianco ogni pagina avrebbe
 * bisogno di un bordo per esistere. Su un grigio dichiarato le pagine si
 * staccano da sole.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalAccent = staticCompositionLocalOf { AccentColor.BLUE }
val LocalCardStyle = staticCompositionLocalOf { CardStyle.ROUNDED }

// ------------------------------------------------------------------- Accenti

/**
 * Le sette tinte che servono a un accento.
 *
 * [onBase] è il colore del testo sopra un pieno di [base]: non è sempre bianco.
 * In tema scuro gli accenti sono schiariti per restare leggibili sul fondo, e
 * sopra un pieno così chiaro il bianco sparisce — lì ci va un testo scuro.
 */
internal class AccentTones(
    val base: Color,
    val strong: Color,
    val onBase: Color,
    val container: Color,
    val onContainer: Color,
    val tint: Color,
    val onTint: Color,
    val onTintSoft: Color,
)

private val RustLight = AccentTones(
    base = Color(0xFFB4441F), strong = Color(0xFF8E3316), onBase = Color(0xFFFFFFFF),
    container = Color(0xFFF7E2DA), onContainer = Color(0xFF4A1808),
    tint = Color(0xFFFAF0EB), onTint = Color(0xFF6B2712), onTintSoft = Color(0xFF8A5B47),
)
private val RustDark = AccentTones(
    base = Color(0xFFD0714C), strong = Color(0xFFE08C6B), onBase = Color(0xFF2B0F05),
    container = Color(0xFF4A2113), onContainer = Color(0xFFF3D6C8),
    tint = Color(0xFF2E1B12), onTint = Color(0xFFEBC3B0), onTintSoft = Color(0xFFB08A78),
)

private val BlueLight = AccentTones(
    base = Color(0xFF2B4C8C), strong = Color(0xFF1E3A6E), onBase = Color(0xFFFFFFFF),
    container = Color(0xFFDEE5F3), onContainer = Color(0xFF12244A),
    tint = Color(0xFFEFF2F9), onTint = Color(0xFF24406F), onTintSoft = Color(0xFF5A6B8C),
)
private val BlueDark = AccentTones(
    base = Color(0xFF8FAAD9), strong = Color(0xFFA9BFE5), onBase = Color(0xFF0C1526),
    container = Color(0xFF22334F), onContainer = Color(0xFFD6E1F3),
    tint = Color(0xFF1A2231), onTint = Color(0xFFC3D1E8), onTintSoft = Color(0xFF8E9CB5),
)

private val PlumLight = AccentTones(
    base = Color(0xFF5E4B8B), strong = Color(0xFF48386D), onBase = Color(0xFFFFFFFF),
    container = Color(0xFFE6E1F1), onContainer = Color(0xFF291D48),
    tint = Color(0xFFF2EFF8), onTint = Color(0xFF453470), onTintSoft = Color(0xFF6F6690),
)
private val PlumDark = AccentTones(
    base = Color(0xFF9B8AC4), strong = Color(0xFFB3A5D5), onBase = Color(0xFF1B1230),
    container = Color(0xFF322A4A), onContainer = Color(0xFFDED6EE),
    tint = Color(0xFF221D30), onTint = Color(0xFFCDC3E2), onTintSoft = Color(0xFF9990AE),
)

// Verde bottiglia, non menta: abbastanza scuro da funzionare come inchiostro su
// fondo chiaro, cosa che il menta non faceva.
private val GreenLight = AccentTones(
    base = Color(0xFF2E6B45), strong = Color(0xFF1F5233), onBase = Color(0xFFFFFFFF),
    container = Color(0xFFDCEBE1), onContainer = Color(0xFF103324),
    tint = Color(0xFFEDF4EF), onTint = Color(0xFF26583A), onTintSoft = Color(0xFF566F5E),
)
private val GreenDarkTones = AccentTones(
    base = Color(0xFF6DB287), strong = Color(0xFF8AC79F), onBase = Color(0xFF06281A),
    container = Color(0xFF234A34), onContainer = Color(0xFFCDE6D6),
    tint = Color(0xFF182A1F), onTint = Color(0xFFBADCC6), onTintSoft = Color(0xFF8FAE9A),
)

internal fun tonesFor(accent: AccentColor, dark: Boolean): AccentTones = when (accent) {
    AccentColor.RUST -> if (dark) RustDark else RustLight
    AccentColor.BLUE -> if (dark) BlueDark else BlueLight
    AccentColor.PLUM -> if (dark) PlumDark else PlumLight
    AccentColor.GREEN -> if (dark) GreenDarkTones else GreenLight
}

// ------------------------------------------------------------------- Chiaro

// Quasi bianco. Con una pagina così chiara il rilievo non può più andare verso
// il bianco, perché il bianco è già la pagina: le superfici sopra sono appena
// più grigie, che è il modello classico delle carte grigie su fondo chiaro. Il
// bianco pieno resta riservato alle pagine scansionate, che con il bordo di
// PaperEdge si staccano lo stesso.
private val LSurface = Color(0xFFFAFAF9)
private val LSurfaceDim = Color(0xFFEDEDEB)
private val LSurfaceContainer = Color(0xFFF1F1EF)
private val LSurfaceHigh = Color(0xFFF4F4F2)

private val LOnSurface = Color(0xFF20201E)
private val LOnSurfaceStrong = Color(0xFF33332F)
private val LOnSurfaceMid = Color(0xFF4C4C48)
private val LOnSurfaceSoft = Color(0xFF3E3E3A)
private val LOnSurfaceVariant = Color(0xFF77776F)
private val LOnSurfaceFaint = Color(0xFF8D8D87)
private val LOnSurfaceGhost = Color(0xFFB4B4AE)

private val LOutline = Color(0xFFD9D9D4)
private val LOutlineSoft = Color(0xFFE5E5E1)
private val LOutlineFaint = Color(0xFFEEEEEB)
private val LOutlineDashed = Color(0xFFCBCBC5)

private val LDangerText = Color(0xFF8C2F1E)
private val LDangerContainer = Color(0xFFF6E2DD)
private val LWarnText = Color(0xFF8A5A08)
private val LWarnContainer = Color(0xFFF5EBD8)

private val LToastBg = Color(0xFF2A2A28)
private val LToastText = Color(0xFFF1F1EE)

private val LPaperInk = Color(0xFF2A2A28)
private val LPaperLine = Color(0xFFC6C6C1)
private val LPaperLineSoft = Color(0xFFD9D9D4)
private val LPaperEdge = Color(0xFFDCDCD6)
private val LPaperStack1 = Color(0xFFEFEFEB)
private val LPaperStack2 = Color(0xFFE5E5E0)
private val LBottomBar = Color(0xFFF2F2F0)
private val LScrim = Color(0x6B121212)

// -------------------------------------------------------------------- Scuro

private val DSurface = Color(0xFF1C1C1B)
private val DSurfaceDim = Color(0xFF151514)
private val DSurfaceContainer = Color(0xFF262625)
private val DSurfaceHigh = Color(0xFF313130)

private val DOnSurface = Color(0xFFE9E9E5)
private val DOnSurfaceStrong = Color(0xFFD6D6D1)
private val DOnSurfaceMid = Color(0xFFC0C0BA)
private val DOnSurfaceSoft = Color(0xFFC9C9C3)
private val DOnSurfaceVariant = Color(0xFF9A9A93)
private val DOnSurfaceFaint = Color(0xFF7C7C76)
private val DOnSurfaceGhost = Color(0xFF5C5C57)

private val DOutline = Color(0xFF464643)
private val DOutlineSoft = Color(0xFF2F2F2D)
private val DOutlineFaint = Color(0xFF262624)
private val DOutlineDashed = Color(0xFF575752)

private val DDangerText = Color(0xFFF2B4A2)
private val DDangerContainer = Color(0xFF4A1E14)
private val DWarnText = Color(0xFFF3CB8B)
private val DWarnContainer = Color(0xFF42300B)

// Il toast si inverte: chiaro su scuro di giorno, scuro su chiaro di notte.
private val DToastBg = Color(0xFFE9E9E5)
private val DToastText = Color(0xFF1C1C1B)

// La carta scansionata resta chiara anche di notte: è un foglio, non una
// superficie dell'interfaccia. Cambiano solo i fogli impilati dietro e il bordo.
private val DPaperInk = Color(0xFF2A2A28)
private val DPaperLine = Color(0xFFC6C6C1)
private val DPaperLineSoft = Color(0xFFD9D9D4)
private val DPaperEdge = Color(0xFF6C6C67)
private val DPaperStack1 = Color(0xFF33332F)
private val DPaperStack2 = Color(0xFF262623)
private val DBottomBar = Color(0xFF232322)
private val DScrim = Color(0x9E000000)

// -------------------------------------------------- Proprietà theme-aware

/** Sceglie la tinta chiara o scura secondo il tema attivo. */
@Suppress("NOTHING_TO_INLINE")
private inline fun pick(dark: Boolean, light: Color, night: Color) = if (dark) night else light

private val tones: AccentTones
    @Composable @ReadOnlyComposable
    get() = tonesFor(LocalAccent.current, LocalDarkTheme.current)

val Accent: Color @Composable @ReadOnlyComposable get() = tones.base
val AccentStrong: Color @Composable @ReadOnlyComposable get() = tones.strong
val OnAccent: Color @Composable @ReadOnlyComposable get() = tones.onBase
val AccentContainer: Color @Composable @ReadOnlyComposable get() = tones.container
val OnAccentContainer: Color @Composable @ReadOnlyComposable get() = tones.onContainer
val AccentTint: Color @Composable @ReadOnlyComposable get() = tones.tint
val OnAccentTint: Color @Composable @ReadOnlyComposable get() = tones.onTint
val OnAccentTintSoft: Color @Composable @ReadOnlyComposable get() = tones.onTintSoft

/**
 * Tinta di un accento qualsiasi, non solo di quello attivo.
 *
 * Serve alle impostazioni, che devono mostrare tutti e quattro i colori mentre
 * uno solo è in uso. Segue il tema, così la pastiglia mostra il colore che
 * otterresti davvero scegliendolo adesso.
 */
@Composable
@ReadOnlyComposable
fun accentPreview(accent: AccentColor): Color = tonesFor(accent, LocalDarkTheme.current).base

val Surface: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LSurface, DSurface)
val SurfaceDim: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LSurfaceDim, DSurfaceDim)
val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LSurfaceContainer, DSurfaceContainer)
val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LSurfaceHigh, DSurfaceHigh)

val OnSurface: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurface, DOnSurface)
val OnSurfaceStrong: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurfaceStrong, DOnSurfaceStrong)
val OnSurfaceMid: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurfaceMid, DOnSurfaceMid)
val OnSurfaceSoft: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurfaceSoft, DOnSurfaceSoft)
val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurfaceVariant, DOnSurfaceVariant)
val OnSurfaceFaint: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurfaceFaint, DOnSurfaceFaint)
val OnSurfaceGhost: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnSurfaceGhost, DOnSurfaceGhost)

val Outline: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOutline, DOutline)
val OutlineSoft: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOutlineSoft, DOutlineSoft)
val OutlineFaint: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOutlineFaint, DOutlineFaint)
val OutlineDashed: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOutlineDashed, DOutlineDashed)

val DangerText: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LDangerText, DDangerText)
val DangerContainer: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LDangerContainer, DDangerContainer)
val WarnText: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LWarnText, DWarnText)
val WarnContainer: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LWarnContainer, DWarnContainer)

val ToastBg: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LToastBg, DToastBg)
val ToastText: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LToastText, DToastText)

/**
 * Accento del toast, invertito rispetto a tutto il resto.
 *
 * Il toast ha il fondo scuro in tema chiaro e chiaro in tema scuro, quindi qui
 * serve la variante opposta a quella del tema: usare [Accent] darebbe un colore
 * scuro su fondo scuro.
 */
val ToastAccent: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) {
        tonesFor(LocalAccent.current, dark = false).base
    } else {
        tonesFor(LocalAccent.current, dark = true).base
    }

val PaperInk: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperInk, DPaperInk)
val PaperLine: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperLine, DPaperLine)
val PaperLineSoft: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperLineSoft, DPaperLineSoft)
val PaperEdge: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperEdge, DPaperEdge)
val PaperStack1: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperStack1, DPaperStack1)
val PaperStack2: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperStack2, DPaperStack2)
val BottomBar: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LBottomBar, DBottomBar)
val Scrim: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LScrim, DScrim)

/**
 * Il foglio bianco delle miniature, con una luce diagonale appena percettibile.
 *
 * Non è decorazione: un rettangolo bianco piatto sembra un buco nella
 * schermata, mentre due bianchi di poco diversi lo fanno leggere come un
 * oggetto appoggiato. Vale in entrambi i temi, perché il foglio è chiaro sempre.
 */
val PaperSheen: Brush
    @Composable @ReadOnlyComposable
    get() = Brush.linearGradient(listOf(Color.White, Color(0xFFF2F2EF)))

/**
 * Dissolvenza sopra la barra in fondo, dal trasparente al colore della pagina.
 *
 * Serve a dire che la lista continua sotto la barra invece di finire lì.
 */
val BottomFade: Brush
    @Composable @ReadOnlyComposable
    get() = Brush.verticalGradient(
        listOf(Color.Transparent, pick(LocalDarkTheme.current, LSurface, DSurface)),
    )

// Valori grezzi per gli schemi Material, che si costruiscono fuori dalla
// composizione e quindi non possono leggere i CompositionLocal.
internal object LightRaw {
    val surface = LSurface
    val onSurface = LOnSurface
    val surfaceContainer = LSurfaceContainer
    val onSurfaceVariant = LOnSurfaceVariant
    val outline = LOutline
    val outlineSoft = LOutlineSoft
    val dangerText = LDangerText
    val dangerContainer = LDangerContainer
}

internal object DarkRaw {
    val surface = DSurface
    val onSurface = DOnSurface
    val surfaceContainer = DSurfaceContainer
    val onSurfaceVariant = DOnSurfaceVariant
    val outline = DOutline
    val outlineSoft = DOutlineSoft
    val dangerText = DDangerText
    val dangerContainer = DDangerContainer
}
