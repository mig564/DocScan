package it.example.docscan.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import it.example.docscan.R
import it.example.docscan.data.DocumentRecord
import it.example.docscan.data.Folder

/**
 * Etichette leggibili di documenti e scansioni.
 *
 * Stanno qui e non nel modello perché sono testo per l'utente, quindi dipendono
 * dalla lingua. Ogni funzione ha due forme: una composable per le schermate e
 * una che prende [Resources], per il ViewModel dove la composizione non c'è.
 */

/** "3 pagine", oppure "1 foglio A4 · fronte e retro" per una tessera. */
@Composable
fun DocumentRecord.pageLabel(): String =
    if (isSheet) stringResource(R.string.one_a4_sheet)
    else pluralStringResource(R.plurals.pages, pageCount, pageCount)

fun DocumentRecord.pageLabel(res: Resources): String =
    if (isSheet) res.getString(R.string.one_a4_sheet)
    else res.getQuantityString(R.plurals.pages, pageCount, pageCount)

/** Numero di pagine di una scansione ancora in revisione. */
@Composable
fun pendingPageLabel(pageCount: Int, isSheet: Boolean): String =
    if (isSheet) stringResource(R.string.one_a4_sheet)
    else pluralStringResource(R.plurals.pages, pageCount, pageCount)

/**
 * Etichetta di un campo estratto.
 *
 * Se è una chiave nota la traduce, altrimenti la mostra com'è: i campi aggiunti
 * a mano portano il testo scritto dall'utente, che non va tradotto.
 */
@Composable
fun fieldLabel(label: String): String {
    val id = fieldLabelRes(label) ?: return label
    return stringResource(id)
}

fun fieldLabel(res: Resources, label: String): String {
    val id = fieldLabelRes(label) ?: return label
    return res.getString(id)
}

/**
 * Risorsa di una chiave di campo, null se la chiave non è nota.
 *
 * Sta in un solo posto perché le due forme di [fieldLabel] devono restare
 * d'accordo: quando divergono, lo stesso campo esce tradotto sullo schermo e
 * come `field_total` negli appunti.
 */
private fun fieldLabelRes(label: String): Int? = when (label) {
    "field_tax_code" -> R.string.field_tax_code
    "field_surname" -> R.string.field_surname
    "field_given_names" -> R.string.field_given_names
    "field_document_number" -> R.string.field_document_number
    "field_birth_date" -> R.string.field_birth_date
    "field_expiry" -> R.string.field_expiry
    "field_nationality" -> R.string.field_nationality
    "field_total" -> R.string.field_total
    "field_taxable" -> R.string.field_taxable
    "field_vat" -> R.string.field_vat
    "field_issue_date" -> R.string.field_issue_date
    "field_vat_number" -> R.string.field_vat_number
    "field_iban" -> R.string.field_iban
    else -> null
}

/**
 * Nome di una cartella.
 *
 * Le predefinite portano una chiave e si traducono; quelle create dall'utente
 * portano il testo che ha scritto, e quello resta com'è in ogni lingua.
 */
@Composable
fun folderName(folder: Folder): String {
    val id = folderNameRes(folder.nameKey) ?: return folder.name
    return stringResource(id)
}

fun folderName(res: Resources, folder: Folder): String {
    val id = folderNameRes(folder.nameKey) ?: return folder.name
    return res.getString(id)
}

/** Risorsa di una chiave di cartella predefinita, null se la cartella è dell'utente. */
private fun folderNameRes(nameKey: String?): Int? = when (nameKey) {
    "folder_documents" -> R.string.folder_documents
    "folder_cards" -> R.string.folder_cards
    "folder_receipts" -> R.string.folder_receipts
    "folder_invoices" -> R.string.folder_invoices
    "folder_contracts" -> R.string.folder_contracts
    "unsorted" -> R.string.unsorted
    else -> null
}
