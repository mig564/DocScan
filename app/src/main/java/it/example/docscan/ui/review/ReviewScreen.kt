package it.example.docscan.ui.review

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.R
import it.example.docscan.data.FitMode
import it.example.docscan.data.Folder as DocFolder
import it.example.docscan.data.Images
import it.example.docscan.ui.BottomSheet
import it.example.docscan.ui.ExportStage
import it.example.docscan.ui.PaperThumb
import it.example.docscan.ui.PendingScan
import it.example.docscan.ui.folderName
import it.example.docscan.ui.pendingPageLabel
import it.example.docscan.ui.theme.DangerText
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineDashed
import it.example.docscan.ui.theme.OutlineSoft
import it.example.docscan.ui.theme.Surface
import it.example.docscan.ui.theme.SurfaceContainer
import it.example.docscan.ui.theme.SurfaceDim
import java.util.Locale

// Altezze della fascia in fondo, dichiarate qui e non misurate a runtime.
// Lo spazio che la colonna riserva sotto di sé deve restare identico anche
// quando i due pulsanti spariscono: se lo ricavassimo dalla fascia com'è
// disegnata in quel momento, all'apertura della tastiera l'anteprima si
// riallargherebbe di colpo, ed è esattamente il salto da evitare.
private val FileNameFieldHeight = 58.dp   // 6.dp di stacco sopra + 52.dp di campo
private val BottomActionsHeight = 76.dp   // 8.dp sopra + 54.dp di pulsanti + 14.dp sotto
private val BottomBarHeight = FileNameFieldHeight + BottomActionsHeight

/**
 * Stacco fra il campo del nome e il bordo alto della tastiera.
 * Puoi alzarlo fino a 76.dp senza toccare altro: oltre quel valore la fascia
 * diventa più alta dello spazio riservato in fondo alla colonna e comincia a
 * coprire i controlli sopra.
 */
private val KeyboardGap = 16.dp

/**
 * Durata della comparsa e della scomparsa dei due pulsanti.
 *
 * Sta sotto ai ~250ms dell'animazione di sistema della tastiera: i pulsanti
 * devono essere già a posto quando la tastiera finisce di scendere, non
 * arrivare buoni ultimi.
 */
private const val BandAnimationMillis = 200

