package it.example.docscan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import it.example.docscan.ui.theme.CornerSheet
import it.example.docscan.ui.theme.CornerSmall
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineFaint
import it.example.docscan.ui.theme.Scrim
import it.example.docscan.ui.theme.Surface
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
    /**
     * Cosa fa il tasto indietro di sistema, se non chiudere.
     *
     * Serve ai pannelli a più stadi, dove il back deve tornare allo stadio
     * precedente invece di chiudere tutto. Lasciato a null, il back fa la
     * chiusura animata come il tocco fuori.
     */
    onBack: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val scrimSource = remember { MutableInteractionSource() }
    val sheetSource = remember { MutableInteractionSource() }

    // Apertura e chiusura animate: la logica sta in rememberOverlayPhase,
    // perché la usa anche la schermata impostazioni ed è troppo delicata per
    // averne due copie.
    val phase = rememberOverlayPhase(onClosed = onDismiss)

    fun requestDismiss() {
        if (dismissible) phase.close()
    }

    // Il back lo intercetta il pannello, non l'Activity. Passando dal ViewModel
    // il pannello uscirebbe dalla composizione all'istante e l'animazione non
    // farebbe in tempo a partire: è lo stesso taglio netto del tocco fuori,
    // solo per un'altra strada. Questo BackHandler si registra dopo quello
    // dell'Activity e quindi ha la precedenza finché il pannello è aperto.
    BackHandler(enabled = dismissible) {
        if (onBack != null) onBack() else requestDismiss()
    }

    // Un solo AnimatedVisibility per tutti e due, non uno a testa: due
    // AnimatedVisibility sullo stesso MutableTransitionState scriverebbero
    // entrambi il suo stato corrente e si darebbero fastidio. Il contenitore
    // non anima nulla di suo — serve solo a tenere il pannello nella
    // composizione finché l'uscita non è finita — e i due figli si animano
    // ciascuno per conto proprio con animateEnterExit.
    AnimatedVisibility(
        visibleState = phase.state,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Solo qui il tocco chiude: è "fuori dal pannello".
            Box(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(
                        enter = fadeIn(tween(EnterMillis)),
                        exit = fadeOut(tween(ExitMillis)),
                    )
                    .background(Scrim)
                    .clickable(
                        enabled = dismissible,
                        interactionSource = scrimSource,
                        indication = null,
                        onClick = { requestDismiss() },
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Entra rallentando, esce accelerando: un pannello che
                    // scende deve sembrare che cada, non che venga
                    // riaccompagnato giù. Il pannello scorre e basta, senza
                    // dissolvenza: un foglio pieno che sbiadisce mentre si
                    // muove sembra un errore di disegno.
                    .animateEnterExit(
                        enter = slideInVertically(tween(EnterMillis, easing = FastOutSlowInEasing)) { it },
                        exit = slideOutVertically(tween(ExitMillis, easing = FastOutLinearInEasing)) { it },
                    )
                    .offsetY { dragOffset.value.roundToInt() }
                    .fillMaxWidth()
                    .clip(CornerSheet)
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
                                scope.launch {
                                    dragOffset.snapTo((dragOffset.value + delta).coerceAtLeast(0f))
                                }
                            },
                            onDragStopped = {
                                if (dragOffset.value > dismissThresholdPx) {
                                    requestDismiss()
                                } else {
                                    // Torna su con una molla invece che di
                                    // scatto: un trascinamento non arrivato a
                                    // destinazione deve sembrare che rimbalzi
                                    // indietro, non che venga annullato.
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(CornerSmall)
                            .background(if (dismissible) Outline else OutlineFaint),
                    )
                }

                Column(Modifier.padding(contentPadding), content = content)
            }
        }
    }
}

/** Salita del pannello. */
private const val EnterMillis = 280

/** Discesa. Più rapida della salita: chiudere non deve far aspettare. */
private const val ExitMillis = 200

/** Sposta in verticale senza rimisurare il layout. */
private fun Modifier.offsetY(offset: () -> Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(IntOffset(0, offset()))
    }
}
