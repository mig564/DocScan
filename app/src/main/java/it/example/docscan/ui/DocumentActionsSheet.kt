package it.example.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.example.docscan.R
import it.example.docscan.data.DocumentRecord
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.DangerText
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.OnAccent
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.SurfaceContainer
import it.example.docscan.ui.theme.TextBody
import it.example.docscan.ui.theme.TextLabel
import it.example.docscan.ui.theme.TextSubtitle

/**
 * Menu che compare tenendo premuto su un documento. Bottom sheet e non menu
 * ancorato: sul telefono il pollice sta in basso.
 */
@Composable
fun DocumentActionsSheet(
    record: DocumentRecord,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(
        onDismiss = onDismiss,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 26.dp),
    ) {

            Text(
                text = record.title,
                fontSize = TextSubtitle,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.document_pdf, record.pageLabel()),
                fontSize = TextLabel,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            ActionRow(Icons.Default.Edit, stringResource(R.string.rename), Accent, onRename)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.move_to_folder), Accent, onMove)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.Default.Share, stringResource(R.string.share), Accent, onShare)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.Default.Delete, stringResource(R.string.delete), DangerText, onDelete)
    }
}

/** Riga del menu: icona, testo e azione. */
@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CornerMedium)
            .background(SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(21.dp), tint)
        Text(label, fontSize = TextBody, fontWeight = FontWeight.Medium, color = tint)
    }
}

/**
 * Dialogo per un nome. Generico perché serve identico per i documenti e per le
 * cartelle: due copie divergerebbero alla prima modifica.
 */
@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String = stringResource(R.string.save),
    /** Messaggio d'errore, oppure null se il nome va bene. */
    validate: (String) -> String? = { null },
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    // L'errore compare solo dopo un tentativo: segnalarlo mentre si digita
    // significa mostrare "nome vuoto" prima che l'utente abbia scritto nulla.
    var error by remember(initial) { mutableStateOf<String?>(null) }

    AnimatedDialog(onDismiss = onDismiss) { dialog ->
        // Confermare e premere invio fanno la stessa cosa: se il nome non va
        // bene si mostra l'errore, altrimenti si chiude e la rinomina parte a
        // uscita finita.
        fun submit() {
            val problem = validate(draft)
            if (problem != null) error = problem else dialog.confirm { onConfirm(draft) }
        }

        Text(title, fontSize = TextSubtitle, fontWeight = FontWeight.Medium, color = OnSurface)
        Spacer(Modifier.height(14.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(CornerMedium)
                .background(SurfaceContainer)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it; error = null },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                textStyle = TextStyle(fontSize = TextBody, color = OnSurface),
                cursorBrush = SolidColor(Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Spazio riservato: il dialogo non deve cambiare altezza quando
        // l'errore compare.
        Box(Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.CenterStart) {
            error?.let { Text(it, fontSize = TextLabel, color = DangerText) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(CornerMedium)
                    .border(1.dp, Outline, CornerMedium)
                    .clickable { dialog.dismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.cancel), fontSize = TextBody, color = OnSurfaceStrong)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(CornerMedium)
                    .background(Accent)
                    .clickable { submit() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    confirmLabel,
                    fontSize = TextBody,
                    fontWeight = FontWeight.Medium,
                    color = OnAccent,
                )
            }
        }
    }
}

@Composable
fun RenameDialog(record: DocumentRecord, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // Il testo si legge qui: `validate` è una lambda normale, non composable.
    val emptyError = stringResource(R.string.msg_name_empty)
    NameDialog(
        title = stringResource(R.string.rename_document),
        initial = record.title,
        validate = { if (it.trim().isBlank()) emptyError else null },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
