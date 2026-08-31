package it.example.docscan.data

import android.content.Context
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Costruisce il PDF dalle pagine acquisite.
 *
 * ML Kit ne fornisce già uno, ma descrive la sessione com'era: appena l'utente
 * elimina o aggiunge pagine non corrisponde più. Generarlo al salvataggio evita
 * che i due divergano.
 */
object PdfBuilder {

    // A4 in punti PostScript, a 72 dpi.
    private const val A4_SHORT = 595
    private const val A4_LONG = 842

    /** Campionamento delle pagine: circa 200 dpi su A4, leggibile senza pesare. */
    private const val RENDER_WIDTH = 1654

    /**
     * Genera il PDF, una pagina per immagine.
     *
     * Ogni pagina prende le dimensioni della propria immagine: un documento con
     * pagine ruotate in modo diverso resta corretto senza doverle uniformare.
     * Le immagini si decodificano ridotte una alla volta e si liberano subito,
     * perché dieci pagine a piena risoluzione non starebbero in memoria insieme.
     *
     * @return i byte del PDF, o null se nessuna pagina è leggibile
     */
    suspend fun build(context: Context, pageUris: List<Uri>): ByteArray? =
        withContext(Dispatchers.IO) {
            if (pageUris.isEmpty()) return@withContext null
            val doc = PdfDocument()
            try {
                var pageNumber = 1
                for (uri in pageUris) {
                    val bmp = Images.decodeSampled(context, uri, RENDER_WIDTH) ?: continue

                    // La pagina segue l'orientamento dell'immagine: forzare una
                    // scansione orizzontale in un A4 verticale sprecherebbe
                    // mezzo foglio.
                    val landscape = bmp.width > bmp.height
                    val pw = if (landscape) A4_LONG else A4_SHORT
                    val ph = if (landscape) A4_SHORT else A4_LONG

                    val info = PdfDocument.PageInfo.Builder(pw, ph, pageNumber).create()
                    val page = doc.startPage(info)

                    val scale = min(pw / bmp.width.toFloat(), ph / bmp.height.toFloat())
                    val w = bmp.width * scale
                    val h = bmp.height * scale
                    val left = (pw - w) / 2f
                    val top = (ph - h) / 2f
                    page.canvas.drawBitmap(bmp, null, RectF(left, top, left + w, top + h), null)

                    doc.finishPage(page)
                    bmp.recycle()
                    pageNumber++
                }
                if (pageNumber == 1) return@withContext null
                ByteArrayOutputStream().use { out ->
                    doc.writeTo(out)
                    out.toByteArray()
                }
            } catch (_: Exception) {
                null
            } finally {
                doc.close()
            }
        }
}
