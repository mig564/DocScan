package it.example.docscan.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.R
import it.example.docscan.data.AppLanguage
import it.example.docscan.data.AppSettings
import it.example.docscan.data.ThemeMode
import it.example.docscan.ui.BottomSheet
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.GreenContainer
import it.example.docscan.ui.theme.OnGreenContainer
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.SurfaceContainer

/**
 * Foglio impostazioni. Stesso gesto del foglio di salvataggio, perché due sole
 * impostazioni non giustificano una schermata intera.
 */
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onPickFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(
        onDismiss = onDismiss,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 26.dp),
    ) {

            Text(stringResource(R.string.settings), fontSize = 19.sp, color = OnSurface)
            Spacer(Modifier.height(20.dp))

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
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
                    .clickable(onClick = onPickFolder)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Default.Folder, null, Modifier.size(22.dp), Green)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = settings.defaultFolderLabel ?: stringResource(R.string.ask_every_time),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (settings.defaultFolderUri == null)
                            stringResource(R.string.pick_folder_hint)
                        else stringResource(R.string.change_folder_hint),
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                    )
                }
                if (settings.defaultFolderUri != null) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Outline, RoundedCornerShape(8.dp))
                            .clickable(onClick = onClearFolder)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.reset), fontSize = 12.sp, color = OnSurfaceStrong)
                    }
                }
            }

            Text(
                text = stringResource(R.string.folder_note),
                fontSize = 11.5.sp,
                color = OnSurfaceFaint,
                modifier = Modifier.padding(top = 10.dp),
            )
    }
}

// ------------------------------------------------------------------ Parti

/** Titoletto di una sezione. */
@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 10.5.sp, color = OnSurfaceVariant)
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
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.background(GreenContainer)
                else Modifier.border(1.dp, Outline, RoundedCornerShape(14.dp)),
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
                tint = if (selected) OnGreenContainer else OnSurfaceStrong,
            )
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    Modifier.size(13.dp),
                    OnGreenContainer,
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) OnGreenContainer else OnSurfaceStrong,
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
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(GreenContainer)
                else Modifier.border(1.dp, Outline, RoundedCornerShape(12.dp)),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) OnGreenContainer else OnSurfaceStrong,
        )
    }
}
