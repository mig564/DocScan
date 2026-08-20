package it.example.docscan.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.data.DocumentRecord
import it.example.docscan.data.Folder as DocFolder
import it.example.docscan.ui.FOLDER_RECENT
import it.example.docscan.ui.FilterPill
import it.example.docscan.ui.PaperThumb
import it.example.docscan.ui.UiState
import it.example.docscan.ui.theme.BottomBar
import it.example.docscan.ui.theme.DangerContainer
import it.example.docscan.ui.theme.DangerText
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.GreenContainer
import it.example.docscan.ui.theme.OnGreenContainer
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceGhost
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineDashed
import it.example.docscan.ui.theme.OutlineSoft
import it.example.docscan.ui.theme.PaperStack1
import it.example.docscan.ui.theme.PaperStack2
import it.example.docscan.ui.theme.Surface
import it.example.docscan.ui.theme.SurfaceContainer
import it.example.docscan.ui.theme.SurfaceHigh
import it.example.docscan.ui.theme.WarnContainer
import it.example.docscan.ui.theme.WarnText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Schermata principale: ricerca, filtri, mensole e pulsante Scansiona. */
@Composable
fun LibraryScreen(
    state: UiState,
    shelves: List<Pair<DocFolder, List<DocumentRecord>>>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onToggleEditing: () -> Unit,
    onOpenDocument: (DocumentRecord) -> Unit,
    onDeleteFolder: (DocFolder) -> Unit,
    onMoveFolder: (DocFolder, Int) -> Unit,
    onRenameFolder: (DocFolder) -> Unit,
    onPurgeUnreadable: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (DocFolder) -> Unit,
    onDocumentActions: (DocumentRecord) -> Unit,
    onScan: () -> Unit,
) {
    val totalPages = state.records.sumOf { it.pageCount }

    Box(Modifier.fillMaxSize().background(Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 108.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Documenti", fontSize = 26.sp, color = OnSurface)
                        Text(
                            text = "${state.records.size} scansioni · $totalPages pagine · solo su questo telefono",
                            fontSize = 12.5.sp,
                            color = OnSurfaceVariant,
                        )
                    }
                    EditToggle(editing = state.editing, onClick = onToggleEditing)
                    Spacer(Modifier.width(6.dp))
                    // Ancorato al bordo: la pillola "Fine" si allarga verso
                    // sinistra e l'ingranaggio non si sposta mai.
                    SettingsButton(enabled = !state.editing, onClick = onOpenSettings)
                }
            }

            if (state.unreadable.isNotEmpty()) {
                item { UnreadableBanner(state.unreadable.size, onPurgeUnreadable) }
            }

            item { SearchField(state.query, onQueryChange) }

            item {
                val labels = listOf(UiState.FILTER_ALL) + state.folders.map { it.name }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    labels.forEach { label ->
                        FilterPill(
                            label = label,
                            selected = state.filter == label,
                            onClick = { onFilterChange(label) },
                        )
                    }
                }
            }

            if (shelves.all { it.second.isEmpty() } && !state.editing) {
                item { EmptyState(state.query.isNotBlank()) }
            }

            shelves.forEach { (folder, docs) ->
                item(key = folder.id) {
                    Shelf(
                        folder = folder,
                        docs = docs,
                        editing = state.editing && folder.id != FOLDER_RECENT,
                        onOpenDocument = onOpenDocument,
                        onDocumentActions = onDocumentActions,
                        onOpenFolder = { onOpenFolder(folder) },
                        onRename = { onRenameFolder(folder) },
                        onDelete = { onDeleteFolder(folder) },
                        onMoveUp = { onMoveFolder(folder, -1) },
                        onMoveDown = { onMoveFolder(folder, 1) },
                    )
                }
            }

            if (state.editing) {
                item { NewFolderButton(onCreateFolder) }
            }
        }

        ScanBar(onScan, Modifier.align(Alignment.BottomCenter))
    }
}

// ------------------------------------------------------------------ Sezioni

/** Passa fra modalità normale e modifica delle cartelle. */
@Composable
private fun EditToggle(editing: Boolean, onClick: () -> Unit) {
    if (editing) {
        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(Green)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Fine", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    } else {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(GreenContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Edit, "Gestisci cartelle", Modifier.size(20.dp), OnGreenContainer)
        }
    }
}

/** Ingranaggio, spento durante la modifica. */
@Composable
private fun SettingsButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .border(1.dp, Outline, RoundedCornerShape(19.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Impostazioni",
            modifier = Modifier.size(20.dp),
            tint = if (enabled) OnSurfaceStrong else OnSurfaceGhost,
        )
    }
}

/**
 * Avviso sui file dell'archivio che non si riescono a leggere. Senza, quei
 * documenti sparirebbero in silenzio e l'archivio sembrerebbe vuoto.
 */
