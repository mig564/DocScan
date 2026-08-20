package it.example.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.data.DocKind
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.OnSurfaceSoft
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.PaperEdge
import it.example.docscan.ui.theme.PaperInk
import it.example.docscan.ui.theme.PaperLine
import it.example.docscan.ui.theme.PaperLineSoft
import it.example.docscan.ui.theme.SurfaceContainer

/**
 * Anteprima stilizzata del documento.
 *
 * Non è l'immagine reale: si mostra finché la miniatura cifrata non è pronta.
 * Disegnarla a blocchi invece di un rettangolo grigio rende la libreria
 * leggibile a colpo d'occhio senza decifrare nulla.
 */
@Composable
fun PaperThumb(kind: DocKind, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, PaperEdge, RoundedCornerShape(6.dp))
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
                    Box(Modifier.width(22.dp).height(5.dp).background(Green))
                }
            }

            DocKind.FORM -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(width = 20.dp, height = 24.dp)
                            .clip(RoundedCornerShape(2.dp))
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

/** Chip filtro del design: pieno quando selezionato, contornato quando no. */
@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .then(
                if (selected) Modifier.background(Green)
                else Modifier.border(1.dp, Outline, RoundedCornerShape(9.dp)),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            color = if (selected) Color.White else OnSurfaceSoft,
        )
    }
}
