package it.example.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.data.DocumentRecord
import it.example.docscan.ui.theme.DangerText
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.Scrim
import it.example.docscan.ui.theme.Surface
import it.example.docscan.ui.theme.SurfaceContainer

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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${record.pageLabel} · PDF",
                fontSize = 12.5.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            ActionRow(Icons.Default.Edit, "Rinomina", Green, onRename)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.AutoMirrored.Filled.DriveFileMove, "Sposta in un'altra cartella", Green, onMove)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.Default.Share, "Condividi", Green, onShare)
            Spacer(Modifier.height(6.dp))
            ActionRow(Icons.Default.Delete, "Elimina", DangerText, onDelete)
    }
}

/** Riga del menu: icona, testo e azione. */
@Composable
private fun ActionRow(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(21.dp), tint)
        Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = tint)
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
    confirmLabel: String = "Salva",
    /** Messaggio d'errore, oppure null se il nome va bene. */
    validate: (String) -> String? = { null },
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    // L'errore compare solo dopo un tentativo: segnalarlo mentre si digita
    // significa mostrare "nome vuoto" prima che l'utente abbia scritto nulla.
    var error by remember(initial) { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .clickable(onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                // Consuma i tocchi: altrimenti un tap sulla scheda arriva allo
                // scrim sottostante e chiude il dialogo.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(20.dp),
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = OnSurface)
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it; error = null },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = OnSurface),
                    cursorBrush = SolidColor(Green),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Spazio riservato: il dialogo non deve cambiare altezza quando
            // l'errore compare.
            Box(Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.CenterStart) {
                error?.let {
                    Text(it, fontSize = 12.sp, color = DangerText)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Outline, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Annulla", fontSize = 14.sp, color = OnSurfaceStrong)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Green)
                        .clickable {
                            val problem = validate(draft)
                            if (problem != null) error = problem else onConfirm(draft)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }
    }
}

/** Dialogo di rinomina di un documento. */
@Composable
fun RenameDialog(record: DocumentRecord, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    NameDialog(
        title = "Rinomina documento",
        initial = record.title,
        validate = { if (it.trim().isBlank()) "Il nome non può essere vuoto" else null },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
