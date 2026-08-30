package it.example.docscan.ui.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.example.docscan.R
import it.example.docscan.data.DocumentRecord
import it.example.docscan.data.FitMode
import it.example.docscan.ui.AnimatedDialog
import it.example.docscan.ui.BackButton
import it.example.docscan.ui.PaperThumb
import it.example.docscan.ui.fieldLabel
import it.example.docscan.ui.pageLabel
import it.example.docscan.ui.review.A4Sheet
import it.example.docscan.ui.theme.CornerMedium
import it.example.docscan.ui.theme.CornerRound
import it.example.docscan.ui.theme.CornerSmall
import it.example.docscan.ui.theme.DangerText
import it.example.docscan.ui.theme.Accent
import it.example.docscan.ui.theme.AccentStrong
import it.example.docscan.ui.theme.AccentTint
import it.example.docscan.ui.theme.OnAccent
import it.example.docscan.ui.theme.OnAccentTint
import it.example.docscan.ui.theme.OnAccentTintSoft
import it.example.docscan.ui.theme.OnSurface
import it.example.docscan.ui.theme.OnSurfaceFaint
import it.example.docscan.ui.theme.OnSurfaceGhost
import it.example.docscan.ui.theme.OnSurfaceMid
import it.example.docscan.ui.theme.OnSurfaceStrong
import it.example.docscan.ui.theme.OnSurfaceVariant
import it.example.docscan.ui.theme.Outline
import it.example.docscan.ui.theme.OutlineDashed
import it.example.docscan.ui.theme.OutlineFaint
import it.example.docscan.ui.theme.OutlineSoft
import it.example.docscan.ui.theme.Surface
import it.example.docscan.ui.theme.SurfaceContainer
import it.example.docscan.ui.theme.TextBody
import it.example.docscan.ui.theme.TextLabel
import it.example.docscan.ui.theme.TextMeta
import it.example.docscan.ui.theme.TextSubtitle
import it.example.docscan.ui.theme.ToastAccent
import it.example.docscan.ui.theme.ToastBg
import it.example.docscan.ui.theme.ToastText
import it.example.docscan.ui.theme.WarnContainer
import it.example.docscan.ui.theme.WarnText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Documento aperto: pagine o foglio, dati estratti, testo. */
@Composable
fun DetailScreen(
    record: DocumentRecord,
    toast: String?,
    loadPage: suspend (Int) -> Bitmap?,
    onBack: () -> Unit,
    onFieldChange: (Int, String) -> Unit,
    onFieldAdd: (String, String) -> Unit,
    onFieldRemove: (Int) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCopyAll: () -> String,
    onConfirmAll: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboardManager.current

    Box(Modifier.fillMaxSize().background(Surface)) {
        Column(Modifier.fillMaxSize()) {
            DetailTopBar(record, onBack, onShare, onDelete)
            Tabs(tab, record.isSheet) { tab = it }

            if (tab == 0) {
                if (record.isSheet) SheetTab(record, loadPage)
                else PagesTab(record, loadPage)
            } else if (tab == 2) {
                TextTab(record)
            } else {
                DataTab(
                    record = record,
                    onFieldChange = onFieldChange,
                    onFieldAdd = onFieldAdd,
                    onFieldRemove = onFieldRemove,
                    onCopyAll = { clipboard.setText(AnnotatedString(onCopyAll())) },
                    onConfirmAll = onConfirmAll,
                )
            }
        }

        if (toast != null) {
            Toast(toast, Modifier.align(Alignment.BottomCenter))
        }
    }
}

// -------------------------------------------------------------------- Testa

/** Barra con titolo, condivisione ed eliminazione. */
@Composable
private fun DetailTopBar(record: DocumentRecord, onBack: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BackButton(onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(
                text = record.title,
                fontSize = TextSubtitle,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.detail_subtitle, record.pageLabel(), longDate(record.createdAtEpochMs)),
                fontSize = TextLabel,
                color = OnSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CornerRound)
                .clickable(onClick = onShare),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Share, stringResource(R.string.share_document), Modifier.size(21.dp), OnSurfaceStrong)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CornerRound)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Delete, stringResource(R.string.delete_document), Modifier.size(22.dp), OnSurfaceStrong)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
}

/** Le tre schede. La prima cambia nome se il documento è un foglio A4. */
@Composable
private fun Tabs(selected: Int, isSheet: Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val firstTab = if (isSheet) stringResource(R.string.tab_sheet) else stringResource(R.string.tab_pages)
        listOf(firstTab, stringResource(R.string.tab_data), stringResource(R.string.tab_text)).forEachIndexed { index, label ->
            val active = index == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.height(43.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        fontSize = TextBody,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        color = if (active) AccentStrong else OnSurfaceMid,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (active) Accent else Color.Transparent),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineSoft))
}

// -------------------------------------------------------------------- Schede

/**
 * Un documento composto su A4 va mostrato come foglio, con le facciate negli
 * slot in cui finiranno in stampa.
 */