/** Revisione dopo la scansione: anteprima, nome file, salvataggio. */
@Composable
fun ReviewScreen(
    pending: PendingScan?,
    busy: Boolean,
    exportStage: ExportStage,
    folders: List<DocFolder>,
    onBack: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onOpenExport: () -> Unit,
    onCloseExport: () -> Unit,
    onShowFolders: () -> Unit,
    onBackToDestinations: () -> Unit,
    onSaveToFolder: (DocFolder) -> Unit,
    onExportExternal: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onRemovePage: (Int) -> Unit,
    onAddPages: () -> Unit,
    onShare: () -> Unit,
    onFitModeChange: (FitMode) -> Unit,
    captureLabel: String?,
) {
    // Lo stato "sto rinominando" segue il fuoco del campo, non l'ingombro della
    // tastiera. L'inset cresce durante l'animazione, quindi per un paio di
    // fotogrammi i controlli sono già spariti ma l'altezza non si è ancora
    // ridotta: è lì che l'anteprima faceva quel salto a schermo intero. Il fuoco
    // invece cambia di colpo al tocco.
    var renaming by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Chiudendo la tastiera in qualunque modo il cursore deve sparire: senza
    // questo resta a lampeggiare in un campo che non sta più scrivendo nessuno.
    // L'inset viene letto dentro un derivedStateOf perché durante l'animazione
    // cambia a ogni fotogramma: letto qui direttamente, ricomporrebbe l'intera
    // schermata sessanta volte al secondo per una risposta che è sempre la
    // stessa. Così la schermata si sveglia solo quando la tastiera si apre o
    // si chiude davvero.
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val imeVisible by remember(ime, density) {
        derivedStateOf { ime.getBottom(density) > 0 }
    }
    LaunchedEffect(imeVisible) {
        if (!imeVisible) focus.clearFocus()
    }

    // Il back di sistema, mentre rinomini, chiude la tastiera come la freccia.
    BackHandler(enabled = renaming) {
        keyboard?.hide()
        focus.clearFocus()
    }

    Box(Modifier.fillMaxSize().background(SurfaceDim)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                pending = pending,
                busy = busy,
                renaming = renaming,
                // Durante la rinomina la freccia chiude la tastiera invece di
                // uscire: è il passo indietro più vicino, e uscire dalla
                // revisione con un tocco solo sarebbe troppo facile per sbaglio.
                onBack = { if (renaming) { keyboard?.hide(); focus.clearFocus() } else onBack() },
            )

            // L'anteprima riempie lo spazio fra barra e controlli. Non serve
            // più congelarne l'altezza: la tastiera non tocca questa colonna,
            // quindi non c'è nulla da cui difendersi.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val fmt = pending?.scanMode?.format
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        pending == null -> Loading()

                        fmt != null -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            A4Preview(
                                pageUris = pending.pageUris,
                                format = fmt,
                                fitMode = pending.fitMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterHorizontally),
                            )
                            Text(
                                text = stringResource(R.string.a4_caption, fmt.label),
                                fontSize = 11.sp,
                                color = OnSurfaceFaint,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        else -> PagePreview(pending, busy)
                    }
                }
            }

            // I controlli restano visibili anche mentre rinomini. Nasconderli
            // liberava spazio un istante prima che la tastiera lo togliesse, e
            // in quel momento il layout si riassestava: era quel salto. A fare
            // posto al campo bastano i due pulsanti in fondo.
            if (pending != null) {
                if (pending.scanMode.format != null) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (captureLabel != null) {
                            CaptureBackButton(captureLabel, onAddPages)
                        }
                        FitModeSelector(pending.fitMode, onFitModeChange)
                    }
                } else {
                    PageStrip(pending, onSelectPage, onAddPages)
                    PageActions(pending, onRemovePage)
                }
            }

            // Spazio riservato alla fascia in fondo, che è disegnata a parte.
            // È sempre lo stesso, anche mentre i pulsanti sono nascosti: così
            // niente di ciò che sta sopra si muove quando appare la tastiera.
            Spacer(Modifier.height(BottomBarHeight))
        }

        // La fascia sale con la tastiera, il contenuto dietro no: è il motivo
        // per cui sta fuori dalla colonna e si posiziona da sola in fondo.
        if (pending != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    // Il fondo pieno serve solo quando la fascia sale sopra
                    // l'anteprima: senza, il campo galleggerebbe sopra la
                    // pagina e si leggerebbe male.
                    .background(SurfaceDim),
            ) {
                FileNameField(pending.fileName, onFileNameChange) { renaming = it }
                // Lo stacco dalla tastiera si richiude mentre i pulsanti si
                // riaprono: due animazioni della stessa durata, così il campo
                // scende con la tastiera e risale dentro la fascia in un
                // movimento solo.
                val gap by animateDpAsState(
                    targetValue = if (renaming) KeyboardGap else 0.dp,
                    animationSpec = tween(BandAnimationMillis),
                    label = "keyboardGap",
                )
                Spacer(Modifier.height(gap))
                AnimatedVisibility(
                    visible = !renaming,
                    enter = expandVertically(tween(BandAnimationMillis)) +
                            fadeIn(tween(BandAnimationMillis)),
                    exit = shrinkVertically(tween(BandAnimationMillis)) +
                            fadeOut(tween(BandAnimationMillis)),
                ) {
                    BottomActions(
                        enabled = !busy,
                        onSave = onOpenExport,
                        onShare = onShare,
                    )
                }
            }
        }

        if (pending != null && exportStage != ExportStage.CLOSED) {
            ExportSheet(
                pending = pending,
                stage = exportStage,
                folders = folders,
                onShowFolders = onShowFolders,
                onBackToDestinations = onBackToDestinations,
                onSaveToFolder = onSaveToFolder,
                onExportExternal = onExportExternal,
                onDismiss = onCloseExport,
            )
        }
    }
}

// -------------------------------------------------------------------- Parti

/** Attesa durante il riconoscimento del testo. */
@Composable
private fun Loading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(color = Green)
        Text(stringResource(R.string.review_reading), fontSize = 13.sp, color = OnSurfaceVariant)
    }
}

