package it.example.docscan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import it.example.docscan.data.SettingsStore
import it.example.docscan.ui.DocScanViewModel
import it.example.docscan.ui.DocumentActionsSheet
import it.example.docscan.ui.ExportStage
import it.example.docscan.ui.FolderPickerSheet
import it.example.docscan.ui.NameDialog
import it.example.docscan.ui.RenameDialog
import it.example.docscan.ui.Screen
import it.example.docscan.ui.WithLanguage
import it.example.docscan.ui.detail.DetailScreen
import it.example.docscan.ui.folder.FolderScreen
import it.example.docscan.ui.folderName
import it.example.docscan.ui.library.LibraryScreen
import it.example.docscan.ui.review.ReviewScreen
import it.example.docscan.ui.scan.ScanModeSheet
import it.example.docscan.ui.settings.SettingsScreen
import it.example.docscan.ui.theme.DocScanTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val vm: DocScanViewModel by viewModels()

    /**
     * Lo scanner di ML Kit ha una sua interfaccia a schermo intero, non
     * personalizzabile. Il limite di pagine dipende dalla modalità: le carte ne
     * acquisiscono una per sessione, così fronte e retro finiscono negli slot
     * giusti.
     */
    private fun scannerOptions(pageLimit: Int) = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(false)
        .setPageLimit(pageLimit)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .build()

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        val scan = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        if (scan == null) {
            // Annullato. Se c'era già una revisione in corso l'utente stava
            // aggiungendo pagine: restiamo lì, altrimenti perderebbe il lavoro fatto.
            if (vm.state.value.pending == null) vm.backToLibrary()
            return@registerForActivityResult
        }
        val pages: List<Uri> = scan.pages.orEmpty().mapNotNull { it.imageUri }
        if (pages.isEmpty()) return@registerForActivityResult
        vm.onScanned(pages)
    }

    /**
     * Esportazione fuori dalla sandbox tramite SAF.
     * Il selettore di sistema copre Download, scheda SD e qualsiasi provider
     * installato, senza richiedere alcun permesso runtime.
     */
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) vm.onExternalDestinationChosen(uri)
        else vm.closeExport()
    }

    /**
     * Selettore di cartella SAF. takePersistableUriPermission è obbligatorio:
     * senza, il permesso scade alla chiusura dell'app.
     */
    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        val label = SettingsStore(this)
            .prettyLabel(DocumentsContract.getTreeDocumentId(treeUri))
        vm.setDefaultFolder(treeUri, label)
    }

    /** Costruisce l'interfaccia e collega gli overlay alle schermate. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()

            WithLanguage(state.settings.language) {
                DocScanTheme(
                    themeMode = state.settings.themeMode,
                    accent = state.settings.accent,
                    cardStyle = state.settings.cardStyle,
                ) {

                    // I toast del design sono transitori: si spengono da soli.
                    LaunchedEffect(state.toast) {
                        if (state.toast != null) {
                            delay(4.seconds)
                            vm.consumeToast()
                        }
                    }

                    // L'Activity consuma la Uri e lancia l'intent: il ViewModel non
                    // ha bisogno di conoscere il Context dell'Activity.
                    LaunchedEffect(state.pendingShareUri) {
                        state.pendingShareUri?.let {
                            startShare(it.toUri())
                            vm.consumeShare()
                        }
                    }

                    val canGoBack = state.query.isNotBlank() ||
                        state.exportStage != ExportStage.CLOSED ||
                        state.showScanModes ||
                        state.renamingFolder != null ||
                        state.creatingFolder ||
                        state.movingDoc != null ||
                        state.showSettings ||
                        state.actionsFor != null ||
                        state.renaming != null ||
                        state.screen != Screen.LIBRARY
                    BackHandler(enabled = canGoBack) { vm.goBack() }

                    // La Surface riempie tutta la finestra, quindi il colore del
                    // tema arriva anche dietro la barra di stato: niente banda
                    // bianca in alto. L'inset lo prende solo il contenuto.
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                // Solo le barre di sistema: la tastiera la gestisce
                                // ogni schermata per conto suo, perché non tutte
                                // devono spostare tutto il contenuto verso l'alto.
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .consumeWindowInsets(WindowInsets.systemBars),
                        ) {
                            when (state.screen) {
                                Screen.LIBRARY -> LibraryScreen(
                                    state = state,
                                    shelves = vm.shelves(state),
                                    onQueryChange = vm::setQuery,
                                    onFilterChange = vm::setFilter,
                                    onToggleEditing = vm::toggleEditing,
                                    onOpenDocument = vm::openDocument,
                                    onDeleteFolder = vm::deleteFolder,
                                    onMoveFolder = vm::moveFolder,
                                    onRenameFolder = vm::startFolderRename,
                                    onPurgeUnreadable = vm::purgeUnreadable,
                                    onCreateFolder = vm::startFolderCreate,
                                    onOpenSettings = vm::openSettings,
                                    onOpenFolder = vm::openFolder,
                                    onDocumentActions = vm::showActions,
                                    onScan = vm::openScanModes,
                                )

                                Screen.FOLDER -> {
                                    val folder = state.openFolder
                                    if (folder == null) {
                                        LaunchedEffect(Unit) { vm.backToLibrary() }
                                    } else {
                                        FolderScreen(
                                            folder = folder,
                                            documents = vm.folderDocuments(state),
                                            sortField = state.sortField,
                                            sortAscending = state.sortAscending,
                                            onBack = vm::closeFolder,
                                            onSortFieldChange = vm::setSortField,
                                            onToggleDirection = vm::toggleSortDirection,
                                            onOpenDocument = vm::openDocument,
                                            onDocumentActions = vm::showActions,
                                        )
                                    }
                                }

                                Screen.REVIEW -> ReviewScreen(
                                    pending = state.pending,
                                    busy = state.busy,
                                    exportStage = state.exportStage,
                                    folders = state.folders,
                                    onBack = vm::discardScan,
                                    onFileNameChange = vm::setFileName,
                                    onOpenExport = vm::openExport,
                                    onCloseExport = vm::closeExport,
                                    onShowFolders = vm::showFolderPicker,
                                    onBackToDestinations = vm::backToDestinations,
                                    onSaveToFolder = vm::saveToFolder,
                                    onShare = vm::shareScan,
                                    onFitModeChange = vm::setFitMode,
                                    captureLabel = if (state.scanMode.isTwoSided &&
                                        !vm.bothSidesCaptured(state)
                                    ) getString(R.string.scan_back) else null,
                                    onExportExternal = {
                                        val saved = vm.defaultFolderUri()
                                        if (saved != null) vm.exportToDefaultFolder(saved.toUri())
                                        else exportLauncher.launch(vm.exportFileName())
                                    },
                                    onSelectPage = vm::selectPage,
                                    onRemovePage = vm::removePage,
                                    onAddPages = ::startScan,
                                )

                                Screen.DETAIL -> {
                                    val doc = state.openDoc
                                    if (doc == null) {
                                        LaunchedEffect(Unit) { vm.backToLibrary() }
                                    } else {
                                        DetailScreen(
                                            record = doc,
                                            toast = state.toast,
                                            loadPage = { index -> vm.pageBitmap(doc, index) },
                                            onBack = vm::goBack,
                                            onFieldChange = { i, value -> vm.updateField(doc, i, value) },
                                            onFieldAdd = { l, v -> vm.addField(doc, l, v) },
                                            onFieldRemove = { i -> vm.removeField(doc, i) },
                                            onDelete = { vm.deleteDocument(doc) },
                                            onShare = { vm.shareDocument(doc) },
                                            onCopyAll = { vm.copyAllText(doc) },
                                            onConfirmAll = { vm.confirmAllFields(doc) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    state.renamingFolder?.let { folder ->
                        NameDialog(
                            title = stringResource(R.string.rename_folder_action),
                            initial = folderName(folder),
                            validate = { vm.folderNameError(it, exceptId = folder.id) },
                            onConfirm = { vm.renameFolder(folder, it) },
                            onDismiss = vm::cancelFolderRename,
                        )
                    }

                    if (state.creatingFolder) {
                        NameDialog(
                            title = stringResource(R.string.new_folder),
                            initial = "",
                            confirmLabel = stringResource(R.string.add),
                            validate = { vm.folderNameError(it) },
                            onConfirm = vm::createFolder,
                            onDismiss = vm::cancelFolderCreate,
                        )
                    }

                    state.movingDoc?.let { doc ->
                        FolderPickerSheet(
                            title = stringResource(R.string.move_to),
                            subtitle = doc.title,
                            folders = state.folders,
                            currentFolderId = doc.folderId,
                            onPick = { vm.moveDocument(doc, it) },
                            onDismiss = vm::cancelMove,
                        )
                    }

                    if (state.showScanModes) {
                        ScanModeSheet(
                            selected = state.scanMode,
                            onSelect = vm::setScanMode,
                            onStart = { vm.closeScanModes(); startScan() },
                            onDismiss = vm::closeScanModes,
                            buttonLabel = vm.scanButtonLabel(state),
                            stepLabel = vm.scanStepLabel(state),
                        )
                    }

                    state.actionsFor?.let { doc ->
                        DocumentActionsSheet(
                            record = doc,
                            onRename = { vm.startRename(doc) },
                            onMove = { vm.startMove(doc) },
                            onShare = { vm.shareDocument(doc) },
                            onDelete = { vm.deleteDocument(doc) },
                            onDismiss = vm::hideActions,
                        )
                    }

                    state.renaming?.let { doc ->
                        RenameDialog(
                            record = doc,
                            onConfirm = { vm.renameDocument(doc, it) },
                            onDismiss = vm::cancelRename,
                        )
                    }

                    if (state.showSettings) {
                        SettingsScreen(
                            settings = state.settings,
                            onThemeChange = vm::setThemeMode,
                            onAccentChange = vm::setAccent,
                            onCardStyleChange = vm::setCardStyle,
                            onLanguageChange = vm::setLanguage,
                            onPickFolder = { folderLauncher.launch(null) },
                            onClearFolder = vm::clearDefaultFolder,
                            onBack = vm::closeSettings,
                        )
                    }
                }
            }
        }
    }

    /**
     * I PDF decifrati per la condivisione non sopravvivono al rientro nell'app.
     * Se l'utente ha gia condiviso, la copia e' partita; se ha annullato, sparisce.
     */
    override fun onResume() {
        super.onResume()
        vm.clearShareCache()
    }

    /** Apre il selettore di sistema per mandare il PDF a un'altra app. */
    private fun startShare(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(Intent.createChooser(intent, getString(R.string.share_document))) }
            .onFailure {
                Toast.makeText(this, getString(R.string.msg_no_app), Toast.LENGTH_SHORT).show()
            }
    }

    /** Avvia lo scanner di ML Kit con il limite di pagine della modalità attiva. */
    private fun startScan() {
        GmsDocumentScanning.getClient(scannerOptions(vm.pageLimitForMode()))
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    getString(R.string.msg_scanner_unavailable, it.message ?: ""),
                    Toast.LENGTH_LONG,
                ).show()
            }
    }
}
