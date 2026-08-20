package it.example.docscan.ocr

import it.example.docscan.data.DocKind
import it.example.docscan.data.ExtractedField

/**
 * Estrae campi etichettati dal testo riconosciuto.
 *
 * Tre regole:
 *
 * 1. Ancorarsi alle etichette, non ai formati. Su una fattura il valore sta
 *    sulla stessa riga della sua etichetta ("Totale € 2.480,00"). Cercare il
 *    solo formato produce falsi positivi.
 * 2. Non estrarre da pagine che non sono documenti. La pagina viene prima
 *    classificata: identità, commerciale o generica. Su una generica non si
 *    estrae nulla.
 * 3. Verificare il verificabile. Codice fiscale, MRZ, IBAN e partita IVA hanno
 *    cifre di controllo: chi le passa vale 100%, chi no lo dichiara.
 */
object FieldExtractor {

    /** Sotto questa soglia il campo non viene mostrato: è rumore, non un dato. */
    private const val CONFIDENCE_FLOOR = 0.5f

    enum class DocType { IDENTITY, COMMERCIAL, GENERIC }

    data class Result(
        val fields: List<ExtractedField>,
        val kind: DocKind,
        val suggestedTitle: String,
        val docType: DocType,
        /**
         * Testo da salvare per la ricerca, con i numeri di carta di pagamento
         * mascherati. Non è il testo grezzo: vedi [redactPaymentNumbers].
         */
        val searchableText: String = "",
    )

    // ------------------------------------------------------------- Pattern

    /** Importo in formato italiano: 2.480,00 oppure 2480,00. */
    private val AMOUNT = Regex("""\d{1,3}(?:[.\u00A0 ]\d{3})*,\d{2}|\d+,\d{2}""")

    private val IBAN = Regex("""\bIT\d{2}(?:\s?[A-Z0-9]){23}\b""", RegexOption.IGNORE_CASE)

    /** Un numero documento contiene sempre almeno una cifra. */
    private val DOC_NUMBER = Regex("""\b(?=[A-Za-z0-9/\-]*\d)[A-Za-z0-9][A-Za-z0-9/\-]{2,}\b""")

    private val DATE = Regex("""\b(\d{1,2})[./\-](\d{1,2})[./\-](\d{2,4})\b""")

    private val VAT_NUMBER = Regex("""\b\d{11}\b""")

    /**
     * Parole che indicano un documento commerciale. Servono almeno due segnali
     * distinti: "totale" da solo compare anche in un tema di matematica.
     */
    private val COMMERCIAL_HINTS = listOf(
        "fattura", "ricevuta", "scontrino", "imponibile", "iva",
        "partita iva", "p.iva", "totale", "importo", "pagamento",
        "cliente", "fornitore", "quantit", "prezzo", "subtotale", "€", "eur",
    )

    // -------------------------------------------------------------- Etichette

    private data class Label(
        val name: String,
        val keys: List<String>,
        val kind: ValueKind,
    )

    private enum class ValueKind { AMOUNT, DATE, DOC_NUMBER, VAT, IBAN_VALUE }

    private val LABELS = listOf(
        // L'ordine conta: "totale documento" prima di "totale", altrimenti la
        // chiave più corta vincerebbe sempre.
        Label("Totale", listOf("totale documento", "totale da pagare", "importo totale", "totale complessivo", "totale"), ValueKind.AMOUNT),
        Label("Imponibile", listOf("imponibile", "subtotale"), ValueKind.AMOUNT),
        Label("IVA", listOf("iva", "imposta"), ValueKind.AMOUNT),
        Label("Numero documento", listOf("fattura n", "fattura numero", "documento n", "ricevuta n", "numero documento", "n. fattura"), ValueKind.DOC_NUMBER),
        Label("Data emissione", listOf("data emissione", "data documento", "data fattura", "data"), ValueKind.DATE),
        Label("Scadenza", listOf("scadenza", "pagamento entro", "da pagare entro"), ValueKind.DATE),
        Label("Partita IVA", listOf("partita iva", "p. iva", "p.iva", "piva"), ValueKind.VAT),
        Label("IBAN", listOf("iban", "coordinate bancarie"), ValueKind.IBAN_VALUE),
    )

    // ----------------------------------------------------------- Entry point