/** Barra con il numero di pagine acquisite. */
@Composable
private fun TopBar(
    pending: PendingScan?,
    busy: Boolean,
    renaming: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Surface)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), Modifier.size(23.dp), OnSurfaceStrong)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    if (renaming) R.string.rename_file else R.string.review_title,
                ),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
            )
            if (!renaming) {
                Text(
                    text = when {
                        pending == null -> "…"
                        busy -> stringResource(R.string.review_rereading, pendingPageLabel(pending.pageCount, pending.scanMode.isTwoSided))
                        else -> stringResource(R.string.review_enhanced, pendingPageLabel(pending.pageCount, pending.scanMode.isTwoSided))
                    },
                    fontSize = 12.sp,
                    color = OnSurfaceVariant,
                )
            }
        }

    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
}

/** Anteprima della pagina selezionata, decodificata ridotta. */
@Composable
private fun PagePreview(pending: PendingScan, busy: Boolean) {
    val context = LocalContext.current
    val uri = pending.selectedUri
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = uri?.let { Images.decodeSampled(context, it, 900) }
    }

    Box(contentAlignment = Alignment.Center) {
        // Il rapporto lo detta la scansione, non un valore fisso: un foglio
        // orizzontale o un ritaglio stretto verrebbero altrimenti incorniciati
        // in un rettangolo che non è il loro.
        val ratio = bitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 0.72f
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxHeight(0.96f)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.page_number, pending.selectedPage + 1),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PaperThumb(pending.kind, Modifier.fillMaxSize())
            }
        }

        // Senza contatore, con otto pagine si perde il conto di dove si è.
        if (pending.pageCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC1B1C19))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${pending.selectedPage + 1} / ${pending.pageCount}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                )
            }
        }

        if (busy) {
            CircularProgressIndicator(
                color = Green,
                modifier = Modifier.align(Alignment.BottomCenter).size(22.dp),
            )
        }
    }
}

/** Striscia delle pagine acquisite, con la piastrella finale per aggiungerne. */
@Composable
private fun PageStrip(pending: PendingScan, onSelectPage: (Int) -> Unit, onAddPages: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(pending.pageUris.size) { index ->
            PageThumb(
                index = index,
                selected = index == pending.selectedPage,
                pending = pending,
                onClick = { onSelectPage(index) },
            )
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 60.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .border(1.dp, OutlineDashed, RoundedCornerShape(5.dp))
                        .clickable(onClick = onAddPages),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_pages), Modifier.size(22.dp), Green)
                }
                Text(stringResource(R.string.add), fontSize = 10.sp, color = Green)
            }
        }
    }
}

/** Miniatura di una pagina nel nastro laterale. */
@Composable
private fun PageThumb(index: Int, selected: Boolean, pending: PendingScan, onClick: () -> Unit) {
    val context = LocalContext.current
    val uri = pending.pageUris.getOrNull(index)
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    // Miniature decodificate piccole: dieci pagine a piena risoluzione tenute
    // insieme in memoria farebbero saltare il processo.
    LaunchedEffect(uri) {
        bitmap = uri?.let { Images.decodeSampled(context, it, 160) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 60.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) Green else Outline,
                    shape = RoundedCornerShape(5.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = stringResource(R.string.page_number, index + 1),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = "${index + 1}",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = if (selected) Green else OnSurfaceFaint,
        )
    }
}

/** L'eliminazione nomina la pagina: una X minuscola sulla miniatura si preme per sbaglio. */
@Composable
private fun PageActions(pending: PendingScan, onRemovePage: (Int) -> Unit) {
    if (pending.pageCount <= 1) {
        Spacer(Modifier.height(4.dp))
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRemovePage(pending.selectedPage) }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Delete, null, Modifier.size(17.dp), DangerText)
            Text(
                text = stringResource(R.string.remove_page),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = DangerText,
            )
        }
    }
}

/** Nome con cui il documento verrà salvato. */
@Composable
private fun FileNameField(
    name: String,
    onChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FileNameFieldHeight)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Description, null, Modifier.size(20.dp), OnSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.file_name_label), fontSize = 10.5.sp, color = OnSurfaceVariant)
            BasicTextField(
                value = name,
                onValueChange = onChange,
                singleLine = true,
                // Il tasto d'invio chiude la tastiera e fa ricomparire i pulsanti.
                // Il fuoco si toglie subito, senza aspettare che l'inset arrivi
                // a zero: così i pulsanti si riaprono mentre la tastiera scende,
                // invece che dopo, e il movimento è uno solo.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focus.clearFocus() }),
                textStyle = TextStyle(fontSize = 14.5.sp, color = OnSurface),
                cursorBrush = SolidColor(Green),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChange(it.isFocused) },
            )
        }
    }
}

