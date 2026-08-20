package it.example.docscan.ocr

/**
 * Estrazione dai documenti d'identità italiani.
 *
 * Regola: mai fidarsi dell'OCR senza una verifica matematica. Codice fiscale e
 * MRZ hanno caratteri di controllo che eliminano quasi tutti i falsi positivi
 * dovuti a glifi simili (O/0, I/1, S/5, B/8).
 *
 * Un campo che non passa il checksum non viene scartato: esce a bassa
 * confidenza, così l'utente può correggerlo.
 *
 * Gli algoritmi sono verificati contro valori di riferimento pubblicati: il
 * codice fiscale RSSMRA85T10A562S e le cifre di controllo ICAO 9303.
 */
object ItalianDocumentParser {

    data class Field(val value: String, val checksumValid: Boolean)

    data class Mrz(
        val documentNumber: Field,
        val birthDate: Field,
        val expiryDate: Field,
        val sex: String,
        val nationality: String,
        val surname: String,
        val givenNames: String,
        val allChecksumsValid: Boolean,
    )

    // ------------------------------------------------------ Codice fiscale

    private val CF_REGEX = Regex("[A-Z]{6}\\d{2}[ABCDEHLMPRST]\\d{2}[A-Z]\\d{3}[A-Z]")

    /**
     * Cerca un codice fiscale nel testo.
     *
     * Se ne trova più di uno preferisce quello che supera il carattere di
     * controllo: su una tessera sanitaria il codice compare più volte, e uno
     * solo è letto bene. Se nessuno lo supera restituisce il primo, marcato come
     * non verificato, perché un candidato da correggere è più utile di niente.
     */
    fun findCodiceFiscale(text: String): Field? {
        val flat = text.uppercase().replace(" ", "")
        val candidates = CF_REGEX.findAll(flat).map { it.value }.distinct().toList()
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { isCodiceFiscaleValid(it) }?.let { Field(it, true) }
            ?: Field(candidates.first(), false)
    }

    /** Tabella per le posizioni dispari (contate da 1) dell'algoritmo del codice fiscale. */
    private val ODD = mapOf(
        '0' to 1, '1' to 0, '2' to 5, '3' to 7, '4' to 9,
        '5' to 13, '6' to 15, '7' to 17, '8' to 19, '9' to 21,
        'A' to 1, 'B' to 0, 'C' to 5, 'D' to 7, 'E' to 9,
        'F' to 13, 'G' to 15, 'H' to 17, 'I' to 19, 'J' to 21,
        'K' to 2, 'L' to 4, 'M' to 18, 'N' to 20, 'O' to 11,
        'P' to 3, 'Q' to 6, 'R' to 8, 'S' to 12, 'T' to 14,
        'U' to 16, 'V' to 10, 'W' to 22, 'X' to 25, 'Y' to 24, 'Z' to 23,
    )

    /** Valore di un carattere nelle posizioni pari: cifre come sono, lettere da A=0. */
    private fun evenValue(c: Char): Int = if (c.isDigit()) c - '0' else c - 'A'

    /**
     * Controlla formato e carattere finale di controllo.
     *
     * Somma i quindici caratteri con due tabelle diverse a seconda della
     * posizione, poi il resto della divisione per 26 dà la lettera attesa. Un
     * codice inventato ha una probabilità su ventisei di passare per caso, ed è
     * il motivo per cui questa verifica da sola non basta a dire che il codice
     * esiste: dice solo che non è stato letto male.
     */
    fun isCodiceFiscaleValid(cf: String): Boolean {
        if (cf.length != 16 || !CF_REGEX.matches(cf)) return false
        var sum = 0
        for (i in 0 until 15) {
            val c = cf[i]
            // i parte da 0, quindi le posizioni "dispari" dell'algoritmo sono
            // gli indici pari
            sum += if (i % 2 == 0) (ODD[c] ?: return false) else evenValue(c)
        }
        return cf[15] == 'A' + (sum % 26)
    }

    // ------------------------------------------------------------------- MRZ

    /**
     * Ripulisce una riga MRZ.
     *
     * Il riempitivo `<` viene letto spesso come `«`, `≤` o `‹`, e vanno
     * riportati tutti alla forma giusta prima di contare le posizioni. Tutto ciò
     * che non è lettera, cifra o riempitivo viene scartato.
     */
    private fun normalizeMrzLine(line: String): String =
        line.uppercase()
            .replace(" ", "")
            .map { c -> if (c == '«' || c == '≤' || c == '‹') '<' else c }
            .joinToString("")
            .filter { it.isLetterOrDigit() || it == '<' }

