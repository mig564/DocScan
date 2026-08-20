package it.example.docscan.ui.review

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.data.FitMode
import it.example.docscan.data.Folder as DocFolder
import it.example.docscan.data.Images
import it.example.docscan.ui.BottomSheet
import it.example.docscan.ui.ExportStage
import it.example.docscan.ui.PaperThumb
import it.example.docscan.ui.PendingScan
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
    Box(Modifier.fillMaxSize().background(SurfaceDim)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(pending, busy, onBack)

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val fmt = pending?.scanMode?.format
                when {
                    pending == null -> Loading()
                    // Il foglio A4 usa la stessa geometria del PDF: se cambia
                    // una, cambiano entrambe.
                    fmt != null -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 44.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        A4Preview(
                            pageUris = pending.pageUris,
                            format = fmt,
                            fitMode = pending.fitMode,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "A4 · ${fmt.label} · 40 mm dall'alto",
                            fontSize = 11.sp,
                            color = OnSurfaceFaint,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    else -> PagePreview(pending, busy)
                }
            }

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
                FileNameField(pending.fileName, onFileNameChange)
                BottomActions(
                    enabled = !busy,
                    onSave = onOpenExport,
                    onShare = onShare,
                )
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
        Text("Lettura del testo sul dispositivo…", fontSize = 13.sp, color = OnSurfaceVariant)
    }
}

/** Barra con il numero di pagine acquisite. */
@Composable
private fun TopBar(pending: PendingScan?, busy: Boolean, onBack: () -> Unit) {
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", Modifier.size(23.dp), OnSurfaceStrong)
        }
        Column(Modifier.weight(1f)) {
            Text("Rivedi scansione", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = OnSurface)
            Text(
                text = when {
                    pending == null -> "…"
                    busy -> "${pending.pageLabel} · rilettura del testo…"
                    else -> "${pending.pageLabel} · migliorata automaticamente"
                },
                fontSize = 12.sp,
                color = OnSurfaceVariant,
            )
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
        Box(
            modifier = Modifier
                .padding(vertical = 14.dp)
                .width(238.dp)
                .heightIn(min = 200.dp, max = 320.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Pagina ${pending.selectedPage + 1}",
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
                    Icon(Icons.Default.Add, "Aggiungi pagine", Modifier.size(22.dp), Green)
                }
                Text("Aggiungi", fontSize = 10.sp, color = Green)
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
                    contentDescription = "Pagina ${index + 1}",
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
                text = "Elimina pagina ${pending.selectedPage + 1}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = DangerText,
            )
        }
    }
}

/** Nome con cui il documento verrà salvato. */
@Composable
private fun FileNameField(name: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Description, null, Modifier.size(20.dp), OnSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text("NOME FILE", fontSize = 10.5.sp, color = OnSurfaceVariant)
            BasicTextField(
                value = name,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.5.sp, color = OnSurface),
                cursorBrush = SolidColor(Green),
                modifier = Modifier.fillMaxWidth(),
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
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Condividi e' un'azione alla pari del salvataggio, non una
        // destinazione: sta accanto al pulsante, non sepolta nel foglio.
        Row(
            modifier = Modifier
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Outline, RoundedCornerShape(16.dp))
                .clickable(enabled = enabled, onClick = onShare)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Share, null, Modifier.size(20.dp), OnSurfaceStrong)
            Text("Condividi", fontSize = 15.sp, color = OnSurfaceStrong)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Green else Outline)
                .clickable(enabled = enabled, onClick = onSave),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Default.Save, null, Modifier.size(21.dp), Color.White)
            Text(
                "Salva sul telefono",
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
                Text("Salva ${pending.pageLabel}", fontSize = 19.sp, color = OnSurface)
                Text(
                    "PDF · testo ricercabile · ~${pending.fileSizeLabel}",
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                DestinationRow(
                    icon = Icons.Default.Folder,
                    label = "Archivio DocScan",
                    sub = "Cifrato · scegli la cartella",
                    onClick = onShowFolders,
                )
                Spacer(Modifier.height(8.dp))
                DestinationRow(
                    icon = Icons.Default.Download,
                    label = "Esporta fuori dall'app",
                    sub = "Download, scheda SD o altra app",
                    onClick = onExportExternal,
                )
                Text(
                    "Nessun cloud. L'esportazione crea una copia non cifrata: scegli con cura.",
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
                            "Indietro",
                            Modifier.size(21.dp),
                            OnSurfaceStrong,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Scegli una cartella", fontSize = 18.sp, color = OnSurface)
                        Text("Archivio DocScan · sul telefono", fontSize = 12.5.sp, color = OnSurfaceVariant)
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
                            label = folder.name,
                            sub = "Sul telefono",
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
                        Text("Crea le cartelle dall'archivio", fontSize = 14.5.sp, color = OnSurfaceFaint)
                    }
                }
            }

            ExportStage.BUSY -> {
                Text("Salvataggio sul telefono…", fontSize = 19.sp, color = OnSurface)
                Text(
                    "${pending.fileName}.pdf · ${pending.pageLabel}",
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
