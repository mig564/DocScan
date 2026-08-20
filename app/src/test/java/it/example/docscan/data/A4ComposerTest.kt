package it.example.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La geometria del foglio è l'unica cosa che rende utile questa funzione: se la
 * carta non finisce dove deve, o non ci sta, il PDF è carta straccia.
 */
class A4ComposerTest {

    private val a4WidthMm = 210f
    private val a4HeightMm = 297f

    @Test
    fun `entrambi i formati stanno nel foglio`() {
        for (format in CardFormat.entries) {
            val slots = A4Composer.slots(format)
            assertEquals(2, slots.size)
            val last = slots.last()
            assertTrue(
                "${format.label} sfora in altezza",
                last.yMm + last.heightMm <= a4HeightMm,
            )
            assertTrue(
                "${format.label} sfora in larghezza",
                slots.all { it.xMm >= 0f && it.xMm + it.widthMm <= a4WidthMm },
            )
        }
    }

    @Test
    fun `il margine superiore concordato e 40 mm`() {
        assertEquals(40f, A4Composer.slots(CardFormat.ID_1).first().yMm, 0.01f)
    }

    @Test
    fun `le facciate sono centrate in orizzontale`() {
        for (format in CardFormat.entries) {
            val slot = A4Composer.slots(format).first()
            val leftMargin = slot.xMm
            val rightMargin = a4WidthMm - (slot.xMm + slot.widthMm)
            assertEquals(leftMargin, rightMargin, 0.01f)
        }
    }

    @Test
    fun `le frazioni per l anteprima corrispondono ai millimetri`() {
        val slot = A4Composer.slots(CardFormat.ID_1).first()
        val frac = A4Composer.slotFractions(CardFormat.ID_1).first()
        assertEquals(slot.yMm / a4HeightMm, frac.top, 0.0001f)
        assertEquals(slot.widthMm / a4WidthMm, frac.right - frac.left, 0.0001f)
    }

    @Test
    fun `ID-1 e ID-3 hanno le misure ISO`() {
        assertEquals(85.60f, CardFormat.ID_1.widthMm, 0.01f)
        assertEquals(53.98f, CardFormat.ID_1.heightMm, 0.01f)
        assertEquals(125.00f, CardFormat.ID_3.widthMm, 0.01f)
        assertEquals(88.00f, CardFormat.ID_3.heightMm, 0.01f)
    }
}
