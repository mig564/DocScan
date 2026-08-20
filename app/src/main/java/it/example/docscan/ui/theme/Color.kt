package it.example.docscan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette chiara del progetto e variante scura derivata da essa.
 *
 * I nomi pubblici in fondo al file sono proprietà composable: leggono
 * [LocalDarkTheme] e danno la tinta del tema attivo. Le schermate scrivono
 * OnSurface o Green e passano allo scuro da sole.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

// ------------------------------------------------------------------- Chiaro

private val LGreen = Color(0xFF2F7A57)
private val LGreenDark = Color(0xFF1B6244)
private val LGreenContainer = Color(0xFFD8EADD)
private val LOnGreenContainer = Color(0xFF0D3324)
private val LGreenTint = Color(0xFFEDF4EF)
private val LOnGreenTint = Color(0xFF12402D)
private val LOnGreenTintSoft = Color(0xFF3F6553)

private val LSurface = Color(0xFFFBFBF9)
private val LSurfaceDim = Color(0xFFF4F5F1)
private val LSurfaceContainer = Color(0xFFEFF0EC)
private val LSurfaceHigh = Color(0xFFE8EAE3)

private val LOnSurface = Color(0xFF1B1C19)
private val LOnSurfaceStrong = Color(0xFF31352E)
private val LOnSurfaceMid = Color(0xFF4A4E46)
private val LOnSurfaceSoft = Color(0xFF3C4038)
private val LOnSurfaceVariant = Color(0xFF767B72)
private val LOnSurfaceFaint = Color(0xFF8B9088)
private val LOnSurfaceGhost = Color(0xFFB3B7AE)

private val LOutline = Color(0xFFCACCC5)
private val LOutlineSoft = Color(0xFFE5E7E0)
private val LOutlineFaint = Color(0xFFEDEEE9)
private val LOutlineDashed = Color(0xFFB9BEB3)

private val LDangerText = Color(0xFF8C2F1E)
private val LDangerContainer = Color(0xFFFBE7E3)
private val LWarnText = Color(0xFF8A5A08)
private val LWarnContainer = Color(0xFFFBEEDA)

private val LToastBg = Color(0xFF2A2E27)
private val LToastText = Color(0xFFF2F3EF)
private val LToastAccent = Color(0xFF8FE7B7)

private val LPaperInk = Color(0xFF2B2F28)
private val LPaperLine = Color(0xFFC8CBC3)
private val LPaperLineSoft = Color(0xFFDADCD5)
private val LPaperEdge = Color(0xFFD3D6CE)
private val LPaperStack1 = Color(0xFFF4F5F1)
private val LPaperStack2 = Color(0xFFEDEEE9)
private val LBottomBar = Color(0xFFF2F3EF)
private val LScrim = Color(0x6B121410)

// -------------------------------------------------------------------- Scuro

// Il verde del design è troppo cupo per reggere su fondo scuro: qui è schiarito
// quanto basta a restare riconoscibile e leggibile. Le superfici sono neutre
// calde, non nero puro, per non affaticare la vista al buio.
private val DGreen = Color(0xFF74CBA0)
private val DGreenDark = Color(0xFF97DDBA)
private val DGreenContainer = Color(0xFF234B3A)
private val DOnGreenContainer = Color(0xFFCFEADB)
private val DGreenTint = Color(0xFF1B3529)
private val DOnGreenTint = Color(0xFFCFEADB)
private val DOnGreenTintSoft = Color(0xFF9CBFAE)

private val DSurface = Color(0xFF12140F)
private val DSurfaceDim = Color(0xFF0D0F0B)
private val DSurfaceContainer = Color(0xFF1D201A)
private val DSurfaceHigh = Color(0xFF272B23)

private val DOnSurface = Color(0xFFE4E5DF)
private val DOnSurfaceStrong = Color(0xFFD3D5CD)
private val DOnSurfaceMid = Color(0xFFBFC2B9)
private val DOnSurfaceSoft = Color(0xFFC7CAC1)
private val DOnSurfaceVariant = Color(0xFF9BA095)
private val DOnSurfaceFaint = Color(0xFF868B80)
private val DOnSurfaceGhost = Color(0xFF63685E)

private val DOutline = Color(0xFF444941)
private val DOutlineSoft = Color(0xFF2C302A)
private val DOutlineFaint = Color(0xFF23261F)
private val DOutlineDashed = Color(0xFF565C51)

private val DDangerText = Color(0xFFF2B4A2)
private val DDangerContainer = Color(0xFF4A1E14)
private val DWarnText = Color(0xFFF3CB8B)
private val DWarnContainer = Color(0xFF42300B)

// Il toast si inverte: chiaro su scuro di giorno, scuro su chiaro di notte.
private val DToastBg = Color(0xFFE4E5DF)
private val DToastText = Color(0xFF1B1C19)
private val DToastAccent = Color(0xFF1B6244)

// La carta scansionata resta chiara anche di notte: è un foglio, non una
// superficie dell'interfaccia. Cambiano solo i fogli impilati dietro e il bordo.
private val DPaperInk = Color(0xFF2B2F28)
private val DPaperLine = Color(0xFFC8CBC3)
private val DPaperLineSoft = Color(0xFFDADCD5)
private val DPaperEdge = Color(0xFF6E736A)
private val DPaperStack1 = Color(0xFF33382F)
private val DPaperStack2 = Color(0xFF262A22)
private val DBottomBar = Color(0xFF1A1D16)
private val DScrim = Color(0x9E000000)

// -------------------------------------------------- Proprietà theme-aware

/** Sceglie la tinta chiara o scura secondo il tema attivo. */
@Suppress("NOTHING_TO_INLINE")
private inline fun pick(dark: Boolean, light: Color, night: Color) = if (dark) night else light

val Green: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LGreen, DGreen)
val GreenDark: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LGreenDark, DGreenDark)
val GreenContainer: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LGreenContainer, DGreenContainer)
val OnGreenContainer: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnGreenContainer, DOnGreenContainer)
val GreenTint: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LGreenTint, DGreenTint)
val OnGreenTint: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnGreenTint, DOnGreenTint)
val OnGreenTintSoft: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LOnGreenTintSoft, DOnGreenTintSoft)

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
val ToastAccent: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LToastAccent, DToastAccent)

val PaperInk: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperInk, DPaperInk)
val PaperLine: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperLine, DPaperLine)
val PaperLineSoft: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperLineSoft, DPaperLineSoft)
val PaperEdge: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperEdge, DPaperEdge)
val PaperStack1: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperStack1, DPaperStack1)
val PaperStack2: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LPaperStack2, DPaperStack2)
val BottomBar: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LBottomBar, DBottomBar)
val Scrim: Color @Composable @ReadOnlyComposable get() = pick(LocalDarkTheme.current, LScrim, DScrim)

// Valori grezzi per gli schemi Material, che si costruiscono fuori dalla
// composizione e quindi non possono leggere il CompositionLocal.
internal object LightRaw {
    val green = LGreen
    val greenContainer = LGreenContainer
    val onGreenContainer = LOnGreenContainer
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
    val green = DGreen
    val greenContainer = DGreenContainer
    val onGreenContainer = DOnGreenContainer
    val surface = DSurface
    val onSurface = DOnSurface
    val surfaceContainer = DSurfaceContainer
    val onSurfaceVariant = DOnSurfaceVariant
    val outline = DOutline
    val outlineSoft = DOutlineSoft
    val dangerText = DDangerText
    val dangerContainer = DDangerContainer
}
