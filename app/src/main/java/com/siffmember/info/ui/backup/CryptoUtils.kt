package com.siffmember.info.ui.backup

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_NAME = "backup_aes_key" // base64 encoded
    private const val AES_KEY_SIZE = 32 // 256-bit
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val IV_LENGTH = 12 // bytes recommended for GCM

    // Get or create AES key stored (encrypted) in EncryptedSharedPreferences
    fun getAesKey(context: Context): SecretKey {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var base64 = prefs.getString(KEY_NAME, null)
        if (base64 == null) {
            val rnd = SecureRandom()
            val key = ByteArray(AES_KEY_SIZE)
            rnd.nextBytes(key)
            base64 = Base64.encodeToString(key, Base64.NO_WRAP)
            prefs.edit().putString(KEY_NAME, base64).apply()
        }
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    // Encrypt file -> outFile (ciphertext format: IV (12 bytes) + ciphertext)
    fun encryptFile(context: Context, input: File, outFile: File) {
        val key = getAesKey(context)
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        FileOutputStream(outFile).use { fos ->
            // write IV first
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(input).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    // Decrypt file -> outFile. Expects first bytes to be IV_LENGTH bytes of IV.
    fun decryptFile(context: Context, input: File, outFile: File) {
        val key = getAesKey(context)
        FileInputStream(input).use { fis ->
            val iv = ByteArray(IV_LENGTH)
            val read = fis.read(iv)
            if (read != IV_LENGTH) throw IllegalArgumentException("Invalid input file")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
        }
    }
}
