package it.example.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.example.docscan.R
import it.example.docscan.data.CardStyle
import it.example.docscan.data.DocKind
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.CornerRound
import it.example.docscan.ui.theme.CornerSmall
import it.example.docscan.ui.theme.LocalCardStyle
import it.example.docscan.ui.theme.OnAccent
import it.example.docscan.ui.theme.OnSurfaceSoft
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.PaperEdge
import it.example.docscan.ui.theme.PaperSheen
import it.example.docscan.ui.theme.PaperInk
import it.example.docscan.ui.theme.PaperLine
import it.example.docscan.ui.theme.PaperLineSoft
import it.example.docscan.ui.theme.SurfaceContainer
import it.example.docscan.ui.theme.TextLabel

/**
 * Anteprima stilizzata del documento.
 *
 * Non è l'immagine reale: si mostra finché la miniatura cifrata non è pronta.
 * Disegnarla a blocchi invece di un rettangolo grigio rende la libreria
 * leggibile a colpo d'occhio senza decifrare nulla.
 */
@Composable
fun PaperThumb(kind: DocKind, modifier: Modifier = Modifier, shape: Shape = CornerSmall) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(PaperSheen)
            .border(1.dp, PaperEdge, shape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        when (kind) {
            DocKind.RECEIPT -> {
                Bar(0.44f, 6.dp, PaperInk, Alignment.CenterHorizontally)
                Bar(0.30f, 3.dp, PaperLine, Alignment.CenterHorizontally)
                Spacer(Modifier.height(2.dp))
                repeat(3) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Box(Modifier.fillMaxWidth(0.42f).height(3.dp).background(PaperLineSoft))
                        Box(Modifier.width(16.dp).height(3.dp).background(PaperLineSoft))
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(PaperEdge))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Box(Modifier.width(24.dp).height(5.dp).background(PaperInk))
                    Box(Modifier.width(22.dp).height(5.dp).background(Accent))
                }
            }

            DocKind.FORM -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(width = 20.dp, height = 24.dp)
                            .clip(CornerSmall)
                            .background(SurfaceContainer),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(Modifier.fillMaxWidth().height(5.dp).background(PaperInk))
                        Box(Modifier.fillMaxWidth(0.6f).height(3.dp).background(PaperLine))
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(PaperEdge))
                listOf(0.88f, 0.96f, 0.72f, 0.92f, 0.40f).forEach {
                    Box(Modifier.fillMaxWidth(it).height(3.dp).background(PaperLineSoft))
                }
            }
        }
    }
}

/** Barretta orizzontale usata nelle anteprime di carta. */
@Composable
private fun Bar(fraction: Float, height: Dp, color: Color, align: Alignment.Horizontal) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align,
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(height)
                .background(color),
        )
    }
}

/**
 * Freccia indietro circolare, uguale in tutte le barre.
 *
 * Le misure sono parametri perché due punti la vogliono più piccola — la barra
 * della selezione e il foglio di salvataggio — ma restano gli stessi 44.dp
 * ovunque non venga detto il contrario: sotto quella soglia il bersaglio da
 * toccare diventa scomodo.
 */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 23.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CornerRound)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            modifier = Modifier.size(iconSize),
            tint = OnSurfaceStrong,
        )
    }
}

/**
 * Filtro della libreria, nei due aspetti scelti nelle impostazioni.
 *
 * Con le carte arrotondate è la pastiglia di sempre: piena d'accento quando è
 * attiva, contornata quando non lo è. Con le sottolineate è un'etichetta nuda
 * con il filetto sotto.
 *
 * I due aspetti stanno nello stesso componente e non in due perché la scelta
 * non è del chiamante: la libreria chiede un filtro, l'impostazione decide come
 * si veda. L'altezza è 34dp in entrambi i casi anche quando il riquadro non si
 * vede, perché il bersaglio da toccare non deve rimpicciolirsi solo perché è
 * sparito lo sfondo.
 */
@Composable
fun LibraryFilter(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalCardStyle.current == CardStyle.ROUNDED) {
        Box(
            modifier = modifier
                .height(34.dp)
                .clip(CornerMedium)
                .then(
                    if (selected) Modifier.background(Accent)
                    else Modifier.border(1.dp, Outline, CornerMedium),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = TextLabel,
                color = if (selected) OnAccent else OnSurfaceSoft,
            )
        }
        return
    }

    Column(
        // La larghezza è quella del testo su una riga sola: serve perché il
        // filetto sotto possa essere largo quanto la parola invece che quanto
        // lo spazio disponibile, che è tutta la riga.
        modifier = modifier
            .height(34.dp)
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = TextLabel,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) Accent else OnSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) Accent else Color.Transparent),
        )
    }
}