@Composable
private fun SheetTab(record: DocumentRecord, loadPage: suspend (Int) -> Bitmap?) {
    val format = record.scanMode.format ?: return
    var front by remember(record.id) { mutableStateOf<Bitmap?>(null) }
    var back by remember(record.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(record.id) {
        front = loadPage(0)
        back = loadPage(1)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 36.dp, end = 36.dp, top = 20.dp, bottom = 28.dp),
    ) {
        item {
            A4Sheet(format) { index ->
                val bmp = if (index == 0) front else back
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = if (index == 0) stringResource(R.string.front) else stringResource(R.string.back_side),
                        contentScale = if (record.fitMode == FitMode.TRUE_SCALE)
                            ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(
                    R.string.sheet_caption,
                    format.label,
                    format.widthMm.toString(),
                    format.heightMm.toString(),
                    stringResource(record.fitMode.labelRes).lowercase(),
                ),
                fontSize = TextMeta,
                color = OnSurfaceFaint,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** Le pagine una sotto l'altra. */
@Composable
private fun PagesTab(record: DocumentRecord, loadPage: suspend (Int) -> Bitmap?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(record.pageCount, key = { "page-$it" }) { i ->
            PageImage(i, loadPage)
        }
    }
}

/** Una pagina, decifrata quando entra in vista. */
@Composable
private fun PageImage(index: Int, loadPage: suspend (Int) -> Bitmap?) {
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(index) { bitmap = loadPage(index) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CornerMedium)
            .background(Color.White)
            .border(1.dp, Outline, CornerMedium),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = stringResource(R.string.page_number, index + 1),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.decrypting), fontSize = TextLabel, color = OnSurfaceVariant)
            }
        }
    }
}

/** Elenco dei campi estratti, con le azioni in fondo. */
@Composable
private fun DataTab(
    record: DocumentRecord,
    onFieldChange: (Int, String) -> Unit,
    onFieldAdd: (String, String) -> Unit,
    onFieldRemove: (Int) -> Unit,
    onCopyAll: () -> Unit,
    onConfirmAll: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
    ) {
        item { SummaryBanner(record) }

        record.fields.forEachIndexed { index, field ->
            item(key = "field-$index") {
                FieldRow(
                    label = fieldLabel(field.label),
                    value = field.value,
                    needsReview = field.needsReview,
                    confidencePercent = field.confidencePercent,
                    onChange = { onFieldChange(index, it) },
                    onRemove = { onFieldRemove(index) },
                )
            }
        }

        if (record.fields.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_fields),
                    fontSize = TextBody,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            }
        }

        item { AddFieldButton(onFieldAdd) }
        item { Actions(onCopyAll, onConfirmAll, record.needsReviewCount) }
    }
}

/** Riepilogo in cima: quanti campi e quanti da verificare. */
@Composable
private fun SummaryBanner(record: DocumentRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .clip(CornerMedium)
            .background(AccentTint)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaperThumb(record.kind, Modifier.size(width = 58.dp, height = 76.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.text_recognized, record.pageLabel()),
                fontSize = TextBody,
                fontWeight = FontWeight.Medium,
                color = OnAccentTint,
            )
            Text(
                text = if (record.needsReviewCount > 0)
                    pluralStringResource(R.plurals.fields_summary_review, record.fields.size, record.fields.size, record.needsReviewCount)
                else
                    pluralStringResource(R.plurals.fields_summary, record.fields.size, record.fields.size),
                fontSize = TextLabel,
                color = OnAccentTintSoft,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/**
 * Riga di campo modificabile. Correggere un valore porta la confidenza al
 * massimo: un dato confermato da una persona è più affidabile dell'OCR.
 */
@Composable
private fun FieldRow(
    label: String,
    value: String,
    needsReview: Boolean,
    confidencePercent: Int,
    onChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(value) { mutableStateOf(value) }
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            fontSize = TextLabel,
            color = OnSurfaceVariant,
            modifier = Modifier.width(124.dp).padding(top = 2.dp),
        )
        Column(Modifier.weight(1f)) {
            if (editing) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = TextBody, color = OnSurface),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SmallAction(stringResource(R.string.cancel), DangerText) { draft = value; editing = false }
                    SmallAction(stringResource(R.string.confirm), Accent) { onChange(draft); editing = false }
                }
            } else {
                Text(value, fontSize = TextBody, color = OnSurface)
                if (needsReview) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .height(22.dp)
                            .clip(CornerSmall)
                            .background(WarnContainer)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Default.Info, null, Modifier.size(15.dp), WarnText)
                        Text(
                            stringResource(R.string.needs_review, confidencePercent),
                            fontSize = TextMeta,
                            fontWeight = FontWeight.Medium,
                            color = WarnText,
                        )
                    }
                }
            }
        }
        if (!editing) {
            // Icone esplicite invece di gesti: prima toccare la riga apriva la
            // modifica e copiare un singolo valore era impossibile.
            RowIcon(Icons.Default.ContentCopy, stringResource(R.string.copy_field, label)) {
                clipboard.setText(AnnotatedString(value))
            }
            RowIcon(Icons.Default.Edit, stringResource(R.string.edit_field, label)) { editing = true }
            RowIcon(Icons.Default.Close, stringResource(R.string.remove_field, label), onClick = onRemove)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineFaint))
}

