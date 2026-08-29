package it.example.docscan.ui.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.example.docscan.R
import it.example.docscan.data.DocumentRecord
import it.example.docscan.data.Folder as DocFolder
import it.example.docscan.ui.BackButton
import it.example.docscan.ui.PaperThumb
import it.example.docscan.ui.SortField
import it.example.docscan.ui.folderName
import it.example.docscan.ui.pageLabel
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.AccentContainer
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.OnAccentContainer
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceSoft
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineFaint
import it.example.docscan.ui.theme.OutlineSoft
import it.example.docscan.ui.theme.Surface
import it.example.docscan.ui.theme.SurfaceContainer
import it.example.docscan.ui.theme.TextBody
import it.example.docscan.ui.theme.TextLabel
import it.example.docscan.ui.theme.TextMeta
import it.example.docscan.ui.theme.TextSubtitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Contenuto di una cartella, una riga per documento. Nella libreria le carte
 * sono in orizzontale perché conta il colpo d'occhio; qui conta scorrere e
 * confrontare.
 */
@Composable
fun FolderScreen(
    folder: DocFolder,
    documents: List<DocumentRecord>,
    sortField: SortField,
    sortAscending: Boolean,
    onBack: () -> Unit,
    onSortFieldChange: (SortField) -> Unit,
    onToggleDirection: () -> Unit,
    onOpenDocument: (DocumentRecord) -> Unit,
    onDocumentActions: (DocumentRecord) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Surface)) {
        TopBar(folder, documents.size, onBack)
        SortBar(sortField, sortAscending, onSortFieldChange, onToggleDirection)

        if (documents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.folder_screen_empty),
                        fontSize = TextBody,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceStrong,
                    )
                    Text(
                        stringResource(R.string.folder_screen_empty_hint),
                        fontSize = TextLabel,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(documents.size, key = { documents[it].id }) { i ->
                    DocumentRow(
                        doc = documents[i],
                        onClick = { onOpenDocument(documents[i]) },
                        onLongClick = { onDocumentActions(documents[i]) },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------- Parti

/** Barra con nome della cartella e numero di documenti. */
@Composable
private fun TopBar(folder: DocFolder, count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackButton(onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(
                text = folderName(folder),
                fontSize = TextSubtitle,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.document_count, count, count),
                fontSize = TextLabel,
                color = OnSurfaceVariant,
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
}

/** Criterio di ordinamento e verso. */
@Composable
private fun SortBar(
    field: SortField,
    ascending: Boolean,
    onFieldChange: (SortField) -> Unit,
    onToggleDirection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortPill(stringResource(R.string.sort_name), field == SortField.NAME) { onFieldChange(SortField.NAME) }
        SortPill(stringResource(R.string.sort_modified), field == SortField.MODIFIED) { onFieldChange(SortField.MODIFIED) }

        Spacer(Modifier.weight(1f))

        // Una sola freccia che si inverte, non due pulsanti: lo stato attuale
        // e l'azione successiva sono la stessa cosa, e occupa metà spazio.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CornerMedium)
                .background(SurfaceContainer)
                .clickable(onClick = onToggleDirection),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (ascending) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = if (ascending) stringResource(R.string.sort_ascending)
                else stringResource(R.string.sort_descending),
                modifier = Modifier.size(20.dp),
                tint = OnSurfaceStrong,
            )
        }
    }
}

/** Pillola selezionabile del criterio. */
@Composable
private fun SortPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(CornerMedium)
            .then(
                if (selected) Modifier.background(AccentContainer)
                else Modifier.border(1.dp, Outline, CornerMedium),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = TextLabel,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) OnAccentContainer else OnSurfaceSoft,
        )
    }
}

/** Una riga: miniatura, titolo, data e menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(doc: DocumentRecord, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaperThumb(doc.kind, Modifier.size(width = 40.dp, height = 52.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = doc.title,
                fontSize = TextBody,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(doc.pageLabel(), fontSize = TextLabel, color = OnSurfaceVariant)
                Text(
                    text = "  ·  ${fullDate(doc.createdAtEpochMs)}",
                    fontSize = TextLabel,
                    fontFamily = FontFamily.Monospace,
                    color = OnSurfaceFaint,
                )
            }
            if (doc.needsReviewCount > 0) {
                Text(
                    text = stringResource(R.string.fields_to_check, doc.needsReviewCount),
                    fontSize = TextMeta,
                    color = Accent,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CornerMedium)
                .clickable(onClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.actions), Modifier.size(19.dp), OnSurfaceVariant)
        }
    }
    Box(
        Modifier
            .padding(start = 70.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(OutlineFaint),
    )
}

/** Data e ora, per distinguere scansioni dello stesso giorno. */
private fun fullDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yy HH:mm", Locale.ITALY).format(Date(epochMs))
