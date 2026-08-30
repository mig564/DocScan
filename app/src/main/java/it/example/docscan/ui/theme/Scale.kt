package it.example.docscan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Le due scale del progetto: raggi degli angoli e corpi del testo.
 *
 * Esistono perché prima non esistevano. I raggi in uso erano diciassette valori
 * diversi fra 2 e 23dp, e i corpi diciassette fra 10 e 26sp, mezzi punti
 * compresi. Nessuno dei due insieme era una scala: erano numeri scelti uno alla
 * volta, ognuno ragionevole da solo. È il motivo per cui l'interfaccia sembrava
 * assemblata invece che disegnata — quando ogni riquadro è arrotondato in modo
 * appena diverso da quello accanto, l'occhio non trova nessun ordine a cui
 * appoggiarsi, e legge il tutto come una serie di scelte automatiche.
 *
 * Da qui in poi: tre raggi e sei corpi. Se serve un valore nuovo, quasi sempre
 * vuol dire che il posto giusto è uno di questi.
 */

// ---------------------------------------------------------------- Raggi

/** Dentro le carte: miniature, quadratini, pastiglie piccole. */
val CornerSmall = RoundedCornerShape(4.dp)

/** Contenitori veri: campi, carte, fogli, riquadri di scelta. */
val CornerMedium = RoundedCornerShape(12.dp)

/**
 * Superfici che coprono la schermata: i riquadri di dialogo.
 *
 * Va usato solo dove l'altezza non è fissa. Un riquadro alto arrotondato con
 * [CornerRound] non diventa una pastiglia ma un ovale, e le curve si mangiano
 * il contenuto agli angoli — il titolo in cima sparisce.
 */
val CornerLarge = RoundedCornerShape(20.dp)

/**
 * Tondo pieno, per i pulsanti circolari e le pastiglie.
 *
 * È una percentuale, non una misura: vale metà del lato più corto. Su un
 * quadrato di 44dp dà un cerchio, su una fascia alta 38dp una pastiglia. Su
 * qualcosa di alto e senza misura fissa dà un ovale, che non è mai quello che
 * si vuole: lì ci va [CornerLarge].
 */
val CornerRound = RoundedCornerShape(50)

/** Il bordo alto dei fogli che salgono dal basso. */
val CornerSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

// ---------------------------------------------------------------- Corpi

/** Titolo di schermata. L'unico corpo grande dell'app. */
val TextDisplay = 26.sp

/** Titolo di sezione o di foglio. */
val TextTitle = 19.sp

/** Sottotitolo, voci di elenco importanti. */
val TextSubtitle = 17.sp

/** Testo corrente: pulsanti, righe, contenuto. */
val TextBody = 15.sp

/** Testo di servizio: etichette, voci secondarie. */
val TextLabel = 13.sp

/** Dati e contatori, di solito in monospazio maiuscolo. */
val TextMeta = 11.sp
