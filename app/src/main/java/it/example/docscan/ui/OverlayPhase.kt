package it.example.docscan.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Apertura e chiusura animate per un sovrapposto che il chiamante mostra con un
 * `if`.
 *
 * Il problema che risolve è sempre lo stesso: chi ci mette nella composizione lo
 * fa con una condizione, e quando quella condizione si spegne il sovrapposto
 * sparisce all'istante. Avvisare il chiamante appena si tocca "chiudi"
 * significa quindi non avere il tempo di animare l'uscita.
 *
 * Qui la chiusura è in due tempi: [close] spegne l'animazione, e solo quando è
 * finita davvero si avvisa il chiamante, che a quel punto può togliere tutto
 * dalla composizione senza che si veda un salto.
 *
 * [state] va passato a un `AnimatedVisibility(visibleState = ...)`, che è quello
 * che fa sapere alla fase quando l'animazione è conclusa.
 */
class OverlayPhase internal constructor(val state: MutableTransitionState<Boolean>) {
    /** Avvia l'uscita. Il chiamante viene avvisato quando è finita. */
    fun close() {
        state.targetState = false
    }
}

/**
 * Crea una [OverlayPhase] che si apre al primo fotogramma e chiama [onClosed]
 * quando l'uscita è terminata.
 */
@Composable
fun rememberOverlayPhase(onClosed: () -> Unit): OverlayPhase {
    val phase = remember { OverlayPhase(MutableTransitionState(false)) }

    // Al primo fotogramma siamo già nella composizione, quindi l'entrata parte
    // da chiuso e si accende subito dopo: senza questo non ci sarebbe niente da
    // animare, perché lo stato sarebbe già quello finale.
    LaunchedEffect(Unit) { phase.state.targetState = true }

    // Il chiamante può cambiare fra una composizione e l'altra; ci serve quello
    // buono al momento in cui l'uscita finisce, non quello di quando la fase è
    // stata creata.
    val notify by rememberUpdatedState(onClosed)

    // L'avviso va dato solo dopo che il sovrapposto è stato visto almeno una
    // volta. Al primo fotogramma stato corrente e obiettivo sono entrambi
    // falsi, che è indistinguibile da "uscita conclusa": senza questa guardia
    // il sovrapposto si chiuderebbe da solo appena aperto.
    var everShown by remember { mutableStateOf(false) }
    LaunchedEffect(phase.state.currentState) {
        if (phase.state.currentState) everShown = true
    }
    LaunchedEffect(phase.state.isIdle, phase.state.currentState) {
        if (everShown && phase.state.isIdle && !phase.state.currentState) notify()
    }

    return phase
}
