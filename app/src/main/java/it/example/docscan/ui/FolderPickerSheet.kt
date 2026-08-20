package it.example.docscan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.data.Folder
import it.example.docscan.ui.theme.Green
import it.example.docscan.ui.theme.GreenContainer
import it.example.docscan.ui.theme.OnGreenContainer
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.SurfaceContainer

/**
 * Selettore di cartella, per spostare un documento già salvato.
 * La cartella attuale è segnata e non cliccabile.
 */
@Composable
fun FolderPickerSheet(
    title: String,
    subtitle: String,
    folders: List<Folder>,
    currentFolderId: String?,
    onPick: (Folder) -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheet(
        onDismiss = onDismiss,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {

            Text(title, fontSize = 18.sp, color = OnSurface)
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                folders.forEach { folder ->
                    val isCurrent = folder.id == currentFolderId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) GreenContainer else SurfaceContainer)
                            .then(
                                if (isCurrent) Modifier
                                else Modifier.clickable { onPick(folder) },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isCurrent) OnGreenContainer else Green,
                        )
                        Text(
                            text = folder.name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCurrent) OnGreenContainer else OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (isCurrent) {
                            Icon(
                                Icons.Default.Check,
                                "Cartella attuale",
                                Modifier.size(19.dp),
                                OnGreenContainer,
                            )
                        }
                    }
                }
            }
    }
}
