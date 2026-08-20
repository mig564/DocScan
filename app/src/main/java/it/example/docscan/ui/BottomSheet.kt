package it.example.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineFaint
import it.example.docscan.ui.theme.Scrim
import it.example.docscan.ui.theme.Surface
import kotlin.math.roundToInt

/**
 * Pannello a scomparsa dal basso, unico per tutta l'app.
 *
 * Il pannello consuma i tocchi: in Compose un elemento non cliccabile li lascia
 * passare allo scrim sottostante, che lo chiuderebbe. Si chiude solo toccando
 * fuori o trascinando la maniglia verso il basso; l'area di presa è alta 30 dp,
 * perché 4 dp non si prendono col pollice.
 */
@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    /**
     * Falso durante un'operazione che non si può interrompere: chiudere il
     * pannello mentre un salvataggio è in corso lascerebbe l'utente senza
     * sapere se è andato a buon fine.
     */
    dismissible: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val scrimSource = remember { MutableInteractionSource() }
    val sheetSource = remember { MutableInteractionSource() }

    Box(Modifier.fillMaxSize()) {
        // Solo qui il tocco chiude: è "fuori dal pannello".
        Box(
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .clickable(
                    enabled = dismissible,
                    interactionSource = scrimSource,
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offsetY { dragOffsetPx.roundToInt() }
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Surface)
                // Consuma i tocchi: senza questo un tap su una zona vuota del
                // pannello arriva allo scrim e lo chiude.
                .clickable(
                    interactionSource = sheetSource,
                    indication = null,
                    onClick = {},
                )
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .draggable(
                        enabled = dismissible,
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            // Solo verso il basso: trascinare in su non deve
                            // scollare il pannello dal fondo.
                            dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
                        },
                        onDragStopped = {
                            if (dragOffsetPx > dismissThresholdPx) onDismiss()
                            else dragOffsetPx = 0f
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (dismissible) Outline else OutlineFaint),
                )
            }

            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Sposta in verticale senza rimisurare il layout. */
private fun Modifier.offsetY(offset: () -> Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(IntOffset(0, offset()))
    }
}