/** Pulsantino di conferma o annullamento durante la modifica. */
@Composable
private fun SmallAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(CornerMedium)
            .border(1.dp, color, CornerMedium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = TextLabel, fontWeight = FontWeight.Medium, color = color)
    }
}

/** Copia tutto e conferma dei campi ancora da verificare. */
@Composable
private fun Actions(onCopyAll: () -> Unit, onConfirmAll: () -> Unit, pendingReview: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(CornerMedium)
                .border(1.dp, Outline, CornerMedium)
                .clickable(onClick = onCopyAll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Default.ContentCopy, null, Modifier.size(19.dp), OnSurfaceStrong)
            Text(stringResource(R.string.copy_all), fontSize = TextBody, color = OnSurfaceStrong)
        }
        // "Conferma" marca come verificati i campi rimasti a bassa confidenza:
        // è l'operatore che si assume la responsabilità della lettura.
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(CornerMedium)
                .background(if (pendingReview > 0) Accent else Outline)
                .clickable(enabled = pendingReview > 0, onClick = onConfirmAll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Default.Check, null, Modifier.size(19.dp), if (pendingReview > 0) OnAccent else Color.White)
            Text(
                text = if (pendingReview > 0) stringResource(R.string.confirm_count, pendingReview) else stringResource(R.string.all_verified),
                fontSize = TextBody,
                fontWeight = FontWeight.Medium,
                color = if (pendingReview > 0) OnAccent else Color.White,
            )
        }
    }
}

/** Messaggio temporaneo in fondo allo schermo. */
@Composable
private fun Toast(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 22.dp)
            .height(52.dp)
            .clip(CornerMedium)
            .background(ToastBg)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Save, null, Modifier.size(20.dp), ToastAccent)
        Text(
            text = message,
            fontSize = TextLabel,
            color = ToastText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Data estesa, per la barra del titolo. */
private fun longDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.ITALY).format(Date(epochMs))

/** Icona cliccabile in fondo alla riga di un campo. */
@Composable
private fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CornerMedium)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(17.dp), OnSurfaceGhost)
    }
}

/** Scheda Testo: contenuto OCR completo, selezionabile e copiabile. */
@Composable
private fun TextTab(record: DocumentRecord) {
    val clipboard = LocalClipboardManager.current
    val text = record.searchText

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        if (text.isBlank()) {
            item {
                Text(
                    stringResource(R.string.no_text),
                    fontSize = TextBody,
                    color = OnSurfaceVariant,
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .height(44.dp)
                        .clip(CornerMedium)
                        .border(1.dp, Outline, CornerMedium)
                        .clickable { clipboard.setText(AnnotatedString(text)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp), OnSurfaceStrong)
                    Text(stringResource(R.string.copy_all_text), fontSize = TextBody, color = OnSurfaceStrong)
                }
            }
            item {
                SelectionContainer {
                    Text(
                        text = text,
                        fontSize = TextBody,
                        lineHeight = 21.sp,
                        color = OnSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CornerMedium)
                            .background(SurfaceContainer)
                            .padding(14.dp),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.select_hint),
                    fontSize = TextLabel,
                    color = OnSurfaceFaint,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

/** Pulsante che apre il dialogo per aggiungere un campo. */
@Composable
private fun AddFieldButton(onAdd: (String, String) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(48.dp)
            .clip(CornerMedium)
            .border(1.dp, OutlineDashed, CornerMedium)
            .clickable { open = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(Icons.Default.Add, null, Modifier.size(19.dp), Accent)
        Text(stringResource(R.string.add_field), fontSize = TextBody, fontWeight = FontWeight.Medium, color = Accent)
    }

    if (open) {
        AddFieldDialog(
            onConfirm = { l, v -> onAdd(l, v); open = false },
            onDismiss = { open = false },
        )
    }
}

/** Dialogo con etichetta e valore del nuovo campo. */
@Composable
private fun AddFieldDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AnimatedDialog(onDismiss = onDismiss) { dialog ->
        Text(stringResource(R.string.new_field), fontSize = TextSubtitle, fontWeight = FontWeight.Medium, color = OnSurface)
        Spacer(Modifier.height(14.dp))
        DialogField(stringResource(R.string.field_label), label) { label = it }
        Spacer(Modifier.height(10.dp))
        DialogField(stringResource(R.string.field_value), value) { value = it }
        Spacer(Modifier.height(18.dp))
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
                    .clickable { dialog.confirm { onConfirm(label, value) } },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.add),
                    fontSize = TextBody,
                    fontWeight = FontWeight.Medium,
                    color = OnAccent,
                )
            }
        }
    }
}

/** Campo di testo con segnaposto, usato nei dialoghi. */
@Composable
private fun DialogField(placeholder: String, value: String, onChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CornerMedium)
            .background(SurfaceContainer)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = TextBody, color = OnSurfaceVariant)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            textStyle = TextStyle(fontSize = TextBody, color = OnSurface),
            cursorBrush = SolidColor(Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
