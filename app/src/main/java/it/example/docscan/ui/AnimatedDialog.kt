package it.example.docscan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import it.example.docscan.ui.theme.CornerLarge
import it.example.docscan.ui.theme.Scrim
import it.example.docscan.ui.theme.Surface

/** Comandi di chiusura passati al contenuto di un [AnimatedDialog]. */
class DialogHandle internal constructor(private val closeWith: ((() -> Unit)?) -> Unit) {
    /** Chiude annullando: al termine dell'uscita parte `onDismiss`. */
    fun dismiss() = closeWith(null)

    /**
     * Chiude eseguendo [action] a uscita finita.
     *
     * L'azione arriva dopo e non prima perché è quasi sempre quella che fa
     * sparire il riquadro dalla composizione: eseguirla subito toglierebbe di
     * mezzo il riquadro nel mezzo dell'animazione.
     */
    fun confirm(action: () -> Unit) = closeWith(action)
}

/**
 * Riquadro di dialogo centrato, con velo dietro e apertura animata.
 *
 * Nasce da due dialoghi — rinomina e nuovo campo — che erano la stessa
 * struttura scritta due volte: stesso velo, stesso centraggio, stessi insets,
 * stesso consumo dei tocchi. Averla in un posto solo evita che divergano, ed è
 * il motivo per cui l'animazione si scrive una volta invece che due.
 *
 * Il movimento è diverso da quello dei pannelli: un riquadro centrato non ha un
 * bordo da cui arrivare, quindi non scorre ma compare sul posto, sfumando e
 * crescendo da 0.92 alla misura piena. Chiudere è più rapido di aprire, come
 * ovunque nell'app.
 *
 * Alla chiusura toglie anche il fuoco e la tastiera. Sono riquadri con dentro
 * un campo di testo: lasciando la tastiera alzata mentre il riquadro sfuma, si
 * vedrebbero due movimenti scollegati invece di uno.
 */
@Composable
fun AnimatedDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(handle: DialogHandle) -> Unit,
) {
    // Cosa fare a uscita finita: null vuol dire "annulla".
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    val phase = rememberOverlayPhase(onClosed = { pending?.invoke() ?: onDismiss() })

    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val handle = DialogHandle { action ->
        pending = action
        focus.clearFocus(force = true)
        keyboard?.hide()
        phase.close()
    }

    // Il back lo intercetta il riquadro: passando dal ViewModel uscirebbe dalla
    // composizione prima che l'animazione possa vedersi.
    BackHandler { handle.dismiss() }

    AnimatedVisibility(
        visibleState = phase.state,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(120)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // safeDrawing tiene conto insieme di barre di sistema, ritaglio
                // del display e tastiera. Sommare systemBarsPadding e imePadding
                // contava due volte la barra di navigazione e il riquadro non
                // saliva abbastanza.
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { handle.dismiss() },
            )
            Column(
                modifier = Modifier
                    .animateEnterExit(
                        enter = scaleIn(tween(180, easing = FastOutSlowInEasing), initialScale = 0.92f),
                        exit = scaleOut(tween(130, easing = FastOutLinearInEasing), targetScale = 0.94f),
                    )
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(CornerLarge)
                    .background(Surface)
                    // Consuma i tocchi: altrimenti un tap sulla scheda arriva
                    // allo scrim sottostante e chiude il dialogo. Toccare il
                    // riquadro fuori dal campo toglie il cursore lampeggiante e
                    // la tastiera, senza chiudere.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { focus.clearFocus() }
                    // Su schermi bassi con la tastiera aperta il riquadro non ci
                    // starebbe: meglio scorrevole che tagliato.
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                content = { content(handle) },
            )
        }
    }
}
