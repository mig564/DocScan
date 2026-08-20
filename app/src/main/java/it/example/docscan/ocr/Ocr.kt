package it.example.docscan.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Riconoscimento del testo, interamente sul dispositivo.
 *
 * Il modello text-recognition è dentro l'APK: nessuna chiamata di rete, funziona
 * in modalità aereo. Il manifest non dichiara il permesso INTERNET, il che rende
 * la cosa verificabile dall'esterno.
 */
class Ocr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Righe riconosciute, nell'ordine in cui compaiono sulla pagina.
     *
     * Su una fattura etichetta e valore stanno sulla stessa riga
     * ("Totale   € 2.480,00"): appiattire la pagina in una stringa butta via il
     * segnale più affidabile e costringe a indovinare con le regex.
     */
    suspend fun readLines(context: Context, imageUri: Uri): List<String> =
        suspendCancellableCoroutine { cont ->
            val image = try {
                InputImage.fromFilePath(context, imageUri)
            } catch (e: Exception) {
                cont.resumeWithException(e)
                return@suspendCancellableCoroutine
            }
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks
                        .flatMap { block -> block.lines }
                        .map { it.text.trim() }
                        .filter { it.isNotBlank() }
                    cont.resume(lines)
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /** Righe di tutte le pagine, nell'ordine di scansione. */
    suspend fun readAllLines(context: Context, imageUris: List<Uri>): List<String> {
        val all = mutableListOf<String>()
        for (uri in imageUris) {
            all += runCatching { readLines(context, uri) }.getOrDefault(emptyList())
        }
        return all
    }

    /** Rilascia il riconoscitore. Da chiamare quando il ViewModel viene distrutto. */
    fun close() = recognizer.close()
}
