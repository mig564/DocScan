package it.example.docscan.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.R
import it.example.docscan.data.DocumentRecord
import it.example.docscan.data.Folder as DocFolder
import it.example.docscan.ui.FOLDER_RECENT
import it.example.docscan.ui.FOLDER_SEARCH
import it.example.docscan.ui.FilterPill
import it.example.docscan.ui.PaperThumb
import it.example.docscan.ui.UiState
import it.example.docscan.ui.folderName
import it.example.docscan.ui.pageLabel
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
    // Vero quando la tastiera occupa spazio: l'inset è l'unica fonte attendibile.
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Modalità ricerca: la barra prende il posto del titolo. Vale anche a
    // campo vuoto, perché basta toccarlo per volerla usare.
    val searching = imeVisible || state.query.isNotBlank()

    fun exitSearch() {
        onQueryChange("")
        focus.clearFocus()      // via il cursore lampeggiante
        keyboard?.hide()
    }

    Box(Modifier.fillMaxSize().background(Surface)) {
        Column(Modifier.fillMaxSize()) {
        // L'intestazione si ritira invece di sparire di colpo: 180 ms sono
        // abbastanza da leggere il movimento e troppo pochi da far aspettare.
        // L'uscita è più rapida dell'entrata, così toccando la barra la
        // reazione sembra immediata.
        AnimatedVisibility(
            visible = !searching,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(140)) + fadeOut(tween(90)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.library_title), fontSize = 26.sp, color = OnSurface)
                    Text(
                        text = stringResource(
                            R.string.library_subtitle,
                            pluralStringResource(R.plurals.scan_count, state.records.size, state.records.size),
                            pluralStringResource(R.plurals.page_count, totalPages, totalPages),
                        ),
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
            UnreadableBanner(state.unreadable.size, onPurgeUnreadable)
        }

        SearchField(state.query, searching, onQueryChange, ::exitSearch)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                bottom = if (!imeVisible && state.query.isBlank()) 108.dp else 24.dp,
            ),
        ) {

            item {
                val chips = listOf(UiState.FILTER_ALL to stringResource(R.string.filter_all)) +
                    state.folders.map { it.id to folderName(it) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { (id, label) ->
                        FilterPill(
                            label = label,
                            selected = state.filter == id,
                            onClick = { onFilterChange(id) },
                        )
                    }
                }
            }

            // Il messaggio generale compare solo quando non c'è proprio nulla
            // da mostrare. Se una mensola c'è, è lei a dire che è vuota, e lo
            // fa meglio: porta il nome della cartella.
            if (shelves.isEmpty() && !state.editing) {
                item { EmptyState(state.query.isNotBlank()) }
            }

            shelves.forEach { (folder, docs) ->
                item(key = folder.id) {
                    Shelf(
                        folder = folder,
                        docs = docs,
                        editing = state.editing &&
                            folder.id != FOLDER_RECENT &&
                            folder.id != FOLDER_SEARCH,
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

        // Il pulsante sparisce quando la tastiera è aperta o c'è una ricerca in
        // corso. La condizione guarda la tastiera e non il testo scritto: basta
        // toccare la barra di ricerca perché il pulsante salga appiccicato alla
        // tastiera, ed è quello che dava fastidio.
        }

        if (!imeVisible && state.query.isBlank()) {
            ScanBar(onScan, Modifier.align(Alignment.BottomCenter))
        }
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
            Text(stringResource(R.string.done), fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Color.White)
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
            Icon(Icons.Default.Edit, stringResource(R.string.manage_folders), Modifier.size(20.dp), OnGreenContainer)
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
            contentDescription = stringResource(R.string.settings),
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
                text = stringResource(
                    R.string.unreadable_title,
                    pluralStringResource(R.plurals.unreadable_count, count, count),
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WarnText,
            )
        }
        Text(
            text = stringResource(R.string.unreadable_body),
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
            Text(stringResource(R.string.unreadable_action), fontSize = 12.5.sp, color = WarnText)
        }
    }
}

/** Campo di ricerca nel testo delle scansioni. */
@Composable
private fun SearchField(
    query: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    onExit: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = if (searching) 10.dp else 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // In ricerca la freccia prende il posto del titolo: è il modo più
        // diretto per uscire, e libera la fascia in alto per i risultati.
        AnimatedVisibility(
            visible = searching,
            enter = expandHorizontally(tween(180)) + fadeIn(tween(220)),
            exit = shrinkHorizontally(tween(140)) + fadeOut(tween(90)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onExit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back),
                        Modifier.size(22.dp),
                        OnSurfaceStrong,
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
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
                    Text(stringResource(R.string.search_hint), fontSize = 14.5.sp, color = OnSurfaceVariant)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    textStyle = TextStyle(fontSize = 14.5.sp, color = OnSurface),
                    cursorBrush = SolidColor(Green),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // La crocetta svuota il campo senza uscire dalla ricerca.
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp), OnSurfaceVariant)
                }
            }
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
            text = if (searching) stringResource(R.string.empty_no_results) else stringResource(R.string.empty_category),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceStrong,
        )
        Text(
            text = if (searching) stringResource(R.string.empty_no_results_hint)
            else stringResource(R.string.empty_category_hint),
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
                text = folderName(folder),
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
                SquareButton(Icons.Default.Edit, stringResource(R.string.rename_folder_action), onRename)
                Spacer(Modifier.width(6.dp))
                SquareButton(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up), onMoveUp)
                Spacer(Modifier.width(6.dp))
                SquareButton(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down), onMoveDown)
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
                    Text(stringResource(R.string.delete), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DangerText)
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
                        stringResource(R.string.open_folder, folderName(folder)),
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
                    stringResource(R.string.folder_empty),
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
            text = "${doc.pageLabel()} · ${shortDate(doc.createdAtEpochMs)}",
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
        Text(stringResource(R.string.new_folder), fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Green)
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
                Text(stringResource(R.string.scan), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

/** Data breve, sotto il titolo di una carta. */
private fun shortDate(epochMs: Long): String =
    SimpleDateFormat("d MMM", Locale.ITALY).format(Date(epochMs))