    /**
     * Analizza le righe riconosciute e restituisce i campi trovati.
     *
     * Ordine di lavoro: prima si cerca un documento d'identità, perché codice
     * fiscale e MRZ si verificano da soli e non lasciano dubbi. Se non c'è, si
     * guarda se la pagina sembra commerciale; se non lo è, non si estrae nulla.
     * Meglio nessun campo che un campo inventato.
     *
     * @param lines le righe della pagina nell'ordine in cui compaiono
     * @return campi, tipo di documento e testo già ripulito dai numeri di carta
     */
    fun extract(lines: List<String>): Result {
        val clean = lines.map { it.trim() }.filter { it.isNotBlank() }
        val joined = clean.joinToString("\n")
        val safeText = redactPaymentNumbers(joined)

        identityFields(joined)?.let { fields ->
            return Result(
                fields = stripPaymentData(fields),
                kind = DocKind.FORM,
                suggestedTitle = identityTitle(joined),
                docType = DocType.IDENTITY,
                searchableText = safeText,
            )
        }

        if (!looksCommercial(clean)) {
            return Result(
                fields = emptyList(),
                kind = DocKind.FORM,
                suggestedTitle = genericTitle(clean),
                docType = DocType.GENERIC,
                searchableText = safeText,
            )
        }

        val fields = stripPaymentData(
            commercialFields(clean, joined)
                .filter { it.confidence >= CONFIDENCE_FLOOR }
                .distinctBy { it.label },
        )

        val kind = if (fields.none { it.label == "Partita IVA" } && fields.size <= 3)
            DocKind.RECEIPT else DocKind.FORM

        return Result(
            fields = fields,
            kind = kind,
            suggestedTitle = commercialTitle(clean, fields),
            docType = DocType.COMMERCIAL,
            searchableText = safeText,
        )
    }

    // ------------------------------------------------------------ Identità

    /**
     * Campi di un documento d'identità, oppure null se la pagina non ne contiene.
     *
     * La confidenza è piena solo per i campi il cui checksum torna. Gli altri
     * escono intorno al 55-60%: abbastanza da mostrarli, non da darli per buoni.
     */
    private fun identityFields(text: String): List<ExtractedField>? {
        val cf = ItalianDocumentParser.findCodiceFiscale(text)
        val mrz = ItalianDocumentParser.findMrz(text)
        if (cf == null && mrz == null) return null

        val fields = mutableListOf<ExtractedField>()
        cf?.let {
            fields += ExtractedField("Codice fiscale", it.value, if (it.checksumValid) 1f else 0.55f)
        }
        mrz?.let { m ->
            val c = if (m.allChecksumsValid) 1f else 0.6f
            if (m.surname.isNotBlank()) fields += ExtractedField("Cognome", m.surname, c)
            if (m.givenNames.isNotBlank()) fields += ExtractedField("Nome", m.givenNames, c)
            fields += ExtractedField(
                "Numero documento", m.documentNumber.value,
                if (m.documentNumber.checksumValid) 1f else 0.58f,
            )
            fields += ExtractedField(
                "Data di nascita", ItalianDocumentParser.formatMrzDate(m.birthDate.value),
                if (m.birthDate.checksumValid) 1f else 0.58f,
            )
            fields += ExtractedField(
                "Scadenza", ItalianDocumentParser.formatMrzDate(m.expiryDate.value),
                if (m.expiryDate.checksumValid) 1f else 0.58f,
            )
            if (m.nationality.isNotBlank()) fields += ExtractedField("Cittadinanza", m.nationality, c)
        }
        return fields
    }

    // ---------------------------------------------------------- Commerciale

    /** Servono almeno due segnali distinti, per non scambiare una lista della spesa. */
    private fun looksCommercial(lines: List<String>): Boolean {
        val lower = lines.joinToString(" ").lowercase()
        val hits = COMMERCIAL_HINTS.count { lower.contains(it) }
        return hits >= 2
    }

