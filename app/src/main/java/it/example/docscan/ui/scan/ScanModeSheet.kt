package it.example.docscan.ui.scan

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.R
import it.example.docscan.data.ScanMode
import it.example.docscan.ui.BottomSheet
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.AccentContainer
import it.example.docscan.ui.theme.AccentTint
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.OnAccent
import it.example.docscan.ui.theme.OnAccentContainer
import it.example.docscan.ui.theme.OnAccentTint
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.TextBody
import it.example.docscan.ui.theme.TextLabel
import it.example.docscan.ui.theme.TextMeta
import it.example.docscan.ui.theme.TextSubtitle

/**
 * Pannello che si apre da "Scansiona".
 *
 * Tre modalità: carta d'identità, tessera sanitaria, patente e bancomat hanno
 * la stessa forma ID-1 e darebbero quattro pulsanti identici. Il tipo lo deduce
 * l'OCR.
 */
@Composable
fun ScanModeSheet(
    selected: ScanMode,
    onSelect: (ScanMode) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    buttonLabel: String,
    stepLabel: String,
) {
    BottomSheet(
        onDismiss = onDismiss,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 22.dp),
    ) {

            Text(stringResource(R.string.mode_question), fontSize = TextSubtitle, color = OnSurface)
            Spacer(Modifier.height(14.dp))

            // L'etichetta la porta l'enum, non la piastrella: il nome di una
            // modalità è una proprietà della modalità, e ripeterlo qui
            // significherebbe tenerne due copie allineate a mano.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeTile(
                    icon = Icons.Default.Description,
                    label = stringResource(ScanMode.DOCUMENT.labelRes),
                    selected = selected == ScanMode.DOCUMENT,
                    onClick = { onSelect(ScanMode.DOCUMENT) },
                    modifier = Modifier.weight(1f),
                )
                ModeTile(
                    icon = Icons.Default.CreditCard,
                    label = stringResource(ScanMode.CARD.labelRes),
                    selected = selected == ScanMode.CARD,
                    onClick = { onSelect(ScanMode.CARD) },
                    modifier = Modifier.weight(1f),
                )
                ModeTile(
                    icon = Icons.Default.Flight,
                    label = stringResource(ScanMode.PASSPORT.labelRes),
                    selected = selected == ScanMode.PASSPORT,
                    onClick = { onSelect(ScanMode.PASSPORT) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Riquadro ad altezza fissa: le tre descrizioni occupano una o due
            // righe, e senza uno spazio riservato il pannello cambiava altezza
            // passando da una modalita all'altra.
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(CornerMedium)
                    .background(AccentTint)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(selected.descriptionRes),
                    fontSize = TextLabel,
                    lineHeight = 17.sp,
                    color = OnAccentTint,
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CornerMedium)
                    .background(Accent)
                    .clickable(onClick = onStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(21.dp), OnAccent)
                Text(
                    text = buttonLabel,
                    fontSize = TextBody,
                    fontWeight = FontWeight.Medium,
                    color = OnAccent,
                )
            }

            Text(
                text = stepLabel,
                fontSize = TextMeta,
                color = OnSurfaceFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            )
    }
}

/** Riquadro di una modalità: icona ed etichetta. */
@Composable
private fun ModeTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(CornerMedium)
            .then(
                if (selected) Modifier.background(AccentContainer)
                else Modifier.border(1.dp, Outline, CornerMedium),
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (selected) OnAccentContainer else OnSurfaceStrong,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = TextLabel,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) OnAccentContainer else OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
