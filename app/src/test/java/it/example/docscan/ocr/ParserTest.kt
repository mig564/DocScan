package it.example.docscan.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test sulla logica di parsing: gira sulla JVM, senza emulatore.
 *
 * I valori attesi non sono inventati: il codice fiscale RSSMRA85T10A562S e le
 * cifre di controllo ICAO 9303 sono esempi di riferimento pubblicati. Se un
 * giorno qualcuno "ottimizza" l'algoritmo del check char, questi test lo
 * fermano prima che finisca in produzione.
 */
class ParserTest {

    // ------------------------------------------------------- Codice fiscale

    @Test
    fun `codice fiscale valido viene accettato`() {
        assertTrue(ItalianDocumentParser.isCodiceFiscaleValid("RSSMRA85T10A562S"))
        assertTrue(ItalianDocumentParser.isCodiceFiscaleValid("MRTMTT25D09F205Z"))
    }

    @Test
    fun `carattere di controllo errato viene respinto`() {
        assertFalse(ItalianDocumentParser.isCodiceFiscaleValid("RSSMRA85T10A562A"))
    }

    @Test
    fun `formato non valido viene respinto`() {
        assertFalse(ItalianDocumentParser.isCodiceFiscaleValid("RSSMRA85T10A56"))
        // 'G' non è una lettera-mese valida
        assertFalse(ItalianDocumentParser.isCodiceFiscaleValid("RSSMRA85G10A562S"))
    }

    @Test
    fun `codice fiscale estratto da testo rumoroso`() {
        val found = ItalianDocumentParser.findCodiceFiscale(
            "TESSERA SANITARIA\nCod. Fisc. RSSMRA85T10A562S\nScadenza 2030",
        )
        assertNotNull(found)
        assertEquals("RSSMRA85T10A562S", found!!.value)
        assertTrue(found.checksumValid)
    }

    // ---------------------------------------------------------------- MRZ

    @Test
    fun `cifre di controllo ICAO 9303`() {
        assertEquals('3', ItalianDocumentParser.checkDigit("L898902C<"))
        assertEquals('1', ItalianDocumentParser.checkDigit("690806"))
        assertEquals('6', ItalianDocumentParser.checkDigit("940623"))
    }

    @Test
    fun `MRZ TD3 di un passaporto viene letta per intero`() {
        val td3 = """
            P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<
            L898902C36UTO7408122F1204159ZE184226B<<<<<10
        """.trimIndent()

        val mrz = ItalianDocumentParser.findMrz(td3)
        assertNotNull(mrz)
        requireNotNull(mrz)

        assertEquals("ERIKSSON", mrz.surname)
        assertEquals("ANNA MARIA", mrz.givenNames)
        assertEquals("UTO", mrz.nationality)
        assertEquals("740812", mrz.birthDate.value)
        assertTrue(mrz.allChecksumsValid)
    }

    @Test
    fun `date MRZ convertite con la finestra secolo corretta`() {
        assertEquals("12/08/1974", ItalianDocumentParser.formatMrzDate("740812"))
        assertEquals("01/01/2025", ItalianDocumentParser.formatMrzDate("250101"))
    }

    // --------------------------------------------------------------- IBAN

    @Test
    fun `IBAN validato con mod 97`() {
        assertTrue(FieldExtractor.isIbanValid("IT60X0542811101000000123456"))
        assertFalse(FieldExtractor.isIbanValid("IT60X0542811101000000123457"))
    }

    // -------------------------------------------------------- Estrazione

    @Test
    fun `da una fattura estrae i campi dalle etichette`() {
        val fattura = listOf(
            "MediaLab Studio Srl",
            "Fattura n. ML-2026-0418",
            "Data emissione 18/07/2026",
            "Imponibile      2.032,79",
            "Totale          2.480,00",
            "Partita IVA 12345678903",
            "IBAN IT60 X054 2811 1010 0000 0123 456",
        )

        val fields = FieldExtractor.extract(fattura).fields

        assertEquals("ML-2026-0418", fields.first { it.label == "field_document_number" }.value)
        // Il totale viene dall'etichetta, non dall'importo più alto: qui le due
        // cose coincidono, ma l'imponibile deve restare un campo distinto.
        assertEquals("\u20AC 2.480,00", fields.first { it.label == "field_total" }.value)
        assertEquals("\u20AC 2.032,79", fields.first { it.label == "field_taxable" }.value)
        assertEquals(1f, fields.first { it.label == "field_vat_number" }.confidence, 0.001f)
        assertEquals(1f, fields.first { it.label == "field_iban" }.confidence, 0.001f)
    }

    @Test
    fun `una pagina di prosa non produce campi inventati`() {
        // Regressione: "n." non deve valere come inizio di numero documento.
        // Con IGNORE_CASE ogni parola che comincia per "n" diventava un numero
        // di fattura: "Nonostante" dava "onostante".
        val libro = listOf(
            "Il capitolo si apre con una considerazione sul metodo.",
            "Nonostante le apparenze, la questione non era risolta.",
            "Numerosi studiosi hanno proposto letture alternative.",
        )

        val result = FieldExtractor.extract(libro)
        assertEquals(FieldExtractor.DocType.GENERIC, result.docType)
        assertTrue(result.fields.isEmpty())
    }