    /**
     * Campi di una fattura o ricevuta, cercati riga per riga.
     *
     * Ogni riga viene confrontata con le etichette note; il valore si prende da
     * quello che segue l'etichetta, o dalla riga sotto se lì non c'è nulla,
     * perché nei layout a due colonne il valore scende.
     *
     * Alla fine due reti di sicurezza: se nessuna riga diceva "totale" si
     * propone l'importo più alto a bassa confidenza, e un IBAN valido viene
     * accettato anche senza etichetta, perché si verifica da solo.
     */
    private fun commercialFields(lines: List<String>, joined: String): List<ExtractedField> {
        val found = mutableListOf<ExtractedField>()
        val usedLabels = mutableSetOf<String>()

        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()
            for (label in LABELS) {
                if (label.name in usedLabels) continue
                val key = label.keys.firstOrNull { lower.contains(it) } ?: continue

                // Il valore sta dopo l'etichetta sulla stessa riga; se lì non
                // c'è, guardiamo la riga successiva (layout a due colonne).
                val after = line.substring(
                    (lower.indexOf(key) + key.length).coerceAtMost(line.length),
                )
                val value = valueFrom(after, label.kind)
                    ?: lines.getOrNull(index + 1)?.let { valueFrom(it, label.kind) }
                    ?: continue

                found += ExtractedField(label.name, value.text, value.confidence)
                usedLabels += label.name
            }
        }

