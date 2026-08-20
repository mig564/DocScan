package it.example.docscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodifica le immagini riducendole a una larghezza obiettivo.
 *
 * Una scansione a piena risoluzione occupa decine di MB: con dieci pagine e un
 * nastro di miniature si arriva presto a OutOfMemory. Due passaggi: prima solo i
 * bordi con inJustDecodeBounds, poi la decodifica già ridotta.
 */
object Images {

    /**
     * Decodifica l'immagine puntata da [uri] riducendola a circa [reqWidth]
     * pixel di larghezza.
     *
     * Due passaggi: il primo legge solo le dimensioni senza allocare i pixel, il
     * secondo decodifica già in scala ridotta. `inSampleSize` accetta solo
     * potenze di due, quindi la larghezza finale non è esatta, ma non scende mai
     * sotto l'obiettivo.
     *
     * @return null se l'immagine non è leggibile
     */
    suspend fun decodeSampled(context: Context, uri: Uri, reqWidth: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0) return@runCatching null

                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSizeFor(bounds.outWidth, reqWidth)
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }.getOrNull()
        }

    /** La potenza di due più grande che tiene la larghezza sopra l'obiettivo. */
    fun sampleSizeFor(sourceWidth: Int, reqWidth: Int): Int {
        var sample = 1
        while (reqWidth > 0 && sourceWidth / (sample * 2) >= reqWidth) {
            sample *= 2
        }
        return sample
    }
}
