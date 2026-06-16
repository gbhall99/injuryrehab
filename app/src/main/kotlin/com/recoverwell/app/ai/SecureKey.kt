package com.recoverwell.app.ai

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the Groq API key at rest using a hardware-backed AndroidKeyStore key,
 * so it isn't sitting in the database as plaintext.
 *
 * Both directions fail safe: if the Keystore is unavailable, [protect] stores
 * plaintext (marked by the absence of the prefix) and [reveal] passes legacy
 * plaintext through unchanged - so a Keystore quirk can never lock the user out
 * of re-entering or using their key.
 */
object SecureKey {

    private const val ALIAS = "recoverwell_ai_key"
    private const val PREFIX = "enc1:"

    private fun keystore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun secret(): SecretKey {
        val ks = keystore()
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return kg.generateKey()
    }

    /** Encrypt a key for storage; falls back to plaintext if the Keystore fails. */
    fun protect(plain: String): String {
        if (plain.isBlank()) return ""
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, secret())
            val iv = Base64.encodeToString(c.iv, Base64.NO_WRAP)
            val ct = Base64.encodeToString(c.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            "$PREFIX$iv:$ct"
        } catch (e: Exception) {
            plain
        }
    }

    /**
     * True when [stored] is an encrypted value that can no longer be decrypted
     * (e.g. the Keystore key was invalidated by a lock-screen change or a restore
     * to a new device) - distinct from "no key set", so the UI can ask the user to
     * re-enter rather than silently showing "no key".
     */
    fun isUnreadable(stored: String): Boolean =
        stored.startsWith(PREFIX) && reveal(stored).isBlank()

    /** Decrypt a stored value; passes legacy plaintext through unchanged. */
    fun reveal(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        return try {
            val parts = stored.removePrefix(PREFIX).split(":")
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ct = Base64.decode(parts[1], Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, iv))
            String(c.doFinal(ct), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
