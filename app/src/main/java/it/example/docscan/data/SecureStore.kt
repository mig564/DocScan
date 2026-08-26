package it.example.docscan.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Archivio locale cifrato: AES-256-GCM con chiave non esportabile nell'Android
 * Keystore, su hardware dedicato dove il dispositivo lo supporta.
 *
 * Niente androidx.security:security-crypto: fermo da anni, la 1.1 non è mai
 * uscita da alpha, e qui bastano una cinquantina di righe.
 *
 * I file stanno in filesDir: nessun permesso, privati all'app, rimossi con la
 * disinstallazione.
 */
class SecureStore(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "docscan_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
        private const val DIR = "archivio"
    }

    private val dir: File
        get() = File(context.filesDir, DIR).apply { mkdirs() }

    /**
     * Chiave AES-256 dell'archivio, risolta una volta sola per processo.
     *
     * Interrogare l'Android Keystore costa: è un servizio di sistema, e la
     * chiamata attraversa Binder. Farlo a ogni file significava pagarlo una
     * volta per documento a ogni apertura dell'archivio, e con qualche centinaio
     * di documenti si sente. La chiave non lascia comunque il Keystore: quello
     * che teniamo qui è un riferimento opaco, non il materiale crittografico.
     */
    private val key: SecretKey by lazy { getOrCreateKey() }

    /**
     * Chiave AES-256 dell'archivio: la prende dal Keystore o la genera al primo uso.
     *
     * La chiave non esce mai dal Keystore. Si cifra e decifra passandogli i dati,
     * non recuperandola, quindi un archivio copiato su un altro telefono resta
     * illeggibile. È il comportamento voluto, non un limite.
     */
    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Per alzare il livello, .setUserAuthenticationRequired(true)
            // obbliga all'autenticazione biometrica prima di ogni decifratura.
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    /**
     * Cifra [plain] e lo scrive in [fileName].
     *
     * Su disco finisce l'IV di 12 byte, poi il testo cifrato con il tag GCM in
     * coda. L'IV lo genera il Cipher a ogni chiamata: riusarlo con la stessa
     * chiave manderebbe all'aria le garanzie di GCM.
     *
     * @return il file scritto, dentro filesDir/archivio
     */
    fun write(fileName: String, plain: ByteArray): File {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val out = File(dir, fileName)
        out.outputStream().use { stream ->
            stream.write(cipher.iv)
            stream.write(cipher.doFinal(plain))
        }
        return out
    }

    /**
     * Legge [fileName] e lo decifra.
     *
     * @throws IllegalArgumentException se il file è più corto dell'IV, quindi troncato
     * @throws javax.crypto.AEADBadTagException se il contenuto è stato alterato:
     *         GCM se ne accorge, e a quel punto non c'è nulla da recuperare
     */
    fun read(fileName: String): ByteArray {
        val bytes = File(dir, fileName).readBytes()
        require(bytes.size > IV_SIZE) { "File cifrato corrotto o troncato: $fileName" }
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val payload = bytes.copyOfRange(IV_SIZE, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(payload)
    }

    /** Vero se [fileName] esiste. Non dice nulla sul contenuto: anche un file corrotto esiste. */
    fun exists(fileName: String): Boolean = File(dir, fileName).exists()

    /** Nomi dei file nell'archivio, in ordine alfabetico. */
    fun list(): List<String> = dir.listFiles()?.map { it.name }?.sorted() ?: emptyList()

    /** Cancella [fileName]. @return false se non c'era. */
    fun delete(fileName: String): Boolean = File(dir, fileName).delete()

}