    /**
     * La CIE usa il formato TD1 (3 righe da 30 caratteri), il passaporto TD3
     * (2 righe da 44). L'OCR sbaglia spesso la lunghezza, quindi si tollera uno
     * scarto e si completa con padding prima di leggere.
     */
    fun findMrz(text: String): Mrz? {
        val lines = text.lines()
            .map { normalizeMrzLine(it) }
            .filter { it.length >= 28 && it.count { ch -> ch == '<' } >= 2 }

        for (i in 0..lines.size - 3) {
            val block = lines.subList(i, i + 3)
            if (block.all { it.length in 28..32 }) {
                return runCatching { parseTd1(block.map { it.padEnd(30, '<').take(30) }) }.getOrNull()
            }
        }
        for (i in 0..lines.size - 2) {
            val block = lines.subList(i, i + 2)
            if (block.all { it.length in 42..46 }) {
                return runCatching { parseTd3(block.map { it.padEnd(44, '<').take(44) }) }.getOrNull()
            }
        }
        return null
    }

    /**
     * Legge una MRZ in formato TD1, tre righe da trenta caratteri: la carta
     * d'identità elettronica.
     *
     * I campi si leggono per posizione, non per separatore. Numero documento,
     * data di nascita e scadenza hanno ciascuno la propria cifra di controllo,
     * verificata singolarmente: così si sa quale campo è stato letto male, non
     * solo che qualcosa non torna.
     */
    private fun parseTd1(l: List<String>): Mrz {
        val docRaw = l[0].substring(5, 14)
        val birth = l[1].substring(0, 6)
        val expiry = l[1].substring(8, 14)
        val names = l[2].split("<<", limit = 2)

        val docOk = checkDigit(docRaw) == l[0][14]
        val birthOk = checkDigit(birth) == l[1][6]
        val expiryOk = checkDigit(expiry) == l[1][14]

        return Mrz(
            documentNumber = Field(docRaw.trimEnd('<'), docOk),
            birthDate = Field(birth, birthOk),
            expiryDate = Field(expiry, expiryOk),
            sex = l[1][7].toString(),
            nationality = l[1].substring(15, 18),
            surname = names.getOrElse(0) { "" }.replace('<', ' ').trim(),
            givenNames = names.getOrElse(1) { "" }.replace('<', ' ').trim(),
            allChecksumsValid = docOk && birthOk && expiryOk,
        )
    }

    /**
     * Legge una MRZ in formato TD3, due righe da quarantaquattro caratteri: il
     * passaporto. Stesse verifiche del TD1, posizioni diverse.
     */
    private fun parseTd3(l: List<String>): Mrz {
        val names = l[0].substring(5).split("<<", limit = 2)
        val docRaw = l[1].substring(0, 9)
        val birth = l[1].substring(13, 19)
        val expiry = l[1].substring(21, 27)

        val docOk = checkDigit(docRaw) == l[1][9]
        val birthOk = checkDigit(birth) == l[1][19]
        val expiryOk = checkDigit(expiry) == l[1][27]

        return Mrz(
            documentNumber = Field(docRaw.trimEnd('<'), docOk),
            birthDate = Field(birth, birthOk),
            expiryDate = Field(expiry, expiryOk),
            sex = l[1][20].toString(),
            nationality = l[1].substring(10, 13),
            surname = names.getOrElse(0) { "" }.replace('<', ' ').trim(),
            givenNames = names.getOrElse(1) { "" }.replace('<', ' ').trim(),
            allChecksumsValid = docOk && birthOk && expiryOk,
        )
    }

    /** ICAO 9303 check digit: repeating weights of 7, 3 and 1. */
    fun checkDigit(field: String): Char {
        val weights = intArrayOf(7, 3, 1)
        var sum = 0
        field.forEachIndexed { i, c ->
            val v = when {
                c.isDigit() -> c - '0'
                c in 'A'..'Z' -> c - 'A' + 10
                else -> 0
            }
            sum += v * weights[i % 3]
        }
        return '0' + (sum % 10)
    }

    /** MRZ YYMMDD to a readable date. Years 00-30 map to 2000s, the rest to 1900s. */
    fun formatMrzDate(yymmdd: String): String {
        if (yymmdd.length != 6 || !yymmdd.all { it.isDigit() }) return yymmdd
        val yy = yymmdd.substring(0, 2).toInt()
        val year = if (yy <= 30) 2000 + yy else 1900 + yy
        return "${yymmdd.substring(4, 6)}/${yymmdd.substring(2, 4)}/$year"
    }
}