/** Condividi e salva, affiancati. */
@Composable
private fun BottomActions(enabled: Boolean, onSave: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomActionsHeight)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Condividi e' un'azione alla pari del salvataggio, non una
        // destinazione: sta accanto al pulsante, non sepolta nel foglio.
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .clickable(enabled = enabled, onClick = onShare)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Share, null, Modifier.size(20.dp), OnSurfaceStrong)
            Text(stringResource(R.string.share), fontSize = 15.sp, color = OnSurfaceStrong)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Green else Outline)
                .clickable(enabled = enabled, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Default.Save, null, Modifier.size(21.dp), Color.White)
            Text(
                stringResource(R.string.save_to_phone),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}

// ------------------------------------------------------------- Foglio salva

/** Pannello di salvataggio: destinazione, cartella, avanzamento. */
@Composable
private fun ExportSheet(
    pending: PendingScan,
    stage: ExportStage,
    folders: List<DocFolder>,
    onShowFolders: () -> Unit,
    onBackToDestinations: () -> Unit,
    onSaveToFolder: (DocFolder) -> Unit,
    onExportExternal: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(
        onDismiss = onDismiss,
        dismissible = stage != ExportStage.BUSY,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        // Altezza minima comune alle tre fasi: senza, il pannello saltava
        // passando da "dove salvo" a "quale cartella" a "sto salvando".
        Column(Modifier.heightIn(min = 248.dp)) {
            when (stage) {
                ExportStage.DESTINATIONS -> {
                    Text(stringResource(R.string.save_sheet_title, pendingPageLabel(pending.pageCount, pending.scanMode.isTwoSided)), fontSize = 19.sp, color = OnSurface)
                    Text(
                        stringResource(R.string.save_sheet_subtitle, String.format(Locale.getDefault(), "%.1f MB", pending.fileSizeMb)),
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    DestinationRow(
                        icon = Icons.Default.Folder,
                        label = stringResource(R.string.dest_archive),
                        sub = stringResource(R.string.dest_archive_desc),
                        onClick = onShowFolders,
                    )
                    Spacer(Modifier.height(8.dp))
                    DestinationRow(
                        icon = Icons.Default.Download,
                        label = stringResource(R.string.dest_external),
                        sub = stringResource(R.string.dest_external_desc),
                        onClick = onExportExternal,
                    )
                    Text(
                        stringResource(R.string.save_sheet_note),
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                ExportStage.FOLDERS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .clickable(onClick = onBackToDestinations),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back),
                                Modifier.size(21.dp),
                                OnSurfaceStrong,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.choose_folder), fontSize = 18.sp, color = OnSurface)
                            Text(stringResource(R.string.choose_folder_subtitle), fontSize = 12.5.sp, color = OnSurfaceVariant)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .heightIn(max = 186.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        folders.forEach { folder ->
                            DestinationRow(
                                icon = Icons.Default.Folder,
                                label = folderName(folder),
                                sub = stringResource(R.string.on_phone),
                                onClick = { onSaveToFolder(folder) },
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, OutlineDashed, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null, Modifier.size(22.dp), Green)
                            Text(stringResource(R.string.folders_from_archive), fontSize = 14.5.sp, color = OnSurfaceFaint)
                        }
                    }
                }

                ExportStage.BUSY -> {
                    Text(stringResource(R.string.saving), fontSize = 19.sp, color = OnSurface)
                    Text(
                        "${pending.fileName}.pdf · ${pendingPageLabel(pending.pageCount, pending.scanMode.isTwoSided)}",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Green,
                    )
                    Spacer(Modifier.height(26.dp))
                }

                ExportStage.CLOSED -> Unit
            }
        }
    }
}

/** Riga di una destinazione o di una cartella. */
@Composable
private fun DestinationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), Green)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = OnSurface)
            Text(sub, fontSize = 12.sp, color = OnSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(19.dp), OnSurfaceFaint)
    }
}