    @Test
    fun `partita IVA con cifra di controllo errata viene scartata`() {
        assertTrue(FieldExtractor.isPartitaIvaValid("12345678903"))
        assertFalse(FieldExtractor.isPartitaIvaValid("12345678901"))

        val fattura = listOf("Acme Srl", "Fattura n. 2026/77", "Totale 120,00", "Partita IVA 12345678901")
        val fields = FieldExtractor.extract(fattura).fields
        assertFalse(fields.any { it.label == "field_vat_number" })
    }

    @Test
    fun `date impossibili vengono rifiutate`() {
        val doc = listOf("Ricevuta n. 5", "Totale 10,00", "Data 45/19/2026")
        val fields = FieldExtractor.extract(doc).fields
        assertFalse(fields.any { it.label == "field_issue_date" })
    }

    @Test
    fun `l'imposta non si prende il totale della riga sotto`() {
        // Regressione: "partita iva" contiene "iva", quindi anche l'etichetta
        // dell'imposta matchava questa riga. Non trovando un importo accanto a
        // sé, scendeva di una riga e si portava via il totale.
        val fattura = listOf("Acme Srl", "Fattura n. 7", "Partita IVA 12345678903", "Totale 2.480,00")

        val fields = FieldExtractor.extract(fattura).fields

        assertFalse(fields.any { it.label == "field_vat" })
        assertEquals("\u20AC 2.480,00", fields.first { it.label == "field_total" }.value)
    }

    @Test
    fun `un numero documento di una sola cifra viene riconosciuto`() {
        // Regressione: il numero era troppo corto per la regex, quindi il campo
        // scendeva alla riga successiva e si prendeva la partita IVA. Il numero
        // documento non ha cifra di controllo: qualsiasi cosa peschi, ci crede.
        val fattura = listOf("Acme Srl", "Fattura n. 7", "Partita IVA 12345678903", "Totale 2.480,00")

        val fields = FieldExtractor.extract(fattura).fields

        assertEquals("7", fields.first { it.label == "field_document_number" }.value)
    }

    @Test
    fun `i campi MRZ escono con chiavi traducibili`() {
        // Regressione: numero documento, data di nascita e scadenza avevano
        // l'etichetta scritta a mano in italiano, e restavano in italiano con
        // l'app in inglese.
        val cie = listOf(
            "RSSMRA85T10A562S",
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10",
        )

        val fields = FieldExtractor.extract(cie).fields

        assertTrue(fields.any { it.label == "field_document_number" })
        assertTrue(fields.any { it.label == "field_birth_date" })
        assertTrue(fields.any { it.label == "field_expiry" })
    }

    @Test
    fun `le reti di sicurezza non duplicano i campi già trovati`() {
        // Regressione: le due guardie confrontavano "Totale" e "IBAN" con un
        // insieme che contiene le chiavi field_*, quindi non scattavano mai e
        // il ripiego girava anche quando l'etichetta era già stata letta.
        val fattura = listOf(
            "Fattura n. 12",
            "Totale 2.480,00",
            "IBAN IT60 X054 2811 1010 0000 0123 456",
        )

        val fields = FieldExtractor.extract(fattura).fields

        assertEquals(1, fields.count { it.label == "field_total" })
        assertEquals(1, fields.count { it.label == "field_iban" })
    }

    @Test
    fun `su un documento d'identità non inventa campi commerciali`() {
        val cie = listOf(
            "REPUBBLICA ITALIANA",
            "RSSMRA85T10A562S",
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10",
        )

        val result = FieldExtractor.extract(cie)
        assertEquals(FieldExtractor.DocType.IDENTITY, result.docType)
        assertTrue(result.fields.any { it.label == "field_tax_code" })
        assertFalse(result.fields.any { it.label == "field_total" || it.label == "field_vat_number" })
    }

    @Test
    fun `il numero di carta di pagamento non viene conservato`() {
        assertTrue(FieldExtractor.isLuhnValid("4539 5787 6362 1486"))
        assertFalse(FieldExtractor.isLuhnValid("4539578763621487"))

        val carta = listOf(
            "BANCA ESEMPIO",
            "Carta di credito",
            "4539 5787 6362 1486",
            "Scadenza 08/29",
            "Totale 0,00",
        )
        val result = FieldExtractor.extract(carta)

        // Il PAN non deve finire nè nei campi nè nel testo ricercabile.
        assertFalse(result.fields.any { it.value.contains("4539") })
        assertFalse(result.searchableText.contains("4539"))
        // Le ultime quattro cifre restano: servono a riconoscere la carta e da
        // sole non consentono nulla.
        assertTrue(result.searchableText.contains("1486"))
    }

    @Test
    fun `un numero lungo che non e un PAN resta leggibile`() {
        val text = "Protocollo 2026000123456789 del 18/07/2026"
        assertTrue(FieldExtractor.redactPaymentNumbers(text).contains("2026000123456789"))
    }
}