@Composable
private fun UnreadableBanner(count: Int, onPurge: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WarnContainer)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, Modifier.size(19.dp), WarnText)
            Spacer(Modifier.width(9.dp))
            Text(
                text = if (count == 1) "1 documento non leggibile"
                else "$count documenti non leggibili",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WarnText,
            )
        }
        Text(
            text = "I file sono presenti ma non si riesce a decifrarli. Potrebbero essere " +
                "recuperabili da un backup del telefono: rimuovili solo se sei sicuro.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = WarnText,
            modifier = Modifier.padding(top = 6.dp),
        )
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, WarnText, RoundedCornerShape(9.dp))
                .clickable(onClick = onPurge)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Rimuovi i file danneggiati", fontSize = 12.5.sp, color = WarnText)
        }
    }
}

/** Campo di ricerca nel testo delle scansioni. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(SurfaceContainer)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(20.dp), OnSurfaceVariant)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Cerca scansione", fontSize = 14.5.sp, color = OnSurfaceVariant)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.5.sp, color = OnSurface),
                cursorBrush = SolidColor(Green),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Messaggio quando non c'è nulla da mostrare. */
@Composable
private fun EmptyState(searching: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Folder, null, Modifier.size(40.dp), Outline)
        Text(
            text = if (searching) "Nessun risultato" else "Nessuna scansione in questa categoria",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceStrong,
        )
        Text(
            text = if (searching) "Prova con un altro termine."
            else "Tocca Scansiona per aggiungere il primo documento.",
            fontSize = 13.sp,
            color = OnSurfaceVariant,
        )
    }
}

/** Una cartella con i suoi documenti in orizzontale. */
@Composable
private fun Shelf(
    folder: DocFolder,
    docs: List<DocumentRecord>,
    editing: Boolean,
    onOpenDocument: (DocumentRecord) -> Unit,
    onDocumentActions: (DocumentRecord) -> Unit,
    onOpenFolder: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                modifier = Modifier.clickable(enabled = !editing, onClick = onOpenFolder),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = docs.size.toString(),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = OnSurfaceFaint,
            )
            Spacer(Modifier.weight(1f))
            if (editing) {
                SquareButton(Icons.Default.Edit, "Rinomina cartella", onRename)
                Spacer(Modifier.width(6.dp))
                SquareButton(Icons.Default.KeyboardArrowUp, "Sposta su", onMoveUp)
                Spacer(Modifier.width(6.dp))
                SquareButton(Icons.Default.KeyboardArrowDown, "Sposta giù", onMoveDown)
                Spacer(Modifier.width(6.dp))
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DangerContainer)
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(17.dp), DangerText)
                    Text("Elimina", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DangerText)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .clickable(onClick = onOpenFolder),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Apri ${folder.name}",
                        Modifier.size(20.dp),
                        OnSurfaceVariant,
                    )
                }
            }
        }

        if (docs.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Cartella vuota — scansiona un documento per riempirla",
                    fontSize = 12.5.sp,
                    color = OnSurfaceFaint,
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(docs.size) { i ->
                    DocCard(
                        doc = docs[i],
                        onClick = { onOpenDocument(docs[i]) },
                        onLongClick = { onDocumentActions(docs[i]) },
                    )
                }
            }
        }
    }
}

/** Pulsante quadrato usato nella modalità modifica. */
@Composable
private fun SquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(18.dp), OnSurfaceStrong)
    }
}

/** Carta di un documento. Pressione lunga per il menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocCard(doc: DocumentRecord, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier
            .width(104.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            // Le due carte dietro suggeriscono lo spessore del documento.
            Box(
                Modifier
                    .padding(start = 5.dp, top = 6.dp)
                    .size(width = 104.dp, height = 132.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PaperStack2),
            )
            Box(
                Modifier
                    .padding(start = 3.dp, top = 3.dp)
                    .size(width = 104.dp, height = 132.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PaperStack1),
            )
            PaperThumb(
                kind = doc.kind,
                modifier = Modifier.size(width = 104.dp, height = 138.dp),
            )
        }
        Text(
            text = doc.title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            text = "${doc.pageLabel} · ${shortDate(doc.createdAtEpochMs)}",
            fontSize = 11.sp,
            color = OnSurfaceFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Pulsante tratteggiato per creare una cartella. */
@Composable
private fun NewFolderButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, OutlineDashed, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.CreateNewFolder, null, Modifier.size(22.dp), Green)
        Text("Nuova cartella", fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Green)
    }
}

/** Barra in fondo con il pulsante Scansiona. */
@Composable
private fun ScanBar(onScan: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .background(BottomBar),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Green)
                    .clickable(onClick = onScan)
                    .padding(start = 22.dp, end = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(25.dp), Color.White)
                Text("Scansiona", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

/** Data breve, sotto il titolo di una carta. */
private fun shortDate(epochMs: Long): String =
    SimpleDateFormat("d MMM", Locale.ITALY).format(Date(epochMs))
