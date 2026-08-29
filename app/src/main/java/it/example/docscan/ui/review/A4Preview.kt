package it.example.docscan.ui.review

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.example.docscan.R
import it.example.docscan.data.A4Composer
import it.example.docscan.data.CardFormat
import it.example.docscan.data.FitMode
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.AccentContainer
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.CornerSmall
import it.example.docscan.ui.theme.OnAccentContainer
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineDashed
import it.example.docscan.ui.theme.PaperEdge
import it.example.docscan.ui.theme.TextBody
import it.example.docscan.ui.theme.TextLabel
import it.example.docscan.ui.theme.TextMeta

/**
 * Foglio A4 vuoto, con i due riquadri delle facciate al posto giusto.
 *
 * Le posizioni vengono da [A4Composer.slotFractions], le stesse del PDF:
 * duplicare i numeri qui li farebbe divergere alla prima modifica.
 *
 * Il layout usa i pesi invece di misurare il foglio, così le frazioni del PDF
 * diventano direttamente pesi di riga e di colonna.
 *
 * @param slotContent disegna la facciata di indice 0 (fronte) o 1 (retro)
 */
@Composable
fun A4Sheet(
    format: CardFormat,
    modifier: Modifier = Modifier,
    slotContent: @Composable (Int) -> Unit,
) {
    val slots = remember(format) { A4Composer.slotFractions(format) }

    // Niente fillMaxWidth qui: con `aspectRatio` su un modifier che riempie
    // tutta l'area, Compose sceglie da solo il lato limitante e il foglio
    // diventa il più grande che ci sta. Se invece il chiamante passa solo la
    // larghezza, l'altezza la deriva dalle proporzioni.
    Column(
        modifier = modifier
            .aspectRatio(A4Composer.a4AspectRatio)
            .clip(CornerSmall)
            .background(Color.White)
            .border(1.dp, PaperEdge, CornerSmall),
    ) {
        var cursor = 0f
        slots.forEachIndexed { index, r ->
            Spacer(Modifier.weight(r.top - cursor))
            Row(Modifier.fillMaxWidth().weight(r.bottom - r.top)) {
                Spacer(Modifier.weight(r.left))
                Box(Modifier.fillMaxHeight().weight(r.right - r.left)) { slotContent(index) }
                Spacer(Modifier.weight(1f - r.right))
            }
            cursor = r.bottom
        }
        Spacer(Modifier.weight(1f - cursor))
    }
}

/** Anteprima del foglio durante la revisione, con le pagine già acquisite. */
@Composable
fun A4Preview(
    pageUris: List<Uri>,
    format: CardFormat,
    fitMode: FitMode,
    modifier: Modifier = Modifier,
) {
    A4Sheet(format, modifier) { index ->
        val uri = pageUris.getOrNull(index)
        if (uri != null) {
            SlotImage(uri, fitMode)
        } else {
            Box(
                Modifier.fillMaxSize().border(1.dp, OutlineDashed, CornerSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (index == 0) stringResource(R.string.front) else stringResource(R.string.back_side),
                    fontSize = TextMeta,
                    color = OnSurfaceFaint,
                )
            }
        }
    }
}

/** Immagine di una facciata dentro il suo riquadro sul foglio. */
@Composable
private fun SlotImage(uri: Uri, fitMode: FitMode) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            // Fit rispecchia la scala reale (margini bianchi se il ritaglio non
            // è perfetto); Crop rispecchia il riempimento del riquadro.
            contentScale = if (fitMode == FitMode.TRUE_SCALE) ContentScale.Fit
            else ContentScale.Crop,
            // fillMaxSize: l'immagine riempie il riquadro della carta, e la
            // proporzione la garantisce ContentScale, non la misura.
            modifier = Modifier
                .fillMaxSize()
                .clip(CornerSmall)
                .background(Color.White),
        )
    } else {
        Box(Modifier.background(Color(0xFFF0F0EC)))
    }
}

/** Scelta fra scala reale e riempimento, mostrata sotto l'anteprima. */
@Composable
fun FitModeSelector(selected: FitMode, onSelect: (FitMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FitMode.entries.forEach { mode ->
            val active = mode == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CornerMedium)
                    .then(
                        if (active) Modifier.background(AccentContainer)
                        else Modifier.border(1.dp, Outline, CornerMedium),
                    )
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(mode.labelRes),
                    fontSize = TextLabel,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    color = if (active) OnAccentContainer else OnSurfaceStrong,
                )
                Text(
                    text = stringResource(mode.descriptionRes),
                    fontSize = TextMeta,
                    color = if (active) OnAccentContainer else OnSurfaceVariant,
                )
            }
        }
    }
}

/** Pulsante per acquisire la facciata mancante. */
@Composable
fun CaptureBackButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CornerMedium)
            .border(1.dp, Accent, CornerMedium)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Text(label, fontSize = TextBody, fontWeight = FontWeight.Medium, color = Accent)
    }
}
