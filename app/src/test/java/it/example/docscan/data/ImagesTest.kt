package it.example.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fattore di riduzione con cui si decodificano le scansioni.
 *
 * È un calcolo su due interi, ma sbagliarlo si paga in due modi opposti e
 * entrambi visibili: un fattore troppo basso tiene in memoria decine di MB per
 * pagina e fa morire l'app per OutOfMemory con un documento lungo, uno troppo
 * alto produce un PDF sgranato in cui l'OCR non legge più niente.
 *
 * Sono le uniche righe di [Images] che si possono provare senza un dispositivo:
 * tutto il resto chiama BitmapFactory e vuole pixel veri.
 */
class ImagesTest {

    @Test
    fun `un'immagine gia piccola non viene ridotta`() {
        // Ridurre sotto l'obiettivo perderebbe dettaglio senza guadagnare nulla.
        assertEquals(1, Images.sampleSizeFor(sourceWidth = 1600, reqWidth = 1654))
        assertEquals(1, Images.sampleSizeFor(sourceWidth = 3000, reqWidth = 1654))
    }

    @Test
    fun `una scansione grande viene dimezzata`() {
        // 4000 px con obiettivo 1654: /2 dà 2000, ancora sopra; /4 darebbe 1000,
        // sotto l'obiettivo. Quindi 2.
        assertEquals(2, Images.sampleSizeFor(sourceWidth = 4000, reqWidth = 1654))
    }

    @Test
    fun `il fattore e sempre una potenza di due`() {
        // BitmapFactory arrotonda comunque inSampleSize alla potenza di due
        // inferiore: restituire 3 significherebbe ottenere 2 senza saperlo.
        for (width in listOf(800, 2000, 5000, 12000, 40000)) {
            val sample = Images.sampleSizeFor(width, reqWidth = 1654)
            assertTrue(
                "$width px ha dato un fattore non valido: $sample",
                sample > 0 && (sample and (sample - 1)) == 0,
            )
        }
    }

    @Test
    fun `la larghezza finale non scende mai sotto l'obiettivo`() {
        // È l'invariante che rende innocua la riduzione: si perde peso, non
        // leggibilità.
        val target = 1654
        for (width in listOf(1700, 2500, 4000, 9000, 20000)) {
            val finale = width / Images.sampleSizeFor(width, target)
            assertTrue("$width px ridotta a $finale, sotto l'obiettivo", finale >= target)
        }
    }

    @Test
    fun `un obiettivo a zero non manda il calcolo in ciclo infinito`() {
        // Difesa: con reqWidth 0 la condizione sarebbe sempre vera e il ciclo
        // non finirebbe mai. La guardia c'è, questo test la tiene lì.
        assertEquals(1, Images.sampleSizeFor(sourceWidth = 4000, reqWidth = 0))
    }

    @Test
    fun `un'immagine illeggibile con larghezza zero non rompe il calcolo`() {
        assertEquals(1, Images.sampleSizeFor(sourceWidth = 0, reqWidth = 1654))
    }
}