        // Rete di sicurezza: documento commerciale senza riga "Totale"
        // riconosciuta, ma con importi in pagina. L'importo più alto è quasi
        // sempre il totale — quasi, quindi confidenza bassa e dichiarata.
        if ("Totale" !in usedLabels) {
            val amounts = AMOUNT.findAll(joined).map { it.value }.toList()
            amounts.maxByOrNull { parseAmount(it) }?.let {
                found += ExtractedField("Totale", "€ $it", 0.55f)
            }
        }
        // Un IBAN valido è autoverificante: vale anche senza etichetta.
        if ("IBAN" !in usedLabels) {
            IBAN.find(joined)?.value?.replace(" ", "")?.let { iban ->
                if (isIbanValid(iban)) found += ExtractedField("IBAN", formatIban(iban), 1f)
            }
        }
        return found
    }

    private data class Value(val text: String, val confidence: Float)

    /**
     * Estrae il valore dal pezzo di riga che segue l'etichetta.
     *
     * Per gli importi prende l'ultimo della riga: su "Imponibile 2.032,79 IVA
     * 447,21" il numero che conta è quello a destra.
     */
    private fun valueFrom(fragment: String, kind: ValueKind): Value? = when (kind) {
        ValueKind.AMOUNT -> AMOUNT.findAll(fragment).lastOrNull()?.value
            ?.let { Value("€ $it", 0.85f) }

        ValueKind.DATE -> DATE.find(fragment)?.let { m ->
            normalizeDate(m)?.let { Value(it, 0.85f) }
        }

        ValueKind.DOC_NUMBER -> {
            val stripped = fragment.trimStart(':', '.', '°', 'n', 'N', ' ', '\u00B0')
            DOC_NUMBER.find(stripped)?.value?.let { Value(it, 0.8f) }
        }

        ValueKind.VAT -> VAT_NUMBER.find(fragment)?.value?.let {
            Value(it, if (isPartitaIvaValid(it)) 1f else 0.45f)
        }

        ValueKind.IBAN_VALUE -> IBAN.find(fragment)?.value?.replace(" ", "")?.let {
            Value(formatIban(it), if (isIbanValid(it)) 1f else 0.5f)
        }
    }

    // ----------------------------------------------- Dati di pagamento

    /**
     * Sequenze di 13-19 cifre: la forma di un numero di carta (PAN).
     * Ammette spazi e trattini, come sono stampati sulla plastica.
     */
    private val PAN_CANDIDATE = Regex("""\b\d(?:[ -]?\d){12,18}\b""")

    /**
     * Verifica di Luhn. Serve a distinguere un vero PAN da una qualsiasi lunga
     * sequenza di cifre: senza di essa mascheremmo anche numeri di protocollo e
     * codici a barre, rendendo il testo inutilizzabile.
     */
    fun isLuhnValid(digits: String): Boolean {
        val d = digits.filter { it.isDigit() }
        if (d.length !in 13..19) return false
        var sum = 0
        var alternate = false
        for (i in d.length - 1 downTo 0) {
            var n = d[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    /**
     * Maschera i numeri di carta lasciando le ultime quattro cifre, che servono
     * a riconoscere la carta e da sole non consentono nulla. Il numero completo
     * non deve finire in un archivio.
     */
    fun redactPaymentNumbers(text: String): String =
        PAN_CANDIDATE.replace(text) { m ->
            if (isLuhnValid(m.value)) {
                val last4 = m.value.filter { it.isDigit() }.takeLast(4)
                "•••• •••• •••• $last4"
            } else {
                m.value
            }
        }

    /** Nessun campo estratto può contenere un PAN valido. */
    private fun stripPaymentData(fields: List<ExtractedField>): List<ExtractedField> =
        fields.filterNot { field ->
            PAN_CANDIDATE.findAll(field.value).any { isLuhnValid(it.value) }
        }

    // ------------------------------------------------------------ Verifiche

    /**
     * Cifra di controllo della partita IVA italiana (variante di Luhn).
     * Trasforma un'ipotesi in un dato certo, o la scarta: undici cifre
     * qualsiasi sono facilissime da trovare per sbaglio in una pagina.
     */
    fun isPartitaIvaValid(piva: String): Boolean {
        if (piva.length != 11 || !piva.all { it.isDigit() }) return false
        var sum = 0
        for (i in 0 until 10) {
            val d = piva[i] - '0'
            sum += if (i % 2 == 0) d else (d * 2).let { if (it > 9) it - 9 else it }
        }
        return piva[10] - '0' == (10 - sum % 10) % 10
    }

    /** Verifica mod-97 dell'IBAN. */
    fun isIbanValid(iban: String): Boolean {
        if (iban.length !in 15..34) return false
        val rearranged = iban.substring(4) + iban.substring(0, 4)
        var remainder = 0
        for (c in rearranged) {
            val chunk = when {
                c.isDigit() -> (c - '0').toString()
                c.isLetter() -> (c.uppercaseChar() - 'A' + 10).toString()
                else -> return false
            }
            for (d in chunk) remainder = (remainder * 10 + (d - '0')) % 97
        }
        return remainder == 1
    }

    // -------------------------------------------------------------- Utilità

    /** Converte un importo in formato italiano (2.480,00) in numero. Zero se non ci riesce. */
    private fun parseAmount(s: String): Double =
        s.replace(".", "").replace(" ", "").replace("\u00A0", "")
            .replace(",", ".").toDoubleOrNull() ?: 0.0

    /** Divide l'IBAN in gruppi di quattro caratteri. */
    private fun formatIban(iban: String): String = iban.uppercase().chunked(4).joinToString(" ")

    /** Scarta date impossibili: l'OCR confonde volentieri le cifre. */
    private fun normalizeDate(m: MatchResult): String? {
        val d = m.groupValues[1].toIntOrNull() ?: return null
        val mo = m.groupValues[2].toIntOrNull() ?: return null
        val yRaw = m.groupValues[3]
        val y = (if (yRaw.length == 2) "20$yRaw" else yRaw).toIntOrNull() ?: return null
        if (d !in 1..31 || mo !in 1..12 || y !in 1900..2100) return null
        return "%02d/%02d/%04d".format(d, mo, y)
    }

    // ---------------------------------------------------------------- Titoli

    /** Titolo proposto per un documento d'identità. */
    private fun identityTitle(text: String): String {
        val mrz = ItalianDocumentParser.findMrz(text)
        if (mrz != null && mrz.surname.isNotBlank()) {
            val cognome = mrz.surname.lowercase().replaceFirstChar { it.uppercase() }
            val iniziale = mrz.givenNames.firstOrNull()?.let { "$it. " } ?: ""
            return "Documento · $iniziale$cognome"
        }
        return "Documento d'identità"
    }

    /**
     * Titolo proposto per una fattura: emittente e numero documento.
     *
     * L'emittente si cerca nelle prime tre righe, dove sta l'intestazione: è il
     * nome che l'utente cercherà nell'elenco, più del numero di protocollo.
     */
    private fun commercialTitle(lines: List<String>, fields: List<ExtractedField>): String {
        val numero = fields.firstOrNull { it.label == "Numero documento" }?.value
        // L'intestazione è quasi sempre nelle prime righe: è lì che sta il nome
        // dell'emittente, che è ciò che l'utente cerca nell'elenco.
        val emittente = lines.take(3).firstOrNull { it.length in 4..40 && it.any { c -> c.isLetter() } }
        return listOfNotNull(emittente, numero).joinToString(" · ").ifBlank { "Documento" }
    }

    /**
     * Titolo proposto per una pagina qualsiasi: la prima riga di lunghezza
     * ragionevole che contenga almeno tre lettere. Salta numeri di pagina e
     * intestazioni fatte di soli simboli.
     */
    private fun genericTitle(lines: List<String>): String =
        lines.firstOrNull { it.length in 4..48 && it.count { c -> c.isLetter() } >= 3 }
            ?.replaceFirstChar { it.uppercase() }
            ?: "Scansione"
}
