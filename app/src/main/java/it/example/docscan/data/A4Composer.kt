package it.example.docscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Dispone fronte e retro di una carta su un foglio A4 in scala fisica reale.
 *
 * La geometria parte dai millimetri e finisce in punti PostScript: è ciò che fa
 * sopravvivere la scala 1:1 alla stampa. Chi riceve il foglio può misurare la
 * carta col righello e trovare 85,6 mm.
 */
object A4Composer {

    /** One millimetre in PostScript points at 72 dpi. */
    private const val MM_TO_PT = 72f / 25.4f

    private const val A4_WIDTH_MM = 210f
    private const val A4_HEIGHT_MM = 297f

    /** Distanza dal bordo alto del foglio alla prima carta. */
    const val TOP_MARGIN_MM = 40f

    /** Distanza verticale fra le due facciate. */
    const val GAP_MM = 12f

    /** Image sampling width: roughly 300 dpi at card size. */
    private const val RENDER_WIDTH = 1200

    /** Rettangolo di una facciata sul foglio, in millimetri dall'angolo alto-sinistra. */
    data class Slot(val xMm: Float, val yMm: Float, val widthMm: Float, val heightMm: Float)

    /**
     * Posizione delle due facciate sul foglio, in millimetri.
     *
     * Impilate verticalmente e centrate in orizzontale, la prima a
     * [TOP_MARGIN_MM] dal bordo alto. Sotto resta spazio libero per timbri e
     * firme: 137 mm con una ID-1, 69 mm con una ID-3.
     */
    fun slots(format: CardFormat): List<Slot> {
        val x = (A4_WIDTH_MM - format.widthMm) / 2f
        val front = Slot(x, TOP_MARGIN_MM, format.widthMm, format.heightMm)
        val back = Slot(x, TOP_MARGIN_MM + format.heightMm + GAP_MM, format.widthMm, format.heightMm)
        return listOf(front, back)
    }

    /**
     * Le stesse posizioni di [slots], espresse come frazioni da 0 a 1 del foglio.
     *
     * Servono all'anteprima, che così disegna con la geometria del PDF invece di
     * numeri propri, destinati prima o poi a divergere.
     */
    fun slotFractions(format: CardFormat): List<RectF> = slots(format).map {
        RectF(
            it.xMm / A4_WIDTH_MM,
            it.yMm / A4_HEIGHT_MM,
            (it.xMm + it.widthMm) / A4_WIDTH_MM,
            (it.yMm + it.heightMm) / A4_HEIGHT_MM,
        )
    }

    val a4AspectRatio: Float get() = A4_WIDTH_MM / A4_HEIGHT_MM

    /**
     * Compone il PDF A4 a pagina singola.
     *
     * Basta una facciata: se l'utente si ferma dopo il fronte il foglio esce lo
     * stesso, con il fronte al suo posto. Le facciate oltre la seconda vengono
     * ignorate, perché sul foglio non c'è posto.
     *
     * @param pageUris fronte e, se c'è, retro, nell'ordine di acquisizione
     * @param format ID-1 per le tessere, ID-3 per il passaporto
     * @param fitMode come inserire l'immagine nel riquadro della carta
     * @return i byte del PDF, o null se le pagine non sono leggibili
     */
    suspend fun build(
        context: Context,
        pageUris: List<Uri>,
        format: CardFormat,
        fitMode: FitMode,
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (pageUris.isEmpty()) return@withContext null

        val widthPt = (A4_WIDTH_MM * MM_TO_PT).roundToInt()
        val heightPt = (A4_HEIGHT_MM * MM_TO_PT).roundToInt()

        val doc = PdfDocument()
        try {
            val info = PdfDocument.PageInfo.Builder(widthPt, heightPt, 1).create()
            val page = doc.startPage(info)
            page.canvas.drawColor(Color.WHITE)

            val paint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            val positions = slots(format)

            pageUris.take(positions.size).forEachIndexed { index, uri ->
                val bmp = Images.decodeSampled(context, uri, RENDER_WIDTH)
                    ?: return@forEachIndexed
                val slot = positions[index]
                val target = RectF(
                    slot.xMm * MM_TO_PT,
                    slot.yMm * MM_TO_PT,
                    (slot.xMm + slot.widthMm) * MM_TO_PT,
                    (slot.yMm + slot.heightMm) * MM_TO_PT,
                )
                drawInto(page.canvas, bmp, target, fitMode, paint)
                bmp.recycle()
            }

            doc.finishPage(page)
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

    /**
     * TRUE_SCALE: l'immagine entra nel rettangolo mantenendo le proporzioni. Un
     * ritaglio impreciso lascia margini bianchi, ma nessuna misura è falsata.
     * FILL: ritaglia al centro, la scala non è più garantita.
     */
    private fun drawInto(canvas: Canvas, bmp: Bitmap, target: RectF, fitMode: FitMode, paint: Paint) {
        when (fitMode) {
            FitMode.TRUE_SCALE -> {
                val scale = min(
                    target.width() / bmp.width.toFloat(),
                    target.height() / bmp.height.toFloat(),
                )
                val w = bmp.width * scale
                val h = bmp.height * scale
                val left = target.left + (target.width() - w) / 2f
                val top = target.top + (target.height() - h) / 2f
                canvas.drawBitmap(bmp, null, RectF(left, top, left + w, top + h), paint)
            }

            FitMode.FILL -> {
                val targetRatio = target.width() / target.height()
                val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
                val src = if (bmpRatio > targetRatio) {
                    val cropW = (bmp.height * targetRatio).roundToInt()
                    val x = (bmp.width - cropW) / 2
                    Rect(x, 0, x + cropW, bmp.height)
                } else {
                    val cropH = (bmp.width / targetRatio).roundToInt()
                    val y = (bmp.height - cropH) / 2
                    Rect(0, y, bmp.width, y + cropH)
                }
                canvas.drawBitmap(bmp, src, target, paint)
            }
        }
    }
}
