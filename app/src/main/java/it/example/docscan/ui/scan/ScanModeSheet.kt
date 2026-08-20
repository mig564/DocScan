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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.data.ScanMode
import it.example.docscan.ui.BottomSheet
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.GreenContainer
import it.example.docscan.ui.theme.GreenTint
import it.example.docscan.ui.theme.OnGreenContainer
import it.example.docscan.ui.theme.OnGreenTint
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline

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

            Text("Cosa scansioni?", fontSize = 16.sp, color = OnSurface)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeTile(
                    icon = Icons.Default.Description,
                    label = "Documento",
                    selected = selected == ScanMode.DOCUMENT,
                    onClick = { onSelect(ScanMode.DOCUMENT) },
                    modifier = Modifier.weight(1f),
                )
                ModeTile(
                    icon = Icons.Default.CreditCard,
                    label = "Tessera",
                    selected = selected == ScanMode.CARD,
                    onClick = { onSelect(ScanMode.CARD) },
                    modifier = Modifier.weight(1f),
                )
                ModeTile(
                    icon = Icons.Default.Flight,
                    label = "Passaporto",
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenTint)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = selected.description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = OnGreenTint,
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Green)
                    .clickable(onClick = onStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(21.dp), Color.White)
                Text(
                    text = buttonLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }

            Text(
                text = stepLabel,
                fontSize = 11.5.sp,
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
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.background(GreenContainer)
                else Modifier.border(1.dp, Outline, RoundedCornerShape(14.dp)),
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (selected) OnGreenContainer else OnSurfaceStrong,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) OnGreenContainer else OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
